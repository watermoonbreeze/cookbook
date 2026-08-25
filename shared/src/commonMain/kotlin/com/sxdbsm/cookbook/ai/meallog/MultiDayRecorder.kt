package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.autogen.AutoGenContext
import com.sxdbsm.cookbook.domain.autogen.AutoGenPreview
import com.sxdbsm.cookbook.domain.autogen.AutoGenResult
import com.sxdbsm.cookbook.domain.autogen.DayAutoGenerator
import com.sxdbsm.cookbook.domain.autogen.DishAutoGenerator
import com.sxdbsm.cookbook.domain.autogen.IngredientAliasResolver
import com.sxdbsm.cookbook.domain.autogen.IngredientAutoGenerator
import com.sxdbsm.cookbook.domain.autogen.MergeMode
import com.sxdbsm.cookbook.domain.autogen.ResolveKind
import com.sxdbsm.cookbook.domain.autogen.SemanticDay
import com.sxdbsm.cookbook.domain.autogen.SemanticDish
import com.sxdbsm.cookbook.domain.autogen.SemanticIngredient
import com.sxdbsm.cookbook.domain.autogen.SemanticMeal
import com.sxdbsm.cookbook.platform.ioDispatcher
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordUseCase
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * @File : MultiDayRecorder
 * @Time : 2026/07/29
 * @Author : SXD-AI
 * @Desc : 多天餐食入库编排——薄适配器：DayMealJson → Semantic* → DayAutoGenerator
 * <p>
 * [AI修改] 自动化基础能力层 Phase 3：降为适配器，委托给 domain/autogen/ 能力层。
 * recordAll 签名不变·K1 零破坏。
 * <p>
 * 原编排流程（Phase 0-4）现由 DayAutoGenerator 统一处理；本类仅做 JSON→Semantic 映射。
 **/

// MergeMode 已迁移至 com.sxdbsm.cookbook.domain.autogen.MergeMode·保留 import 供旧调用方编译兼容

/** 单天入库结果。[AI生成] */
data class DayRecordResult(
    val date: LocalDate,
    val mealsSaved: Int,
    val dishesCreated: Int,
    val ingredientsCreated: Int,
    val ingredientNamesCreated: List<String> = emptyList(),
    val dishNamesCreated: List<String> = emptyList(),
)

/** 多天入库汇总。[AI生成] */
data class MultiDayRecordResult(
    val days: List<DayRecordResult> = emptyList(),
    val totalMealsSaved: Int = 0,
    val totalDishesCreated: Int = 0,
    val totalIngredientsCreated: Int = 0,
) {
    val allIngredientNamesCreated: List<String> get() = days.flatMap { it.ingredientNamesCreated }
    val allDishNamesCreated: List<String> get() = days.flatMap { it.dishNamesCreated }
}

