package com.sxdbsm.cookbook.android.ui.link

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image as ImageIcon
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sxdbsm.cookbook.android.ui.component.CapsuleButton
import com.sxdbsm.cookbook.android.ui.component.rememberImageBitmap

/**
 * 解析结果 BottomSheet——三态同一弹层的内容切换（交互规范 A 节·蓝图 STEP-L4-9）。[AI生成]
 *
 * - 载体=M3 ModalBottomSheet（skipPartiallyExpanded，dragHandle/scrim 保留默认）；
 *   onDismissRequest=关闭即安全（链接已落库可从列表找回·无守卫），与 onSaveLinkOnly（Toast+finish）不同。
 * - 签名元素=成功态顶部「菜谱卡」（16:9 封面+菜名+来源行）；折叠不显步骤图、展开才显（G.1 渐进披露）。
 * - 能力显隐由回调决定（§9.3 红线）：onSaveLinkOnly 传入才渲染「仅保存链接」。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParseSheet(
    state: ParseUiState,
    prefetch: PrefetchState,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSaveDish: () -> Unit,
    onRetry: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onSaveLinkOnly: (() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        when (state.phase) {
            ParsePhase.LOADING -> LoadingBody(state, onSaveLinkOnly)
            ParsePhase.SUCCESS -> SuccessBody(state, prefetch, saving, onSaveDish, onOpenInBrowser)
            ParsePhase.FAILED -> FailedBody(onRetry, onSaveLinkOnly)
        }
    }
}

/**
 * 态一：加载中（A.1·wrapContent 约 0.4 屏；「加载网页」与「提取菜谱」合并为单文案）。[AI生成]
 */
@Composable
private fun LoadingBody(state: ParseUiState, onSaveLinkOnly: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "正在加载网页…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "来自${state.sourceName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            displayUrlOf(state.url),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        if (onSaveLinkOnly != null) {
            TextButton(onClick = onSaveLinkOnly) { Text("仅保存链接") }
        }
    }
}

