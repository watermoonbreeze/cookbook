package com.sxdbsm.cookbook.ai

/**
 * @File : FreePairingEngine
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 食材自由搭配（离线规则轻搭配）
 * <p>
 * 不依赖已有菜品、不依赖 AI：按在手食材的类别(荤/素/蛋/豆/主食)与在手调味，用模板规则拼出
 * "食材组合 + 建议做法"的轻搭配提示（非完整菜谱，无精确步骤/用量），离线可用，物尽其用。
 * 纯逻辑、可单测。
 * <p>
 * [AI生成] 待办"自由搭配"一期：离线规则轻搭配。
 **/

/** 搭配用的食材角色。[AI生成] */
enum class PairRole { PROTEIN, EGG, VEGETABLE, BEAN, STAPLE, SEASONING, OTHER }

/** 一味在手食材(名+角色)。[AI生成] */
data class PairIngredient(val name: String, val role: PairRole)

/** 一条搭配建议：用到的食材 + 建议做法 + 一句提示。[AI生成] */
data class PairingSuggestion(val items: List<String>, val method: String, val hint: String)

object FreePairingEngine {
    /**
     * 按在手食材生成轻搭配建议。[AI生成]
     *
     * 优先 荤×素、蛋×素、豆×素；无荤时给纯素/主食搭配。做法按在手调味推断。
     * @param maxSuggestions 最多返回条数。
     */
    fun suggest(pantry: List<PairIngredient>, maxSuggestions: Int = 8): List<PairingSuggestion> {
        val proteins = pantry.filter { it.role == PairRole.PROTEIN }
        val eggs = pantry.filter { it.role == PairRole.EGG }
        val veggies = pantry.filter { it.role == PairRole.VEGETABLE }
        val beans = pantry.filter { it.role == PairRole.BEAN }
        val staples = pantry.filter { it.role == PairRole.STAPLE }
        val seasonings = pantry.filter { it.role == PairRole.SEASONING }.map { it.name }.toSet()

        val out = LinkedHashSet<PairingSuggestion>() // 去重(同组合同做法只留一条)
        fun add(items: List<String>, method: String, hint: String): Boolean {
            out += PairingSuggestion(items, method, hint)
            return out.size >= maxSuggestions
        }

        // 荤 × 素：主搭配
        for (p in proteins) for (v in veggies) {
            val m = methodFor(hasProtein = true, seasonings)
            if (add(listOf(p.name, v.name), m, "${p.name} 配 ${v.name}，${m}即可")) return out.toList()
        }
        // 蛋 × 素
        for (e in eggs) for (v in veggies) {
            if (add(listOf(e.name, v.name), "炒", "${e.name} 与 ${v.name} 同炒，家常快手")) return out.toList()
        }
        // 豆制品 × 素/荤
        for (b in beans) {
            val partner = veggies.firstOrNull()?.name ?: proteins.firstOrNull()?.name
            val items = listOfNotNull(b.name, partner)
            val m = methodFor(hasProtein = proteins.isNotEmpty(), seasonings)
            if (add(items, m, "${b.name} 搭配$m，补充植物蛋白")) return out.toList()
        }
        // 无荤蛋豆：纯素搭配(两两)或素+主食
        if (proteins.isEmpty() && eggs.isEmpty() && beans.isEmpty()) {
            for (i in veggies.indices) for (j in i + 1 until veggies.size) {
                if (add(listOf(veggies[i].name, veggies[j].name), "清炒", "两样素菜同炒，清爽")) return out.toList()
            }
            for (v in veggies) for (s in staples) {
                if (add(listOf(s.name, v.name), "煮", "${s.name} 配 ${v.name}，简单一餐")) return out.toList()
            }
        }
        return out.toList()
    }

    /** 按在手调味推断做法。[AI生成] */
    private fun methodFor(hasProtein: Boolean, seasonings: Set<String>): String = when {
        seasonings.any { it.contains("豆瓣") } -> "爆炒"
        seasonings.any { it.contains("老抽") || it.contains("蚝油") } && hasProtein -> "红烧"
        seasonings.any { it.contains("生抽") } -> "炒"
        seasonings.any { it.contains("醋") } -> if (hasProtein) "炒" else "醋溜"
        else -> "清炒"
    }
}
