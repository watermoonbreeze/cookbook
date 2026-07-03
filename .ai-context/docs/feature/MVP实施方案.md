# MVP 细化实施方案

## 一、实施总览

基于当前 KMP 脚手架（`shared` + `androidApp` + `iosApp`），分 **5 个阶段** 逐步实施 MVP 功能。每个阶段完成后可独立验证，确保增量可用。

```
阶段1：基础设施搭建        ──→  依赖引入、数据库、DI 框架跑通
阶段2：数据层实现          ──→  Repository、数据存取能力就绪
阶段3：Domain 层实现       ──→  UseCase、业务逻辑可测试
阶段4：UI 层实现（核心页面） ──→  四个页面逐个搭建
阶段5：串联打磨            ──→  主题适配、预置数据、体验优化
```

---

## 阶段1：基础设施搭建

> 目标：引入所有依赖，配置 SQLDelight、Koin、Navigation，确保项目可编译运行

### 1.1 更新版本目录 `gradle/libs.versions.toml`

需要新增的依赖：

| 库 | 用途 | 建议版本 |
|---|------|---------|
| SQLDelight | 跨平台数据库 | 2.0.1 |
| Koin | 依赖注入 | 3.5.3 (core) / 3.5.3 (android) |
| kotlinx-datetime | 日期处理 | 0.5.0 |
| kotlinx-coroutines | 协程 | 1.7.3 |
| Navigation Compose | Android 导航 | 2.7.6 |
| lifecycle-viewmodel-compose | ViewModel | 2.7.0 |

```toml
# 新增 versions
[versions]
sqldelight = "2.0.1"
koin = "3.5.3"
kotlinx-datetime = "0.5.0"
kotlinx-coroutines = "1.7.3"
navigation-compose = "2.7.6"
lifecycle-viewmodel-compose = "2.7.0"

# 新增 libraries
[libraries]
sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }

koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }

kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }

navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation-compose" }
lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle-viewmodel-compose" }

# 新增 plugins
[plugins]
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

### 1.2 配置 `shared/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)        // 新增
}

