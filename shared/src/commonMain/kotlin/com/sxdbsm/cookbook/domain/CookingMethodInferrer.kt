package com.sxdbsm.cookbook.domain

/**
 * @File : CookingMethodInferrer
 * @Time : 2026/09/03
 * @Author : SXD-AI
 * @Desc : 菜名 → 烹饪方式关键词 推断（AUTOGEN-UNIFY STEP-AU-6·从 RuleMealParser 抽出的共享组件）
 * <p>
 * 单字包含匹配（菜名含任一方式字即命中），字表与 RuleMealParser 原实现逐字一致（行为等价·回归锁定）。
 * 消费方：RuleMealParser（规则解析引擎·委托）、NewDishViewModel（菜名自动预选做法·仅取与字典交集）。
 * 注意：返回的是**单字**（如"烧"），消费方自行做与烹饪方式字典（炒/蒸/煮/炖/烤/凉拌/煎/炸/焖/卤）的
 * 名交集过滤——「拌」不在字典（字典名是"凉拌"）、烧/熘/焗/烩/涮/煲/炝/熬 共 8 字不在预设字典。
 * <p>
 * [AI生成] AUTOGEN-UNIFY：抽共享防双份逻辑漂移（此前仅规则引擎私有）。
 **/
object CookingMethodInferrer {

    /** 烹饪方式关键词字表（冻结值·与 RuleMealParser 原字表逐字一致；改动须按 GC-26 登记修订记录）。 */
    private val METHODS = listOf("炒", "煮", "蒸", "炸", "煎", "烤", "炖", "拌", "烧", "焖", "卤", "熘", "焗", "烩", "涮", "煲", "炝", "熬")

    /** 从菜名中识别烹饪方式关键词（菜名含任一字即命中，保字表序）。[AI生成] */
    fun inferFromName(dishName: String): List<String> = METHODS.filter { dishName.contains(it) }
}
