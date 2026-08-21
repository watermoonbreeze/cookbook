package com.sxdbsm.cookbook.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.domain.projection.MealDayCardProjector
import com.sxdbsm.cookbook.util.DateTime
import com.sxdbsm.cookbook.platform.MealDataTraceLogger
import kotlinx.datetime.LocalDate
import com.sxdbsm.cookbook.domain.FoodGroup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.isoDayNumber
import kotlin.math.roundToInt

/** 首页"下一餐"卡展示菜数(一餐一荤一素一汤/主食的家庭直觉)。[AI生成] 阶段2 */
private const val HOME_DISH_COUNT = 1 // [AI修改] 首页卡 v2：轻量单菜引流(原3道)——只推一道+一句人话·点击进AI全页看整桌

/**
 * 首页 UI 状态。[AI修改]
 *
 * Compose 页面只读取这个不可变对象；状态变化时用 copy 生成新对象。
 */
data class HomeUiState(
    // [AI修改] 用户 2026-07-16：首页去"热门/最近"发现区 → 移除 popular/recent，不再观察这两路 DB(省查询)。
    val plans: List<DayMealCardData> = emptyList(),
)

/** 今日卡成员切换 chip 一项。[AI生成] 多人关注 */
data class FocusChip(val id: Long, val name: String)

/** 今日卡成员切换器状态：关注集合 + 当前查看人。members.size<2 时今日卡不显切换器(1人零变化)。[AI生成] 多人关注 */
data class FocusSwitcher(
    val members: List<FocusChip> = emptyList(),
    val viewingId: Long? = null,
)

/** 首页"下一餐"推荐卡状态。[AI修改] v2：轻量单菜引流(单菜 dish?·非列表) */
data class NextMealUi(
    val slotLabel: String = "", // 下一餐餐次(早餐/中餐/晚餐/明天·早餐)
    val dish: NextDishUi? = null, // [AI修改] v2：单菜引流——只推一道(空/加载/空库时 null)
    val isSample: Boolean = false, // 无库存/新用户→随机兜底,标"示例·记录后更懂你"(诚实告知)
    val canShuffle: Boolean = false, // 候选>1 才可"换一道"
    val loading: Boolean = false,
)

/** 首页推荐卡单菜。[AI修改] v2：加 emoji 视觉锚点(荤/主食/素·VM 按 isMeat/isStaple 映射·无新数据)
 * [AI修改] 修"推荐图片与菜不符":带该菜真实封面/缩略图,有图优先显真图、无图才回退 emoji 锚点。 */
data class NextDishUi(
    val id: Long,
    val name: String,
    val note: String,
    val emoji: String = "🥗",
    val imagePath: String = "", // [AI生成] 该菜封面(空=无图,回退 emoji)
    val thumbnailPath: String = "", // [AI生成] 该菜缩略图(优先显,免解码大图)
)

/**
 * 首页 ViewModel。[AI修改]
 *
 * 负责把热门菜品、最近菜品、今天/未来计划三路数据合并成一个 StateFlow。
 */
