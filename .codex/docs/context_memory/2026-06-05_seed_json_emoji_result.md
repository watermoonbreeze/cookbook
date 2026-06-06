# 2026-06-05 基础数据 JSON 化与食材 emoji 结果

- 任务编排：标准级别；原因是涉及 shared 资源、Seeder、数据库迁移、UI 展示和测试。[AI生成]
- 分类 JSON：`shared/src/commonMain/resources/seed/food_categories.json`，使用稳定 `code` 和 `parent` code 维护分类树。[AI生成]
- 食材 JSON：`shared/src/commonMain/resources/seed/ingredients.json`，每条食材维护 `code/name/unit/emoji/categories`。[AI生成]
- 数据库：SQLDelight version 升到 5；`ingredient` 新增 `emoji TEXT NOT NULL DEFAULT '🥗'`；迁移文件 `4.sqm`。[AI生成]
- 展示优先级：真实图片/缩略图 > `ingredient.emoji` > `FoodEmoji.kt` 名称兜底 > `🥗`。[AI生成]
- Seeder：已有同名食材不重复插入，但会补齐 emoji 和分类；软删除同名预设食材会恢复有效。[AI生成]
- 资源读取：`SeedResourceLoader` 读取 seed JSON；Android sourceSet 和测试 sourceSet 显式挂载 common resources，测试另有文件路径兜底。[AI生成]
- 测试：`PresetDataSeederTest` 增加 JSON 必填 emoji、重复 seed 幂等、已有食材分类补齐验证。[AI生成]
- 已验证：`:shared:testDebugUnitTest`、`:shared:verifyCommonMainCookbookDatabaseMigration`、`:shared:verifySqlDelightMigration`、`:androidApp:assembleDebug` 均通过。[AI生成]
