package com.sxdbsm.cookbook.data.parser

import com.sxdbsm.cookbook.domain.CookingMethodInferrer
import com.sxdbsm.cookbook.domain.model.IngredientsCfg
import com.sxdbsm.cookbook.domain.model.NoteCfg
import com.sxdbsm.cookbook.domain.model.ParseConfig
import com.sxdbsm.cookbook.domain.model.ParsedIngredient
import com.sxdbsm.cookbook.domain.model.ParsedRecipe
import com.sxdbsm.cookbook.domain.model.ParsedStep
import com.sxdbsm.cookbook.domain.model.StepsCfg
import com.sxdbsm.cookbook.domain.model.TitleCfg
import com.sxdbsm.cookbook.platform.CookbookDiag

/**
 * 菜谱纯文本解析器（无状态纯函数·方案 §3.2 行配对算法）。[AI生成] STEP-L4-2.4
 *
 * 输入=WebView 层 ExtractedPage + 配置 + 烹饪方式字典名列表；输出=ParsedRecipe。
 * 烹饪方式=菜名+步骤全文合并跑 CookingMethodInferrer 字表后与传入字典交集，**按传入字典序**输出。
 */
class RecipeParser {

    fun parse(
        page: ExtractedPage,
        config: ParseConfig,
        availableCookingMethodNames: List<String>,
    ): ParsedRecipe {
        val title = page.title.ifBlank { fallbackTitle(page.innerText, config.title) }
        val ingredients = parseIngredients(page.innerText, config.ingredients)
        val steps = parseSteps(page.innerText, config.steps, page.stepImages)
        val inferred = if (config.cookingMethods.fromTitle || config.cookingMethods.fromSteps) {
            // [AI生成] §3.4：菜名常无方式字(汤/羹类)，步骤文本是更可靠来源——合并跑字表。
            CookingMethodInferrer.inferFromName(title + steps.joinToString("") { it.text })
        } else emptyList()
        val cookingMethods = availableCookingMethodNames.filter { it in inferred } // 交集+按传入字典序
        val note = extractNote(page.innerText, config.note)
        CookbookDiag.log(TAG) {
            "parse_result title_blank=${title.isBlank()} ingredient_count=${ingredients.size} " +
                "step_count=${steps.size} method_count=${cookingMethods.size} main_count=${ingredients.count { it.isMain }}"
        }
        return ParsedRecipe(title, ingredients, steps, cookingMethods, note, page.imageUrl)
    }

    /**
     * 用料区行配对解析：奇数行=名、紧随整行命中 quantityPattern 的行为量行。[AI生成] STEP-L4-2.4
     * 名行后无量行也单独成项（quantity=0/unit=""）；清洗后名为空的行跳过。
     */
    private fun parseIngredients(text: String, cfg: IngredientsCfg): List<ParsedIngredient> {
        val lines = sectionBetween(text, cfg.sectionStart, cfg.sectionEnd)
        val qtyRe = Regex(cfg.quantityPattern)
        val result = mutableListOf<ParsedIngredient>()
        var i = 0
        while (i < lines.size) {
            val name = cleanIngredientName(lines[i])
            if (name.isEmpty()) { // 全括号说明等清洗后为空的行不成项
                i += 1
                continue
            }
            val qtyLine = lines.getOrNull(i + 1)
            if (qtyLine != null && qtyRe.find(qtyLine.trim()) != null) {
                val q = parseQuantity(qtyLine)
                val u = parseUnit(qtyLine)
                // [AI生成] DP-P1-11：isMain 折算口径见 toGrams——parse 层无计件克数字典，Phase2 接 piece_gram 再精化。
                result.add(ParsedIngredient(name, q, u, isMain = toGrams(q, u) >= 100.0))
                i += 2
            } else {
                result.add(ParsedIngredient(name))
                i += 1
            }
        }
        return result
    }

    /**
     * 取 startPattern 首个命中行之后、endPattern 首个命中行（在起点之后找）之前的行列表。[AI生成]
     * 去空行（trim 后非空保留原行内容——配对按原行）；任一界找不到返回空列表。
     */
    private fun sectionBetween(text: String, startPattern: String, endPattern: String): List<String> {
        val lines = text.lines()
        val startRe = Regex(startPattern)
        val endRe = Regex(endPattern)
        val startIdx = lines.indexOfFirst { startRe.find(it) != null }
        if (startIdx < 0) return emptyList()
        val endRel = lines.drop(startIdx + 1).indexOfFirst { endRe.find(it) != null }
        if (endRel < 0) return emptyList()
        return lines.subList(startIdx + 1, startIdx + 1 + endRel).filter { it.trim().isNotEmpty() }
    }

    /**
     * 按整行命中 markerPattern 的行切块：每块=标记行之后到下一标记行/列表尾，joinToString("\n")。[AI生成]
     * 首标记前的内容（无主归属）丢弃；无任何标记行返回空列表。
     */
    private fun splitByMarkers(lines: List<String>, markerPattern: String): List<String> {
        val markerRe = Regex(markerPattern)
        val blocks = mutableListOf<String>()
        var current: MutableList<String>? = null
        for (line in lines) {
            if (markerRe.find(line.trim()) != null) {
                current?.let { blocks.add(it.joinToString("\n")) }
                current = mutableListOf()
            } else {
                current?.add(line)
            }
        }
        current?.let { blocks.add(it.joinToString("\n")) }
        return blocks
    }

