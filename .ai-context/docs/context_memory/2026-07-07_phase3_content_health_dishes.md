# 阶段3 内容补充 + 健康档案统一 + 预设菜 + 标注复核（2026-07-07）

[AI生成] 承接食材层重构，本段完成内容填充与两项功能。均已 build+test+push。

## 已完成（提交链已 push 到 b68fb77 + 本地 34284cc）

### 阶段3 内容补充（食材/详情）
- 批次1：+32 食材填空分类；批次2：+109 详情；批次3：+40 常见食材。
- 结果：食材 **225**、详情 **100% 覆盖**。脚本 `scratchpad/add_content_batch*.py` + 引用完整性校验。
- 数据性质：AI 依据通用营养/烹饪常识整理，非权威逐条核对。已在「我的→关于→数据来源」如实声明 + 免责。

### 健康档案统一到调养病种（schema 变更）
- 根因：健康档案用旧 `crowd_type`(4项)，调养类是 `food_category` 的 `care_*`(17病种)，两套并行且调养 Tab 重复。
- 改法：`user_health_profile` 由引用 crowd_type 改引用 food_category(care_)；`12.sqm` v12→v13 重建表 + 软删旧 crowd_${id}；seeder 停止生成 crowd_${id}；HealthProfileRepository 改用 `selectCareCategories`。
- UI：健康档案覆盖全部 17 病种；我的页标签横向滚动、弹框纵向滚动。
- ⚠️ 迁移会清空旧的健康档案勾选（用户重选），已真机验证。Schema.version=13。

### 预设经典菜（Task 2-A）
- 新增 `dishes.json`（16 道家常菜）+ SeedDish 类 + `seedDishes`（补齐式幂等，按名解析主料 is_main/配料/步骤/烹饪方式）+ `selectPresetDishIdByName` + 纳入内容指纹。
- 详情"相关菜品"区按烹饪方式显示（`selectDishesByIngredientMatch` 已支持）。16 道见 commit b68fb77。

### 营养·调养标注复核（Task 2-B）
- 修正 4 处：带鱼 中→高嘌呤；四季豆 中→低嘌呤；柿子/榴莲 高GI→中GI。
- 163 条调养规则逐病种扫查，未发现明显错误。

## 关键约定/坑
- seed 大批量改动一律用脚本 + 引用完整性校验 + 单测；未知食材/分类 code seeder 静默跳过（不崩）。
- 改 general 大类名会打断测试按名断言（历史坑）。
- dishes/details 内容为 AI 参考数据，待权威源替换。

## 下一步（用户说「再继续」）
- 待用户指定方向（更多食材/菜品、AI 层、或其它）。
