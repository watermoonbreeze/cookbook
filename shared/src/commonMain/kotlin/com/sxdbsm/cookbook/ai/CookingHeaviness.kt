package com.sxdbsm.cookbook.ai

/**
 * @File : CookingHeaviness
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 烹饪方式"重油/清淡"族群判定（纯烹饪常识分类·非健康断言）
 * <p>
 * 供推荐多样性打散用：一批菜可能主料/菜系/做法名各异，却全是"煎炸红烧"重口——做法名 Jaccard 只在
 * 做法名**完全相同**时算相似（红烧≠干煸≠油煎），感知不到"都很油"。本函数把做法名归到粗族群
 * （清淡/中性/重油），让 MMR 据此让同一批油腻度错落。**纯烹饪常识、非营养/健康断言**，不触免责红线；
 * 无做法数据→中性（不误判）。
 * <p>
 * [AI生成] 算法打磨·MMR 重油族维度：防"一批全煎炸红烧"。
 **/

private val HEAVY_KEYWORDS = listOf("炸", "煎", "红烧", "干煸", "干炸", "油焖", "油淋", "酥", "爆")
private val LIGHT_KEYWORDS = listOf("蒸", "灼", "煮", "炖", "焯", "汆", "拌", "卤")

/**
 * 一组做法名的油腻度族：0=清淡族 / 1=中性(默认/无匹配) / 2=重油族。[AI生成]
 *
 * 一道菜多做法时取**最油**的一味（任一重油→整体偏重油；否则任一清淡→清淡；余为中性）。
 * 做法名为空→中性（无数据不误判）。命中优先级：重油 > 清淡 > 中性。
 */
fun cookingHeaviness(methodNames: List<String>): Int {
    if (methodNames.isEmpty()) return 1
    var sawLight = false
    for (m in methodNames) {
        if (HEAVY_KEYWORDS.any { m.contains(it) }) return 2 // 任一重油→整体重油
        if (LIGHT_KEYWORDS.any { m.contains(it) }) sawLight = true
    }
    return if (sawLight) 0 else 1
}
