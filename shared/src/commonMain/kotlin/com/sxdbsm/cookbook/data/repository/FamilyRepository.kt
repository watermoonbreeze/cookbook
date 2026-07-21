package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.BodyMetrics
import com.sxdbsm.cookbook.domain.model.CalorieExemptStage
import com.sxdbsm.cookbook.domain.model.FamilyMember
import com.sxdbsm.cookbook.domain.model.Gender
import com.sxdbsm.cookbook.platform.ioDispatcher
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * @File : FamilyRepository
 * @Time : 2026/07/15
 * @Author : SXD-AI
 * @Desc : 家庭成员档案仓库（多人记菜）
 * <p>
 * 管理家庭成员的身体数据/饭量系数/病种。忌口取全家启用成员的病种并集；每日目标/达标按主要关注成员。
 * 首次使用时幂等建默认成员「我」并迁移旧单套身体数据(偏好)+旧健康档案(user_health_profile)。
 * <p>
 * [AI生成] 多人家庭档案 P1。
 **/
class FamilyRepository(
    private val db: CookbookDatabase,
    private val prefs: PreferenceRepository,
) {
    private val q = db.cookbookQueries

    /**
     * 幂等初始化：无任何成员时建默认「我」(is_self=1,is_focus=1)，承接旧单套身体数据 + 旧启用病种。[AI生成]
     *
     * 只在成员表为空时执行一次；老库升级(迁移只建空表)与全新装(Schema.create)都走这里，保证有个默认成员。
     */
    suspend fun ensureInitialized() = withContext(ioDispatcher) {
        if (q.countFamilyMembers().executeAsOne() > 0L) return@withContext
        val body = prefs.observeBodyMetrics().first()
        val coeff = FamilyMember.defaultCoefficient(body.gender, body.age)
        db.transaction {
            q.insertFamilyMember(
                name = "我",
                gender = body.gender,
                height_cm = body.heightCm,
                weight_kg = body.weightKg,
                age = body.age?.toLong(),
                activity = body.activity,
                portion_coefficient = coeff,
                is_self = 1L,
                is_focus = 1L,
                sort = 0L,
                created_at = DateTime.nowEpochSeconds(),
            )
            val selfId = q.selectSelfMemberId().executeAsOne()
            // 迁移旧健康档案(全局启用病种)到「我」
            q.selectEnabledHealthProfiles().executeAsList().forEach { row ->
                q.insertMemberCare(member_id = selfId, care_category_id = row.care_category_id)
            }
        }
    }

    /** 监听全部家庭成员(含各自病种)。[AI生成] */
    fun observeMembers(): Flow<List<FamilyMember>> =
        q.selectAllFamilyMembers().asFlow().mapToList(ioDispatcher)
            .map { rows -> val exempt = calorieExemptCareIds(); rows.map { toModel(it.id, it.name, it.gender, it.height_cm, it.weight_kg, it.age, it.activity, it.portion_coefficient, it.is_self, it.is_focus, exempt) } }
            .flowOn(ioDispatcher)

    /** 读取全部家庭成员(含各自病种)。[AI生成] */
    suspend fun listMembers(): List<FamilyMember> = withContext(ioDispatcher) {
        val exempt = calorieExemptCareIds()
        q.selectAllFamilyMembers().executeAsList().map {
            toModel(it.id, it.name, it.gender, it.height_cm, it.weight_kg, it.age, it.activity, it.portion_coefficient, it.is_self, it.is_focus, exempt)
        }
    }

    /** 监听当前查看成员的身体数据(每日目标用)：按"当前查看"指针,自愈回退。[AI修改] 多人关注 */
    fun observeFocusBody(): Flow<BodyMetrics> =
        combine(observeMembers(), prefs.observeFocusViewingMemberId()) { ms, vid ->
            resolveViewing(ms, vid)?.toBodyMetrics() ?: BodyMetrics()
        }

    /** 监听当前查看成员的餐食份额占比(饭量系数 / 全家系数和)。[AI修改] 多人关注 */
    fun observeFocusShare(): Flow<Double> =
        combine(observeMembers(), prefs.observeFocusViewingMemberId()) { ms, vid ->
            val f = resolveViewing(ms, vid) ?: return@combine 1.0
            val sum = ms.sumOf { it.portionCoefficient }
            if (sum > 0.0) (f.portionCoefficient / sum) else 1.0
        }

    /** 监听当前查看成员昵称(展示用；单人或无则空)。[AI修改] 多人关注 */
    fun observeFocusName(): Flow<String> =
        combine(observeMembers(), prefs.observeFocusViewingMemberId()) { ms, vid ->
            if (ms.size <= 1) "" else (resolveViewing(ms, vid)?.name ?: "")
        }

    /**
     * 当前查看成员。[AI生成] 多人关注
     *
     * 指针∈关注集合→用它;否则关注集合首位(selectAllFamilyMembers 已按 is_self DESC,sort 排);
     * 关注集合空→isSelf/first。指针失效(指向已删/未关注成员)自愈回退。
     */
    private fun resolveViewing(ms: List<FamilyMember>, viewingId: Long?): FamilyMember? {
        val focus = ms.filter { it.isFocus }
        if (focus.isNotEmpty()) return focus.firstOrNull { it.id == viewingId } ?: focus.first()
        return ms.firstOrNull { it.isSelf } ?: ms.firstOrNull()
    }

    /** 监听"当前查看成员"完整模型(含病种·今日卡慢病提示等消费点用·随指针切换)。[AI生成] 多人关注 */
    fun observeViewingMember(): Flow<FamilyMember?> =
        combine(observeMembers(), prefs.observeFocusViewingMemberId()) { ms, vid -> resolveViewing(ms, vid) }

    // ===== 缺席微调（按天持久化） =====

    /** 监听某天缺席成员 id。[AI生成] */
    fun observeAbsenteeIds(date: String): Flow<Set<Long>> =
        q.selectAbsenteesByDate(date).asFlow().mapToList(ioDispatcher).map { it.toSet() }.flowOn(ioDispatcher)

    /** 标记/取消某天某成员缺席。[AI生成] */
    suspend fun setAbsent(date: String, memberId: Long, absent: Boolean) = withContext(ioDispatcher) {
        if (absent) q.insertAbsentee(date, memberId) else q.deleteAbsentee(date, memberId)
    }

    /** 实时查某天缺席成员 id 集(供 toggle 按最新状态翻转，避免读到冻结的 stateIn 值)。[AI生成] */
    suspend fun absenteeIds(date: String): Set<Long> = withContext(ioDispatcher) {
        q.selectAbsenteesByDate(date).executeAsList().toSet()
    }

    /** 翻转某成员某天在场/缺席(读实时状态再取反，修"点没吃后再点回不来")。[AI生成] */
    suspend fun toggleAbsent(date: String, memberId: Long) = withContext(ioDispatcher) {
        val absent = q.selectAbsenteesByDate(date).executeAsList().toSet()
        if (memberId in absent) q.deleteAbsentee(date, memberId) else q.insertAbsentee(date, memberId)
    }

    /**
     * 监听某天的关注成员份额：关注系数 ÷ 当天在场成员系数和。[AI生成]
     *
     * 关注成员当天自己缺席 → 返回 0（其当天摄入=0，UI 显"未在家吃"不判偏低）。
     */
    fun observeFocusShareForDate(date: String): Flow<Double> =
        combine(observeMembers(), observeAbsenteeIds(date), prefs.observeFocusViewingMemberId()) { ms, absent, vid ->
            val f = resolveViewing(ms, vid) ?: return@combine 1.0
            if (f.id in absent) return@combine 0.0
            val presentSum = ms.filter { it.id !in absent }.sumOf { it.portionCoefficient }
            if (presentSum > 0.0) f.portionCoefficient / presentSum else 1.0
        }

    /** 同步「我」的身体数据(设置里"我的每日热量目标"快捷编辑，保持与成员档案一致)。[AI生成] */
    suspend fun updateSelfBody(body: BodyMetrics) = withContext(ioDispatcher) {
        val selfId = q.selectSelfMemberId().executeAsOneOrNull() ?: return@withContext
        val self = q.selectFamilyMemberById(selfId).executeAsOneOrNull() ?: return@withContext
        q.updateFamilyMember(
            name = self.name, gender = body.gender, height_cm = body.heightCm, weight_kg = body.weightKg,
            age = body.age?.toLong(), activity = body.activity, portion_coefficient = self.portion_coefficient, id = selfId,
        )
    }

    /** 当前查看成员(按指针·自愈回退)。[AI修改] 多人关注:达标/色系墙默认围绕他。 */
    suspend fun focusMember(): FamilyMember? = withContext(ioDispatcher) {
        val ms = listMembers()
        resolveViewing(ms, prefs.focusViewingMemberId())
    }

    /**
     * 加入/移出关注集合(多选·至少留 1)。[AI生成] 多人关注
     *
     * @return false=拒绝(取消最后一个关注人)，UI 提示"至少关注一位家人"；true=已切换。
     */
    suspend fun toggleFocus(id: Long): Boolean = withContext(ioDispatcher) {
        // [AI修改] 审查建议1:计数+写入收进 db.transaction 保原子(并发点星标下守住下限1·防 TOCTOU 穿透)。
        var ok = true
        db.transaction {
            val focusIds = q.selectAllFamilyMembers().executeAsList().filter { it.is_focus == 1L }.map { it.id }.toSet()
            when {
                id !in focusIds -> q.updateMemberFocus(1L, id)      // 加入关注
                focusIds.size <= 1 -> ok = false                    // 取消最后一个→拒绝(至少关注一位)
                else -> q.updateMemberFocus(0L, id)                 // 移出关注
            }
        }
        ok
    }

    /** 设置"当前查看"成员指针(今日卡/报告切换器共用·一处切两处同步)。[AI生成] 多人关注 */
    suspend fun setViewingMember(id: Long) = withContext(ioDispatcher) {
        prefs.setFocusViewingMemberId(id)
    }

    /** 新建成员，返回 id。[AI生成] */
    suspend fun createMember(m: FamilyMember): Long = withContext(ioDispatcher) {
        var newId = 0L
        db.transaction {
            q.insertFamilyMember(
                name = m.name.trim().ifBlank { "成员" },
                gender = m.gender,
                height_cm = m.heightCm,
                weight_kg = m.weightKg,
                age = m.age?.toLong(),
                activity = m.activity,
                portion_coefficient = m.portionCoefficient,
                is_self = 0L,
                is_focus = 0L,
                sort = (q.countFamilyMembers().executeAsOne()),
                created_at = DateTime.nowEpochSeconds(),
            )
            newId = q.lastInsertRowId().executeAsOne()
            m.careCategoryIds.forEach { q.insertMemberCare(member_id = newId, care_category_id = it) }
            m.avoidCategoryIds.forEach { q.insertMemberAvoidCategory(member_id = newId, category_id = it) } // [AI生成] v29:个人忌口分类
            m.avoidIngredientIds.forEach { q.insertMemberAvoidIngredient(member_id = newId, ingredient_id = it) } // [AI生成] 阶段4:个人忌口具体食材
        }
        newId
    }

    /** 更新成员身体数据/系数/病种。[AI生成] */
    suspend fun updateMember(m: FamilyMember) = withContext(ioDispatcher) {
        db.transaction {
            q.updateFamilyMember(
                name = m.name.trim().ifBlank { "成员" },
                gender = m.gender,
                height_cm = m.heightCm,
                weight_kg = m.weightKg,
                age = m.age?.toLong(),
                activity = m.activity,
                portion_coefficient = m.portionCoefficient,
                id = m.id,
            )
            q.deleteAllMemberCare(m.id)
            m.careCategoryIds.forEach { q.insertMemberCare(member_id = m.id, care_category_id = it) }
            // [AI生成] v29:个人忌口分类全量替换(仿 member_care)。
            q.deleteAllMemberAvoidCategories(m.id)
            m.avoidCategoryIds.forEach { q.insertMemberAvoidCategory(member_id = m.id, category_id = it) }
            // [AI生成] 阶段4:个人忌口具体食材全量替换。
            q.deleteAllMemberAvoidIngredients(m.id)
            m.avoidIngredientIds.forEach { q.insertMemberAvoidIngredient(member_id = m.id, ingredient_id = it) }
        }
    }

    /** 删除成员(仅非「我」)。[AI修改] 多人关注:删后若关注集合空,补关注剩余首位(保下限≥1·不 clearAll 免误清其他关注人)。 */
    suspend fun deleteMember(id: Long) = withContext(ioDispatcher) {
        db.transaction {
            q.softDeleteFamilyMember(id)
            val remaining = q.selectAllFamilyMembers().executeAsList() // status=1，已排除刚删的
            if (remaining.none { it.is_focus == 1L }) {
                remaining.firstOrNull()?.let { q.setFocusMember(it.id) } // 删掉了唯一关注人→补关注剩余首位
            }
        }
    }

    /** 设主要关注成员(单选)。[AI生成] */
    suspend fun setFocus(id: Long) = withContext(ioDispatcher) {
        db.transaction { q.clearAllFocus(); q.setFocusMember(id) }
    }

    /**
     * 忌口口径 = 全家成员病种 ∪ 旧个人健康档案(user_health_profile 启用)，去重。[AI生成]
     *
     * 并入旧档案：默认成员「我」由旧档案迁移而来；且「我的」页仍可编辑个人档案，
     * 两处都汇入忌口并集，避免迁移后编辑漂移导致漏忌口。
     */
    suspend fun allEnabledCareIds(): List<Long> = withContext(ioDispatcher) {
        val fromMembers = q.selectAllEnabledCareIds().executeAsList()
        val fromLegacy = q.selectEnabledHealthProfiles().executeAsList().map { it.care_category_id }
        (fromMembers + fromLegacy).distinct()
    }

    /** 全家个人忌口分类并集(推荐展开为食材 id)。[AI生成] v29 */
    suspend fun allPersonalAvoidCategoryIds(): List<Long> = withContext(ioDispatcher) {
        q.selectAllPersonalAvoidCategoryIds().executeAsList()
    }

    /** 全家个人忌口的具体食材 id 并集(推荐直接进 avoid·无需展开)。[AI生成] 阶段4 */
    suspend fun allPersonalAvoidIngredientIds(): List<Long> = withContext(ioDispatcher) {
        q.selectAllPersonalAvoidIngredientIds().executeAsList()
    }

    /**
     * 个人忌口可选分类项（成员编辑弹层 chip 白名单·按 §9.22）。[AI生成] v29
     *
     * food_category 表无 code 列→按**分类名**映射到 id(缺失静默跳过防崩)。只列日常真会"整类不吃"的分类，
     * 荤类给具体子类(避免与"肉类"父级并列困惑)，其余给大类。label 用口语短标签(非分类库 name)。
     */
    suspend fun listAvoidCategoryOptions(): List<com.sxdbsm.cookbook.domain.model.AvoidCategoryOption> = withContext(ioDispatcher) {
        val idByName = q.selectAllFoodCategories().executeAsList()
            .filter { it.dimension == "general" }.associate { it.name to it.id }
        AVOID_CATEGORY_WHITELIST.mapNotNull { (catName, labelGroup) ->
            idByName[catName]?.let { com.sxdbsm.cookbook.domain.model.AvoidCategoryOption(it, labelGroup.first, labelGroup.second) }
        }
    }

    private companion object {
        // [AI生成] v29:忌口 chip 白名单 分类名→(chip短标签, 分组)。荤类只给子类,素食口味给大类。code 见 §9.22。
        val AVOID_CATEGORY_WHITELIST: List<Pair<String, Pair<String, String>>> = listOf(
            "猪肉类" to ("猪肉" to "荤食"),
            "牛肉类" to ("牛肉" to "荤食"),
            "羊肉类" to ("羊肉" to "荤食"),
            "禽肉类" to ("鸡鸭禽肉" to "荤食"),
            "动物内脏类" to ("动物内脏" to "荤食"),
            "鱼类" to ("鱼" to "荤食"),
            "虾蟹类" to ("虾蟹" to "荤食"),
            "贝类" to ("贝类" to "荤食"),
            "蛋类" to ("蛋" to "荤食"),
            "奶类" to ("奶类" to "素食与口味"),
            "大豆及坚果" to ("大豆坚果" to "素食与口味"),
            "食用菌类" to ("菌菇" to "素食与口味"),
            "藻类" to ("藻类海带" to "素食与口味"),
            "葱蒜类" to ("葱蒜" to "素食与口味"),
            "香辛料类" to ("香辛料" to "素食与口味"),
        )
    }

    private fun toModel(
        id: Long, name: String, gender: String, height: Double?, weight: Double?, age: Long?,
        activity: String, coeff: Double, isSelf: Long, isFocus: Long, exemptCareIds: Set<Long>,
    ): FamilyMember {
        val careIds = q.selectMemberCareIds(id).executeAsList()
        return FamilyMember(
            id = id, name = name, gender = gender, heightCm = height, weightKg = weight, age = age?.toInt(),
            activity = activity, portionCoefficient = coeff, isSelf = isSelf == 1L, isFocus = isFocus == 1L,
            careCategoryIds = careIds,
            avoidCategoryIds = q.selectMemberAvoidCategoryIds(id).executeAsList(), // [AI生成] v29:个人忌口分类回显
            avoidIngredientIds = q.selectMemberAvoidIngredientIds(id).executeAsList(), // [AI生成] 阶段4:个人忌口具体食材回显
            // [AI生成] A2:该成员是否标记孕期/哺乳期(生命阶段 care)→toBodyMetrics 透传→dailyTarget 不评热量。
            //   [AI修改] Google审🟡:数据层加性别 gate(仅女性)——健康红线最终防线在此,不只靠 UI 切男性清理(防"男性+孕期"历史脏数据误判)。
            isCalorieExempt = gender == Gender.FEMALE.name && careIds.any { it in exemptCareIds },
        )
    }

    /** 「生命阶段」中"不评热量"的 care 分类 id(孕期/哺乳期)。[AI生成] A2:复用已有 care 信号·非新字段·名称匹配同 CalorieExemptStage。 */
    private fun calorieExemptCareIds(): Set<Long> =
        q.selectCareCategories().executeAsList()
            .filter { CalorieExemptStage.contains(listOf(it.name)) }
            .map { it.id }.toSet()
}
