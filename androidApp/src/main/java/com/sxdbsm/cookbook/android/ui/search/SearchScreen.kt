package com.sxdbsm.cookbook.android.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.AppSearchField
import com.sxdbsm.cookbook.android.ui.component.DayMealCardView
import com.sxdbsm.cookbook.android.ui.component.DishRow
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.android.ui.component.EmptyLineText
import com.sxdbsm.cookbook.android.ui.component.IngredientCard
import com.sxdbsm.cookbook.android.ui.component.SectionHeader
import com.sxdbsm.cookbook.platform.BusinessTrace
import com.sxdbsm.cookbook.platform.TraceId
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

/**
 * 全局搜索页面。[AI生成]
 *
 * 入口来自首页搜索图标；结果按“菜品 / 食材 / 餐食”分组展示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenDish: (Long) -> Unit,
    onEditMealDate: (LocalDate) -> Unit,
    onOpenIngredient: (com.sxdbsm.cookbook.domain.model.Ingredient) -> Unit = {}, // [AI修改] 点食材结果跳到该食材并高亮。
    onNewDish: () -> Unit = {}, // [AI生成] 全无结果"新建菜品"→导航新建菜品页(预填菜名走总线)
    createdDishId: Long? = null,
    lifecycleTraceId: String? = null,
    onCreatedDishConsumed: () -> Unit = {},
    onNewIngredient: () -> Unit = {}, // [AI生成] 全无结果"新建食材"→导航食材页(按名开编辑器走总线)
    vm: SearchViewModel = koinViewModel(),
) {
    val prefillBus = org.koin.compose.koinInject<com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefillBus>()
    val createBus = org.koin.compose.koinInject<com.sxdbsm.cookbook.android.ui.ingredients.IngredientCreateBus>()
    val ui by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(createdDishId) {
        if (createdDishId?.takeIf { it > 0 } == null) return@LaunchedEffect
        // [AI生成] RESTORE：新建菜品返回的标量结果已恢复，关键词仍由 ViewModel 保持。
        val traceId = lifecycleTraceId?.let(TraceId::fromValue)
        BusinessTrace.stateRestore("new_dish", "success", "created_dish_search_keyword", traceId)
        // [AI生成] MERGE：仅在关键词重查完成后记录合并成功，不复制结果列表到导航参数。
        vm.refresh {
            BusinessTrace.stateMergeResult("food_search", "searching", "created_dish", "results_refreshed", traceId)
            onCreatedDishConsumed()
        }
    }

    LaunchedEffect(listState, ui.meals.size, ui.canLoadMoreMeals, ui.loadingMoreMeals) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible to listState.isScrollInProgress
        }.collect { (lastVisible, scrolling) ->
            if (scrolling && ui.canLoadMoreMeals && !ui.loadingMoreMeals && lastVisible >= listState.layoutInfo.totalItemsCount - 2) {
                vm.loadMoreMeals()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 避免页面 Scaffold 和根 Scaffold 重复避让系统栏。
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ), // [AI修改] 搜索页顶栏按暖杏规范使用页面背景一体化样式。
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    AppSearchField(
                        value = ui.keyword,
                        onValueChange = vm::setKeyword,
                        placeholder = "搜索菜品、食材、餐食日期",
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ui.keyword.isBlank()) {
                item { EmptyState(text = "输入名称或日期开始搜索", icon = "🔎") }
            } else if (ui.dishes.isEmpty() && ui.ingredients.isEmpty() && ui.meals.isEmpty() && !ui.loading) {
                // [AI生成] 全无结果:统一给"未找到"+两按钮(新建菜品/食材),点即预填名进对应新建页。
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("🔎", style = MaterialTheme.typography.displaySmall)
                        Text("未找到「${ui.keyword.trim()}」", style = MaterialTheme.typography.titleSmall)
                        Text("可直接新建：", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                                text = "＋ 新建菜品",
                                onClick = {
                                    prefillBus.request(com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefill(name = ui.keyword.trim()))
                                    onNewDish()
                                },
                            )
                            OutlinedButton(onClick = {
                                createBus.request(ui.keyword.trim())
                                onNewIngredient()
                            }) { Text("＋ 新建食材") }
                        }
                    }
                }
            } else {
                item { SectionHeader(title = "菜品") }
                item {
                    if (ui.dishes.isEmpty() && !ui.loading) {
                        EmptyLineText("没有匹配菜品")
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(ui.dishes, key = { it.id }) { dish ->
                                Box(Modifier.width(320.dp)) {
                                    DishRow(dish = dish, onClick = { onOpenDish(dish.id) })
                                }
                            }
                        }
                    }
                }

                item { SectionHeader(title = "食材") }
                item {
                    if (ui.ingredients.isEmpty() && !ui.loading) {
                        EmptyLineText("没有匹配食材")
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(ui.ingredients, key = { it.id }) { ingredient ->
                                IngredientCard(
                                    ingredient = ingredient,
                                    modifier = Modifier.width(96.dp),
                                    onClick = { onOpenIngredient(ingredient) }, // [AI修改] 点食材结果跳到该食材并高亮。
                                )
                            }
                        }
                    }
                }

                item { SectionHeader(title = "餐食") }
                if (ui.meals.isEmpty() && !ui.loading) {
                    item { EmptyLineText("没有匹配餐食") }
                } else {
                    items(ui.meals, key = { it.date.toString() }) { card ->
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            DayMealCardView(
                                data = card,
                                onDishClick = { dish -> onOpenDish(dish.id) },
                                onEditClick = { onEditMealDate(card.date) },
                            )
                        }
                    }
                }
                if (ui.loading || ui.loadingMoreMeals) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(Modifier.size(28.dp))
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
