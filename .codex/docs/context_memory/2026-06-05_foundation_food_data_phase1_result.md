# 2026-06-05 基础食材与多维分类第一阶段结果

- 已记录方案文档：`.codex/docs/feature/基础食材与多维分类方案.md`。
- `PresetDataSeeder` 改为补齐式 seed：分类和食材不再只在表为空时执行，旧库也能补新增数据。
- 扩展单位：新增碗、块、根、条、段、瓣、只等。
- 扩展分类：水果、坚果、油脂，以及主食/蔬菜/水果/肉类/水产/蛋奶豆/坚果/调味料/油脂下的二级分类。
- 扩展营养维度：低/中/高嘌呤，低/中/高 GI，低糖，高膳食纤维，优质蛋白，低脂，高脂，低钠，高钠，高钾，高钙，全谷物，深色蔬菜。
- 扩展基础食材：第一阶段 80+，覆盖主食、蔬菜、水果、肉类、水产、蛋奶豆、坚果、调味料、油脂。
- 新增 `PresetDataSeederTest`，验证 seed 后基础食材数量、燕麦、低脂牛奶、水果、低 GI、高嘌呤等存在。
- 当前测试：`./gradlew :shared:testDebugUnitTest` 通过，shared 实际执行 7 个测试。
- 当前验证：`./gradlew :shared:verifyCommonMainCookbookDatabaseMigration :shared:verifySqlDelightMigration :androidApp:assembleDebug` 通过。
- 下一阶段建议：补 `crowd_ingredient` 慢病 recommend/limit/avoid 规则，并继续扩充到 300+ 食材。