class MultiDayRecorder(
    private val ingredientRepo: IngredientRepository,
    private val dishRepo: DishRepository,
    private val mealRepo: MealRecordRepository,
    private val nutritionRepo: NutritionRepository,
    private val aliasResolver: IngredientAliasResolver,
    private val db: CookbookDatabase, // [AI修改] 每次新鲜 load AutoGenContext，避免 suspend 进 Koin + 确保字典反映最新 DB 状态
    private val mealRecordUseCase: MealRecordUseCase = MealRecordUseCase(mealRepo),
) {

    /** DayMealJson → SemanticDay 映射（供 recordAll / previewAll 共用）。[AI修改] */
    private fun toSemanticDay(dayJson: DayMealJson): SemanticDay = SemanticDay(
        date = dayJson.date,
        dateOffset = dayJson.date_offset,
        meals = dayJson.meals.map { mealJson ->
            SemanticMeal(
                mealTypeCode = mealJson.meal_type,
                mealTime = mealJson.meal_time,
                note = mealJson.note,
                dishes = mealJson.dishes.map { dishRef ->
                    val dishName = dishRef.name.ifBlank { dishRef.dish?.name ?: "" }
                    val dishJson = dishRef.dish
                    SemanticDish(
                        name = dishName,
                        ingredients = dishJson?.ingredients?.map { diJson ->
                            val ingredientName = diJson.ref ?: diJson.food?.name ?: ""
                            SemanticIngredient(
                                name = ingredientName,
                                quantity = diJson.quantity,
                                unit = diJson.unit,
                                isMain = diJson.is_main,
                            )
                        } ?: emptyList(),
                        cookingMethods = dishJson?.cooking_methods ?: emptyList(),
                        tags = dishJson?.tags ?: emptyList(),
                        description = dishJson?.description ?: "",
                        specialNote = dishJson?.special_note ?: "",
                        steps = dishJson?.steps ?: emptyList(),
                        eatenRatio = dishRef.eaten_ratio,
                        source = dishJson?.source?.ifBlank { "ai" } ?: "ai",
                    )
                },
            )
        },
    )

    /** 构建能力层三件套（无上下文·用于 commit 阶段）。[AI修改] */
    private fun buildDayGen(): DayAutoGenerator {
        val ingredientGen = IngredientAutoGenerator(ingredientRepo, nutritionRepo)
        val dishGen = DishAutoGenerator(dishRepo, ingredientGen)
        return DayAutoGenerator(dishGen, mealRepo, mealRecordUseCase)
    }

    /**
     * 只预览·零写库（P2-1 K1a：UI 确认前调用）。[AI修改]
     *
     * 解析 DayMealJson → SemanticDay → DayAutoGenerator.preview()，返回完整预览含营养估算。
     * K1a 新增：REUSE 菜品（AI 识别为库内已有菜，preview 时不重算营养、nutrition=null）在此
     * **一次批量**查询 `nutritionRepo.dishNutrition(reuseIds)` 回填真实营养，避免逐个查询（N+1）。
     */
    suspend fun previewAll(
        days: List<DayMealJson>,
        today: LocalDate,
    ): AutoGenPreview = withContext(ioDispatcher) {
        val autoGenContext = AutoGenContext.load(db, aliasResolver)
        val dayGen = buildDayGen()
        val semanticDays = days.map { toSemanticDay(it) }
        val preview = dayGen.preview(semanticDays, today, autoGenContext)

        // REUSE 菜品批量营养回填：收集全部 REUSE id → 一次查询 → 逐层 .copy() 回填。
        // 注意：dishNutrition() 用 associateWith 保证每个请求 id 都有非空条目（即使 hasData=false 也是非空对象），
        // 因此 REUSE 菜回填后必为"尝试计算但无数据"(②类)，不会与"从未尝试计算"(①类)混淆（INV-K1A-04）。
        val reuseIds = preview.days.asSequence()
            .flatMap { it.meals }
            .flatMap { it.dishes }
            .filter { it.resolution == ResolveKind.REUSE }
            .mapNotNull { it.existingId }
            .distinct()
            .toList()
        if (reuseIds.isEmpty()) return@withContext preview
        val nutritionById = nutritionRepo.dishNutrition(reuseIds) // 一次批量查询，非循环（INV-K1A-02）
        preview.copy(days = preview.days.map { day ->
            day.copy(meals = day.meals.map { meal ->
                meal.copy(dishes = meal.dishes.map { dish ->
                    if (dish.resolution == ResolveKind.REUSE && dish.existingId != null)
                        dish.copy(nutrition = nutritionById[dish.existingId]) else dish
                })
            })
        })
    }

    /**
     * 提交预览结果入库（P2-1 K1a：用户确认后调用）。[AI修改]
     *
     * 接受 previewAll() 产出的 AutoGenPreview，执行 DayAutoGenerator.commit()。
     */
    suspend fun commitPreview(
        preview: AutoGenPreview,
        mergeMode: MergeMode = MergeMode.MERGE,
    ): AutoGenResult = withContext(ioDispatcher) {
        buildDayGen().commit(preview, mergeMode)
    }

    /**
     * 批量入库多天餐食。[AI修改] Phase 3：DayMealJson→Semantic*→DayAutoGenerator。
     *
     * @param days 待入库的多天餐食列表
     * @param today 基准日期（用于 date_offset 推算）
     * @param ingredientNames 已废弃（新层用 AutoGenContext 预取词典；保留参数兼容旧调用）
     * @param mergeMode 合并模式（默认 MERGE：同餐次追加）
     * @return 逐天入库结果
     */
    suspend fun recordAll(
        days: List<DayMealJson>,
        today: LocalDate,
        @Suppress("UNUSED_PARAMETER") ingredientNames: List<String> = emptyList(),
        mergeMode: MergeMode = MergeMode.MERGE,
    ): MultiDayRecordResult = withContext(ioDispatcher) {
        val autoGenPreview = previewAll(days, today)
        val result = commitPreview(autoGenPreview, mergeMode)

        // 按 preview 中每天的实际日期构建 DayRecordResult（不再硬编码 today）
        // 总计数平均分配到各天（AutoGenResult 无逐天明细，此为最优近似）
        val savedDays = autoGenPreview.days
        val dayCount = savedDays.size.coerceAtLeast(1)
        val dayResults = savedDays.map { dayPreview ->
            DayRecordResult(
                date = dayPreview.date,
                mealsSaved = result.mealsSaved / dayCount,
                dishesCreated = result.dishesCreated / dayCount,
                ingredientsCreated = result.ingredientsCreated / dayCount,
                ingredientNamesCreated = result.createdIngredientNames,
                dishNamesCreated = result.createdDishNames,
            )
        }

        MultiDayRecordResult(
            days = dayResults,
            totalMealsSaved = result.mealsSaved,
            totalDishesCreated = result.dishesCreated,
            totalIngredientsCreated = result.ingredientsCreated,
        )
    }

    /** 检查指定日期是否已有餐食记录。[AI生成] */
    suspend fun hasExistingMeals(date: LocalDate): Boolean {
        return mealRepo.loadDayMealsForEdit(date).isNotEmpty()
    }

    /** 批量检查多天是否有已有餐食。[AI生成] */
    suspend fun findConflicts(
        dates: List<LocalDate>,
    ): Map<LocalDate, Boolean> {
        return dates.associateWith { hasExistingMeals(it) }
    }
}
