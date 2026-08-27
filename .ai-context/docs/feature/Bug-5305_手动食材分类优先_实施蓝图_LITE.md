# Bug-5305 · 手动食材分类优先 — BLUEPRINT-LITE

状态：**BLUEPRINT_READY / TURN=CODE**
冻结日期：2026-08-27
事实基线：当前 `HEAD`（开工前 CODE 须重新记录精确 SHA）
功能：F-INGREDIENT；范围：Android 食材编辑器的“营养大类”预选/手选优先级。

## ROLE_CONTRACT

- 你（CODE / Terra）只能改 §3 allowlist；只能完成等价的可测试化与回归测试，不得扩展任何分类能力。
- 你不得修改 `FoodGroup.classify`、分类词典、Category/Ingredient model、Repository、SQLDelight schema/migration/seed、DI、营养推演算法或 UI 文案/布局。
- 你若发现当前 `groupTouched` 不能表达本蓝图的手选合同，须停止代码，在本文件 §8 追加 `Q-5305-NN`，交 ARCH 裁决；不得自行新增第二个状态源。
- 你交付时只能写 `CODE_COMPLETE / TURN=REVIEW`；不得自写 `ARCH_ACCEPTED` 或将真机项标 PASS。

## 1. Reality / 根因裁决

当前 runtime 已有手选保护，Bug-5305 不是一个仍可由当前 HEAD 复现的覆盖缺陷：

1. `IngredientEditorDialogs.kt` 的 `LaunchedEffect(name, ui.editorCategoryIds, groupOptions, ui.editorLoading)` 是唯一给 `selectedGroup` 自动赋值的链路；它由 `FoodGroup.classify(name)`（编辑且改名时优先）或既有顶层分类得出候选。
2. 营养大类 chip 的点击在同一 Composable 内写入 `selectedGroup = group` 和 `groupTouched = true`。
3. 上述自动 effect 先判 `if (groupTouched) return@LaunchedEffect`；因此手选后的名称、分类加载或 options 重组均不可调用 `classify` 改写 `selectedGroup`。
4. 保存链只使用当下 `selectedGroup?.name` 建立顶层关联；`IngredientPickerViewModel.saveIngredientEditor()` 不会再次 `classify(name)`。

风险来源是 K10 的“编辑改名时 classify 优先”规则：若日后有人删除/绕过 `groupTouched` guard，手选会回退成按名覆盖。现状缺少可执行单测，故本批仅把已有优先级冻结为纯策略断言；**不改变用户可见业务语义**。

## 2. 行为合同 / 不变量

| ID | 冻结合同 |
|---|---|
| INV-5305-01 | `groupTouched=false` 时，自动预选可按 K10：编辑且改名优先 `classify(name)`；未改名优先既有顶层大类；新建按 `classify(name)`。 |
| INV-5305-02 | 用户点任一“营养大类” chip 后，`groupTouched=true`；此后的任一自动预选触发不得改变 `selectedGroup`，包括名称修改、DB 分类异步加载、`groupOptions` 重建与旋转恢复。 |
| INV-5305-03 | 保存使用当前手选 group；仅写入该 group 对应既有顶层分类和既有 `food_group`，不得由 `classify(name)` 追加或替换。 |
| INV-5305-04 | 新建“保存并继续”表单复位后才允许回到自动预选（`selectedGroup=null`、`groupTouched=false`）；同一食材编辑会话内不得复位手选意图。 |
| INV-5305-05 | 不新增/删除 Category、FoodGroup 或数据库字段；`FoodGroup.classify` 的启发式词典与其 shared 测试原样保留。 |

## 3. 精确 allowlist

| 类型 | 路径 | 允许的唯一动作 |
|---|---|---|
| 生产 UI | `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientEditorDialogs.kt` | 将既有“自动候选 vs 已手选”的选择收敛为包内可测的纯策略函数，或等价地暴露现有 guard；调用结果必须与 §2 完全等价。不可改 Composable 流程、文案、布局。 |
| Android 单测 | `androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/picker/IngredientEditorFoodGroupPolicyTest.kt`（新建） | 仅测试该纯策略/guard 的自动与手动分支。 |
| 功能状态 | `.ai-context/docs/projectReview/features/F-INGREDIENT/40_缺陷.md` | 实施后回写测试/构建证据与状态。 |
| 蓝图 | `.ai-context/docs/feature/Bug-5305_手动食材分类优先_实施蓝图_LITE.md` | 只追加 §8 交付/审查台账。 |
| 真机清单 | `.ai-context/docs/真机验证/真机待验证清单_<最新时间戳>.md` | 实施后原位追加 DEV-5305-01，保持 PENDING，并按规则改名；不得新建第二份。 |

### 禁区

除 allowlist 外一律禁止。尤其禁止修改：

- `shared/**/domain/FoodGroup.kt` 与 `FoodGroupClassifyTest.kt`；
- `IngredientPickerViewModel.kt`、`IngredientRepository.kt`、`NutritionRepository.kt`；
- 所有 `.sq` / `.sqm` / seed、model、DI、Gradle 依赖；
- `CategoryDialogs.kt`、`IngredientPickerScreen.kt`、文案、主题、自动营养推演和属性/care 行为。

