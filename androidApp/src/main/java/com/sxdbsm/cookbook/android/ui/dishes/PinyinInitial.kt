package com.sxdbsm.cookbook.android.ui.dishes

/**
 * @File : PinyinInitial
 * @Time : 2026/05/30
 * @Author : SXD-AI
 * @Desc : 菜品首字母索引工具（薄委托）
 * <p>
 * 拼音首字母逻辑已提取到通用组件 `ui/component/PinyinIndex.kt(pinyinInitial)` 供跨列表复用；
 * 本处保留 `dishInitial` 名称向后兼容菜品页调用。
 * <p>
 * [AI修改] 提取通用 pinyinInitial 后改为委托，避免逻辑双份。
 **/
internal fun dishInitial(name: String): String =
    com.sxdbsm.cookbook.android.ui.component.pinyinInitial(name)
