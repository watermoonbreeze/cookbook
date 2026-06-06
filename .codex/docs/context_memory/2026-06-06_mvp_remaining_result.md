# 2026-06-06 MVP 剩余项推进结果

- 任务模式：深度级别；按自动任务编排执行，已分派 DEV_SA、DEV_PM、DEV_ARCH 做只读分析和方案复核。[AI生成]
- 已完成 P0：首页主题按钮直接弹框；食历复用到今天/明天；添加餐食中新建菜品后自动带回当前餐食模块。[AI生成]
- 已完成 P1：健康档案 UI；备份/恢复 UI；收藏组合入口；慢病食材规则 JSON seed。[AI生成]
- 新增文档：`.codex/docs/feature/MVP剩余项审计与推进.md`。[AI生成]
- 关键新增文件：`ThemeModeDialog.kt`、`FavoriteCombo.kt`、`FavoriteComboRepository.kt`、`crowd_rules.json`、`FavoriteComboRepositoryTest.kt`。[AI生成]
- 验证通过：`:shared:testDebugUnitTest`、`:shared:verifyCommonMainCookbookDatabaseMigration`、`:shared:verifySqlDelightMigration`、`:androidApp:compileDebugKotlin`、`:androidApp:assembleDebug`。[AI生成]
- P2 保留专项：iOS target 当前注释，启用前需补 iOS `SeedResourceLoader` actual；删除策略和图片持久化需真机回归。[AI生成]
