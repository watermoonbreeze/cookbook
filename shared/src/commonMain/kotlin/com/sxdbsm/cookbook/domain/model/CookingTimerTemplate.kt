package com.sxdbsm.cookbook.domain.model

/**
 * @File : CookingTimerTemplate
 * @Time : 2026/06/12
 * @Author : SXD-AI
 * @Desc : 烹饪计时模板领域模型
 * <p>
 * 保存用户常用的烹饪倒计时配置。运行态、暂停态、响铃态只属于页面临时状态，不进入数据库。
 * <p>
 * [AI生成] 用户要求烹饪计时可本地保存，便于下次烹饪直接复用。
 **/
data class CookingTimerTemplate(
    val id: Long = 0,
    val name: String,
    val durationSeconds: Int,
    val note: String = "",
    val ringtoneUri: String = "",
    val ringtoneTitle: String = "系统默认铃声",
    val sortOrder: Int = 0,
)