kotlin {
    // ... 现有配置保持不变

    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

// SQLDelight 配置
sqldelight {
    databases {
        create("CookbookDatabase") {
            packageName.set("com.sxdbsm.cookbook.db")
        }
    }
}
```

### 1.3 配置 `androidApp/build.gradle.kts`

新增依赖：

```kotlin
dependencies {
    // ... 现有依赖保持不变
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
}
```

### 1.4 验证标准

- `./gradlew :shared:build` 编译通过
- `./gradlew :androidApp:assembleDebug` 编译通过
- APP 可正常启动（仍显示原来的 Hello 页面即可）

---

## 阶段2：数据层实现（shared 模块）

> 目标：SQLDelight 表定义、Platform Driver、Repository 实现，数据存取能力就绪

### 2.1 SQLDelight 表定义

文件：`shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/Cookbook.sq`

```sql
-- 菜品表
CREATE TABLE dish (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT '',
    created_at INTEGER NOT NULL
);

-- 餐次记录表
CREATE TABLE meal (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    date TEXT NOT NULL,
    meal_type TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

-- 餐次-菜品关联表
CREATE TABLE meal_dish (
    meal_id INTEGER NOT NULL REFERENCES meal(id) ON DELETE CASCADE,
    dish_id INTEGER NOT NULL REFERENCES dish(id),
    PRIMARY KEY (meal_id, dish_id)
);

-- 收藏组合表
CREATE TABLE favorite_combo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE favorite_combo_dish (
    combo_id INTEGER NOT NULL REFERENCES favorite_combo(id) ON DELETE CASCADE,
    dish_id INTEGER NOT NULL REFERENCES dish(id),
    PRIMARY KEY (combo_id, dish_id)
);

-- 核心查询语句 --

-- 搜索菜品（模糊匹配）
searchDish:
SELECT * FROM dish WHERE name LIKE '%' || :query || '%' ORDER BY name;

-- 获取所有菜品
getAllDishes:
SELECT * FROM dish ORDER BY created_at DESC;

-- 插入菜品，返回 id
insertDish:
INSERT INTO dish (name, category, created_at) VALUES (?, ?, ?) RETURNING id;

-- 根据日期获取该天的所有餐次记录
getMealsByDate:
SELECT * FROM meal WHERE date = ? ORDER BY meal_type;

-- 根据日期范围获取餐次记录（周视图用）
getMealsByDateRange:
SELECT * FROM meal WHERE date >= :startDate AND date <= :endDate ORDER BY date, meal_type;

-- 获取某餐的所有菜品
getDishesByMealId:
SELECT d.* FROM dish d
INNER JOIN meal_dish md ON d.id = md.dish_id
WHERE md.meal_id = :mealId;

-- 插入一餐记录
insertMeal:
INSERT INTO meal (date, meal_type, created_at) VALUES (?, ?, ?) RETURNING id;

-- 添加菜品到某餐
insertMealDish:
INSERT OR IGNORE INTO meal_dish (meal_id, dish_id) VALUES (?, ?);

-- 从某餐移除菜品
removeMealDish:
DELETE FROM meal_dish WHERE meal_id = ? AND dish_id = ?;

-- 删除空餐次（没有关联菜品的餐次记录）
deleteEmptyMeal:
DELETE FROM meal WHERE id = ? AND NOT EXISTS (SELECT 1 FROM meal_dish WHERE meal_id = meal.id);

-- 获取最近使用的菜品（最近7天，按使用频率排序）
getRecentDishes:
SELECT d.*, COUNT(*) AS use_count FROM dish d
INNER JOIN meal_dish md ON d.id = md.dish_id
INNER JOIN meal m ON md.meal_id = m.id
WHERE m.date >= :sinceDate
GROUP BY d.id
ORDER BY use_count DESC
LIMIT :limit;

-- 获取历史记录（按日期倒序，分页）
getHistoryDates:
SELECT DISTINCT date FROM meal ORDER BY date DESC LIMIT :limit OFFSET :offset;

-- 收藏组合相关
getAllCombos:
SELECT * FROM favorite_combo ORDER BY created_at DESC;

getComboById:
SELECT * FROM favorite_combo WHERE id = ?;

getDishesByComboId:
SELECT d.* FROM dish d
INNER JOIN favorite_combo_dish fcd ON d.id = fcd.dish_id
WHERE fcd.combo_id = :comboId;

insertCombo:
INSERT INTO favorite_combo (name, created_at) VALUES (?, ?) RETURNING id;

insertComboDish:
INSERT OR IGNORE INTO favorite_combo_dish (combo_id, dish_id) VALUES (?, ?);

deleteCombo:
DELETE FROM favorite_combo WHERE id = ?;
```

### 2.2 平台 Driver 实现

**`shared/src/commonMain/kotlin/com/sxdbsm/cookbook/data/db/DatabaseDriverFactory.kt`**

```kotlin
// expect 声明
expect class DatabaseDriverFactory {
    fun create(): SqlDriver
}
```

**`shared/src/androidMain/kotlin/com/sxdbsm/cookbook/data/db/DatabaseDriverFactory.android.kt`**

```kotlin
// Android actual —— 需要 Context
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun create(): SqlDriver {
        return AndroidSqliteDriver(CookbookDatabase.Schema, context, "cookbook.db")
    }
}
```

**`shared/src/iosMain/kotlin/com/sxdbsm/cookbook/data/db/DatabaseDriverFactory.ios.kt`**

```kotlin
// iOS actual
actual class DatabaseDriverFactory {
    actual fun create(): SqlDriver {
        return NativeSqliteDriver(CookbookDatabase.Schema, "cookbook.db")
    }
}
```

### 2.3 Domain Model 定义

文件位置：`shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/model/`

```kotlin
// MealType.kt
enum class MealType { BREAKFAST, LUNCH, DINNER }

// Dish.kt
data class Dish(
    val id: Long = 0,
    val name: String,
    val category: String = "",
)

// Meal.kt
data class Meal(
    val id: Long = 0,
    val date: LocalDate,
    val type: MealType,
    val dishes: List<Dish>,
)

// DayMenu.kt
data class DayMenu(
    val date: LocalDate,
    val breakfast: Meal?,
    val lunch: Meal?,
    val dinner: Meal?,
)

// FavoriteCombo.kt
data class FavoriteCombo(
    val id: Long = 0,
    val name: String,
    val dishes: List<Dish>,
)
```

### 2.4 Repository 实现

**`shared/src/commonMain/kotlin/com/sxdbsm/cookbook/data/repository/DishRepository.kt`**

```kotlin
class DishRepository(private val db: CookbookDatabase) {

    fun searchDishes(query: String): Flow<List<Dish>>
        // 调用 db.cookbookQueries.searchDish(query)，映射为 Domain Model

    fun getRecentDishes(limit: Int = 20): Flow<List<Dish>>
        // 调用 getRecentDishes，sinceDate = 7天前

    suspend fun addDish(name: String, category: String = ""): Long
        // 调用 insertDish

    fun getAllDishes(): Flow<List<Dish>>
}
```

**`shared/src/commonMain/kotlin/com/sxdbsm/cookbook/data/repository/MealRepository.kt`**

```kotlin
class MealRepository(private val db: CookbookDatabase) {

    fun getDayMenu(date: LocalDate): Flow<DayMenu>
        // 查询该日期的所有 meal，再查每个 meal 的 dishes，聚合为 DayMenu

    fun getWeekMenus(startDate: LocalDate): Flow<List<DayMenu>>
        // 查 7 天的数据，聚合为 List<DayMenu>

    suspend fun addDishToMeal(date: LocalDate, mealType: MealType, dishId: Long)
        // 查找或创建 meal 记录，再 insertMealDish

    suspend fun removeDishFromMeal(mealId: Long, dishId: Long)
        // removeMealDish + deleteEmptyMeal

    suspend fun copyMealToDate(sourceMealId: Long, targetDate: LocalDate, targetMealType: MealType)
        // 读取源 meal 的所有 dishes，逐个 addDishToMeal 到目标

    fun getHistoryDates(page: Int, pageSize: Int = 20): Flow<List<DayMenu>>
        // 分页查询历史
}
```

**`shared/src/commonMain/kotlin/com/sxdbsm/cookbook/data/repository/ComboRepository.kt`**

```kotlin
class ComboRepository(private val db: CookbookDatabase) {

    fun getAllCombos(): Flow<List<FavoriteCombo>>

    suspend fun createCombo(name: String, dishIds: List<Long>): Long

    suspend fun deleteCombo(id: Long)
}
```

### 2.5 验证标准

- 编写 `commonTest` 单元测试，验证 Repository 的 CRUD 操作
- `./gradlew :shared:allTests` 通过

---

## 阶段3：Domain 层 UseCase 实现（shared 模块）

> 目标：封装业务逻辑，供 UI 层 ViewModel 调用

### 3.1 UseCase 列表

文件位置：`shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/usecase/`

| UseCase | 职责 | 调用的 Repository |
|---------|------|-------------------|
| `GetWeekMenuUseCase` | 获取指定周的 7 天菜单 | MealRepository |
| `GetDayMenuUseCase` | 获取某天的三餐详情 | MealRepository |
| `SaveMealUseCase` | 向某餐添加/移除菜品 | MealRepository |
| `CopyMealUseCase` | 复用历史某餐到目标日期 | MealRepository |
| `SearchDishUseCase` | 搜索菜品 + 获取最近使用 | DishRepository |
| `CreateDishUseCase` | 新建自定义菜品 | DishRepository |
| `GetHistoryUseCase` | 分页获取历史菜单 | MealRepository |
| `ManageComboUseCase` | 收藏组合的增删查 | ComboRepository |

### 3.2 关键 UseCase 伪实现

```kotlin
class GetWeekMenuUseCase(private val mealRepo: MealRepository) {
    operator fun invoke(weekStartDate: LocalDate): Flow<List<DayMenu>> {
        return mealRepo.getWeekMenus(weekStartDate)
    }
}

class SaveMealUseCase(private val mealRepo: MealRepository, private val dishRepo: DishRepository) {
    // 添加菜品到某餐（如果菜品不存在则先创建）
    suspend fun addDish(date: LocalDate, mealType: MealType, dishName: String): Long {
        val dish = dishRepo.findByName(dishName)
            ?: dishRepo.addDish(dishName)
        mealRepo.addDishToMeal(date, mealType, dish)
        return dish
    }

    suspend fun removeDish(mealId: Long, dishId: Long) {
        mealRepo.removeDishFromMeal(mealId, dishId)
    }
}

class CopyMealUseCase(private val mealRepo: MealRepository) {
    // 复用：可选整天或单餐
    suspend fun copyMeal(sourceMealId: Long, targetDate: LocalDate, targetMealType: MealType) {
        mealRepo.copyMealToDate(sourceMealId, targetDate, targetMealType)
    }

    suspend fun copyDayMenu(sourceDate: LocalDate, targetDate: LocalDate) {
        // 逐餐复制
    }
}
```

### 3.3 Koin DI 配置

**`shared/src/commonMain/kotlin/com/sxdbsm/cookbook/di/SharedModule.kt`**

```kotlin
val sharedModule = module {
    // Database
    single { get<DatabaseDriverFactory>().create() }
    single { CookbookDatabase(get()) }

    // Repository
    single { DishRepository(get()) }
    single { MealRepository(get()) }
    single { ComboRepository(get()) }

    // UseCase
    factory { GetWeekMenuUseCase(get()) }
    factory { GetDayMenuUseCase(get()) }
    factory { SaveMealUseCase(get(), get()) }
    factory { CopyMealUseCase(get()) }
    factory { SearchDishUseCase(get()) }
    factory { CreateDishUseCase(get()) }
    factory { GetHistoryUseCase(get()) }
    factory { ManageComboUseCase(get()) }
}
```

### 3.4 验证标准

- UseCase 单元测试通过（mock Repository）
- Koin module check 通过

---

## 阶段4：UI 层实现（androidApp 模块）

> 目标：逐个实现四个核心页面，每完成一个页面即可交互验证

### 实施顺序与依赖关系

```
4.1 主题 + 导航骨架 + Koin 初始化
 │
 ├→ 4.2 首页（周视图）      ── 可独立验证
 │    │
 │    └→ 4.3 当日菜单详情    ── 依赖首页导航进入
 │         │
 │         ├→ 4.4 添加菜品弹窗  ── 依赖菜单详情触发
 │         │
 │         └→ 4.5 历史菜单页    ── 依赖菜单详情触发
 │
 └→ 4.6 收藏组合功能        ── 融入添加弹窗和菜单详情
```

### 4.1 主题 + 导航骨架 + Koin 初始化

**4.1.1 Application 类（Koin 初始化）**

新建 `androidApp/src/main/java/com/sxdbsm/cookbook/android/CookbookApp.kt`

```kotlin
class CookbookApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@CookbookApp)
            modules(sharedModule, androidModule)
        }
    }
}
```

`AndroidManifest.xml` 中注册 `android:name=".CookbookApp"`。

**4.1.2 Android DI 模块**

`androidApp/.../di/AndroidModule.kt`

```kotlin
val androidModule = module {
    single { DatabaseDriverFactory(get()) }  // Android Context → Driver

    // ViewModel
    viewModel { HomeViewModel(get()) }
    viewModel { (date: LocalDate) -> DayMenuViewModel(date, get(), get(), get()) }
    viewModel { (date: LocalDate, mealType: MealType) -> AddDishViewModel(date, mealType, get(), get(), get()) }
    viewModel { HistoryViewModel(get()) }
}
```

**4.1.3 主题配置**

改造现有 `MyApplicationTheme.kt`，加入规划中的绿色系配色：

```kotlin
// Light 主色调
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A6741),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCBF0BA),
    onPrimaryContainer = Color(0xFF082100),
    secondary = Color(0xFF54634D),
    secondaryContainer = Color(0xFFD7E8CD),
    tertiary = Color(0xFF386663),
    tertiaryContainer = Color(0xFFBBECE8),
    background = Color(0xFFFDFDF5),
    surface = Color(0xFFFDFDF5),
    surfaceVariant = Color(0xFFDFE4D7),
    error = Color(0xFFBA1A1A),
)
```

**4.1.4 导航骨架**

`androidApp/.../navigation/AppNavigation.kt`

```kotlin
// 路由定义
object Routes {
    const val HOME = "home"
    const val DAY_MENU = "day_menu/{date}"     // 参数: yyyy-MM-dd
    const val HISTORY = "history"
    // AddDish 使用 ModalBottomSheet，不走导航路由
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(navController) }
        composable(Routes.DAY_MENU, arguments = ...) { DayMenuScreen(navController) }
        composable(Routes.HISTORY) { HistoryScreen(navController) }
    }
}
```

**验证**：APP 启动显示空的首页框架，导航可跳转。

---

### 4.2 首页 — 周视图

**涉及文件**：
- `ui/home/HomeScreen.kt` — 页面 Composable
- `ui/home/HomeViewModel.kt` — 状态管理
- `ui/components/WeekCalendar.kt` — 周日历组件
- `ui/components/MealCard.kt` — 单餐卡片组件

**HomeViewModel 状态设计**：

```kotlin
data class HomeUiState(
    val currentWeekStart: LocalDate,    // 当前周的周一
    val selectedDate: LocalDate,        // 选中的日期（默认今天）
    val weekMenus: List<DayMenu>,       // 7天菜单数据
    val isLoading: Boolean = false,
)

