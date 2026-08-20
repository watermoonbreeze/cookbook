package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.ai.AiRuntimeType
import com.sxdbsm.cookbook.ai.ConsentSource
import com.sxdbsm.cookbook.ai.ConsentStatus
import com.sxdbsm.cookbook.ai.DeviceAiGrade
import com.sxdbsm.cookbook.android.ai.DeviceAiCapability
import com.sxdbsm.cookbook.android.ai.DeviceAiReport
import androidx.compose.ui.platform.LocalContext
import com.sxdbsm.cookbook.util.DateTime
import org.koin.androidx.compose.koinViewModel

/**
 * @File : AiSettingsScreen
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : AI 设置页（三档来源 + 云端选模型 + 按厂商 Key 设置/编辑）
 * <p>
 * [AI修改] 云端可下拉选择具体模型(多厂商)，未配置 Key 弹框输入、已配置可编辑；改动即时生效。
 * [AI修改] L1：云端 AI 首启同意 + 常驻状态块 + 关闭/重新启用，宿主直接管理多个独立弹层。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    vm: AiSettingsViewModel = koinViewModel(),
) {
    val state = vm.state
    var keyDialogOpen by remember { mutableStateOf(false) }
    var guideOpen by remember { mutableStateOf(false) } // [AI生成] "如何申请密钥"指南 sheet 开关
    // [AI修改] L1 v2b：改用 remember（非 rememberSaveable）——API Key 草稿不应落进 SavedStateRegistry/Bundle 持久化存储，
    //   否则进程被杀后系统可能把明文密钥暂存到磁盘（蓝图 §10 v2 挑战第20项）；代价是配置变更/进程重建会丢草稿，可接受。
    var pendingKeyDraft by remember { mutableStateOf("") }
    var vendorConfirmOpen by remember { mutableStateOf(false) }
    var consentPanelOpen by remember { mutableStateOf(false) }
    var grandfatherPanelOpen by remember { mutableStateOf(false) }
    var readonlyPanelOpen by remember { mutableStateOf(false) }
    var closeSheetOpen by remember { mutableStateOf(false) }
    var pendingSnackbar by remember { mutableStateOf<String?>(null) }
    // [AI修改] L1 v2b：hoist 在 Composable 作用域读取（CompositionLocal.current 是 @Composable getter，
    //   不能在 LaunchedEffect 的 suspend block 里直接读——蓝图 §10 v2 挑战第10项）；类型可空（AppSnackbar.kt:51）。
    val snackbar = com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar.current
    val context = LocalContext.current

    // [AI修改] L1 v2b：以 state.loaded 为 key（reload() 完成后从 false 翻 true 触发）；额外三弹层互斥守卫，
    //   避免冷启动首屏 loaded 翻转之前用户已手动打开其他弹层时与 grandfather 面板同屏堆叠（蓝图 §10 v2 挑战第12项）。
    LaunchedEffect(state.loaded, keyDialogOpen, consentPanelOpen, vendorConfirmOpen) {
        if (state.loaded && state.cloudAiConsent.status == ConsentStatus.GRANDFATHER_PENDING &&
            !keyDialogOpen && !consentPanelOpen && !vendorConfirmOpen
        ) grandfatherPanelOpen = true
    }
    // [AI修改] L1：宿主级 pendingSnackbar——弹层关闭后的下一次重组展示，不撞 Dialog 遮挡（蓝图 §10 C-13）。
    pendingSnackbar?.let { msg ->
        LaunchedEffect(msg) { snackbar?.showMessage(msg); pendingSnackbar = null }
    }

    Scaffold(
        // [AI修改] D2 家族化卡化：灰底(Scaffold 默认 background)+ contentWindowInsets 0(与设置族 FeatureSettingsScreen 一致)。
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            // [AI修改] B-8(§9.15)：带返回二级页统一 AppTopBar 收敛(原用默认 surface 色,收敛后与全局一致)。
            com.sxdbsm.cookbook.android.ui.component.AppTopBar(
                title = "AI 设置",
                onBack = onBack,
            )
        },
    ) { padding ->
        // [AI修改] D2：三档来源收敛进单个 InsetGroup 白卡(对齐设置族)；外层 Column 去 padding(16)、由卡/页脚各自内缩。
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            com.sxdbsm.cookbook.android.ui.component.InsetGroup(title = "模型来源") {
                // —— 云端 ——（选中即展开云端子块，条件 if 插入=安全，禁 early return·崩溃红线）
                RuntimeRow("云端大模型", AiRuntimeType.CLOUD, state.type, enabled = true) { vm.onTypeChange(it) }
                if (state.type == AiRuntimeType.CLOUD) {
                    CloudSection(
                        state = state,
                        onSelectModel = { vm.onSelectModel(it) },
                        onEditKey = { keyDialogOpen = true },
                        onShowGuide = { guideOpen = true },
                        onShowStatusDetail = { readonlyPanelOpen = true }, // [AI修改] L1："发送哪些内容"
                        onCloseCloudAi = { closeSheetOpen = true }, // [AI修改] L1：常驻状态块"关闭"
                        onReenableConsent = {
                            val draft = reenableKeyDraft(state.keyByVendor, vm.selectedModel().vendor)
                            if (draft.isBlank()) {
                                // [AI修改] hotfix v1.1 E-L1-03兄弟场景：密钥已被删除，没有"重新启用"的资格，引导先补填 Key
                                //   （填完后 KeyDialog.onConfirm 的 routeOnSave 会判到 FULL_CONSENT，走正常首次同意路径）。
                                keyDialogOpen = true
                            } else {
                                pendingKeyDraft = draft
                                consentPanelOpen = true
                            }
                        }, // [AI修改] L1：DECLINED 后"重新启用"
                    )
                }
                com.sxdbsm.cookbook.android.ui.component.InsetDivider(startIndent = 48)

                // —— 规则 ——
                RuntimeRow("规则推荐（不用模型，离线可用）", AiRuntimeType.MOCK, state.type, enabled = true) { vm.onTypeChange(it) }
                com.sxdbsm.cookbook.android.ui.component.InsetDivider(startIndent = 48)

                // —— 端侧 ——（末行后不加分隔线）
                RuntimeRow("端侧本地模型（接入中）", AiRuntimeType.ON_DEVICE, state.type, enabled = false) { vm.onTypeChange(it) }
                OnDeviceSelfTestSection() // [AI生成] 端侧设备自测：先给流畅度预估，让用户对能否流畅使用有预期。
            }

            // [AI修改] D2：隐私小字移到卡外页脚(裸 Text·不进白卡)，与设置族一致。
            // [AI修改] L1 copywriter 🟡#9：措辞对齐 CloudAiDisclosure.WILL_SEND（补第四项"你说的话"）+ 全角引号（🔴#1）。
            Text(
                "隐私：启用云端 AI 后，只发送在手食材名、粗健康标签（如「忌高嘌呤」）、候选菜名和你在 AI 记一餐里输入的那句话，不发送完整健康档案。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (keyDialogOpen) {
        val model = vm.selectedModel()
        KeyDialog(
            vendorName = model.vendorName,
            initial = state.keyByVendor[model.vendor].orEmpty(),
            onConfirm = { key ->
                // [AI修改] L1：先关 KeyDialog，再按同意状态分流（INV-L1-04/05）——routeOnSave 纯函数，可直接 JVM 单测。
                keyDialogOpen = false
                when (routeOnSave(state.cloudAiConsent, model.vendor, key)) {
                    SaveRoute.DIRECT -> vm.onSaveVendorKey(model.vendor, key)
                    SaveRoute.VENDOR_CONFIRM -> { pendingKeyDraft = key; vendorConfirmOpen = true }
                    SaveRoute.FULL_CONSENT -> { pendingKeyDraft = key; consentPanelOpen = true }
                }
            },
            onDismiss = { keyDialogOpen = false },
        )
    }

    // [AI修改] L1：轻量换厂商确认（同意已满足但 vendor 未确认）。"看看发送哪些内容"下钻只读面板，叠在换厂商确认框之上，关闭后外层原样可继续操作（§5.1"不自动弹回"简化已被 hotfix v1.1 E-L1-06 推翻，见主蓝图 §5.1 标注）。
    if (vendorConfirmOpen) {
        val model = vm.selectedModel()
        AlertDialog(
            onDismissRequest = { vendorConfirmOpen = false; pendingKeyDraft = "" },
            title = { Text("切换到${model.vendorName}云端模型？") },
            text = {
                Column {
                    Text("你即将切换到${model.vendorName}，请先确认发送范围。") // [AI修改] copywriter 🔴#3：消"该厂商由不同服务商提供"歧义
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "看看发送哪些内容 ›",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable {
                                // [AI修改] hotfix E-L1-06：不关闭外层"切换到 xxx"确认框——CloudAiConsentPanel 是独立 Dialog，
                                //   天然可叠在 AlertDialog 之上，"知道了"关闭后外层确认框应原样保留可继续操作。
                                readonlyPanelOpen = true
                            }
                            .padding(vertical = 2.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.confirmVendorSwitch(model.vendor, pendingKeyDraft)
                    vendorConfirmOpen = false
                    pendingKeyDraft = ""
                    pendingSnackbar = "已启用云端 AI"
                }) { Text("继续") }
            },
            dismissButton = {
                TextButton(onClick = { vendorConfirmOpen = false; pendingKeyDraft = "" }) { Text("取消") }
            },
        )
    }

    // [AI修改] L1：完整同意面板（FULL_CONSENT / DECLINED 后重新启用 / 首次启用共用）。
    if (consentPanelOpen) {
        val model = vm.selectedModel()
        CloudAiConsentPanel(
            vendorName = model.vendorName,
            onAgree = {
                vm.grantConsent(model.vendor, pendingKeyDraft, ConsentSource.EXPLICIT_FIRST_ENABLE)
                consentPanelOpen = false
                pendingKeyDraft = ""
                pendingSnackbar = "已启用云端 AI"
            },
            onDecline = {
                vm.declineConsent()
                consentPanelOpen = false
                pendingKeyDraft = ""
                pendingSnackbar = "已关闭云端 AI"
            },
            onClose = {
                consentPanelOpen = false // 返回键/点外部：仅关闭+清草稿，不写任何 consent 状态（等价"这次先不处理"）
                // [AI修改] Google 质量终审 ⚪#5：清空 pendingKeyDraft 安全——用户重新点 KeyDialog 保存时，
                //   onConfirm 的 when(routeOnSave(...)) 分支会重新给 pendingKeyDraft 赋值，不会以空串进 grantConsent。
                pendingKeyDraft = ""
            },
        )
    }

    // [AI修改] L1：只读态披露面板（常驻状态块"发送哪些内容"入口）。只读面板本就叠在换厂商确认框之上（若从该入口打开），"知道了"只关自己，外层原样保留（hotfix v1.1 E-L1-06）。
    if (readonlyPanelOpen) {
        val model = vm.selectedModel()
        CloudAiConsentPanel(
            vendorName = model.vendorName,
            onAgree = null,
            onDecline = null,
            onClose = { readonlyPanelOpen = false },
        )
    }

    // [AI修改] L1：grandfather 补确认面板（差异化措辞；onClose 仅关闭，不写任何 consent 状态=下次进页再问）。
    if (grandfatherPanelOpen) {
        val model = vm.selectedModel()
        CloudAiConsentPanel(
            vendorName = model.vendorName,
            grandfather = true,
            onAgree = {
                vm.resolveGrandfather(confirm = true)
                grandfatherPanelOpen = false
                pendingSnackbar = "已启用云端 AI"
            },
            onDecline = {
                vm.resolveGrandfather(confirm = false)
                grandfatherPanelOpen = false
                pendingSnackbar = "已关闭云端 AI"
            },
            onClose = { grandfatherPanelOpen = false },
        )
    }

    // [AI修改] L1：常驻状态块"关闭"——ActionSheet 两档，默认项在前、破坏项(删除密钥)标红在后（§5.3）。
    if (closeSheetOpen) {
        val model = vm.selectedModel()
        com.sxdbsm.cookbook.android.ui.component.ActionSheet(
            title = "关闭云端 AI",
            message = "关闭后，AI 记一餐等功能将回退到本地规则，不再向云端发送数据。", // [AI修改] copywriter 🟡#10：说人话补"向云端"
            actions = listOf(
                com.sxdbsm.cookbook.android.ui.component.SheetAction("保留密钥并关闭") {
                    vm.closeCloudAi(model.vendor, deleteKey = false)
                    pendingSnackbar = "已关闭云端 AI"
                },
                com.sxdbsm.cookbook.android.ui.component.SheetAction("关闭并删除密钥", destructive = true) {
                    vm.closeCloudAi(model.vendor, deleteKey = true)
                    pendingSnackbar = "已关闭并删除密钥"
                },
            ),
            onDismiss = { closeSheetOpen = false },
        )
    }

    if (guideOpen) {
        val model = vm.selectedModel()
        ApiKeyGuideSheet(
            model = model,
            onOpenUrl = { url ->
                // [AI生成] 浏览器打开官网申请页；主动点击不做二次确认(少一步)。
                //   [AI修改] 审查建议2/3：加 NEW_TASK 提升非 Activity context 成功率；失败给 Toast(别静默,让用户能手抄 URL)。
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure {
                    android.widget.Toast.makeText(context, "未找到可打开网页的应用，请手动访问：$url", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { guideOpen = false },
        )
    }
}

/** 某厂商 API Key 申请的精简步骤(vendor -> 有序步骤)。[AI生成] AI 配置指南 */
private val VENDOR_KEY_STEPS: Map<String, List<String>> = mapOf(
    "zhipu" to listOf("注册登录（手机号）", "完成个人实名认证", "进「API Keys」页添加新的 Key", "复制粘贴到上方设置"),
    "deepseek" to listOf("注册登录", "进「API Keys」创建 Key（新用户有试用额度）", "复制粘贴到上方设置"),
    "dashscope" to listOf("注册/登录阿里云百炼", "开通服务并完成实名", "创建 API-KEY", "复制粘贴到上方设置"),
    "moonshot" to listOf("注册登录", "进「API Key 管理」新建 Key", "复制粘贴到上方设置"),
)

