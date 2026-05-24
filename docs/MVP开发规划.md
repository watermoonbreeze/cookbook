# 慢性病饮食规划 APP — MVP 开发规划

> 最后更新：2026-05-19（v2 全面重写，对齐 5 Tab 架构 + 纯 Compose）
> 详细界面设计见：`.claude/docs/feature/界面探讨.md`
> 详细数据库设计见：`.claude/docs/feature/数据库设计方案.md`
> 配色/主题规范见：`docs/技术栈与主题风格.md`

## 一、MVP 目标

解决慢性病人群（自己）每天的核心痛点：

1. **不知道吃什么** → 主页热门/最近/计划，决策疲劳缓解
2. **记录麻烦** → 中间 + 号统一录入入口，菜品/食材选择全屏弹框
3. **复用困难** → 食历长按复用，新建菜品支持导入已有作模板
4. **慢病食材风险** → 食材选择按人群分类（高血压/糖尿病/...），推荐角标

MVP 交付完整 5 Tab 应用骨架：**首页 / 食历 / +号 / 菜品 / 我的**。

## 二、技术栈

详见 `docs/技术栈与主题风格.md`。

核心点：
- KMP 跨平台，shared 模块装 Domain + Data，**纯本地 SQLite**
- Android 端 **单 MainActivity + 纯 Compose NavHost**（不混 Fragment）
- iOS 端 **MVP 不开发**，只保证 shared 能编出 framework
- 主题：Material 3 + Light/Dark 双套 + Extended Colors（Success/Warning/Danger）

## 三、代码架构

### 3.1 整体分层

```
┌─────────────────────────────────────────────────┐
│  androidApp (纯 Compose)         │  iosApp (一期) │  ← UI 层
│  MainActivity + NavHost          │   SwiftUI      │
│  + 5 Screens + 组件 + ViewModel  │                │
├─────────────────────────────────────────────────┤
│              shared / commonMain                 │
│  ┌───────────────────┐  ┌─────────────────────┐ │
│  │  Domain 层 (业务)  │  │    Data 层 (数据)    │ │
│  │  UseCase, Model    │  │  Repository, SQLDelight │
│  └───────────────────┘  └─────────────────────┘ │
│              shared / androidMain                │
│  ┌─────────────────────────────────────────┐    │
│  │ expect 实现：BackupManager / ThemePersist│    │
│  └─────────────────────────────────────────┘    │
└─────────────────────────────────────────────────┘
```

### 3.2 项目模块结构

```
Cookbook/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/libs.versions.toml
│
├── shared/
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/com/sxdbsm/cookbook/
│       │   │   ├── data/
│       │   │   │   ├── db/                    # SQLDelight 包装
│       │   │   │   ├── repository/            # DishRepository、MealRecordRepository、
│       │   │   │   │                          # IngredientRepository、FoodCategoryRepository、
│       │   │   │   │                          # PreferenceRepository、HealthProfileRepository
│       │   │   │   ├── mapper/                # DB ↔ Domain Model
│       │   │   │   └── seed/                  # PresetDataSeeder（预置数据灌入）
│       │   │   ├── domain/
│       │   │   │   ├── model/                 # Dish、Ingredient、MealRecord、
│       │   │   │   │                          # DayMealCardData、HealthProfile、UserPreference
│       │   │   │   └── usecase/               # GetHomePageData、GetTimelineData、
│       │   │   │                              # SaveMealRecord、GetPopularDishes、
│       │   │   │                              # SearchDishes、GetIngredientsByCategory ...
│       │   │   ├── platform/                  # expect class BackupManager
│       │   │   └── di/                        # Koin SharedModule
│       │   └── sqldelight/com/sxdbsm/cookbook/db/
│       │       └── Cookbook.sq                # 表定义 + 查询
│       ├── androidMain/                       # actual BackupManager（文件复制）
│       └── iosMain/                           # actual BackupManager（NSFileManager，一期）
│
├── androidApp/
│   └── src/main/java/com/sxdbsm/cookbook/android/
│       ├── MainActivity.kt                    # 唯一 Activity，含 NavHost + Theme
│       ├── ui/
│       │   ├── nav/NavGraph.kt
│       │   ├── home/HomeScreen.kt + HomeViewModel
│       │   ├── timeline/FoodTimelineScreen.kt + ViewModel + TimelineSyncController
│       │   ├── addmeal/AddDayFoodScreen.kt + ViewModel
│       │   ├── dishes/DishesScreen.kt + DishesViewModel + DishesMode
│       │   ├── mine/MineScreen.kt + 子页（HealthProfile/Backup/About）
│       │   ├── newdish/NewDishScreen.kt + ViewModel
│       │   ├── picker/
│       │   │   ├── DishPickerScreen.kt        # 全屏 Compose Dialog
│       │   │   └── IngredientPickerScreen.kt
│       │   ├── component/                     # DayMealCardView、DishMiniCard、
│       │   │                                  # DishRow、IngredientChip、EmptyState
│       │   └── theme/Theme.kt + ExtendedColors + ThemeRepository
│       └── di/AndroidModule.kt
│
└── iosApp/                                    # MVP 不实现
```

