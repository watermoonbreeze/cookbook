package com.sxdbsm.cookbook.data.parser

import com.sxdbsm.cookbook.domain.model.ParsedRecipe
import com.sxdbsm.cookbook.platform.CookbookDiag

/** 链接导入预填映射（纯函数集·可测性承载）。[AI生成] STEP-L4-9.1 */
object LinkPrefillMapper {

    /** shared 自有轻量图片引用（R-1：禁引 androidApp 的 StoredImagePair——VM 侧一行映射后传入）。 */
    data class StoredImageRef(val name: String, val thumbName: String)

    data class DraftStep(val text: String, val imageFile: String = "", val thumbFile: String = "")

    data class DraftIngredient(
        val name: String,
        val quantity: Double?,
        val unitId: Long?,
        val unitName: String,
        val ingredientId: Long,
        val isMain: Boolean = false,
    )

    /** androidApp 侧 NewDishPrefill 的 shared 镜像（VM 组装 DishIngredient 用·Y-6）。 */
    data class LinkPrefillDraft(
        val name: String,
        val ingredients: List<DraftIngredient>,
        val cookingMethodNames: List<String>,
        val steps: List<DraftStep>,
        val coverImage: String = "",
        val coverThumb: String = "",
        val description: String = "",
        val sourceTag: String = "user",
        val linkId: Long? = null,
    )

    /** URL 尾随中文标点集合（S-03）：分享文本里 URL 常被句读黏尾，逐字符剥离。 */
    private val TAIL_PUNCTUATION = setOf('。', '，', '！', '？', '；', '：', '」', '）')

    /**
     * 解析结果→预填草稿映射（方案 §4.4 单位映射表）。[AI生成] STEP-L4-9.1
     *
     * @param existingIdsByName 清洗后食材名→已有真实 id（调用方按名查库）
     * @param pendingSeq 库外名占位负 id 发号器（递减）
     * @param unitIdsByName 单位名→id（含 "g"；计件单位按名查）
     * @param unitNamesByName 单位名→展示名（与 unitIdsByName 同源）
     * @param defaultGramFor (食材名)->默认克数（调料 3/10g、普通 100g·SeasoningDefaults 口径）
     * @param images 远程图 URL→已下载本地文件引用（未下载/失败不含或值为 null）
     */
    fun mapToPrefill(
        recipe: ParsedRecipe,
        existingIdsByName: Map<String, Long>,
        pendingSeq: () -> Long,
        unitIdsByName: Map<String, Long>,
        unitNamesByName: Map<String, String>,
        defaultGramFor: (String) -> Int,
        images: Map<String, StoredImageRef?>,
        sourceTag: String = "user",
        linkId: Long? = null,
    ): LinkPrefillDraft {
        val gId = unitIdsByName["g"]
        val gName = unitNamesByName["g"] ?: "g"
        var pendingCount = 0
        var nullQuantityCount = 0
        val drafts = recipe.ingredients.map { pi ->
            val existingId = existingIdsByName[pi.name]
            if (existingId == null) pendingCount++
            val ingredientId = existingId ?: pendingSeq()
            val mapped: Triple<Double?, Long?, String> = when (pi.unit) {
                "克", "g" -> Triple(pi.quantity, gId, gName)
                "斤" -> Triple(pi.quantity * 500, gId, gName)
                "两" -> Triple(pi.quantity * 50, gId, gName)
                "毫升", "ml" -> Triple(pi.quantity, gId, gName) // 近似 1:1
                "升", "L" -> Triple(pi.quantity * 1000, gId, gName)
                "个", "只", "根", "片" ->
                    // 计件单位按名查字典；查不到落克+食材默认克数兜底
                    unitIdsByName[pi.unit]?.let { uid ->
                        Triple(pi.quantity, uid, unitNamesByName[pi.unit] ?: pi.unit)
                    } ?: Triple(defaultGramFor(pi.name).toDouble(), gId, gName)
                // [AI修改] 终审 S-3：勺类数量参与折算（「生抽 2勺」=2×每勺克数；1 勺结果不变=兼容既有断言）。
                "勺", "汤匙", "茶匙" -> Triple(
                    (if (pi.quantity > 0.0) pi.quantity else 1.0) * defaultGramFor(pi.name).toDouble(),
                    gId,
                    gName,
                )
                // [AI修改] 半勺的 quantity 恒 0.0（parseQuantity 对"半"前缀返 0）不能相乘——保持半折特判。
                "半勺" -> Triple(defaultGramFor(pi.name) * 0.5, gId, gName)
                // 适量/少许/空 → quantity=null（红牌·INV-L4-18），让编辑器显式提示用户补数值
                else -> Triple(null, gId, gName)
            }
            if (mapped.first == null) nullQuantityCount++
            DraftIngredient(pi.name, mapped.first, mapped.second, mapped.third, ingredientId, pi.isMain)
        }
        val stepDrafts = recipe.steps.map { s ->
            images[s.imageUrl]?.let { DraftStep(s.text, it.name, it.thumbName) } ?: DraftStep(s.text)
        }
        // 封面：imageUrl 为空串时 map 无命中 → cover 为 null → 空串
        val cover = images[recipe.imageUrl]
        val coverImage = cover?.name ?: ""
        val coverThumb = cover?.thumbName ?: ""
        // [AI生成] 规避 Kotlin 1.9.20 IR backend 崩溃("Exception during IR lowering")：CookbookDiag.log 为 inline，
        // 其 lambda 不直接捕获 map 内被修改的 var（Ref 装箱触发编译器后端异常）——先快照进 val 再进日志 lambda。
        val ingredientCount = drafts.size
        val stepCount = stepDrafts.size
        val pendingSnapshot = pendingCount
        val nullQtySnapshot = nullQuantityCount
        val hasCover = coverImage.isNotBlank()
        CookbookDiag.log(TAG) {
            "map_result ingredient_count=$ingredientCount pending_count=$pendingSnapshot " +
                "null_quantity_count=$nullQtySnapshot step_count=$stepCount has_cover=$hasCover"
        }
        return LinkPrefillDraft(recipe.title, drafts, recipe.cookingMethods, stepDrafts, coverImage, coverThumb, recipe.note, sourceTag, linkId)
    }

    /** 从分享文本提取首个 URL（尾随中文标点「。，！？；：」）」剥离）。[AI生成] STEP-L4-9.1·S-03 */
    fun extractUrl(extraText: String): String? {
        var url = Regex("https?://[^\\s]+").find(extraText)?.value ?: return null
        while (url.isNotEmpty() && url.last() in TAIL_PUNCTUATION) {
            url = url.dropLast(1)
        }
        return url.ifEmpty { null }
    }

    /** 横幅四态纯函数（S-08）。[AI生成] STEP-L4-9.1 */
    fun shouldShowLinkBanner(count: Int, dismissed: Boolean, lastShown: String, today: String): Boolean {
        return !dismissed && count > 0 && lastShown != today
    }

    private const val TAG = "LinkMap"
}