/**
 * 态二：解析成功（A.2+G.1·固定 0.85 屏，内部滚动区+固定 CTA；签名元素「菜谱卡」）。[AI生成]
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuccessBody(
    state: ParseUiState,
    prefetch: PrefetchState,
    saving: Boolean,
    onSaveDish: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    val recipe = state.recipe ?: return
    val colorScheme = MaterialTheme.colorScheme
    var ingredientsExpanded by remember { mutableStateOf(false) }
    var stepsExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f)) {
        // 上部滚动区
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            // 封面（已下载本地图 Crop / 灰底占位·§9.32）
            val coverBitmap = rememberImageBitmap(prefetch.cover?.imagePath, preview = false)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (coverBitmap != null) {
                    Image(
                        bitmap = coverBitmap,
                        contentDescription = "菜谱封面",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    )
                    Icon(
                        Icons.Outlined.ImageIcon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // 菜名
            Text(
                recipe.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            // 来源行 + 做法胶囊（空则整枚不渲染）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp),
            ) {
                Text(
                    "来自${state.sourceName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                val methods = recipe.cookingMethods.joinToString("·")
                if (methods.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colorScheme.surfaceVariant,
                    ) {
                        Text(
                            methods,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                .semantics { contentDescription = "做法：$methods" },
                        )
                    }
                }
            }
            // 食材分区（>8 种折叠·「新」徽标=食材库还没有）
            val unknownCount = state.unknownIngredientNames.size
            Text(
                if (unknownCount > 0) "食材 · ${recipe.ingredients.size} 种（新 $unknownCount 种）"
                else "食材 · ${recipe.ingredients.size} 种",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val shownIngredients =
                    if (ingredientsExpanded || recipe.ingredients.size <= INGREDIENT_COLLAPSE_COUNT) {
                        recipe.ingredients
                    } else {
                        recipe.ingredients.take(INGREDIENT_COLLAPSE_COUNT)
                    }
                shownIngredients.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ingredientDisplay(item.name, item.quantity, item.unit), // 显示解析原样·忠于网页
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface,
                        )
                        if (item.name in state.unknownIngredientNames) {
                            Spacer(Modifier.width(2.dp))
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(colorScheme.primaryContainer)
                                    .semantics { contentDescription = "食材库还没有，保存时自动新建" },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("新", fontSize = 9.sp, color = colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }
            if (recipe.ingredients.size > INGREDIENT_COLLAPSE_COUNT && !ingredientsExpanded) {
                TextButton(
                    onClick = { ingredientsExpanded = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp),
                ) {
                    Text("展开全部 ${recipe.ingredients.size} 种")
                }
            }
            // 步骤分区（>3 步折叠·G.1 折叠不显图、展开后已就绪的图才渲染）
            Text(
                "步骤 · ${recipe.steps.size} 步",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            val shownSteps =
                if (stepsExpanded || recipe.steps.size <= STEP_COLLAPSE_COUNT) {
                    recipe.steps
                } else {
                    recipe.steps.take(STEP_COLLAPSE_COUNT)
                }
            shownSteps.forEachIndexed { index, step ->
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            step.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface,
                            maxLines = if (stepsExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (stepsExpanded && step.imageUrl.isNotBlank()) {
                        // G.1：展开后该步图已就绪才渲染（未就绪/失败整块不渲染·不留灰洞）
                        prefetch.steps[step.imageUrl]?.let { pair ->
                            val stepBitmap = rememberImageBitmap(pair.imagePath, preview = false)
                            if (stepBitmap != null) {
                                Spacer(Modifier.height(8.dp))
                                Image(
                                    bitmap = stepBitmap,
                                    contentDescription = "步骤 ${index + 1} 配图",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 96.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (recipe.steps.size > STEP_COLLAPSE_COUNT && !stepsExpanded) {
                TextButton(
                    onClick = { stepsExpanded = true },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 4.dp),
                ) {
                    Text("展开全部 ${recipe.steps.size} 步")
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        // 下部固定 CTA 区（与 ActionSheet 底距写法一致）
        Divider(color = colorScheme.outlineVariant, thickness = 1.dp)
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            // CapsuleButton 只收 text；saving 态需在按钮内换 spinner，故按其源码同几何自绘。[AI生成]
            Button(
                onClick = onSaveDish,
                enabled = !saving, // 双击防护：正在导入时禁点（B.2）
                shape = RoundedCornerShape(percent = 50),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.primary,
                    contentColor = colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                if (saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("正在导入…", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("存为菜品", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(onClick = onOpenInBrowser) { Text("在浏览器中查看") }
            }
        }
    }
}

/**
 * 态三：解析失败（A.3·wrapContent；LinkOff 灰图不吓唬，文案不责备）。[AI生成]
 */
@Composable
private fun FailedBody(onRetry: () -> Unit, onSaveLinkOnly: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.LinkOff,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "没法解析这个链接",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "这个网页不是菜谱页，或网络不太好",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        CapsuleButton(
            text = "重试",
            onClick = onRetry,
            modifier = Modifier.heightIn(min = 48.dp), // 居中不通宽
        )
        Spacer(Modifier.height(8.dp))
        if (onSaveLinkOnly != null) {
            TextButton(onClick = onSaveLinkOnly) { Text("仅保存链接") }
        }
    }
}

/**
 * 加载态 URL 显示：host+path（去 query 参数·防 utm 长串）。[AI生成]
 */
private fun displayUrlOf(url: String): String =
    runCatching {
        val uri = Uri.parse(url)
        (uri.host ?: "") + (uri.path ?: "")
    }.getOrDefault(url).ifBlank { url }

/**
 * 食材展示文本：显示解析原样（忠于网页·克→g 折算发生在预填层）。[AI生成]
 *
 * 无数字行（适量/少许/半勺/无量）不显 "0.0"，只显名+单位词；有数字整数不带小数位（"冬瓜 500克"）。
 */
private fun ingredientDisplay(name: String, quantity: Double, unit: String): String {
    val qtyText = if (quantity != 0.0 && quantity % 1.0 == 0.0) quantity.toLong().toString() else quantity.toString()
    return when {
        quantity == 0.0 && unit.isEmpty() -> name
        quantity == 0.0 -> "$name $unit"
        else -> "$name $qtyText$unit"
    }
}

private const val INGREDIENT_COLLAPSE_COUNT = 8 // 食材折叠阈值（≤8 全显）
private const val STEP_COLLAPSE_COUNT = 3 // 步骤折叠阈值（≤3 全显）
