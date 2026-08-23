# 2026-08-23 CookBook AI Recommend Fix

## 任务

- 基线：`9e0bbc4`
- 范围：`record_meal_manual -> ai_recommend -> back -> unified_add_meal`
- 目标：修复 TURN handoff；修复 AI Recommend 返回后的状态恢复。
- 禁止：修改推荐算法/策略、数据库结构。

## 执行前状态

- 原 `BLUEPRINT_STATE`：`TURN=REVIEW`，Holder=`ARCH`。
- 已按任务包完成一次性 `ARCH -> CODE` 授权，当前 `TURN=CODE`。
- 工作树存在与本任务无关的既有删除/新增改动，执行中保留且不纳入本批提交。

## 预期不变量

- manual tab 返回后仍为 manual。
- mealType 返回后保持进入前值。
- AI 推荐结果返回后注入 unified add meal。
- 仅修复返回状态/结果传递，不改变推荐生成逻辑、策略或数据库。

## 待验证

- 自动化覆盖进入、返回、tab、mealType、推荐内容。
- `shared:testDebugUnitTest` 与 `androidApp:assembleDebug` 按代码影响范围执行。
- 完成后更新经验、BLUEPRINT_STATE，并恢复 `TURN=REVIEW`。