### 3.3 核心数据模型（Domain Model）

```kotlin
// 菜品（含完整字段）
data class Dish(
    val id: Long = 0,
    val name: String,
    val tags: List<String>,            // dish_tag_rel → dish_tag.name
    val cookingMethod: String?,
    val preference: Double = 0.0,    // 热度 0-100，自动累加，UI 不暴露编辑
    val specialNote: String = "",
    val description: String = "",
    val imagePath: String = "",
    val ingredients: List<DishIngredient>,
)

data class DishIngredient(
    val ingredient: Ingredient,
    val quantity: Double?,
    val unit: String,
    val isMain: Boolean,
)

data class Ingredient(
    val id: Long,
    val name: String,
    val alias: String,
    val pinyin: String,
    val imagePath: String,
    val defaultUnit: String,
)

// 餐次记录
data class MealRecord(
    val id: Long = 0,
    val date: LocalDate,
    val mealTypeId: Long,
    val mealName: String,              // 早餐/中餐/...
    val mealTime: LocalTime,
    val dishes: List<Dish>,
    val note: String = "",
)

// 跨页复用的"日餐卡片"数据
data class DayMealCardData(
    val date: LocalDate,
    val isToday: Boolean,
    val isPlanState: Boolean,           // date > today
    val meals: List<MealSection>,
)

data class MealSection(
    val mealTypeId: Long,
    val mealName: String,
    val mealTime: LocalTime,
    val dishes: List<DishMini>,         // 仅 id/name/imagePath
)

// 健康档案
data class HealthProfile(
    val crowdTypes: List<CrowdType>,    // 多选
)

// 用户偏好
data class UserPreference(
    val themeMode: ThemeMode,           // SYSTEM/LIGHT/DARK
    val recentCount: Int = 6,
    val popularCount: Int = 6,
    val popularDays: Int = 30,
    val popularThreshold: Int = 3,
)
```

### 3.4 数据库表设计

详见 `.claude/docs/feature/数据库设计方案.md`。本节仅列表清单：

**MVP 阶段表（共 18 张）**：
- 基础字典：`cooking_method`、`measurement_unit`、`crowd_type`
- 食材体系：`ingredient`、`food_category`、`ingredient_category`、`crowd_ingredient`
- 菜品体系：`dish`、`dish_tag`、`dish_tag_rel`、`dish_ingredient`
- 餐序：`meal_type`（6 固定 + 用户加餐）
- 餐次记录：`meal_record`、`meal_record_dish`
- 收藏：`favorite_combo`、`favorite_combo_dish`
- 用户：`user_preferences`、`user_health_profile`（2026-05-19 新增）

**一期表**：`nutrient`、`ingredient_nutrient`、`dish_crowd`

## 四、页面功能总览

### 4.1 页面架构图

```
MainActivity (单 Activity)
└── BottomNavigationView
    ├── 🏠 首页    HomeScreen           搜索/热门/最近/计划(2)
    ├── 📅 食历    FoodTimelineScreen   时间轴+15天分页+联动
    ├── ⊕  +号    → AddDayFoodScreen   统一录入入口
    ├── 🥗 菜品    DishesScreen(Browse) 浏览+热度+排序+管理
    └── 👤 我的    MineScreen           主题/健康档案/备份/Coming Soon

跨页弹框（全屏 Compose Dialog）：
- DishPickerScreen        → 嵌入 DishesScreen(Select)
- IngredientPickerScreen  → 独立内容，左右双栏

独立 Screen：
- NewDishScreen           → 新建/编辑菜品（导入 / #复制）
```

### 4.2 各 Screen 简要说明（详见 `界面探讨.md`）