    /** 步骤区解析：文本分块后与 stepImages 按下标配对（缺图空串、多余图静默丢弃·INV-L4-03）。[AI生成] */
    private fun parseSteps(text: String, cfg: StepsCfg, stepImages: List<String>): List<ParsedStep> =
        splitByMarkers(sectionBetween(text, cfg.sectionStart, cfg.sectionEnd), cfg.itemStart)
            .mapIndexed { i, t -> ParsedStep(t, stepImages.getOrNull(i) ?: "") }

    /** 食材名清洗：去掉中英文括号及其内说明，再 trim。[AI生成] STEP-L4-2.4 */
    private fun cleanIngredientName(raw: String): String =
        raw.replace(BRACKET_NOISE, "").trim()

    /** 量行数值：首个数字组 toDouble；"半勺"/"适量/少许"等无数字行一律 0.0（简单口径·合同冻结）。[AI生成] */
    private fun parseQuantity(qtyLine: String?): Double {
        if (qtyLine == null) return 0.0
        return Regex("\\d+(?:\\.\\d+)?").find(qtyLine)?.value?.toDouble() ?: 0.0
    }

    /** 量行单位："适量/少许"返回整行、"半"开头返回整行（如"半勺"）、否则取行尾单位词，无则空串。[AI生成] */
    private fun parseUnit(qtyLine: String?): String {
        if (qtyLine == null) return ""
        val line = qtyLine.trim()
        if (Regex("^(适量|少许)$").find(line) != null) return line
        if (line.startsWith("半")) return line
        return Regex("(克|g|斤|两|个|只|根|片|勺|汤匙|茶匙|毫升|ml|升|L)$").find(line)?.value ?: ""
    }

    /**
     * 单位→克粗折算（DP-P1-11）：克/毫升=×1、斤=×500、两=×50、升=×1000；[AI生成] STEP-L4-2.4
     * 计件（个/只/根/片/勺类）与"适量/少许/空"折算=0.0 → isMain=false（parse 层无每单位克数字典，Phase2 接 piece_gram 再精化）。
     */
    private fun toGrams(quantity: Double, unit: String): Double = when (unit) {
        "克", "g" -> quantity
        "毫升", "ml" -> quantity // 近似 1:1
        "斤" -> quantity * 500.0
        "两" -> quantity * 50.0
        "升", "L" -> quantity * 1000.0
        else -> 0.0
    }

    /**
     * 标题兜底：取"用料"界线之前的行，过滤噪音后取最后一个非空行再套 cleanPattern 提取组1。[AI生成] STEP-L4-2.4
     * 过滤顺序：`^作者` 行（含其紧随的作者值行）优先于通用噪音表——xiachufang.json 的 fallbackNoiseFilter
     * 本身含 "^作者"，若通用表先判会把作者行当普通噪音跳过、"跳过作者值行"永不生效。
     */
    private fun fallbackTitle(text: String, cfg: TitleCfg): String {
        val lines = text.lines()
        val boundaryIdx = lines.indexOfFirst { Regex("^用料$").find(it) != null }
        val scope = if (boundaryIdx >= 0) lines.take(boundaryIdx) else lines
        val noiseRes = cfg.fallbackNoiseFilter.map { Regex(it) }
        val kept = mutableListOf<String>()
        var skipNext = false
        for (line in scope) {
            if (skipNext) { // 作者值行紧随作者行，一并丢弃
                skipNext = false
                continue
            }
            val t = line.trim()
            if (t.isEmpty()) continue
            if (Regex("^作者").find(t) != null) {
                skipNext = true
                continue
            }
            if (noiseRes.any { it.find(t) != null }) continue
            if (t.length > 30) continue // 碎碎念长行非标题
            kept.add(t)
        }
        val last = kept.lastOrNull() ?: return ""
        return Regex(cfg.cleanPattern).find(last)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    /** 小贴士提取：between[0] 行之后（跳过紧随的作者值行 1 行）到 between[1] 行之前的非空行，超 maxLines 截断。[AI生成] */
    private fun extractNote(text: String, cfg: NoteCfg?): String {
        if (cfg == null || cfg.between.size < 2) return ""
        val lines = text.lines()
        val startRe = Regex(cfg.between[0])
        val endRe = Regex(cfg.between[1])
        val startIdx = lines.indexOfFirst { startRe.find(it) != null }
        if (startIdx < 0) return ""
        val endRel = lines.drop(startIdx + 1).indexOfFirst { endRe.find(it) != null }
        if (endRel < 0) return ""
        val endIdx = startIdx + 1 + endRel
        // [AI修改] 终审 R-1：作者行与用料行直接相邻（作者值行缺失·匿名/渲染差异）时 from>endIdx，
        //   subList 会抛 IllegalArgumentException 使进程崩溃——先判界再取（输入是不可控的任意页面文本）。
        val from = startIdx + 2 // +2：跳过作者行与紧随的作者值行
        if (from > endIdx) return ""
        return lines.subList(from, endIdx)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(cfg.maxLines)
            .joinToString("\n")
    }

    private companion object {
        private const val TAG = "LinkParse"
        private val BRACKET_NOISE = Regex("[（(][^）)]*[）)]")
    }
}
