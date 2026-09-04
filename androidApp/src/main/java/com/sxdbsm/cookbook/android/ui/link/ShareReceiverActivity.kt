package com.sxdbsm.cookbook.android.ui.link

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.sxdbsm.cookbook.android.MainActivity
import com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefillBus
import com.sxdbsm.cookbook.android.ui.theme.CookbookTheme
import com.sxdbsm.cookbook.android.util.AppLogger
import com.sxdbsm.cookbook.data.parser.LinkPrefillMapper
import com.sxdbsm.cookbook.data.repository.ShareLinkRepository
import com.sxdbsm.cookbook.domain.model.ThemeMode
import java.net.URI
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.context.GlobalContext
import org.koin.core.parameter.parametersOf

/**
 * host→来源标识（m./www. 前缀剥离后取主域）。[AI生成] STEP-L4-8.3
 */
fun sourceFromHost(host: String): String = host.removePrefix("www.").removePrefix("m.").substringBefore(".")

/**
 * 来源→解析配置资源路径（Phase1 硬编码路由·DP-P1-10；parse_dictionary 为 Phase2 路由表）。[AI生成]
 */
fun configPathFor(source: String): String? = if (source == "xiachufang") "parsers/xiachufang.json" else null

/**
 * 来源显示名。[AI生成]
 */
fun mapSourceName(source: String): String = if (source == "xiachufang") "下厨房" else source

/**
 * 外部菜谱分享接收透明宿主（蓝图 STEP-L4-8·ParseSheet 宿主唯一化）。[AI生成]
 *
 * 双入口（onCreate/onNewIntent 统一走 [handleIntent]）：
 * - **入口 A**（系统分享 ACTION_SEND text/plain）：extractUrl 提取链接→查重（DP-P1-8 命中复用
 *   该行不插新行；state=3 直接回主界面告知已存过）→未命中落库后先过 **T2 隐私弹窗**（交互规范 F）
 *   再进 ParseSheet；
 * - **入口 B**（链接列表转发 EXTRA_LINK_ID）：按行 parseState 分派——0 先 T2、1 本地重建
 *   （不 T2 不联网·DP-P1-7）、2 直接重试、3 仅作死菜兜底（列表点行先查活性，活菜直接跳详情；
 *   软删失活行才转发进宿主并按 1 态本地重建可重存·Y-02）。
 *
 * 窗口为透明宿主（ShareReceiverTheme），本 Activity 不铺任何背景，只承载 T2 AlertDialog 与 ParseSheet。
 * 日志红线 Y-04：只记 linkId/state/source/host，禁完整 URL/菜名/clear_text。
 */
class ShareReceiverActivity : ComponentActivity() {

    // Compose 状态机（Activity 域·setContent 与 handleIntent 共同读写；onNewIntent 复用实例时先复位防串）。[AI生成]
    private var showT2 by mutableStateOf(false)
    private var vmReady by mutableStateOf(false)
    private var activeLinkId by mutableStateOf(0L)
    private var activeUrl by mutableStateOf("")
    private var activeSource by mutableStateOf("")

    // VM 就绪后要执行的挂起动作（handleIntent 拿不到组合内的 VM 实例，在 setContent 里用 LaunchedEffect 分派）。[AI生成]
    private var pendingRebuildRow by mutableStateOf<ShareLinkRepository.ShareLinkRow?>(null)
    private var pendingStart by mutableStateOf(false)