class HomeViewModel(
    private val dishRepo: DishRepository,
    private val mealRepo: MealRecordRepository,
    private val prefs: PreferenceRepository,
    private val nutritionRepo: com.sxdbsm.cookbook.data.repository.NutritionRepository, // [AI生成] 2c：色系墙评级结合当天热量达标度
    private val family: com.sxdbsm.cookbook.data.repository.FamilyRepository, // [AI生成] 达标/摄入按主要关注成员(身体数据+饭量系数份额)。
    private val ingredientRepo: com.sxdbsm.cookbook.data.repository.IngredientRepository, // [AI生成] A1：食材显式营养大类(food_group)覆盖色系/均衡判定。
    private val health: com.sxdbsm.cookbook.data.repository.HealthProfileRepository, // [AI生成] A-1：解析关注成员病种→慢病提示(今日卡"偏咸·高血压留意")。
    private val recoDataSource: com.sxdbsm.cookbook.ai.RecommendationDataSource, // [AI生成] 阶段2：首页"下一餐"推荐卡(纯规则·不调云端·打开即见)。
) : ViewModel() {

    // ============ 首页"下一餐"推荐卡(阶段2·纯规则不调云端·复用推荐引擎 ruleCandidatesFor) ============
    private val _nextMeal = kotlinx.coroutines.flow.MutableStateFlow(NextMealUi(loading = true))
    /** 首页"下一餐"卡状态。[AI生成] 阶段2 */
    val nextMeal: StateFlow<NextMealUi> = _nextMeal
    private var homeRotation = 0 // 首页"换一道"轮次(本地轮播已算好的候选)。
    // [AI生成] 阶段2:缓存已算好的候选——"换一道"纯本地轮播(不重 gather);loadNextMeal 才重取(忌口/钟点实时)。
    // [AI修改] 审查建议:以下缓存字段依赖 viewModelScope 主线程(Main.immediate)串行读写(init/ON_RESUME/换一道都在主线程)——勿把相关段挪到 Dispatchers.IO,否则这几个非 volatile 字段会踩竞态。
    private var cachedCandidates: List<com.sxdbsm.cookbook.ai.model.DishCandidate> = emptyList()
    private var cachedSlotLabel = ""
    private var cachedSample = false
    // [AI生成] 候选菜 id→(封面,缩略图)：随候选一并预取,"换一道"本地轮播时直接取(不再查库)。修"推荐图片与菜不符"。
    private var cachedThumbs: Map<Long, Pair<String, String>> = emptyMap()

    init { loadNextMeal() } // 首页创建即加载"下一餐"卡(打开即见·纯规则快)。

    /**
     * 加载/刷新首页"下一餐"推荐(按当前钟点判餐次·**纯规则不调云端**)。[AI生成] 阶段2
     *
     * 每次调用**重取候选**(忌口/钟点/库存/今日缺口实时·纯规则很轻);返回首页(ON_RESUME)会调它保证忌口即时(健康档案别页可改)。
     * "换一批"不调它(走本地轮播 shuffleNextMeal)。
     */
    fun loadNextMeal() {
        viewModelScope.launch {
            val (slot, slotLabel) = currentSlot()
            // 库存挂钩开→优先库存推荐;关或库存空→随机(全库·保证新用户/无库存也有"今天可以吃什么")。
            val pantryOn = prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.PANTRY_HOOK_ENABLED, default = true).first()
            var mode = if (pantryOn) com.sxdbsm.cookbook.ai.model.RecommendMode.PANTRY else com.sxdbsm.cookbook.ai.model.RecommendMode.RANDOM
            var acceptable = runCatching { recoDataSource.ruleCandidatesFor(mode, slot) }.getOrDefault(emptyList())
                .filter { it.avoidNames.isEmpty() } // 首页"建议吃"不列忌口菜(忌口标红教育职能在 AI 推荐全页)
            var sample = false
            if (acceptable.isEmpty() && mode == com.sxdbsm.cookbook.ai.model.RecommendMode.PANTRY) {
                // 库存空/无可做 → 随机全库兜底,标"示例"(诚实告知:记录后会更懂你)。
                mode = com.sxdbsm.cookbook.ai.model.RecommendMode.RANDOM
                acceptable = runCatching { recoDataSource.ruleCandidatesFor(mode, slot) }.getOrDefault(emptyList())
                    .filter { it.avoidNames.isEmpty() }
                sample = true
            }
            cachedCandidates = acceptable
            cachedSlotLabel = slotLabel
            cachedSample = sample
            // [AI生成] 预取候选菜真实图(修"推荐图片与菜不符")：候选集小,一次批量查;失败退空(回退 emoji)。
            cachedThumbs = runCatching { dishRepo.dishImagesByIds(acceptable.map { it.id }) }.getOrDefault(emptyMap())
            homeRotation = 0 // 重取后从第一批展示
            publishNextMeal()
        }
    }

    /** 首页"换一道"：**纯本地轮播已算好的候选(不重 gather)**。[AI修改] v2：单菜·候选>1 才可换 */
    fun shuffleNextMeal() {
        if (cachedCandidates.size <= HOME_DISH_COUNT) return // 只有一道·换不动(canShuffle=false 时不该触发)
        homeRotation++
        publishNextMeal()
    }

    /** 用当前 rotation 从缓存候选取一道发布到 UI(不重取)。[AI修改] v2：单菜引流 */
    private fun publishNextMeal() {
        val pick = rotateSlice(cachedCandidates, homeRotation, HOME_DISH_COUNT).firstOrNull()
        _nextMeal.value = NextMealUi(
            slotLabel = cachedSlotLabel,
            dish = pick?.let {
                val img = cachedThumbs[it.id]
                NextDishUi(
                    id = it.id, name = it.name, note = homeNote(it, cachedSample), emoji = emojiFor(it),
                    imagePath = img?.first.orEmpty(), thumbnailPath = img?.second.orEmpty(),
                )
            },
            isSample = cachedSample,
            canShuffle = cachedCandidates.size > HOME_DISH_COUNT, // >1 才可换一道
            loading = false,
        )
    }

    /** 单菜视觉锚点 emoji(荤/主食/素·纯按现有判定映射·无新数据)。[AI生成] v2 */
    private fun emojiFor(c: com.sxdbsm.cookbook.ai.model.DishCandidate): String = when {
        c.isStaple -> "🍚"
        c.isMeat -> "🍖"
        else -> "🥗"
    }

    /**
     * 按当前钟点判餐次 + 疑问式标题用的餐次词。[AI修改] v2
     *
     * 标签改口语餐次词(卡片拼"X吃点什么")：去掉"下一餐·明天·早餐"的排班硬承诺(与已有周期计划歧义)。
     * 20 点后→"明早"(比"明天·早餐"更短更口语)。
     */
    // [AI修改] F3(架构审):钟点提为参数(默认取内部时钟·生产不变)——4 个边界分支(4/10/14/20)可测,守"派生别依赖内部时钟"红线。
    private fun currentSlot(hour: Int = DateTime.currentHour()): Pair<com.sxdbsm.cookbook.ai.MealSlot, String> = when (hour) {
        in 4..9 -> com.sxdbsm.cookbook.ai.MealSlot.BREAKFAST to "早餐"
        in 10..13 -> com.sxdbsm.cookbook.ai.MealSlot.LUNCH to "午餐" // [AI修改] 术语统一(早/午/晚/加餐)·去"中餐vs西餐"歧义
        in 14..19 -> com.sxdbsm.cookbook.ai.MealSlot.DINNER to "晚餐"
        else -> com.sxdbsm.cookbook.ai.MealSlot.BREAKFAST to "明早" // 20:00–03:59
    }

    /**
     * 首页单菜"一句人话理由"。[AI修改] v2：只讲一件事·无 emoji·像家里人开口。
     *
     * 优先级(命中即返回·据 apple_ux_designer 规范 §B)：在手食材 > 补今日缺口 > 你常做 > 宜吃 > 主料兜底。
     * 示例态(无库存/新用户随机兜底)改邀请式、不套上表(诚实告知这是示例)。
     * 健康红线：recommendHits 分支只说"对身体友好"，禁"降/治/达标"等医疗断言。
     */
    private fun homeNote(c: com.sxdbsm.cookbook.ai.model.DishCandidate, sample: Boolean): String {
        if (sample) return "记一顿后，会更懂你想吃什么" // 示例态：诚实告知、邀请式
        return when {
            c.onHandNames.isNotEmpty() -> "用你手头的${c.onHandNames.first()}，顺手就能做"
            c.complementary -> "今天正好缺这口，补得上"
            c.frequent -> "你家常做的，顺手一顿"
            c.recommendHits.isNotEmpty() -> "有${c.recommendHits.first()}，家里人都合适" // [AI修改] 守健康红线：宜吃食材是"人群适配"、不替整道菜下健康断言(原"对身体友好"偏背书)
            c.mainNames.isNotEmpty() -> "主料是${c.mainNames.first()}，清爽好搭"
            else -> ""
        }
    }

    /** 轮播取 n 个(不足 n 全给·超过按 rotation 滚动窗口·环绕)。[AI生成] 阶段2 */
    private fun rotateSlice(list: List<com.sxdbsm.cookbook.ai.model.DishCandidate>, rot: Int, n: Int): List<com.sxdbsm.cookbook.ai.model.DishCandidate> {
        if (list.size <= n) return list
        val start = (rot * n) % list.size
        return (list + list).drop(start).take(n)
    }

    /**
     * 首页可观察状态。[AI修改]
     *
     * `combine` 类似把多个 Observable 合并；任意一路变化都会重新生成 HomeUiState。
     */
    private val mealReferenceDate = DateTime.today()

    val uiState: StateFlow<HomeUiState> =
        mealRepo.observeUpcomingMealDayContents(mealReferenceDate)
            .map { contents ->
                HomeUiState(plans = contents.map { MealDayCardProjector.project(it, mealReferenceDate) })
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /**
     * 当前主题模式。[AI生成]
     *
     * 首页主题按钮直接弹框时需要展示当前选中项，数据仍由偏好仓库持久化。
     */
    val themeMode: StateFlow<ThemeMode> = prefs.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) } // [AI生成] 用户选择后立即写入偏好，MainActivity 会自动响应重组。
    }

    /** 删除指定日期的全部餐食（首页计划 Flow 会自动刷新）。[AI生成] */
    fun deleteDay(date: LocalDate) {
        // [AI修改] 删除失败不再全静默(架构评审)：至少落日志，破坏性操作失败可排查。
        viewModelScope.launch {
            runCatching { mealRepo.deleteDayMeals(date) }
                .onFailure { com.sxdbsm.cookbook.android.util.AppLogger.e("HomeVM", "deleteDay 失败: date=$date", it) }
        }
    }

    /** 删整天并支持撤销(§9.12)：先快照→删→回调给 showUndo(点撤销即 saveDayMeals 还原)。[AI生成] B-5 */
    fun deleteDayUndoable(date: LocalDate, showUndo: (onUndo: () -> Unit) -> Unit) {
        viewModelScope.launch {
            // [AI修改] 代码审查#3：快照读失败/为空(异常吞成空)不该照删致无法撤销——先确保拿到快照再删。
            val snapshot = runCatching { mealRepo.snapshotDay(date) }.getOrNull()
            if (snapshot.isNullOrEmpty()) {
                com.sxdbsm.cookbook.android.util.AppLogger.e("HomeVM", "deleteDay 取消: 快照为空/读失败 date=$date")
                return@launch
            }
            runCatching { mealRepo.deleteDayMeals(date) }
                .onSuccess {
                    showUndo {
                        viewModelScope.launch {
                            // [AI修改] 代码审查#1：撤销=原样还原,不抬喜爱度(bumpPreference=false)避免污染排序。
                            runCatching { mealRepo.saveDayMeals(date, snapshot, bumpPreference = false) }
                                .onFailure { com.sxdbsm.cookbook.android.util.AppLogger.e("HomeVM", "restoreDay 失败: date=$date", it) }
                        }
                    }
                }
                .onFailure { com.sxdbsm.cookbook.android.util.AppLogger.e("HomeVM", "deleteDay 失败: date=$date", it) }
        }
    }

    private fun mondayOf(d: LocalDate): LocalDate = DateTime.plusDays(d, -(d.dayOfWeek.isoDayNumber - 1))
    private fun sundayOf(d: LocalDate): LocalDate = DateTime.plusDays(d, 7 - d.dayOfWeek.isoDayNumber)

    private val today = mealReferenceDate
    // [AI修改] 色系墙固定为本公历年(1月1日~12月31日)，整周对齐；UI 定位到今天、可左右滑看本年历史/未来。
    private val wallStart = mondayOf(LocalDate(today.year, 1, 1))
    private val wallEnd = sundayOf(LocalDate(today.year, 12, 31))

    // [AI生成] A1：食材名→显式营养大类(food_group)，覆盖名字无关键词的自定义食材归类。一次加载(编辑后下次进页面刷新)。
    private val explicitGroups: StateFlow<Map<String, FoodGroup.Group>> =
        kotlinx.coroutines.flow.flow { emit(FoodGroup.explicitFrom(ingredientRepo.foodGroupByName())) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * 营养色系墙：本公历年每天的营养均衡级别(整周对齐、含空日)。[AI修改]
     *
     * 固定 1~12 月；UI 默认定位今天所在周、可左右滑。仅功能设置开启营养色系时渲染。
     */
    val nutritionWall: StateFlow<List<DayNutrition>> =
        combine(mealRepo.observeTimelineWindow(wallStart, wallEnd), explicitGroups) { cards, explicit ->
                // [AI修改] 色系墙=全家膳食结构均衡度(蛋白+主食+蔬果齐不齐)，与成员/热量无关；食材显式大类优先于关键词。
                // [AI修改] J15：营养大类判定用"主料优先·空则回退全食材"(修 is_main 全0/缺标的问题菜漏判主食)。
                cards.map { card ->
                    val names = card.meals.flatMap { it.dishes }
                        .flatMap { FoodGroup.classificationNames(it.mainIngredientNames, it.allIngredientNames) }
                    DayNutrition(card.date, FoodGroup.nutritionLevel(FoodGroup.groupsOf(names, explicit)))
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 今日营养概览：当天热量+三大宏量+目标达标。[AI生成] 3c
     *
     * 供首页"今日营养分配"卡；当天无餐食/无营养数据则 null(不显示)。仅营养色系开启时消费。
     */
    // [AI生成] A-1：关注成员的慢病(病种名→HealthCondition)，供今日卡慢病提示(如"偏咸·高血压留意")。
    private val focusConditions: StateFlow<Set<com.sxdbsm.cookbook.domain.HealthCondition>> =
        combine(family.observeViewingMember(), kotlinx.coroutines.flow.flow { emit(health.listAllCrowdTypes()) }) { viewing, crowds ->
            // [AI修改] 多人关注:今日卡慢病提示按"当前查看成员"(指针·非本地firstOrNull),与今日卡数据同一人。
            val nameById = crowds.associate { it.id to it.name }
            viewing?.careCategoryIds.orEmpty()
                .mapNotNull { nameById[it] }
                .flatMap { com.sxdbsm.cookbook.domain.HealthCondition.fromCareName(it) }
                .toSet()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // [AI生成] 多人关注:今日卡顶部成员切换 chip(≥2 关注人才显·1人零变化)。viewingId=当前查看人。
    val focusSwitcher: StateFlow<FocusSwitcher> =
        combine(family.observeMembers(), family.observeViewingMember()) { ms, viewing ->
            FocusSwitcher(
                members = ms.filter { it.isFocus }.map { FocusChip(it.id, it.name) },
                viewingId = viewing?.id,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusSwitcher())

    /** 切"当前查看"成员(今日卡切换 chip·与报告共用指针)。[AI生成] 多人关注 */
    fun setViewing(id: Long) {
        viewModelScope.launch { family.setViewingMember(id) }
    }

    /**
     * 今日餐卡共享上游(单一订阅·防重复)。[AI修改] Google审🟡-2:todayNutrition 与 todayMeals 复用同一条 observeTimelineWindow 订阅。
     *
     * "菜配料变化"+"食用比例变化"令牌已下沉进 observeTimelineWindow(改克数/配料/就地调吃了多少即时重算·改B表不触发A表Flow红线)，
     * 此处不再重复并令牌(避免双重订阅)——observeTimelineWindow 自身已在 eaten_ratio/dish_ingredient 变化时重发新卡片。
     */
    private val todayCards: StateFlow<List<com.sxdbsm.cookbook.domain.model.DayMealCardData>> =
        mealRepo.observeTimelineWindow(today, today)
            .onEach { cards ->
                MealDataTraceLogger.uiStateUpdated(
                    feature = "home",
                    dates = cards.joinToString(",") { it.date.toString() },
                    cardCount = cards.size,
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayNutrition: StateFlow<TodayNutrition?> =
        combine(
            todayCards,
            family.observeFocusBody(),
            family.observeFocusShareForDate(com.sxdbsm.cookbook.util.DateTime.formatDate(today)),
            explicitGroups,
            focusConditions,
        ) { cards, body, share, explicit, conditions ->
                if (share <= 0.0) return@combine null // [AI修改] 关注成员今天未在家吃→不显今日营养卡
                // [AI修改] 食用比例(是否吃完)：按**每道菜实例**(带 eatenRatio)折算个人摄入=Σ(整份×eatenRatio)×share(IntakeCalculator 单一真相源)。
                //   用实例列表(非 distinct id)——同菜在不同餐次可各自吃了多少不同；营养按 distinct id 批量查一次再按实例映射。
                val dishInstances = cards.flatMap { it.meals }.flatMap { it.dishes }
                if (dishInstances.isEmpty()) return@combine null
                val ids = dishInstances.map { it.id }.distinct()
                val nutById = nutritionRepo.dishNutrition(ids).mapValues { it.value.totals }
                // 全字段个人摄入(已×eatenRatio×share)：达标/慢病评估/展示统一读此，不再逐字段各乘(防口径漂移)。
                val personalTotals = com.sxdbsm.cookbook.domain.model.IntakeCalculator.personalIntake(
                    dishInstances.map { d -> (nutById[d.id] ?: com.sxdbsm.cookbook.domain.model.NutritionTotals.EMPTY) to d.eatenRatio },
                    share,
                )
                if (personalTotals.energyKcal <= 0) return@combine null
                val target = com.sxdbsm.cookbook.domain.model.CalorieTarget.dailyTarget(body)
                // [AI生成] 今日"还缺什么"如实解读(缺优质蛋白/主食/蔬菜)，非推荐非医嘱。(食用比例不影响定性——少吃仍是这道菜的属性)
                // [AI修改] J15：营养大类判定用"主料优先·空则回退全食材"(修 is_main 全0/缺标的问题菜漏判主食)；
                //   mains(仅主料)保留给痛风/GI 匹配口径不变(与"健康定性按主料判定"一致)。
                val mains = dishInstances.flatMap { it.mainIngredientNames }
                val groupNames = dishInstances.flatMap { FoodGroup.classificationNames(it.mainIngredientNames, it.allIngredientNames) }
                val dayGroups = FoodGroup.groupsOf(groupNames, explicit)
                val gaps = FoodGroup.nutritionGaps(dayGroups)
                // [AI修改] 今日营养卡按关注成员个人摄入(全家餐×食用比例×份额)展示 + 达标评定。
                val kcal = personalTotals.energyKcal
                val status = target?.let { com.sxdbsm.cookbook.domain.model.CalorieTarget.status(kcal, it) }
                // [AI生成] A-1：慢病温和提示(个人视角，色系墙不动)——按关注成员摄入(含食用比例)的钠 + 热量达标评估，缺数据不下调。
                val baseLevel = FoodGroup.nutritionLevel(dayGroups)
                // [AI生成] P4 痛风：从今日主料名匹配"应避免"高嘌呤定性食物(与"健康定性按主料判定"口径一致)，命中→痛风提示。
                //   [AI修改] 仅登记痛风才算匹配(gate 最外层省无谓匹配)。
                val highPurineHits = if (com.sxdbsm.cookbook.domain.HealthCondition.GOUT in conditions)
                    com.sxdbsm.cookbook.domain.NutritionLevelEvaluator.matchHighPurineFoods(mains) else emptyList()
                // [AI生成] P2 糖尿病：仅登记糖尿病才查 gi 表(gate 省无谓查询)，从今日主料名匹配高GI(≥70)食物→"可换低GI"提示。
                val highGiFoods = if (com.sxdbsm.cookbook.domain.HealthCondition.DIABETES in conditions)
                    com.sxdbsm.cookbook.domain.NutritionLevelEvaluator.matchHighGiFoods(mains, nutritionRepo.giByName()) else emptyList()
                val assessment = com.sxdbsm.cookbook.domain.NutritionLevelEvaluator.evaluate(
                    baseLevel = baseLevel, totals = personalTotals, calorieStatus = status, conditions = conditions,
                    highPurineHits = highPurineHits, highGiFoods = highGiFoods,
                )
                TodayNutrition(
                    kcal = kcal.roundToInt(),
                    proteinG = personalTotals.proteinG.roundToInt(),
                    fatG = personalTotals.fatG.roundToInt(),
                    carbG = personalTotals.carbG.roundToInt(),
                    target = target,
                    status = status,
                    gaps = gaps,
                    concerns = assessment.concerns,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * 今日各餐(带 mealRecordId + 每菜 eatenRatio)供"按实际吃了多少调整"弹层。[AI修改] 食用比例(是否吃完)
     *
     * 派生自 todayCards(=observeTimelineWindow·已内并 eaten_ratio/配料令牌)→改了比例弹层即时反映
     * (DB 为单一真相源·UDF·无本地态漂移·改B表不触发A表Flow红线由 observeTimelineWindow 内令牌兜住)。
     * 只列有 mealRecordId 且非空的餐(可写回)。
     */
    val todayMeals: StateFlow<List<com.sxdbsm.cookbook.domain.model.MealSection>> =
        todayCards.map { cards ->
            cards.flatMap { it.meals }.filter { it.mealRecordId != null && it.dishes.isNotEmpty() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** 就地设某餐某菜的食用比例(分菜调)。[AI生成] */
    fun setDishEaten(mealRecordId: Long, dishId: Long, ratio: Double) {
        viewModelScope.launch { mealRepo.setEatenRatio(mealRecordId, dishId, ratio) }
    }

    /** 整餐一次设为同一食用比例(这一餐整体)。[AI生成] */
    fun setMealEaten(mealRecordId: Long, ratio: Double) {
        viewModelScope.launch { mealRepo.setEatenRatioForMeal(mealRecordId, ratio) }
    }

    /**
     * 往年营养平均色：早于本年、且有餐食记录的年份，取该年"有餐日"的平均营养级别。[AI生成]
     *
     * 显示在色系墙标题下方(块内为年份后两位)；无往年数据则空。随营养色系开关一起显示。
     */
    val yearAverages: StateFlow<List<YearNutrition>> = flow {
        val (min, _) = runCatching { mealRepo.dateRange() }.getOrDefault(null to null)
        if (min == null || min.year >= today.year) {
            emit(emptyList())
            return@flow
        }
        val explicit = FoodGroup.explicitFrom(ingredientRepo.foodGroupByName()) // [AI生成] A1：显式大类覆盖
        emitAll(
            mealRepo.observeTimelineWindow(LocalDate(min.year, 1, 1), LocalDate(today.year - 1, 12, 31)).map { cards ->
                cards.filter { it.meals.isNotEmpty() }
                    .mapNotNull { card ->
                        val mains = card.meals.flatMap { it.dishes }.flatMap { it.mainIngredientNames }
                        val lv = FoodGroup.nutritionLevel(FoodGroup.groupsOf(mains, explicit))
                        if (lv > 0) card.date.year to lv else null
                    }
                    .groupBy({ it.first }, { it.second })
                    .map { (yr, levels) -> YearNutrition(yr, levels.average().roundToInt()) }
                    .sortedByDescending { it.year }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

/** 某天的营养级别(色系墙用)。[AI生成] */
data class DayNutrition(val date: LocalDate, val level: Int)

/** 今日营养概览(首页卡)。[AI生成] 3c */
data class TodayNutrition(
    val kcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbG: Int,
    val target: Int?, // 每日目标(填了身体数据才有)
    val status: com.sxdbsm.cookbook.domain.model.CalorieStatus?,
    val gaps: List<String> = emptyList(), // [AI生成] 今日三支柱里还缺哪几类(优质蛋白/主食/蔬菜)，如实非医嘱
    val concerns: List<String> = emptyList(), // [AI生成] A-1：慢病温和提示(偏咸·高血压留意/热量超标)，缺数据为空、仅供参考非医嘱
)

/** 某一年的平均营养级别(色系墙往年概览用)。[AI生成] */
data class YearNutrition(val year: Int, val level: Int) {
    /** 年份后两位(如 2025→"25")。 */
    val yy: String get() = (year % 100).toString().padStart(2, '0')
}
