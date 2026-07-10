package com.sxdbsm.cookbook.domain.model

/**
 * @File : Cuisine
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 菜系分类常量
 * <p>
 * 参考中国传统八大菜系（川鲁粤苏闽浙湘徽，公认餐饮常识分类）+ 家常菜/西餐/其他。
 * 供预设菜归类、创建/编辑菜系选择器、列表按菜系筛选与 seed 引用完整性校验共用。
 * 归属为便于筛选的参考，可能存在地域交叉，非官方权威认定。
 * <p>
 * [AI生成] 菜品菜系(独立字段)。
 **/
object Cuisines {
    const val HOME = "家常菜"

    /** 可选菜系全集（含家常菜；空字符串表示未分类，不在此列）。 */
    val ALL: List<String> = listOf(
        HOME, "川菜", "粤菜", "鲁菜", "苏菜", "闽菜", "浙菜", "湘菜", "徽菜", "西餐", "其他",
    )
}
