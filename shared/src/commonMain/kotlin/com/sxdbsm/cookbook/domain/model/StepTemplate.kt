package com.sxdbsm.cookbook.domain.model

/**
 * @File : StepTemplate
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 操作步骤模板（可复用的做法步骤集合）
 * <p>
 * 很多菜做法步骤相近，把常用步骤存成模板，编辑菜品时"选择步骤"一键套用。仅存文字步骤，
 * 图片仍按每道菜单独添加。source=preset(内置不可删)/user(自建可删)。
 * <p>
 * [AI生成] #2：降低菜品录入成本的复用能力。
 **/
data class StepTemplate(
    val id: Long = 0,
    val name: String,
    val source: String = "user", // preset / user
    val steps: List<String> = emptyList(), // 每步文字
) {
    val isPreset: Boolean get() = source == "preset"
}
