# 修复：旧库缺失 ingredient.reason 列

时间：2026-07-06。[AI生成] 提交：2c91ea2。

## 根因

上一个功能提交（f549afb）给 `ingredient` 表加了 `reason` 列并写进 Cookbook.sq（Schema.create），
但**对应的迁移文件 `10.sqm` 在编辑过程中丢失、从未落盘**（工具调用中断导致，同批还丢了 DDL/restorePreset 的部分编辑）。
结果：
- 全新安装：Schema.create 建表带 reason → 正常。
- 旧库升级：Schema.version 仍是 10（没有 10.sqm），不触发升级，reason 列始终缺失。
- 单元测试用 `RepositoryTestDatabase.create()` = `Schema.create`（含 reason），**不走迁移路径**，故测试全过而真机失败。

## 受影响查询（读/写 reason，旧库上报 no such column: reason）

- `selectInactiveUserIngredients`（已失效回收站）→ "加载已失效食材失败"
- `selectIngredientsOfDish`（菜品读食材，含 i.reason）→ 菜品详情/编辑/任何加载菜品食材处
- `deleteUserIngredient`（软删写 reason）→ 删除自定义食材
- `updatePresetIngredientStatus`（seed 内，reseed 时）

## 修复

`DatabaseDriverFactory.android.createDriver()` 建驱动后调用 `ensureLegacyColumns(driver)`：
`ALTER TABLE ingredient ADD COLUMN reason ...` 包 runCatching，列已存在时忽略 duplicate column。
幂等，兼容 v10 各种状态（含/不含 reason），**不用版本迁移**避免对已含列设备 duplicate-column 崩溃。
在 seedIfNeeded 之前执行（Koin 建 db 时触发），保证后续查询列已就绪。

## 遗留说明

- 未补 10.sqm：故意。若补版本迁移会让"已含 reason 的 v10 全新安装"升级时 ALTER 重复列崩溃。以代码幂等补列更稳。
- iOS 若将来上线需在 iOS 驱动工厂做同样幂等补列。
- 教训：改 schema（.sq）必须同步加迁移文件；单测用 Schema.create 不覆盖迁移，应考虑加迁移路径测试或 verifyMigrations。

## 待确认（report 1）

用户报"筛选自定义的食材失败"。自定义分类筛选查询 `selectIngredientsByCategoryIds` 不读 reason，
理论上不受本 bug 影响；但删除自定义食材（deleteUserIngredient 写 reason）会失败。
若本修复后"筛选/浏览自定义食材"仍报错，需 /sdcard/cookbook/log/ 日志定位。