    private val linkRepo: ShareLinkRepository by lazy { GlobalContext.get().get<ShareLinkRepository>() }
    private val prefillBus: NewDishPrefillBus by lazy { GlobalContext.get().get<NewDishPrefillBus>() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            // 透明宿主仍需 Material 主题供弹窗/Sheet 取色；不铺 Surface，保持来源 App 可见。[AI生成]
            CookbookTheme(themeMode = ThemeMode.SYSTEM) {
                if (activeLinkId > 0L) {
                    val vm: ParseViewModel = koinViewModel(key = "parse-$activeLinkId") {
                        parametersOf(activeLinkId, mapSourceName(activeSource))
                    }
                    // 挂起动作分派：VM 实例就绪后执行（重建=不联网；重试=联网采集）。[AI生成]
                    LaunchedEffect(vm, pendingRebuildRow) {
                        val row = pendingRebuildRow
                        if (row != null) {
                            pendingRebuildRow = null
                            vm.rebuildFrom(row)
                        }
                    }
                    LaunchedEffect(vm, pendingStart) {
                        if (pendingStart) {
                            pendingStart = false
                            vm.startCollect(activeUrl)
                        }
                    }
                    if (showT2) {
                        T2PrivacyDialog(
                            sourceName = mapSourceName(activeSource),
                            onAgree = {
                                showT2 = false
                                vmReady = true
                                vm.startCollect(activeUrl)
                            },
                            onSaveLinkOnly = { saveLinkOnly() },
                        )
                    }
                    if (vmReady) {
                        ParseSheet(
                            state = vm.state.collectAsState().value,
                            prefetch = vm.prefetch.collectAsState().value,
                            saving = vm.saving.collectAsState().value,
                            onDismiss = { finish() }, // 关闭即安全：链接已落库可从列表找回（A.0 无守卫）
                            onSaveDish = {
                                vm.saveAsDish { prefill ->
                                    prefillBus.request(prefill)
                                    openMainForNewDish()
                                    finish()
                                }
                            },
                            onRetry = { vm.startCollect(activeUrl) },
                            onOpenInBrowser = {
                                runCatching {
                                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activeUrl)))
                                }
                            },
                            onSaveLinkOnly = { saveLinkOnly() },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent) // singleTask 复用实例：第二次分享走这里
    }

    /**
     * 双入口统一解析（onCreate/onNewIntent 共用）：先复位状态机再按 extra 分派。[AI生成]
     */
    private fun handleIntent(intent: Intent?) {
        showT2 = false
        vmReady = false
        activeLinkId = 0L
        activeUrl = ""
        activeSource = ""
        pendingRebuildRow = null
        pendingStart = false
        if (intent == null) {
            finish()
            return
        }
        val linkId = intent.getLongExtra(EXTRA_LINK_ID, -1L)
        if (linkId >= 0L) {
            dispatchById(linkId) // 入口 B：链接列表转发
            return
        }
        handleSend(intent) // 入口 A：系统分享
    }