class HomeViewModel(private val getWeekMenu: GetWeekMenuUseCase) : ViewModel() {
    // 事件：选择日期、切换上/下周
    fun selectDate(date: LocalDate)
    fun previousWeek()
    fun nextWeek()
}
```

**HomeScreen 布局**：

```
TopAppBar: "Cookbook"               [历史]
─────────────────────────────────
WeekCalendar:  一  二  三  四  五  六  日
               27  28  29  30   1   2   3
                       ▲(选中高亮)
─────────────────────────────────
选中日期的三餐摘要（3个 MealCard 纵向排列）
  MealCard(早餐): 牛奶、全麦面包
  MealCard(午餐): 暂未记录
  MealCard(晚餐): 暂未记录
```

**WeekCalendar 组件要点**：
- 基于 `LazyRow` 或 `Row`，显示周一到周日
- 左右箭头切换周 / 支持左右滑动（`HorizontalPager`）
- 今天加圆形背景高亮，选中日期加边框
- 日期下方可显示小圆点表示有记录

**MealCard 组件要点**：
- `OutlinedCard`，标题显示餐次名称
- 内容显示菜品名称（Chip 形式或逗号分隔文本）
- 无记录时显示"暂未记录"灰色文字
- 整个卡片可点击 → 导航到当日菜单详情

**验证**：首页显示周日历，可切换周和日期，点击卡片可导航。

---

### 4.3 当日菜单详情

**涉及文件**：
- `ui/daymenu/DayMenuScreen.kt`
- `ui/daymenu/DayMenuViewModel.kt`
- `ui/components/DishChip.kt` — 菜品标签组件

**DayMenuViewModel 状态设计**：

```kotlin
data class DayMenuUiState(
    val date: LocalDate,
    val dayMenu: DayMenu?,
    val showAddDishSheet: Boolean = false,
    val addDishMealType: MealType? = null,    // 当前正在添加菜品的餐次
)

