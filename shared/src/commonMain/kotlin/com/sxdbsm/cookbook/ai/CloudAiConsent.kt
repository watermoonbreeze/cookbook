package com.sxdbsm.cookbook.ai

import kotlinx.serialization.Serializable

/**
 * @File : CloudAiConsent
 * @Time : 2026/08/08
 * @Author : SXD-AI
 * @Desc : 云端 AI 数据外发同意状态（偏好 JSON 存储，免迁移）。蓝图 L1 §3/§4.4。
 * <p>
 * 覆盖"是否问过 × 是否同意 × 是否历史遗留"全部可区分取值（GC-36）：
 * `GRANDFATHER_PENDING` 是推导态不持久化——只要偏好里没有 consent 记录且检测到任一厂商已有非空 Key，
 * [cloudAiConsent] 每次读取都会重新推导出该值；用户一旦在设置页做显式选择才真正写入 GRANTED/DECLINED 落盘。
 * <p>
 * [AI生成] L1：云端 AI 首启同意 + 合规免责。
 **/
@Serializable
enum class ConsentStatus { NOT_ASKED, GRANTED, DECLINED, GRANDFATHER_PENDING }

/** 同意来源，仅供审计/展示区分，不参与任何行为判据。[AI生成] */
@Serializable
enum class ConsentSource { EXPLICIT_FIRST_ENABLE, GRANDFATHER_CONFIRMED }

/**
 * 云端 AI 同意状态。[AI生成]
 *
 * 默认值即 [ConsentStatus.NOT_ASKED] 语义——不得在任何路径把默认值改写成"已同意"或
 * 用"Key 非空"隐式推断同意（禁暗黑模式红线）。
 */
@Serializable
data class CloudAiConsent(
    val status: ConsentStatus = ConsentStatus.NOT_ASKED,
    val source: ConsentSource? = null,
    val grantedAtEpochSeconds: Long? = null, // [AI修改] v2：原 grantedAtEpochMs 是秒×1000 的假毫秒，改诚实的秒字段
    val scopeVersion: Int = 0,
    val acknowledgedVendors: Set<String> = emptySet(),
)

/** [AI生成] v2：SwitchableAiRuntime 用于表达"云端已选中但同意未满足"的内部信号；message 是可直接展示给用户的人话，
 *  不含任何内部代号——各消费点既有失败处理路径会把它当普通网络失败处理，天然不需要特殊识别这个异常类型，
 *  本类型存在只是为了让测试能精确断言"失败原因是同意未满足"而非猜测字符串。
 *  注意：message 为固定短文案（14 汉字），`AiMealInputViewModel.confirmHealthAdvice()` 有 `.take(120)` 截断——
 *  若未来 message 加长（如带厂商名），须保持 ≤120 字符且同步更新测试断言（Google 质量终审 🟡#2/#7）。 */
class CloudAiConsentRequiredException :
    Exception("还没有同意把数据发给云端 AI") // [AI修改] v2b：不预设"已使用规则推荐"这一结果——confirmHealthAdvice() 这个消费点
    // 没有规则兜底，看到这条消息时并不会真的生成什么规则版结果（蓝图 L1 §10 v2 挑战第5项），message 措辞必须对所有消费点都成立
