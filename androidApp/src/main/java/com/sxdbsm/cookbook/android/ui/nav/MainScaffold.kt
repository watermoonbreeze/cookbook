package com.sxdbsm.cookbook.android.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sxdbsm.cookbook.android.ui.addmeal.AddDayFoodScreen
import com.sxdbsm.cookbook.android.ui.dishdetail.DishDetailScreen
import com.sxdbsm.cookbook.android.ui.dishes.DishesScreen
import com.sxdbsm.cookbook.android.ui.home.HomeScreen
import com.sxdbsm.cookbook.android.ui.mine.MineScreen
import com.sxdbsm.cookbook.android.ui.newdish.NewDishScreen
import com.sxdbsm.cookbook.android.ui.timeline.FoodTimelineScreen

@Composable
fun MainScaffold() {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val currentRoute = current?.destination?.route

    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomBar(nav, currentRoute)
        },
        floatingActionButton = {
            if (showBottomBar) CenterPlusFab { nav.navigate(Routes.ADD_MEAL) }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onAddMeal = { nav.navigate(Routes.ADD_MEAL) },
                    onOpenTimeline = { nav.navigate(Routes.TIMELINE) },
                    onOpenDishes = { nav.navigate(Routes.DISHES) },
                )
            }
            composable(Routes.TIMELINE) { FoodTimelineScreen() }
            composable(Routes.DISHES) {
                DishesScreen(
                    onAddDish = { nav.navigate(Routes.newDish()) },
                    onOpenDish = { id -> nav.navigate(Routes.dishDetail(id)) },
                    onCopyDish = { id -> nav.navigate(Routes.copyDish(id)) },
                )
            }
            composable(Routes.MINE) { MineScreen() }
            composable(Routes.ADD_MEAL) {
                AddDayFoodScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.NEW_DISH) { entry ->
                val dishId = entry.arguments?.getString("dishId")?.toLongOrNull()?.takeIf { it > 0 }
                val importDishId = entry.arguments?.getString("importDishId")?.toLongOrNull()?.takeIf { it > 0 }
                NewDishScreen(
                    editingDishId = dishId,
                    importDishId = importDishId,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.DISH_DETAIL) { entry ->
                val dishId = entry.arguments?.getString("dishId")?.toLongOrNull() ?: return@composable
                DishDetailScreen(
                    dishId = dishId,
                    onBack = { nav.popBackStack() },
                    onEdit = { id -> nav.navigate(Routes.newDish(id)) },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavController, currentRoute: String?) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        bottomTabs.take(2).forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        // 中间留出 FAB 占位
        NavigationBarItem(
            selected = false,
            onClick = { },
            icon = { Spacer(Modifier.size(24.dp)) },
            enabled = false,
        )
        bottomTabs.drop(2).forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    nav.navigate(tab.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    }
}

@Composable
private fun CenterPlusFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary)  // 外环 Tertiary 橙 2dp 描边效果
            .padding(2.dp)
            .clip(CircleShape),
    ) {
        Icon(Icons.Filled.Add, contentDescription = "添加餐食", modifier = Modifier.size(28.dp))
    }
}