class DayMenuViewModel(...) : ViewModel() {
    fun showAddDish(mealType: MealType)    // 打开添加弹窗
    fun dismissAddDish()                    // 关闭添加弹窗
    fun removeDish(mealId: Long, dishId: Long)
    fun navigateToHistory()
    fun saveAsCombo(mealType: MealType, comboName: String)
}
```

**DayMenuScreen 布局**：

```
TopAppBar: ◀ 4月28日 周二
─────────────────────────────────
三餐分区（Column + 3 个 Section）:

  "早餐"标题                [+添加]
  FlowRow: [牛奶 ✕] [全麦面包 ✕]

  "午餐"标题                [+添加]
  "暂未记录"

  "晚餐"标题                [+添加]
  "暂未记录"

─────────────────────────────────
底部操作栏:
  [从历史复用]    [保存为收藏组合]
```

**DishChip 组件**：
- 基于 `InputChip`，显示菜品名称
- 尾部 ✕ 图标，点击触发删除
- 使用 `FlowRow`（Material3 `FlowRow`）自动换行排列

**验证**：从首页点击进入菜单详情，显示三餐分区，可删除菜品标签。

---

### 4.4 添加菜品弹窗

**涉及文件**：
- `ui/adddish/AddDishSheet.kt`
- `ui/adddish/AddDishViewModel.kt`

**AddDishViewModel 状态设计**：

```kotlin
data class AddDishUiState(
    val searchQuery: String = "",
    val searchResults: List<Dish> = emptyList(),
    val recentDishes: List<Dish> = emptyList(),
    val combos: List<FavoriteCombo> = emptyList(),
    val selectedDishes: Set<Long> = emptySet(),   // 多选
)