| Screen | 核心功能 | 详细章节 |
|--------|---------|---------|
| HomeScreen | 搜索 + 热门(6) + 最近(6) + 计划(当天+未来最新) | 第三章 |
| FoodTimelineScreen | 时间轴 + 15 天分页 + 联动滚动 | 第四章 |
| DishesScreen | 热度横滑区 + 排序 tab + DishRow 列表，双模式 | 第五章 |
| MineScreen | 用户卡 + 健康档案/主题/备份/Coming Soon/关于 | 第六章 |
| AddDayFoodScreen | 日期+餐次+时间+菜品多选+备注 | 第七章 |
| NewDishScreen | 菜名+标签+烹饪+食材清单+导入（**热度不展示，系统自动累加**） | 第八章 |
| DishPickerScreen | 全屏 Dialog，嵌入 DishesScreen(Select) | 第九章 |
| IngredientPickerScreen | 全屏 Dialog，左侧分类树+右侧食材网格 | 第十章 |

## 五、MVP 核心交互流程

### 场景 1：主页快加菜品
```
打开 APP → HomeScreen
→ 顶部搜索框点击 → DishPickerScreen (Select 多选模式)
→ 选若干菜品 → [完成] → 写入今天对应餐次（按当前时间推荐）
→ 主页计划模块刷新
```

### 场景 2：+号 完整录入
```
点 + 号 → AddDayFoodScreen
→ 选日期（默认今天）→ 选餐次 → 改时间
→ [+ 添加菜品] → DishPickerScreen (多选)
→ 选完 → 回 Activity → 填备注
→ [保存] → 写入 meal_record + meal_record_dish
   （触发器自动 dish.preference += 0.1）
→ 主页/食历自动刷新
```

### 场景 3：新建菜品
```
DishesScreen 右上 + 号 → NewDishScreen
→ 填菜名 → 加标签 → 选烹饪
→ [+ 添加食材] → IngredientPickerScreen
→ 选食材（按分类筛选，看人群角标）→ [完成]
→ 设主料/辅料 + 用量
→ [保存] → 写入 dish + 关联表
→ DishesScreen 刷新
```

### 场景 4：基于已有菜品另存（导入）
```
NewDishScreen 顶部 [导入] → DishPickerScreen (Select 单选)
→ 选某菜品 → 字段全量回填
→ 标签栏自动追加 #复制 Chip（橙色区分）
→ 用户改菜名/食材某行
→ [保存] → 新 dish 记录（不影响原菜品）
```

### 场景 5：食历回看 + 复用
```
点底部 [食历] → FoodTimelineScreen
→ 滑动列表（时间轴联动）→ 找到某天
→ 长按 DayMealCard → [复用到今天/明天]
→ 启动 AddDayFoodScreen 预填字段
→ 用户确认/微调 → [保存]
```

### 场景 6：设置健康档案
```
点底部 [我的] → MineScreen → 个人健康档案
→ 多选人群（高血压、糖尿病等）→ 保存
→ user_health_profile 表 INSERT
→ IngredientPickerScreen 自动按已选人群高亮角标
```

## 六、开发任务拆解

### 阶段 1：基础设施（2-3 天）
- [ ] Gradle 配置 / 版本目录 / 阿里云镜像
- [ ] shared 模块 / commonMain / androidMain 骨架
- [ ] SQLDelight 配置 + Cookbook.sq 全表 schema（含触发器 `trg_dish_popularity_after_insert`）
- [ ] Koin DI 配置（SharedModule + AndroidModule）
- [ ] MainActivity + NavHost + Theme 骨架
- [ ] PresetDataSeeder（预置 cooking_method/measurement_unit/crowd_type/food_category 等）

### 阶段 2：通用 Compose 组件（2-3 天）
- [ ] Theme.kt + ExtendedColors + ThemeRepository
- [ ] DayMealCardView（最复用，先做）
- [ ] DishMiniCard 80dp（横滑卡片单元）
- [ ] DishRow（DishesScreen 列表行）
- [ ] IngredientChip
- [ ] EmptyState
- [ ] 缩略图占位策略（hash 选色块）

### 阶段 3：菜品体系页面（3-4 天）
- [ ] DishesScreen（Browse 模式）+ ViewModel + 热度查询
- [ ] DishRow 长按菜单（基于此另存/删除）
- [ ] NewDishScreen + 字段表单 + 食材清单编辑
- [ ] DishPickerScreen（全屏 Dialog，嵌入 DishesScreen(Select)）
- [ ] [导入] 流程（单选回填 + #复制 标签）

