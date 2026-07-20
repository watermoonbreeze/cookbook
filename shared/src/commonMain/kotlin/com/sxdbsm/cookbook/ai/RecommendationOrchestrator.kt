package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.DishCandidate
import com.sxdbsm.cookbook.ai.model.MealSuggestion
import com.sxdbsm.cookbook.ai.model.RecommendationInput
import com.sxdbsm.cookbook.ai.model.RecommendationResult
import com.sxdbsm.cookbook.ai.model.RecommendationSource

/**
 * @File : RecommendationOrchestrator
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 推荐下一餐编排：规则筛 → 模型组合 → 校验 → 兜底
 * <p>
 * 先用 HealthRuleEngine 筛出安全候选，再让模型只在候选里搭 2~3 菜的组合；模型输出经解析+校验
 * (dishId 必须来自候选)才采用，任何环节失败都回退到纯规则兜底。忌口由规则层强校验，模型无权绕过。
 * <p>
 * [AI生成] S1：面向 AiRuntime 接口编排，Mock/云端/端侧可换。取数由调用方用 RecommendationDataSource 提供。
 **/
class RecommendationOrchestrator(
    private val runtime: AiRuntime,
    private val engine: HealthRuleEngine = HealthRuleEngine(),
) {
    /**
     * 生成推荐。[AI生成]
     *
     * @param input 取数层聚合的输入（RecommendationDataSource.gather()）
     * @param mealCount 推荐几个不同的餐
     */
    suspend fun recommend(
        input: RecommendationInput,
        mealCount: Int = DEFAULT_MEAL_COUNT,
        rotation: Int = 0, // [AI生成] "换一换"轮次：轮转候选窗口，让规则兜底也能换出不同组合。
    ): RecommendationResult {
        // [AI修改] 增长型 P2：透传画像信号与推荐风格权重。
        val evaluated = engine.evaluate(
            input.dishes, input.pantryIngredientIds, input.constraints, input.recentDishIds,
            input.shortageIngredientIds, input.recentDishDaysAgo,
            weights = input.style.weights(),
            preferenceScores = input.preferenceScores,
            nutritionBalanceScores = input.nutritionBalanceScores,
            mainRepeatCounts = input.mainRepeatCounts,
            conditions = input.conditions, // [AI生成] 慢病数值软约束透传
            giByName = input.giByName,
            tasteProfile = input.tasteProfile, // [AI生成] 口味画像透传
            lastCookedDaysAgo = input.lastCookedDaysAgo, // [AI生成] 时间衰减透传(仅偏新鲜)
        )
        if (evaluated.isEmpty()) {
            return RecommendationResult(emptyList(), evaluated, RecommendationSource.EMPTY)
        }
        // [AI生成] 算法评审#3.1：取批后做 MMR 批内多样性重排(仅"偏新鲜"默认开)，打散同主料霸屏。
        val candidates = diversify(rotate(evaluated, rotation), input.style.diversityLambda())
        // [AI生成] R1(忌口正确性红线)：忌口菜(avoidNames 非空)保留在 candidates 供 UI 标红展示，但**剔除出喂给模型的可选集**——
        //   否则 prompt 里忌口菜和普通候选长一样(原未标忌口)、validate 只查 id∈candidates，模型可能把"忌口五花肉"选进给三高用户的一餐。
        //   把不变量落到代码层：模型只见非忌口候选、无从选起；忌口 UI 仍标红(fallback 也仅在全忌口时才不得已用它)。
        // 注：selectable=可喂模型集(仅排忌口)；下方 fallback 内的 normal=兜底优选层(排忌口+最近)，两者口径不同勿混。
        val selectable = candidates.filter { it.avoidNames.isEmpty() }

        val prompt = RecommendationPrompt.build(
            selectable, input.constraints, mealCount,
            style = input.style,
            preferenceScores = input.preferenceScores,
            nutritionBalanceScores = input.nutritionBalanceScores,
            tasteCuisines = input.tasteProfile.topCuisines(2), // [AI生成] R2:口味汇总喂云端(常吃前2菜系·空则不喂)
        )
        val raw = runCatching { runtime.complete(prompt) }.getOrNull()?.getOrNull()
        val modelSuggestions = raw
            ?.let { RecommendationParser.parse(it) }
            ?.let { validate(it, selectable, mealCount) } // [AI生成] R1:合法 id 用非忌口可选集
            ?.takeIf { it.isNotEmpty() }

        return if (modelSuggestions != null) {
            RecommendationResult(modelSuggestions, candidates, RecommendationSource.MODEL)
        } else {
            RecommendationResult(fallback(candidates, mealCount), candidates, RecommendationSource.RULE_FALLBACK)
        }
    }

    /** 校验模型输出：dishId 必须来自候选集、每餐 1~3 菜、截到 mealCount。[AI生成] */
    private fun validate(
        suggestions: List<MealSuggestion>,
        candidates: List<DishCandidate>,
        mealCount: Int,
    ): List<MealSuggestion> {
        val validIds = candidates.map { it.id }.toSet()
        return suggestions
            .map { it.copy(dishIds = it.dishIds.filter { id -> id in validIds }.distinct()) }
            .filter { it.dishIds.size in 1..MAX_DISHES_PER_MEAL }
            .take(mealCount)
    }

    /**
     * 按整批分页轮转候选：每批 DISPLAY_BATCH(10) 个，"换一换"取**下一批不重复**，全部推完后循环。[AI修改]
     *
     * rotation=0 取第 1 批(分数最优在前)；rotation=N 取第 (N mod 批数) 批。**本函数直接返回该批(≤10)**，
     * 使喂给模型 prompt/validate 的候选与 UI 展示的一批完全一致：既防 prompt 随候选总数膨胀，
     * 也防模型选到列表外(第 11+ 位)的菜。末批不足则少于 10。修旧"drop 未 take 致长尾整体入 prompt"。
     */
    private fun rotate(candidates: List<DishCandidate>, rotation: Int): List<DishCandidate> {
        if (candidates.size <= DISPLAY_BATCH) return candidates
        // [AI修改] 修"随机推荐翻出整批忌口菜"：忌口菜(avoidNames非空)被罚到列表末尾，
        // 而 RANDOM 模式 rotation 是随机数，rotation%batches 可能正好翻到全是忌口菜的末批。
        // 轮转批次只按**非忌口**候选数计算，start 落在可接受区；忌口菜仍保留在末尾(标红)，
        // 只会作为边界批的尾部零星出现，绝不会独占一整批被当推荐推出。
        val acceptable = candidates.indexOfFirst { it.avoidNames.isNotEmpty() }
            .let { if (it < 0) candidates.size else it }
            .coerceAtLeast(1)
        val batches = (acceptable + DISPLAY_BATCH - 1) / DISPLAY_BATCH
        val start = (rotation.coerceAtLeast(0) % batches) * DISPLAY_BATCH
        return candidates.drop(start).take(DISPLAY_BATCH)
    }

    /**
     * MMR 批内多样性重排。[AI生成] 算法评审#3.1
     *
     * 只在"正常菜层"（非最近、非忌口）内部按主料相似度贪心打散，避免同主料菜霸屏
     * （库存有五花肉→满屏五花肉菜）。最近/忌口层是分层末尾（红线，不能被多样性打乱），
     * 原样保留在尾部。λ=1.0（默认"综合/偏熟悉"）或候选≤2 时直接返回，不改分数序。
     * 每步选 `λ·相关度 −(1−λ)·与已选最大相似度` 最高者；相关度=层内 score 归一化，相似度=主料 Jaccard。
     * 纯确定性（无随机），tie-break 取先者，稳定可测。
     */
    private fun diversify(batch: List<DishCandidate>, lambda: Double): List<DishCandidate> {
        if (lambda >= 0.999 || batch.size <= 2) return batch
        val head = batch.takeWhile { !it.isRecent && it.avoidNames.isEmpty() } // 可重排的正常菜层
        if (head.size <= 2) return batch
        val tail = batch.drop(head.size) // 最近/忌口层：保持分层末位不动
        val maxScore = head.maxOf { it.score }
        val minScore = head.minOf { it.score }
        val span = (maxScore - minScore).takeIf { it > 1e-9 }
        fun rel(c: DishCandidate) = if (span == null) 1.0 else (c.score - minScore) / span
        val remaining = head.toMutableList()
        val selected = ArrayList<DishCandidate>(head.size)
        selected.add(remaining.removeAt(0)) // 头部(最高分)先入选，保证首位仍是最相关
        while (remaining.isNotEmpty()) {
            var best = remaining.first()
            var bestVal = Double.NEGATIVE_INFINITY
            for (c in remaining) {
                val sim = selected.maxOf { dishSimilarity(c, it) }
                val mmr = lambda * rel(c) - (1 - lambda) * sim
                if (mmr > bestVal) { bestVal = mmr; best = c }
            }
            selected.add(best)
            remaining.remove(best)
        }
        return selected + tail
    }

    /**
     * 两菜综合相似度[0,1]：主料 0.45 + 重油族 0.15 + 荤素 0.15 + 做法名 0.13 + 菜系 0.12。[AI修改] MMR 多样性扩维·重油族
     *
     * 主料仍主导(最强单调信号·≥0.45);其余维度让同批菜系/做法/荤素/油腻度错落。各维为空→该维不贡献(自然退化,无数据不误伤)。
     * **重油族维度**(新)：做法名 Jaccard 只在做法名完全相同时算相似(红烧≠干煸≠油煎)，感知不到"都很油"→
     *   加"油腻度族(清淡/中性/重油)相同则相似"，避免"主料/做法名各异却整批煎炸红烧"。两菜都有做法数据才比(空→退化不误判)。
     */
    private fun dishSimilarity(a: DishCandidate, b: DishCandidate): Double {
        val main = jaccard(a.mainNames.toSet(), b.mainNames.toSet())
        val cuisine = if (a.cuisine.isNotBlank() && a.cuisine == b.cuisine) 1.0 else 0.0
        val method = jaccard(a.cookingMethodNames.toSet(), b.cookingMethodNames.toSet())
        // [AI生成] D1：荤素结构维度——同为荤或同为素=相似，MMR 据此让同一批荤素交替(避免"主料不同但全是荤菜红烧"一批)。
        val protein = if (a.isMeat == b.isMeat) 1.0 else 0.0
        // [AI生成] 重油族维度：两菜都有做法数据、且油腻度族相同→相似(让 MMR 把"全煎炸红烧"打散)。空做法→退化 0(不误判)。
        val heavy = if (a.cookingMethodNames.isNotEmpty() && b.cookingMethodNames.isNotEmpty() &&
            cookingHeaviness(a.cookingMethodNames) == cookingHeaviness(b.cookingMethodNames)) 1.0 else 0.0
        return SIM_W_MAIN * main + SIM_W_CUISINE * cuisine + SIM_W_METHOD * method +
            SIM_W_PROTEIN * protein + SIM_W_HEAVY * heavy
    }

    /** 集合 Jaccard 相似度[0,1]：|交|/|并|。空∩空=0。[AI生成] */
    private fun jaccard(sa: Set<String>, sb: Set<String>): Double {
        if (sa.isEmpty() && sb.isEmpty()) return 0.0
        val inter = sa.count { it in sb }
        val union = sa.size + sb.size - inter
        return if (union == 0) 0.0 else inter.toDouble() / union
    }

    /**
     * 纯规则兜底：组合级贪心搭配成 mealCount 餐。[AI修改] A1
     *
     * 原实现按分数顺序 chunk(2)，一餐可能"两荤无素无主食"(营养最差组合)。
     * 改为：每餐第 1 道取当前最高分，第 2 道在剩余候选里选"补荤素缺口 / 补主食 / 分数"组合分最高者，
     * 让每餐尽量荤素搭配、尽量含主食(复用 PeriodPlanner 已验证的 BALANCE_BONUS/STAPLE_BONUS 系数)。
     * 只在"正常层"(非忌口非最近)内组合；全是忌口/最近时兜底用全部，保证不空。
     */
    private fun fallback(candidates: List<DishCandidate>, mealCount: Int): List<MealSuggestion> {
        val normal = candidates.filter { it.avoidNames.isEmpty() && !it.isRecent }
        val pool = (normal.ifEmpty { candidates }).toMutableList() // 全忌口/最近时兜底用全部(不空)
        val meals = ArrayList<MealSuggestion>(mealCount)
        repeat(mealCount) {
            if (pool.isEmpty()) return@repeat
            val chunk = ArrayList<DishCandidate>(FALLBACK_DISHES_PER_MEAL)
            chunk.add(pool.removeAt(0)) // 第 1 道：当前最高分(pool 已按分数序)；菜系族由首道定(中式优先已使首道多为中式)。
            val mealWestern = isWesternCuisine(chunk.first().cuisine)
            while (chunk.size < FALLBACK_DISHES_PER_MEAL && pool.isNotEmpty()) {
                // [AI生成] 用户#2:一餐同菜系不混搭——只在同族候选内补;同族耗尽→**宁可少一道也不混中西**(2道连贯>3道混搭)。
                val sameFamily = pool.filter { isWesternCuisine(it.cuisine) == mealWestern }
                if (sameFamily.isEmpty()) break
                // maxByOrNull 平局取先者(pool 已按 score 降序)→确定性可测；勿改成会打乱顺序的实现。
                val next = sameFamily.maxByOrNull { combineScore(it, chunk) } ?: break
                chunk.add(next)
                pool.remove(next)
            }
            meals.add(
                MealSuggestion(
                    dishIds = chunk.map { it.id },
                    reason = "用你现有食材可做：" + chunk.joinToString("、") { it.name },
                    cookingHint = chunk.firstOrNull()?.seasoningsOnHand
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { "在手调料：" + it.joinToString("、") },
                ),
            )
        }
        return meals
    }

    /** A1：候选加入本餐已选 [chosen] 的组合分=基础分 + 补荤素缺口 + 补主食。[AI修改] 组合补分收敛到 MealCompositionScorer(单一真相源)。 */
    private fun combineScore(cand: DishCandidate, chosen: List<DishCandidate>): Double {
        val meat = chosen.count { it.isMeat }
        val veg = chosen.count { !it.isMeat }
        // [AI修改] 荤素/主食补分抽到 MealCompositionScorer(与 PeriodPlanner 共用同一常量/逻辑,防调参漂移);combineScore 调用时 chosen 恒非空,行为不变。
        val composition = MealCompositionScorer.compositionBonus(
            candMeat = cand.isMeat, candStaple = cand.isStaple,
            chosenMeat = meat, chosenVeg = veg, chosenHasStaple = chosen.any { it.isStaple }, // [AI修改] 阻断修复:参数是"已含主食"(any),原误传 none 致主食补分方向反转(与 PeriodPlanner:any 对齐)
        )
        // [AI生成] 一餐内主料不重复：候选主料与本餐已选任一道有重叠→轻罚(防"一餐两道五花肉";真实吃法一餐主料尽量不同)。轻于 BALANCE,不压倒荤素/主食补齐。
        val candMains = cand.mainNames.toSet()
        val mainOverlap = if (candMains.isNotEmpty() && chosen.any { (it.mainNames.toSet() intersect candMains).isNotEmpty() }) MAIN_OVERLAP_PENALTY else 0.0
        return cand.score + composition - mainOverlap // [AI修改] 用户#2:一餐同菜系由 fallback 的同族过滤保证(不在此软罚,结构性硬保证不混搭)
    }

    companion object {
        const val DISPLAY_BATCH = 10 // [AI生成] 库存/随机推荐每批展示菜数；"换一换"取下一批不重复、全部推完循环。
        // [AI修改] MMR 相似度五维权重·和=1.0·主料主导(≥0.45最强单调信号)。加"重油族"维时从做法名/菜系匀出(它们是弱信号)。
        private const val SIM_W_MAIN = 0.45 // 主料(最强单调信号·主导)
        private const val SIM_W_HEAVY = 0.15 // [AI生成] 重油族(清淡/中性/重油·避免一批全煎炸红烧)
        private const val SIM_W_PROTEIN = 0.15 // [AI生成] D1:荤素结构(避免一批全荤/全素)
        private const val SIM_W_METHOD = 0.13 // 做法名 Jaccard(避免一批同做法名·重油族已覆盖粗油腻度故降)
        private const val SIM_W_CUISINE = 0.12 // 菜系(避免一批全川菜)
        // ↑ 0.45+0.15+0.15+0.13+0.12 = 1.00；MMRWeightSumTest 锁定和≈1.0 且主料最大,防后续加维失衡。
        private const val MAIN_OVERLAP_PENALTY = 0.5 // [AI生成] fallback 一餐内主料重叠轻罚(<BALANCE0.7)：防"一餐两道五花肉"(真实吃法一餐主料尽量不同)
        private const val DEFAULT_MEAL_COUNT = 3
        private const val MAX_DISHES_PER_MEAL = 3
        private const val FALLBACK_DISHES_PER_MEAL = 3 // [AI修改] QW-1:2→3,组合更完整(主食+荤+素),贴近一餐(combineScore已补荤素/主食缺口)
        // [AI修改] 组合级搭配补分(BALANCE_BONUS/STAPLE_BONUS)已收敛到 MealCompositionScorer(与 PeriodPlanner 共用单一真相源,防漂移)。
    }
}