class AddDishViewModel(...) : ViewModel() {
    fun updateSearch(query: String)         // 实时搜索
    fun toggleDishSelection(dishId: Long)   // 选中/取消
    fun confirmSelection()                  // 确认添加
    fun createAndAddDish(name: String)      // 新建菜品并添加
    fun addCombo(comboId: Long)             // 一键添加组合
}
```

**AddDishSheet 布局**（`ModalBottomSheet`）：

```
标题: "添加菜品到 [早餐]"
─────────────────────────────────
SearchBar: 🔍 搜索菜品...
─────────────────────────────────
搜索有内容时 → 搜索结果列表
  每项: [菜品名] [✓已选/+选择]
  无结果: [+ 新建菜品 "xxx"]

搜索为空时 → 显示以下两个区域:

  "最近使用"
  FlowRow: [牛奶] [全麦面包] [鸡蛋] [小米粥]

  "收藏组合"
  ListItem: ▸ 日常早餐（牛奶+面包+鸡蛋）
  ListItem: ▸ 清淡晚餐（小米粥+青菜）

─────────────────────────────────
底部: [确认添加 (3)]     ← 显示已选数量
```

**交互要点**：
- 搜索框使用 `debounce`（300ms）避免频繁查询
- 最近使用取最近 7 天的菜品，按频率排序，最多显示 20 个
- 点击收藏组合 → 该组合的所有菜品直接加入选中集合
- 确认后批量调用 `SaveMealUseCase.addDish`

**验证**：在菜单详情点 [+添加] 弹出弹窗，可搜索、选择、新建菜品，确认后菜单详情刷新。

---

### 4.5 历史菜单页

**涉及文件**：
- `ui/history/HistoryScreen.kt`
- `ui/history/HistoryViewModel.kt`

**HistoryViewModel 状态设计**：

```kotlin
data class HistoryUiState(
    val historyMenus: List<DayMenu> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = true,
    val copyTargetDate: LocalDate? = null,    // 复用到哪天（从导航参数获取）
)