### 阶段 4：食材选择 + 预置数据（2-3 天）
- [ ] IngredientPickerScreen（手风琴左侧 + 右侧网格 + 顶部搜索）
- [ ] 三种查询路径（全部/普通分类/人群分类）
- [ ] 预置常见食材数据（50-100 个） + 图片 emoji 占位
- [ ] 预置 food_category 二级（健康饮食 4-5 项、人群分类 4 项）
- [ ] 预置部分 crowd_ingredient 数据（高血压/痛风等的核心禁忌食材）

### 阶段 5：首页 + 食历（3-4 天）
- [ ] HomeScreen + ViewModel + GetHomePageData UseCase
- [ ] 热门/最近横滑（LazyRow + DishMiniCard）
- [ ] 计划模块（DayMealCardView 复用）
- [ ] FoodTimelineScreen + 15 天分页（Paging 3 或手写）
- [ ] 时间轴渲染（Canvas 或 LazyRow）
- [ ] 联动滚动控制器（TimelineSyncController）

### 阶段 6：餐食录入（2 天）
- [ ] AddDayFoodScreen + ViewModel
- [ ] 日期/餐次/时间选择器
- [ ] 调起 DishPickerScreen 流程
- [ ] 计划态横幅（date > today）
- [ ] 长按 DayMealCard "复用到今天/明天" → 启动 AddDayFoodScreen 预填

### 阶段 7：我的 + 健康/主题/备份（2-3 天）
- [ ] MineScreen 列表布局
- [ ] HealthProfileScreen（多选人群）
- [ ] 主题切换（写 user_preferences + 订阅）
- [ ] BackupScreen + BackupManager（expect/actual）
- [ ] AboutScreen
- [ ] "厨房小助手" 占位 Coming Soon

### 阶段 8：联调与收尾（2-3 天）
- [ ] 跨页数据同步验证（Flow 订阅自动刷新）
- [ ] 边界场景（空数据、首次启动、备份恢复）
- [ ] Dark 模式视觉走查
- [ ] 性能（热度查询 / 食历滚动）
- [ ] 应用图标 + 启动页

**预计 MVP 总工作量：18-25 天**（取决于设计调整频率）

## 七、与原 MVP 方案的主要差异（2026-05-19 重写）

| 维度 | 原方案 (v1) | 新方案 (v2) |
|------|------------|------------|
| UI 架构 | Fragment + Compose 混合 | 纯 Compose，单 Activity |
| 一级页 | 单 Activity 周视图 | 5 Tab 底部导航 |
| 录入入口 | 主页面三餐 [+添加] 弹框 | 中间 + 号统一 AddDayFoodScreen |
| 历史浏览 | 历史菜单底部弹框 | 独立 FoodTimelineScreen Tab |
| 菜品库 | 隐藏在添加流程内 | 独立 DishesScreen Tab |
| 数据模型 | 3 张简表 | 18 张完整表（含食材/分类/人群/偏好/档案） |
| 健康提示 | 一期再做 | MVP 食材弹框已显示人群角标 |
| 主题切换 | 一期可选 | MVP 同步上线（Light/Dark/跟随系统） |
| iOS | P1，同步开发 | **MVP 不做**，shared 编译保留 |
| 备份 | 一期 | **MVP 包含**本地备份/恢复（导出推到一期） |

## 七 bis、2026-05-21 二审调整

| 项 | 原 v2 设计 | 二审 v2.1 |
|----|-----------|-----------|
| 菜品热度计算 | 用户手动评 0-5 星 | **自动累加**，每次添加 +0.1，0-100 分，5 颗星 ×20 分/颗 |
| HomeScreen 热门规则 | 30 天 ≥ 3 次，按次数倒序 | **按 `preference` 倒序 + 菜名升序**，无窗口阈值 |
| DishesScreen 热度横滑 | 同 30 天阈值 | 同上规则 |
| `dish.preference` 字段 | INTEGER 0-5 | **REAL 0-100** + SQLite 触发器维护 |
| `meal_record.rating` | 0-5 本餐评分 | **删除字段**（dead-end，未在 UI 任何处展示） |
| AddDayFoodScreen 评分输入 | ☆☆☆☆☆ | **去掉** |
| NewDishScreen 喜爱度输入 | ☆☆☆☆☆ | **隐藏**（新建/编辑都不显示） |
| 热度展示位置 | 无明确 | **仅** DishesScreen DishRow 行尾 + 菜品查看（详情页）顶部 |

## 八、风险与待办

详见 `.claude/docs/context_memory/明日审核清单_2026-05-19.md` + `二审调整_2026-05-21.md`。