/**
 * "如何申请密钥"指南弹层。[AI生成] AI 配置指南
 *
 * 按当前所选模型的厂商动态展示：厂商名(+免费标) + 申请步骤 + "打开官网申请"CTA(浏览器直跳) + 隐私说明。
 * 让"要填 Key 却不知去哪申请"一点直达对应厂商步骤，免翻文档(少操作)。URL/免费为 AI 参考整理，随平台可能调整。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyGuideSheet(
    model: com.sxdbsm.cookbook.ai.CloudModel,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val steps = VENDOR_KEY_STEPS[model.vendor].orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // 标题行 + 免费标(随当前模型 free)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${model.vendorName}密钥申请", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                // [AI生成] 免费标随**当前所选模型**(model.free)非厂商级：同厂 zhipu 有免费(GLM-4-Flash)+收费(GLM-4-Air)两模型，选谁标谁。非 bug。
                if (model.free) {
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "免费",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // 申请步骤(有序)
            steps.forEachIndexed { i, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(20.dp),
                    )
                    Text(step, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                if (i < steps.lastIndex) Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(20.dp))
            // 主 CTA：打开官网(有 URL 才显)
            if (model.applyUrl.isNotBlank()) {
                com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                    text = "打开官网申请",
                    onClick = { onOpenUrl(model.applyUrl) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(
                "API Key 只保存在本机，不上传、不写日志。步骤/入口以厂商官网实际页面为准。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudSection(
    state: AiSettingsUiState,
    onSelectModel: (String) -> Unit,
    onEditKey: () -> Unit,
    onShowGuide: () -> Unit, // [AI生成] 打开"如何申请密钥"指南 sheet
    onShowStatusDetail: () -> Unit, // [AI修改] L1："发送哪些内容"只读披露面板
    onCloseCloudAi: () -> Unit, // [AI修改] L1：常驻状态块"关闭"→ ActionSheet
    onReenableConsent: () -> Unit, // [AI修改] L1：DECLINED 后"重新启用"→ 完整同意面板
) {
    val model = state.models.firstOrNull { it.id == state.selectedModelId } ?: state.models.first()
    val vendorKey = state.keyByVendor[model.vendor].orEmpty()
    var expanded by remember { mutableStateOf(false) }

    // [AI修改] D2：子块外层缩进对齐来源行文字左缘(48dp)，收进 InsetGroup 白卡。
    Column(modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 14.dp)) {
        // 模型下拉
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = model.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("模型") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.models.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.displayName) },
                        onClick = {
                            onSelectModel(m.id)
                            expanded = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Key 状态 + 设置/编辑
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (vendorKey.isBlank()) "${model.vendorName}密钥：未配置" else "${model.vendorName}密钥：已配置 ${maskKey(vendorKey)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (vendorKey.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEditKey) { Text(if (vendorKey.isBlank()) "设置密钥" else "编辑") }
        }
        Text(
            "Key 只保存在本机；未配置/无网络时自动回退规则推荐。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        // [AI修改] 静态"见文档"升级为可点入口：点开按当前厂商展示申请步骤+官网跳转(少一步，不用翻文档)。
        //   未配置 Key 时文案更醒目引导(§9.6 空态给下一步)。
        Text(
            if (vendorKey.isBlank()) "不知道去哪申请？查看步骤 →" else "如何申请密钥？",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onShowGuide)
                .padding(vertical = 2.dp),
        )

        // [AI修改] L1：常驻状态块（INV-L1-10）——双条件：GRANTED 且当前厂商 Key 非空（否则与红色"密钥：未配置"同屏矛盾）。
        if (shouldShowCloudStatusBlock(state.cloudAiConsent, state.keyByVendor, model.vendor)) {
            Spacer(Modifier.height(10.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("云端 AI 已启用", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(
                            "已启用",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(percent = 50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    CloudStatusRow("接收方", model.vendorName)
                    state.cloudAiConsent.grantedAtEpochSeconds?.let { seconds ->
                        CloudStatusRow("同意时间", DateTime.epochSecondsToDate(seconds))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "发送哪些内容 ›",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable(onClick = onShowStatusDetail)
                            .padding(vertical = 2.dp),
                    )
                    Text(
                        "关闭",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clickable(onClick = onCloseCloudAi)
                            .padding(vertical = 2.dp),
                    )
                }
            }
        }
        // [AI修改] L1：DECLINED 后手动把单选切回"云端大模型"但未重新启用——给唯一恢复入口（INV-L1-12）。
        if (state.type == AiRuntimeType.CLOUD && state.cloudAiConsent.status == ConsentStatus.DECLINED) {
            Spacer(Modifier.height(8.dp))
            Text(
                "云端 AI 已被你关闭 · 重新启用 ›",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onReenableConsent)
                    .padding(vertical = 2.dp),
            )
        }
    }
}

/** 常驻状态块的一行"标签：值"。[AI生成] L1 */
@Composable
private fun CloudStatusRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun KeyDialog(
    vendorName: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$vendorName API Key") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("粘贴 $vendorName 的 API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "同厂商多个模型共用一个 Key，只保存在本机。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/**
 * 端侧模型「设备自测」区。[AI生成]
 *
 * 端侧模型运行时尚在接入(Step2)，此处先提供「测试本机能否流畅运行」的规格预估，让用户有预期。
 */
@Composable
private fun OnDeviceSelfTestSection() {
    val context = LocalContext.current
    var report by remember { mutableStateOf<DeviceAiReport?>(null) }

    // [AI修改] D2：端侧自测子块缩进与云端子块一致(对齐来源行文字左缘·收进白卡)。
    Column(modifier = Modifier.padding(start = 48.dp, end = 16.dp, bottom = 14.dp)) {
        OutlinedButton(onClick = { report = DeviceAiCapability.evaluate(context) }) {
            Text(if (report == null) "测试本机能否流畅运行" else "重新测试")
        }
        report?.let { r ->
            Spacer(Modifier.height(8.dp))
            val gradeColor = when (r.grade) {
                DeviceAiGrade.SMOOTH -> MaterialTheme.colorScheme.primary
                DeviceAiGrade.USABLE -> MaterialTheme.colorScheme.primary
                DeviceAiGrade.SLOW -> MaterialTheme.colorScheme.error
                DeviceAiGrade.UNSUPPORTED -> MaterialTheme.colorScheme.error
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("流畅度预估：", style = MaterialTheme.typography.bodyMedium)
                        Text(r.grade.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = gradeColor)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(r.grade.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(r.specLine(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "为规格预估（内存/CPU/架构），非真实推理实测；端侧模型接入后可实测生成速度。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** [AI修改] D2：单选来源行改 InsetGroup 内的 list-row（整行 selectable·16/14 padding·端侧 disabled）。 */
@Composable
private fun RuntimeRow(
    label: String,
    value: AiRuntimeType,
    selected: AiRuntimeType,
    enabled: Boolean,
    onSelect: (AiRuntimeType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = value == selected, enabled = enabled) { onSelect(value) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = value == selected, onClick = { if (enabled) onSelect(value) }, enabled = enabled)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** 打码显示 Key，只露头尾。[AI生成] */
private fun maskKey(key: String): String =
    if (key.length <= 8) "••••" else "${key.take(4)}••••${key.takeLast(4)}"