class HistoryViewModel(...) : ViewModel() {
    fun loadMore()                           // 滚动加载
    fun copyMeal(sourceMealId: Long, targetMealType: MealType)
    fun copyDayMenu(sourceDate: LocalDate)
}
```

**HistoryScreen 布局**：

```
TopAppBar: ◀ 历史菜单
─────────────────────────────────
LazyColumn（按日期倒序）:

  DateHeader: "4月27日 周一"
  CompactMealRow: 早：牛奶、鸡蛋、面包
  CompactMealRow: 午：米饭、红烧肉、炒青菜
  CompactMealRow: 晚：小米粥、凉拌黄瓜    [复用]

  DateHeader: "4月26日 周日"
  ...

  底部加载更多指示器
```

**复用交互流程**：
1. 点击 [复用] → 弹出选择弹窗："复用整天 / 仅复用该餐"
2. 选择后 → 调用 `CopyMealUseCase`
3. 显示 Snackbar 提示"已复用到 X月X日"
4. 返回菜单详情页，数据已更新

**验证**：历史页显示过往记录，点击复用可将菜品复制到目标日期。

---

### 4.6 收藏组合功能

融入已有页面，不需要独立页面：

- **保存组合**：在当日菜单详情页底部 [保存为收藏组合] → 弹出 `AlertDialog`，输入组合名称 → 调用 `ManageComboUseCase.create`
- **使用组合**：在添加菜品弹窗的"收藏组合"区域展示 → 点击一键添加
- **删除组合**：长按收藏组合项 → 确认删除

---

## 阶段5：串联打磨

> 目标：整体体验优化，达到可用状态

### 5.1 预置常见菜品数据

在首次启动（或数据库为空时）预置常见菜品，分类组织：

```
主食：米饭、面条、馒头、全麦面包、小米粥、八宝粥、杂粮饭
蛋奶：鸡蛋、牛奶、豆浆、酸奶
荤菜：红烧肉、清蒸鱼、鸡胸肉、虾仁、牛肉
素菜：西红柿炒蛋、炒青菜、凉拌黄瓜、清炒西兰花、炒豆芽
汤类：紫菜蛋花汤、番茄蛋汤、冬瓜排骨汤
```

在 `CookbookDatabase` 创建后检查 dish 表是否为空，为空则批量插入。

### 5.2 体验细节

- 空状态处理：首页无任何记录时，显示引导文案
- 今日快捷入口：首页直接点击餐次卡片进入菜单详情（已在 4.2 实现）
- 删除确认：删除菜品标签时无需确认（操作轻量、可重新添加），删除收藏组合需确认
- 页面切换动画：使用 Material3 标准过渡动效

### 5.3 验证标准（MVP 完成标准）

- [ ] 首页周视图正常显示，可切换周/日期
- [ ] 可进入当日菜单详情，三餐分区正确
- [ ] 可通过添加弹窗搜索/选择/新建菜品
- [ ] 添加的菜品在菜单详情和首页摘要中正确显示
- [ ] 可删除已添加的菜品
- [ ] 历史菜单按日期倒序显示
- [ ] 可从历史复用整天或单餐菜品
- [ ] 可保存和使用收藏组合
- [ ] 预置菜品首次启动自动填充
- [ ] APP 杀掉重启后数据不丢失
