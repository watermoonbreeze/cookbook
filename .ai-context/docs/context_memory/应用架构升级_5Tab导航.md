# 应用架构升级：5 Tab 底部导航

> 更新时间：2026-05-19（夜间收口）
> 状态：✅ **严谨流程编排完成，详细设计已落到 docs/* 与 .ai-context/docs/feature/***
> 待办：用户明日审核 → 进入编码阶段

## 背景

原设计：单 Activity + 弹框形式（HomeFragment 等同于 MainActivity）。
本轮升级：底部导航 + 5 个一级页 + AddDayFoodActivity 集中录入入口。

## 顶层架构

```
MainActivity
└── BottomNavigationView (5 项，中间是 Action 不是 Tab)
    ├── 首页    → HomeFragment
    ├── 食历    → FoodTimelineFragment
    ├── [+号]   → 点击直接 startActivity(AddDayFoodActivity)
    ├── 菜品    → DishesFragment
    └── 我的    → MineFragment
```

## 各页职责（用户确认版）

### HomeFragment（首页）
- 顶部：搜索框
- 中段：**热门** 和 **最近** 并列两小模块
- 底部：**计划**模块，仅展示"今天 + 大于今天的最近一次餐食"，**只显示两条**

### FoodTimelineFragment（食历）
- 顶部：时间轴，从最早记录到今天（或反向），默认定位当天
- 下方：餐次频次列表，**每 15 天分页**，时间倒序，每行 = 一天的餐食
- **联动滚动**：下方列表滑动时上方时间轴同步移动
- 细节后续补充

### + 号 → AddDayFoodActivity
- 统一的"添加某天餐食"入口，非 Tab，是 Action
- 选日期 + 选餐次 + 选菜品（多个）一次完成
- 替代原 HomeFragment 单餐次 [+添加] 弹框（**原 [+添加] 入口已取消**）

### DishesFragment（菜品）
- 应用菜品库的浏览/搜索/管理 Tab
- **双重身份**：既是导航 Tab，也是菜品选择 DialogFragment 内嵌的 Fragment（一份代码两种模式：BROWSE / SELECT）
- Tab 模式下 [+ 添加菜品] 按钮 → 启动新建菜品 Activity

### MineFragment（我的）
- 主题切换（迁移自原 ⋮ 菜单的"外观"）
- 个人健康档案（选人群标签：高血压/糖尿病/...）
- 数据备份 / 导出 / 恢复
- 关于 / 版本号 / 反馈
- **厨房小助手**（新功能，细节待定）

## 弹框层面

| 弹框 | 形式 | 内嵌 Fragment | 添加按钮 |
|------|------|--------------|---------|
| 菜品选择 DialogFragment | **全屏** | DishesFragment（SELECT 模式） | [+ 添加菜品]（跳新建菜品 Activity） |
| 食材选择 DialogFragment | **全屏** | 独立内容 | [+ 添加食材]（即时新建食材记录） |

两个弹框**各自只有自己的添加按钮**，不交叉。

## 影响范围

需要重新规划的文档：
- `.ai-context/docs/feature/界面探讨.md` — 主页面单 Activity → MainActivity + 5 Fragment；HomeFragment 重新设计（搜索/热门/最近/计划）；新增 FoodTimelineFragment / DishesFragment / MineFragment / AddDayFoodActivity 章节；弹框 C 历史菜单删除（升级为食历 Tab）
- `.ai-context/docs/feature/数据库设计方案.md` — 评估是否需要新表（计划/热门频次/健康档案/备份元数据）
- `docs/MVP开发规划.md` — 模块划分需要重写
- `docs/技术栈与主题风格.md` — 主题切换入口从 ⋮ 菜单改到 MineFragment

## 严谨流程产出（已完成）

| 阶段 | 角色 | 关键产出 | 落地位置 |
|------|------|---------|---------|
| 1 | 上下文保存 | 三份 context_memory 文件 | `.ai-context/docs/context_memory/` |
| 2 | 系统分析师 | DayMealCardView 统一组件抽象、计划/实际日期推导、热门/最近 SQL | 见 `界面探讨.md` 三/四章、`数据库设计方案.md` |
| 3 | 产品经理 | MVP/一期/二期范围、5 个 US + 验收、4 项决策（备份/热门/最近/小助手） | `MVP开发规划.md` 一/四章 |
| 4 | 数据库工程师 | user_preferences、user_health_profile 两张新表 + 索引 + 备份方案 | `数据库设计方案.md` 2.6 节 + 第十节 |
| 5 | 界面工程师 | 5 Screen 详细布局 + BottomNav + 联动 + 配色 + DishesScreen 双模式 + 顶部热度横滑 | `界面探讨.md` 第一至十一章 |
| 6 | 方案架构师 | 模块结构 + 4 项架构决策（纯 Compose / iOS 推迟 / 按页面拆包 / DishesScreen mode 参数） | `技术栈与主题风格.md`、`MVP开发规划.md` 三章 |
| 7 | 用户睡前确认 | 架构认可 + 4 决策拍板 | — |
| 8 | 落地写文档 | 4 份设计文档更新完成 | 见下方 |

## 已落地的文档清单

| 文件 | 改动 |
|------|------|
| `.ai-context/docs/feature/界面探讨.md` | **整体重写**（v2，5 Tab + 纯 Compose） |
| `.ai-context/docs/feature/数据库设计方案.md` | v4：新增 2.6 节、索引清单、第十节备份方案 |
| `docs/技术栈与主题风格.md` | 项目结构改单 Activity + Compose，主题入口迁移到 MineScreen |
| `docs/MVP开发规划.md` | **整体重写**（v2，对齐 5 Tab + 阶段 1-8 任务） |
| `.ai-context/docs/context_memory/明日审核清单_2026-05-19.md` | **新建**，列举所有自主决策项 |

## 用户明日审核重点

1. 主要看 `.ai-context/docs/context_memory/明日审核清单_2026-05-19.md`
2. 不放心的细节再翻对应设计文档
3. 反馈 OK 或定位返工点
