# MVP 剩余项审计与推进

> 更新时间：2026-06-06。[AI修改]

## 状态说明

- `done`：已实现并完成构建/测试验证。[AI生成]
- `doing`：当前无人值守推进中。[AI生成]
- `todo`：已确认缺口，待实现。[AI生成]
- `verify`：已有实现落点，但需要专项验收或补测试。[AI生成]

## P0 阻塞项

| 状态 | 项目 | 验收标准 | 备注 |
|---|---|---|---|
| done | 首页主题按钮直接弹框 | 首页右上主题按钮直接显示“跟随系统/浅色/深色”，选择后即时生效并持久化，不跳转“我的”。 | 已抽取 `ThemeModeDialog` 并复用到首页/我的。[AI生成] |
| done | 食历复用到今天/明天 | 对某天餐食执行复用，可选择今天/明天；保存后首页计划和食历刷新。 | 食历卡片新增复用按钮，目标日期采用整日替换策略。[AI修改] |
| done | 添加餐食中新建菜品自动带回并选中 | 添加餐食打开菜品库后新建菜品，保存返回时新菜出现在菜品库并自动选中。 | 新建菜品保存后通过导航结果回传 id，并自动加入当前餐食模块。[AI修改] |

## P1 完整性缺口

| 状态 | 项目 | 验收标准 | 备注 |
|---|---|---|---|
| done | 个人健康档案 UI | “我的-个人健康档案”可多选高血压/糖尿病/高血脂/高尿酸并保存，再次进入保持选中。 | 已补多选弹框，保存启用/禁用健康档案。[AI修改] |
| done | 本地备份/恢复 UI | “我的-本地备份与恢复”可创建备份、查看备份列表、恢复备份并提示结果。 | 已补备份管理弹框，支持创建/恢复/删除。[AI修改] |
| done | 收藏组合入口 | 可把多个菜品保存为组合，并在添加餐食时一键选择组合菜品。 | 添加餐食页可保存当前餐食模块为组合，也可选择组合加入当前餐次。[AI修改] |
| done | 慢病食材规则补齐 | 选择健康档案后，食材选择器能显示常见 recommend/limit/avoid 规则。 | 已新增 `crowd_rules.json` 并由 Seeder 写入 `crowd_ingredient`。[AI修改] |

## P2 验证/长期项

| 状态 | 项目 | 验收标准 | 备注 |
|---|---|---|---|
| verify | shared iOS framework 编译能力 | 启用/执行 iOS shared 构建时不因 actual/resource 缺失失败。 | MVP 不开发 iOS UI；当前 iOS target 注释，后续启用前需补资源读取 actual。[AI生成] |
| verify | 删除策略一致性 | 被历史餐食引用的菜品/食材删除不破坏历史展示，默认软删除。 | 已有菜品删除引用检查和 status 软删除，但需完整回归。[AI生成] |
| verify | 图片持久化回归 | 拍照/相册图片重启后仍显示，列表默认缩略图，预览加载原图。 | 已有修复记录，需真机专项验收。[AI生成] |

## 无人值守推进顺序

1. 完成 P0：食历复用到今天/明天。[AI生成]
2. 完成 P0：添加餐食中新建菜品自动带回并选中。[AI生成]
3. 完成 P1：健康档案 UI。[AI生成]
4. 完成 P1：备份/恢复 UI。[AI生成]
5. 评估 P1：收藏组合入口与慢病规则 JSON，按实现范围拆分。[AI生成]
6. 执行 P2 验证项并记录结果。[AI生成]

## 本轮验证

- `./gradlew :shared:testDebugUnitTest`：通过，覆盖基础数据 seed、慢病规则、收藏组合等 shared 单元测试。[AI生成]
- `./gradlew :shared:verifyCommonMainCookbookDatabaseMigration :shared:verifySqlDelightMigration`：通过。[AI生成]
- `./gradlew :androidApp:compileDebugKotlin`：通过。[AI生成]
- `./gradlew :androidApp:assembleDebug`：通过。[AI生成]
- P2 中 iOS framework 当前不能直接判定完成，因为 `shared/build.gradle.kts` 中 iOS target 仍按 MVP 策略注释；后续启用 iOS 前需要补 `SeedResourceLoader` 的 iOS actual 与资源打包验证。[AI生成]
