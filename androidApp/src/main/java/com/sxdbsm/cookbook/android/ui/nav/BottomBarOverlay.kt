package com.sxdbsm.cookbook.android.ui.nav

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @File : BottomBarOverlay
 * @Time : 2026/08/30
 * @Author : SXD-AI
 * @Desc : 悬浮底部导航栏的几何常量与避让口径（单一真相源·防各页散落写死 dp 漂移）[AI修改]
 * <p>
 * 布局范式（§9.44 底部导航真悬浮）：BottomBar 悬浮在内容之上（Box 叠加层，不占 Scaffold 布局空间），
 * Tab 页内容铺满全屏、滚动时从胶囊下方穿过；页面滚动容器用 contentPadding.bottom 预留停泊空间
 * （padding 语义，非 margin——列表视口不被裁短，内容可滑进胶囊下方再停泊）。
 **/
object BottomBarOverlay {
    /** 胶囊体高度（NAV-FIX 已确立：58 内竖排 icon22+2+labelSmall=40，留余量不裁字）。 */
    val CapsuleHeight = 58.dp

    /** 底栏行水平屏边距（与全 App 屏边距 16dp 同源）。 */
    val RowHorizontal = 16.dp

    /** 底栏行上下外间距；同时是列表停泊时最后一项与胶囊顶的间隙（一值两用，不另造呼吸值）。 */
    val RowVertical = 12.dp

    /**
     * 胶囊行在系统导航栏 inset 之上的视觉包络高（58+12×2=82）。[AI修改]
     * 给**已自带 navigationBarsPadding 的底栏式组件**（如 SelectionSummaryBar）抬升用——
     * 这类组件内部自己会加系统栏 inset，若用总口径会双份 inset。
     */
    val BodyHeight = CapsuleHeight + RowVertical * 2

    /** 底栏行总预留 = BodyHeight + 系统导航栏 inset（运行时读，三键/手势机型自适应）。 */
    @Composable
    fun reservedTotal(): Dp =
        BodyHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

/**
 * 当前页面底部被悬浮导航栏占用的预留高度（含系统导航栏 inset）。[AI修改]
 * MainScaffold 按"是否 Tab 落地路由"供值：Tab 页=BodyHeight+inset；二级页/弹窗=0（天然不吃避让）。
 * Tab 页滚动容器 contentPadding.bottom 统一读它，禁止各页写死 dp。
 */
val LocalBottomNavReserved = compositionLocalOf { 0.dp }