    /**
     * 入口 A：ACTION_SEND 文本→提取 URL→去重（DP-P1-8）→落库→T2。[AI生成]
     */
    private fun handleSend(intent: Intent) {
        val url = intent.getStringExtra(Intent.EXTRA_TEXT)?.let { LinkPrefillMapper.extractUrl(it) }
        if (url == null) {
            AppLogger.d(TAG, "send_no_url")
            Toast.makeText(this, "没识别到链接", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val host = runCatching { URI(url).host }.getOrNull().orEmpty()
        val source = sourceFromHost(host)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        lifecycleScope.launch {
            val existing = runCatching { linkRepo.findActiveByLink(url) }.getOrNull()
            if (existing != null) {
                // DP-P1-8：命中复用该行，不再插新行。[AI生成]
                AppLogger.d(TAG, "dedup_hit linkId=${existing.id} state=${existing.parseState}")
                if (!subject.isNullOrBlank() && existing.title.isBlank()) {
                    runCatching { linkRepo.updateTitle(existing.id, subject) }
                }
                if (existing.parseState == 3L) {
                    Toast.makeText(this@ShareReceiverActivity, "已存过这道菜", Toast.LENGTH_SHORT).show()
                    openMain()
                    finish()
                } else {
                    dispatchRow(existing) // 0/1/2 统一分派（同入口 B）
                }
            } else {
                val newId = runCatching { linkRepo.insert(source, url, subject.orEmpty()) }.getOrNull()
                if (newId == null) {
                    AppLogger.d(TAG, "insert_failed source=$source")
                    finish()
                    return@launch
                }
                AppLogger.d(TAG, "inserted linkId=$newId source=$source")
                activeLinkId = newId
                activeUrl = url
                activeSource = source
                showT2 = true // T2 事前告知（交互规范 F）
            }
        }
    }

    /**
     * 入口 B：按行 id 查行并分派（行已删/不存在则关宿主）。[AI生成]
     */
    private fun dispatchById(id: Long) {
        lifecycleScope.launch {
            val row = runCatching { linkRepo.getById(id) }.getOrNull()
            if (row == null) {
                AppLogger.d(TAG, "row_missing linkId=$id")
                finish()
                return@launch
            }
            AppLogger.d(TAG, "entry_list linkId=${row.id} state=${row.parseState}")
            if (row.parseState == 3L) {
                // 已存菜品软删失活→按 1 态本地重建可重存（Y-02 死菜兜底·列表点行已先查活性，
                // 只有失活行才会转发进宿主）；正常 3 态由列表直接跳详情，不进宿主。[AI生成]
                dispatchRow(row.copy(parseState = 1L)) // ShareLinkRow 是 data class，copy 降态只影响本次会话、不改库
            } else {
                dispatchRow(row)
            }
        }
    }

    /**
     * 按 parseState 分派（0/1/2·state 只前进不回退）：[AI生成]
     * 0=待解析→先 T2；1=已解析未存→本地重建（不 T2 不联网·DP-P1-7）；2=失败→直接重试（不 T2·当初已同意）。
     */
    private fun dispatchRow(row: ShareLinkRepository.ShareLinkRow) {
        activeLinkId = row.id
        activeUrl = row.link
        activeSource = row.source
        when (row.parseState) {
            0L -> showT2 = true
            1L -> {
                vmReady = true
                pendingRebuildRow = row
            }
            else -> {
                vmReady = true
                pendingStart = true
            }
        }
    }

    /**
     * 「仅保存链接」：Toast 告知后续入口后关宿主；不删行（DP-P1-12·关闭即安全）。[AI生成]
     */
    private fun saveLinkOnly() {
        Toast.makeText(this, "已保存链接，之后可在 菜品 页右上角打开", Toast.LENGTH_SHORT).show()
        finish()
    }

    /** 回主界面（重复分享已存过时；无 extra）。[AI生成] */
    private fun openMain() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    // [AI修改] 终审 S-4：宿主在独立 task（singleTask+excludeFromRecents），无 NEW_TASK 会在隐形 task 里
                    //   再起 MainActivity 实例——对齐项目既有先例（CookingTimerService/TimerAlarm）补 NEW_TASK。
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
    }

    /** 存为菜品后回主界面直达新建菜品（extra key 统一引 MainActivity.EXTRA_OPEN_NEWDISH）。[AI生成] */
    private fun openMainForNewDish() {
        runCatching {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_NEWDISH, true) // STEP-L4-11.3：MainActivity 侧 onCreate/onNewIntent 解析
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), // [AI修改] 终审 S-4 同上。
            )
        }
    }

    /**
     * T2 隐私弹窗（交互规范 F 逐字）：二选一决策、无图标；点外部=「仅保存链接」（用户收回同意·链接保留）。[AI生成]
     */
    @Composable
    private fun T2PrivacyDialog(sourceName: String, onAgree: () -> Unit, onSaveLinkOnly: () -> Unit) {
        AlertDialog(
            onDismissRequest = onSaveLinkOnly,
            title = { Text("打开${sourceName}的网页？") },
            text = {
                Column {
                    Text(
                        "打开时，${sourceName}的服务器会看到这次访问。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "打开后会自动提取菜名、食材、步骤和图片，由你确认后才保存。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onAgree) { Text("同意并打开") }
            },
            dismissButton = {
                TextButton(onClick = onSaveLinkOnly) { Text("仅保存链接") }
            },
        )
    }

    // companion 去 private：EXTRA_LINK_ID 需被 LinkListScreen 跨类引用（private companion 会连带挡住其 public 成员的类名访问）。[AI修改]
    companion object {
        private const val TAG = "LinkRecv"

        /** 链接列表转发入口 extra key。[AI生成] STEP-L4-8.2 */
        const val EXTRA_LINK_ID = "share_link_id"
    }
}
