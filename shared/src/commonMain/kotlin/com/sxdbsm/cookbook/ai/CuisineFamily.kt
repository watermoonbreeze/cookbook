package com.sxdbsm.cookbook.ai

/**
 * @File : CuisineFamily
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 菜系族群判定——"一餐同菜系不混搭 + 中式优先"（用户2026-07-20真机反馈#2）
 * <p>
 * 用户诉求：推荐/规划一餐时别中西混搭（如"排骨汤 + 帕尼尼"不是正常吃法）；国内为主、中式推荐优先、西式排后。
 * 中式=家常菜 + 八大菜系等（默认）；西式="西餐"。空/未知按**中式**处理（国内为主、不误罚）。
 * <p>
 * [AI生成] 纯函数、可单测。菜系连贯与中式优先在推荐评分层落地，不改忌口/健康红线。
 **/

/** 该菜系是否西式(西餐)。空/未知→false(按中式)。[AI生成] */
fun isWesternCuisine(cuisine: String): Boolean {
    val c = cuisine.trim()
    return c == "西餐" || c == "西式"
}

/** 两菜系是否同族（都西式或都中式）——一餐尽量同族不混搭。[AI生成] */
fun sameCuisineFamily(a: String, b: String): Boolean = isWesternCuisine(a) == isWesternCuisine(b)
