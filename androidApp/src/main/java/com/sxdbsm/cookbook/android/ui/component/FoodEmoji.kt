package com.sxdbsm.cookbook.android.ui.component

/**
 * 根据食材名称匹配默认 emoji。[AI生成]
 *
 * 只用于无图片时的占位展示；匹配不到时返回通用食物 emoji。
 */
fun foodEmojiForName(name: String): String {
    val text = name.lowercase()
    return when {
        text.contains("西红柿") || text.contains("番茄") -> "🍅"
        text.contains("黄瓜") || text.contains("青瓜") -> "🥒"
        text.contains("胡萝卜") || text.contains("红萝卜") -> "🥕"
        text.contains("土豆") || text.contains("马铃薯") -> "🥔"
        text.contains("青菜") || text.contains("白菜") || text.contains("生菜") || text.contains("菠菜") -> "🥬"
        text.contains("葱") -> "🧅"
        text.contains("蒜") -> "🧄"
        text.contains("辣椒") || text.contains("椒") -> "🌶"
        text.contains("蘑菇") || text.contains("香菇") -> "🍄"
        text.contains("米饭") || text.contains("饭") -> "🍚"
        text.contains("面") -> "🍜"
        text.contains("鸡蛋") || text.contains("蛋") -> "🥚"
        text.contains("牛奶") || text.contains("奶") -> "🥛"
        text.contains("豆腐") || text.contains("豆") -> "🫘"
        text.contains("猪") || text.contains("肉") -> "🥩"
        text.contains("鸡") -> "🍗"
        text.contains("鱼") -> "🐟"
        text.contains("虾") -> "🦐"
        text.contains("盐") -> "🧂"
        text.contains("油") -> "🫒"
        else -> "🍽"
    }
}
