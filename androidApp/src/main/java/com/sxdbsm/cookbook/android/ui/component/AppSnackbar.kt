package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * @File : AppSnackbar
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 全 App 统一 Snackbar 控制器（§9.12）——单一宿主挂 MainScaffold，各屏经 LocalAppSnackbar 调用
 * <p>
 * 终结"每屏各建 SnackbarHostState + Toast/Snackbar 混用"。日常成功用 showMessage；
 * 可逆破坏操作(删整天/移除菜/删自建)用 showUndo(带撤销)。Toast 仅留纯告知无跟进项。
 * <p>
 * [AI生成] B 批基建：统一保存/撤销反馈宿主。
 **/
class AppSnackbarController(
    val hostState: SnackbarHostState,
    private val scope: CoroutineScope,
) {
    // [AI修改] 代码审查#5：单 job 串行化，避免两次快速调用交错导致未点的撤销被挤丢(新提示取代旧的)。
    private var job: Job? = null

    /** 纯告知轻提示(如"已保存")。[AI生成] */
    fun showMessage(text: String) {
        job?.cancel()
        job = scope.launch {
            hostState.currentSnackbarData?.dismiss()
            hostState.showSnackbar(text, duration = SnackbarDuration.Short)
        }
    }

    /** 可逆破坏操作：带"撤销"，点撤销回调 onUndo（§9.12/9.16 撤销优于确认）。[AI生成] */
    fun showUndo(text: String, actionLabel: String = "撤销", onUndo: () -> Unit) {
        job?.cancel()
        job = scope.launch {
            hostState.currentSnackbarData?.dismiss()
            // [AI修改] 代码审查#4：撤销需用户反应+点击，用 Long(~10s)而非 Short(~4s)。
            val result = hostState.showSnackbar(text, actionLabel = actionLabel, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }
}

/** 各屏读取入口；未提供时为 null（各屏用 `?.` 安全调用）。[AI生成] */
val LocalAppSnackbar = staticCompositionLocalOf<AppSnackbarController?> { null }
