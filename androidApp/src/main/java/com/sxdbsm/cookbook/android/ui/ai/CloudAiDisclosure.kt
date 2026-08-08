package com.sxdbsm.cookbook.android.ui.ai

/**
 * @File : CloudAiDisclosure
 * @Time : 2026/08/08
 * @Author : SXD-AI
 * @Desc : 云端 AI 发送范围披露清单（同意面板/设置页状态块共用唯一真相源）。蓝图 L1 §4.4/§12。
 * <p>
 * 占位文案（已按 blueprint 定稿方向落），交 copywriter 审校定稿；`SCOPE_VERSION` 递增时须同步填 `INCREMENT_NOTES`
 * 的对应增量说明（外发范围扩大的透明披露）。[AI生成] L1。
 **/
object CloudAiDisclosure {
    const val SCOPE_VERSION = 1
    val WILL_SEND: List<String> = listOf(
        "在手食材名（比如“西红柿、鸡蛋”）", "粗略的健康标签（比如“忌高嘌呤”）", "候选菜名", "你在 AI 记一餐里输入的那句话",
    )
    val WONT_SEND: List<String> = listOf(
        "姓名、账号", "体检数值、病历、用药记录", "照片", "完整健康档案、历史餐食",
    )
    /** 外发范围扩大时的增量说明（SCOPE_VERSION 递增时随该次改动补齐）。[AI生成] L1 §12 预留空实现。 */
    val INCREMENT_NOTES: Map<Int, String> = emptyMap()
}