## 4. Terra 实施步骤

1. 重新核对 `IngredientEditorDialogs.kt` 中 effect、chip 点击和 submit 三处仍与 §1 一致；若不一致即按 §8 提 Q，不实施。
2. 在同文件内以最小纯函数表达“`groupTouched` 为真时返回当前手选，否则采用自动候选”；用它替代现有 effect 内的等价分支。不得改 K10 的自动候选优先级。
3. 新建 `IngredientEditorFoodGroupPolicyTest`，锁定 §5 所列 T-5305-01~04。测试只能制造手选标记和候选值，不能 mock 或改写生产期望结果。
4. 运行 §5 命令。所有通过后，在最新唯一真机清单登记 PENDING 项，再更新 `40_缺陷.md` 与本蓝图 §8；状态转 `CODE_COMPLETE / TURN=REVIEW`。

## 5. 测试矩阵 / 验收

| ID | 自动候选 | 已手选/触发 | 断言 |
|---|---|---|---|
| T-5305-01 | `VEGETABLE` | `groupTouched=false` | 采用自动候选，保留 K10 自动模式。 |
| T-5305-02 | `VEGETABLE` | 已手选 `EGG`，模拟名称变化后的 auto effect | 结果仍为 `EGG`。 |
| T-5305-03 | `RED_MEAT` | 已手选 `DAIRY`，模拟 DB 分类加载/options 重建 | 结果仍为 `DAIRY`。 |
| T-5305-04 | 任意 | 表单 reset 的 `groupTouched=false` 且当前 group 为 null | 允许恢复自动候选；仅 reset 可解除手选锁。 |
| T-5305-05 | source audit | — | `FoodGroup.kt`、shared 分类测试、Repository/schema/seed 无 diff；allowlist 差集为空。 |

必跑：

```text
scripts\build-cli.bat :androidApp:testDebugUnitTest --rerun-tasks
scripts\build-cli.bat :androidApp:assembleDebug --rerun-tasks
```

验收失败条件：T-5305 任一失败、构建无成功终态、allowlist 外 diff、`FoodGroup.classify`/schema/repository 有 diff、或手选后仍可由自动候选替换，均阻断交付。

## 6. 真机项

**须登记 PENDING（实施后登记，架构阶段不伪造已新增）。**

- `DEV-5305-01 手动营养大类优先`：食材管理→新增或编辑名称可被自动识别的食材（如“鸡蛋”）→确认自动预选→手动点另一大类（如“蔬菜类”）→修改名称/等待分类加载后保存并重开。预期：显示与持久化均为手选“蔬菜类”；失败为回跳“蛋类”或保存为自动分类。再点“保存并继续”新建下一食材，预期新表单可重新自动预选。

真机项不阻断本批自动化验收，但在设备验证前只能写 `PENDING_DEVICE_VERIFICATION`，不得写 PASS。

## 7. 文档 / 状态回写

CODE 交付必须：

1. 更新 `F-INGREDIENT/40_缺陷.md`：Bug-5305 从 `📄` 转 `🔧`，附 commit、T-5305 结果、两条构建命令结果和真机条目 ID。
2. 在本蓝图 §8 追加“STEP/INV → 文件:行 → T → commit”映射；REVIEW 只在同段追加结论。
3. 维护唯一真机清单的时间戳文件名；不更新 `STATE.yml`、`20_实现.md`、功能路径索引或横轴诊断地图（无架构/路径/产品行为变更）。

## 8. Q / AF / 交付台账（只追加）

| 类型 | ID | 内容 | 状态 |
|---|---|---|---|
| Reality | RV-5305-01 | 当前 `groupTouched` guard 已覆盖“手动选择不得被 classify 覆盖”；本批仅补可回归证据。 | CLOSED@ARCH-20260827 |
| Delivery | D-5305-01 | `resolveIngredientEditorFoodGroup` 收敛现有 guard；`IngredientEditorFoodGroupPolicyTest` 覆盖 T-5305-01~04。`FoodGroup`、Repository、schema/seed 零 diff。`HEAD=e575bce9cae4adb44b070d2ae6e7e6b877eebf87`；工作树未提交。 | CODE_COMPLETE / PENDING ARCH REVIEW |
| Gate | G-5305-01 | 定向 Android 测试 PASS；`:androidApp:testDebugUnitTest --rerun-tasks` BUILD SUCCESSFUL（4m13s）；`:androidApp:assembleDebug --rerun-tasks` BUILD SUCCESSFUL（4m）；architecture quality unittest 8/8 与 check PASS。 | PASS |
| Runtime | DEV-5305-01 | 已登记唯一清单 `真机待验证清单_202608270943.md`；未执行真机。 | PENDING_DEVICE_VERIFICATION |
| Review | R-5305-01 | Sol 最终复核：guard 等价、allowlist 与自动化门禁均通过；无 AF。 | ARCH_ACCEPTED / AUTOMATED_GATES_PASS |
