# L1 云端 AI 首启同意——真机反馈修复 实施蓝图（hotfix）

> 状态：`BLUEPRINT_READY`（可直接执行）
> 关联主蓝图：`L1_云端AI首启同意与合规免责_实施蓝图.md`（本文件是其真机验证阶段暴露问题的补丁蓝图，不重写主蓝图，遇冲突以主蓝图 INV 表为准，本文件只新增/修订两条相关分支）
> 起草：ARCH（Sonnet），根据用户在 `.ai-context/docs/真机验证/真机待验证清单_202608182200.md` 填写的 E-L1-03/E-L1-06/E-L1-11 反馈定位根因。**未升级 Opus**：两处均为清晰的单点状态传递错误，代码路径唯一、根因可由静态读码直接确认，无架构级歧义或多方案取舍，不构成升级理由。
> 颗粒度：轻量（两处独立单点修复，非新功能，不套主蓝图 L7 全套 GC 条款；仍保留"证据→不变量→STEP→验收"的最小闭环）。

---

## §1 问题清单（真机反馈原文见验证清单表格"验证结果/原因"列）

| 编号 | 用户反馈原文 | 判定 |
|---|---|---|
| E-L1-03 | "最后一步 重新启用后，密钥消失了，回到了未配置的地方" | **真实缺陷**，见 §2.1 |
| E-L1-06 | "点击知道了关闭后，切换到xxx云端模型的整个弹框都消失了，又要重新输入密钥。关闭只需要关闭当前打开的弹框…还能继续点击继续进行设置" | **真实缺陷**，见 §2.2 |
| E-L1-11 | "冷启动首屏的设置密钥在哪里，首页并没有设置密钥" | **非代码缺陷，是验证步骤描述有歧义**，见 §2.3（不产出代码改动，只改验证文档措辞） |
| E-L1-12 附带问题 | "另外解释一下 grandfather 面板是啥" | 非缺陷，用户提问，见 §2.4（只在验证文档里补充说明，不改代码） |

---

## §2 根因分析

### 2.1 E-L1-03：重新启用云端 AI 后密钥被清空

**证据链**：

1. `AiSettingsScreen.kt:112`（`CloudSection` 调用处）：
   ```kotlin
   onReenableConsent = { consentPanelOpen = true }, // [AI修改] L1：DECLINED 后"重新启用"
   ```
   直接打开完整同意面板，**没有给 `pendingKeyDraft` 赋值**——`pendingKeyDraft` 此刻是上一次弹层关闭时被清空的空字符串（`AiSettingsScreen.kt:59` 初始值 `""`，历次 `onClose`/`onAgree`/`onDecline` 分支末尾均会重置回 `""`，见 `:201/207/214`）。

2. `AiSettingsScreen.kt:196-203`（`consentPanelOpen` 面板的 `onAgree`）：
   ```kotlin
   onAgree = {
       vm.grantConsent(model.vendor, pendingKeyDraft, ConsentSource.EXPLICIT_FIRST_ENABLE)
       ...
   }
   ```
   把此刻的 `pendingKeyDraft`（空串）原样传给 `grantConsent`。

3. `AiSettingsViewModel.kt:78-92`（`grantConsent`）：
   ```kotlin
   fun grantConsent(vendor: String, key: String, source: ConsentSource) {
       viewModelScope.launch {
           ...
           onSaveVendorKey(vendor, key) // ← key 是空串
       }
   }
   ```
   `grantConsent` 的设计契约（主蓝图 INV-L1-06）是"面板出现前 Key 一定是刚从 `KeyDialog` 输入框读到的新值"——这个契约只对"首次填 Key→触发 FULL_CONSENT"这条路径成立。`onReenableConsent` 是主蓝图 v2b 后追加的 INV-L1-12 新入口，**没有经过 `KeyDialog`**，直接复用了同一个面板和同一个 `grantConsent`，但没有满足它的前置契约——于是 `onSaveVendorKey(vendor, "")` 把已经保存好的 Key 用空串覆盖，用户看到"重新启用"后密钥变回"未配置"。

**修复方向**：`onReenableConsent` 打开面板前，把 `pendingKeyDraft` 显式填充为**当前厂商已保存的 Key**（该 Key 本来就在 `state.keyByVendor` 里，用户没有输入新值，`grantConsent` 只是要把它和 `consent` 一起重新持久化一次，属于幂等重存）。不改 `grantConsent` 本身的契约（改了会影响 INV-L1-04/06 两条已验证通过的路径），只在唯一遗漏契约的调用点补上正确输入。

### 2.2 E-L1-06："看看发送哪些内容"关闭时把外层"切换到 xxx"确认框一起关掉了

**证据链**：`AiSettingsScreen.kt:157-191`，`vendorConfirmOpen` 是一个 `AlertDialog`，其 `text` 内嵌了一个"看看发送哪些内容 ›"可点文字（:166-176）：

```kotlin
Text(
    "看看发送哪些内容 ›",
    ...
    modifier = Modifier
        .clickable {
            vendorConfirmOpen = false   // ← 把外层"切换到 xxx"确认框也关了
            readonlyPanelOpen = true
        }
        .padding(vertical = 2.dp),
)
```

点击这行文字的本意是"在换厂商确认框之上，再弹一层只读披露详情看完就关，回到原来的确认框继续操作"——但代码把宿主外层的 `vendorConfirmOpen` 一并置 `false`，导致外层 `AlertDialog` 直接从组合树移除。`readonlyPanelOpen` 对应的 `CloudAiConsentPanel` 是独立的 `Dialog`（`CloudAiConsentPanel.kt:56`，非嵌在 `vendorConfirmOpen` 的 `AlertDialog` 内部），**两者本可以同时存在、天然可以叠 Dialog-on-top-of-AlertDialog**，只是这里手滑把外层也一起关掉了。用户点"知道了"后 `readonlyPanelOpen=false`，此时外层确认框已经不存在，只能重新走一遍"编辑→输入 Key→保存"从头触发，体验上就是"消失了要重新输入密钥"。

**修复方向**：去掉这一行里的 `vendorConfirmOpen = false`，只置 `readonlyPanelOpen = true`。外层 `AlertDialog` 保持打开，`CloudAiConsentPanel`（`Dialog`）会叠在它上面；点"知道了"后 `readonlyPanelOpen=false`，外层确认框原样露出，用户可以继续点"继续"。

### 2.3 E-L1-11：验证步骤描述歧义（非代码问题）

原验证步骤"冷启动首屏快速点开'设置密钥'"中的"首屏"实际指**AI 设置页本身首次进入时的画面**（`INV-L1-11` 的守卫防的是"AI 设置页 `loaded` 从 false 翻 true 之前，用户已经手动点开了 `KeyDialog`/`consentPanelOpen`/`vendorConfirmOpen` 三者之一"这一竞态窗口，见主蓝图 §3 INV-L1-11），不是 App 首页/主页。App 首页（Home）本身没有任何"设置密钥"入口，代码里 grep 确认（见 §5 验收），该入口只存在于「我的 → AI 设置」页内。判定：**不是代码缺陷，是验证清单第 E-L1-11 行的步骤描述没写清楚"首屏"指哪个页面**，导致用户在 App 首页找不到对应按钮。处置：只改验证文档措辞，不改代码（见 §4）。

### 2.4 grandfather 面板说明（非缺陷，用户提问）

`grandfather`（直译"祖父条款"，软件工程里的常规用词，指"新规则生效前就已存在的老用户/老状态享有过渡性豁免或补充确认，而非直接被新规则拦下"）在本功能里特指：**在"云端 AI 首启同意"这个新规则上线之前，就已经配置过某个厂商 API Key 并在正常使用云端 AI 的老用户**——这批用户从未被问过"是否同意把数据发给云端 AI"，所以进 AI 设置页时会看到"这项功能你已在使用"这一措辞差异化的补确认面板（区别于全新用户看到的"启用云端 AI"首次同意面板），解释的是"你之前就在用，现在请补一次同意"，不是重新引导你从零开始配置。处置：只在验证文档补充这段说明，不改代码。

---

## §3 修复实施

### STEP-1（对应 E-L1-03）

**文件**：`androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/CloudAiSaveRoute.kt`

在文件末尾新增一个纯函数（与既有 `routeOnSave`/`shouldShowCloudStatusBlock` 同风格，供 JVM 单测覆盖）：

```kotlin
/** [AI生成] 重新启用云端 AI 时应带入的 Key 草稿——沿用已保存的 Key，不清空（回应真机 E-L1-03）。 */
fun reenableKeyDraft(keyByVendor: Map<String, String>, vendor: String): String =
    keyByVendor[vendor].orEmpty()
```

**文件**：`androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiSettingsScreen.kt`

原文（约 :112）：
```kotlin
                        onReenableConsent = { consentPanelOpen = true }, // [AI修改] L1：DECLINED 后"重新启用"
```

改为：
```kotlin
                        onReenableConsent = {
                            // [AI修改] hotfix E-L1-03：带入已保存的 Key，避免 grantConsent 用空串覆盖（见 CloudAiSaveRoute.reenableKeyDraft）。
                            pendingKeyDraft = reenableKeyDraft(state.keyByVendor, vm.selectedModel().vendor)
                            consentPanelOpen = true
                        }, // [AI修改] L1：DECLINED 后"重新启用"
```

完成形态判据：`grep -n "reenableKeyDraft" androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/*.kt` 命中 `CloudAiSaveRoute.kt`（定义）与 `AiSettingsScreen.kt`（调用）各一处。

### STEP-2（对应 E-L1-06）

**文件**：`androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiSettingsScreen.kt`

原文（约 :171-174，`vendorConfirmOpen` AlertDialog 内"看看发送哪些内容"的 `clickable`）：
```kotlin
                            .clickable {
                                vendorConfirmOpen = false
                                readonlyPanelOpen = true
                            }
```

改为：
```kotlin
                            .clickable {
                                // [AI修改] hotfix E-L1-06：不关闭外层"切换到 xxx"确认框——CloudAiConsentPanel 是独立 Dialog，
                                //   天然可叠在 AlertDialog 之上，"知道了"关闭后外层确认框应原样保留可继续操作。
                                readonlyPanelOpen = true
                            }
```

完成形态判据：`grep -n "vendorConfirmOpen = false" androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiSettingsScreen.kt` 命中数从改前的 **4** 处（`onDismissRequest`、"继续"按钮、"取消"按钮、"看看发送哪些内容"）降为改后的 **3** 处（"看看发送哪些内容"这一处不再出现，其余三处均为本框自身的正当关闭，不得删改）。**[勘误·google_quality_engineer 复审发现]**：本节初稿误写成"3 处→2 处"（漏数了"继续"按钮里的那处），若后续按错误判据对账会误判"多了一处"进而可能删掉"继续"按钮里的正当那行，造成"点继续后确认框不关闭"的新 bug——对账请以本次修正后的"4→3"为准。

### STEP-3（回归测试，对应 STEP-1）

**文件**：`androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/ai/CloudAiSaveRouteTest.kt`

在文件内新增两个测试用例（沿用文件现有测试风格，函数名/断言写法照抄现有用例节奏，不引入新测试框架）：

- `reenableKeyDraft 在已配置 Key 时返回该 Key`：构造 `keyByVendor = mapOf("zhipu" to "sk-abc123")`，断言 `reenableKeyDraft(keyByVendor, "zhipu") == "sk-abc123"`。
- `reenableKeyDraft 在该厂商未配置 Key 时返回空串`：构造 `keyByVendor = mapOf("deepseek" to "sk-xyz")`，断言 `reenableKeyDraft(keyByVendor, "zhipu") == ""`（不因为其他厂商有 Key 就误取）。

STEP-2 是纯 Compose 弹层堆叠行为（两个独立 `Dialog`/`AlertDialog` 谁盖谁），项目当前没有 Compose UI 测试基建（`androidTest` 目录为空，见根因分析前置检索），不新增 UI 测试基建（不在本次 hotfix 范围内引入新测试框架），该项修复只能靠真机复验 E-L1-06 确认（见 §4）。

---

## §4 真机验证文档更新（CODE 完成编码+构建+单测通过后执行，或由 ARCH 收尾时执行）

更新 `.ai-context/docs/真机验证/真机待验证清单_202608182200.md`：

1. **E-L1-03**：状态列由 `🔧` 改为 `🔧`（保持，待复验），`验证结果`/`原因` 两列清空（等待用户下一轮重新验证结果），操作步骤末尾追加一句"（已修复：重新启用会带入已保存的 Key，不再清空）"。
2. **E-L1-06**：同上处理，操作步骤末尾追加"（已修复：点'看看发送哪些内容'不再关闭外层换厂商确认框，'知道了'后可继续点'继续'）"。
3. **E-L1-11**：操作步骤原文里的"冷启动首屏"改写为"打开【我的 → AI 设置】页面（这是本项测的'首屏'，不是 App 主页）"，状态保持 `🔧`，`验证结果`/`原因` 清空待用户重新验证。
4. **E-L1-12**：`验证结果`列保留"通过"，在旁边补一句 grandfather 面板的解释（§2.4 内容浓缩版），不改状态。
5. 文件头部"最后更新"时间戳改为本次执行的实际日期时间。

---

## §5 验收命令（GC-06 对齐，CODE 执行完必须全部跑且贴 BUILD SUCCESSFUL 证据，不得只看退出码）

```bash
scripts\build-cli.bat :shared:testDebugUnitTest
scripts\build-cli.bat :androidApp:testDebugUnitTest
scripts\build-cli.bat :androidApp:assembleDebug
```

以及 §2.3 判定用到的 grep（证明 App 首页确无"设置密钥"入口，佐证 E-L1-11 是文档措辞问题非代码缺陷）：

```bash
grep -rn "设置密钥" androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/
```
应只命中 `AiSettingsScreen.kt`（`CloudSection` 内的 `TextButton`），不命中任何首页/Home 相关文件。

---

## §6 allowlist

| 文件 | 允许操作 | 禁止操作 |
|---|---|---|
| `AiSettingsScreen.kt` | STEP-1/STEP-2 指定的两处局部编辑；**追加**：STEP-4 指定的 `onReenableConsent` 分支改法 + STEP-6 指定的两处注释勘误 | 改动 `declineConsent`/`confirmVendorSwitch`/`closeCloudAi`/`resolveGrandfather` 任一调用点；改动其余弹层的开关逻辑 |
| `CloudAiSaveRoute.kt` | 新增 `reenableKeyDraft`（STEP-1）；**追加**：STEP-4 需要的分流判定，若抽纯函数则加在本文件 | 修改 `routeOnSave`/`shouldShowCloudStatusBlock` 已有逻辑 |
| `CloudAiSaveRouteTest.kt` | STEP-3 两个测试用例；**追加**：STEP-6 的测试改名 | 删除既有测试用例的断言逻辑（改名不算删除） |
| `AiSettingsViewModel.kt` | **[v1.1 追加，原版本禁止改动，现放开]** 仅 STEP-5 指定的 `grantConsent` 一行改法（`onSaveVendorKey(vendor, key)` → `if (key.isNotBlank()) onSaveVendorKey(vendor, key)`） | 改动 `grantConsent` 之外的任何函数；改动该函数内其余任何一行 |
| `AiSettingsViewModelConsentTest.kt` | **[v1.1 追加]** 仅新增 STEP-5 指定的一个回归测试 | 修改既有测试用例 |
| 主蓝图 `L1_云端AI首启同意与合规免责_实施蓝图.md` | **[v1.1 追加]** 仅 INV-L1-12 一行的"期望行为"/"禁止结果"文案回填（§9 已由 ARCH 完成，CODE 不需要再动） | 改动其余任何 INV 条款 |
| 真机验证清单 `.md` | 仅 §4 列出的编号行文案/状态/时间戳 | 改动其余批次内容 |

**仍然禁止改动**：`CloudAiConsentPanel.kt`（组件本身无问题，是宿主对弹层状态的错误管理）；`grantConsent` 除 STEP-5 指定那一行以外的任何内容（先写 consent 再写 Key 的顺序、`next` 对象构造、`acknowledgedVendors` 扫描逻辑均已验证正确，不得动）。

---

## §7 交付台账（CODE 完成后填）

| STEP | 完成 | Evidence |
|---|---|---|
| STEP-1 | ✅ | `CloudAiSaveRoute.kt:30-31` 新增 `reenableKeyDraft` 函数；`AiSettingsScreen.kt:112-116` 调用该函数赋值 `pendingKeyDraft` |
| STEP-2 | ✅ | `AiSettingsScreen.kt:175-179` clickable 回调内已删除 `vendorConfirmOpen = false`，仅保留 `readonlyPanelOpen = true` |
| STEP-3 | ✅ | `CloudAiSaveRouteTest.kt:80-88` 新增两个测试用例：`reenableKeyDraft 在已配置 Key 时返回该 Key` 和 `reenableKeyDraft 在该厂商未配置 Key 时返回空串`；`:androidApp:testDebugUnitTest` BUILD SUCCESSFUL |
| STEP-4 | ✅ | `AiSettingsScreen.kt:112-121` 改 `onReenableConsent` 分流：`val draft = reenableKeyDraft(...)`，`if (draft.isBlank()) keyDialogOpen=true else consentPanelOpen=true`；`grep -n "draft.isBlank()"` 命中 1 处 |
| STEP-5 | ✅ | (1) `AiSettingsViewModel.kt:90` 改为 `if (key.isNotBlank()) onSaveVendorKey(...)`+注释；(2) `AiSettingsViewModelConsentTest.kt:160-165` 新增测试 `T-L1-03`；改前报 ComparisonFailure at line 44，改后 BUILD SUCCESSFUL in 8s |
| STEP-6 | ✅ | (1) 勘误 `AiSettingsScreen.kt:160` 注释（从"不自动弹回"→"叠层、外层原样保留"）及 `:234` 注释；(2) `CloudAiSaveRouteTest.kt:81,86` 改测试名 `T-L1-03a/b` 并补充文件 KDoc + `reenableKeyDraft` 函数 KDoc |
| §4 真机验证文档更新 | ⏳ | 由 ARCH 在后续收尾时执行（CODE 完成阶段不改验证文档） |
| §5 验收命令 | ✅（v1.0 范围）；v1.1 STEP-4/5/6 完成后需**重跑**三条命令 | `:shared:testDebugUnitTest` BUILD SUCCESSFUL in 5s；`:androidApp:testDebugUnitTest` BUILD SUCCESSFUL in 26s；`:androidApp:assembleDebug` BUILD SUCCESSFUL in 10s；`grep "设置密钥"` 仅命中 `AiSettingsScreen.kt:427` 一处 |

---

## §8 v1.1 追加修复（google_quality_engineer 复审 2026-08-19，4 条 🟡 建议中 3 条本次一并处置，1 条 ⚪ 留档不做）

> 复审结论：v1.0 两处修复本身**正确、无阻断**，构建+双单测通过、无回归。以下是复审挑出的"同一根因的兄弟场景"与"文档-代码一致性"问题，v1.1 补齐。⚪ #5/#6（测试命名细节、真机复验时顺带留意的两条弹层交互）不产出代码改动，已并入 §4 真机验证文档更新与本文档措辞，不再单列 STEP。

### §8.1 STEP-4（对应复审 🟡#1）：删除密钥后点"重新启用"会给出假成功提示

**根因**：`closeCloudAi(deleteKey=true)` 清空 Key 且 `type` 回 `MOCK` 后，用户手动把单选切回"云端大模型"，此时 `status` 仍是 `DECLINED`（`closeCloudAi` 把它置 `DECLINED`），"重新启用"行会渲染（渲染条件只看 `status==DECLINED`，不看 Key 是否还在）。点击后 `reenableKeyDraft` 返回空串，`grantConsent(vendor, "")` 执行后（STEP-5 落地后 Key 不再被清空，但**也没有被填上**——因为本来就没有 Key），`consent` 变 `GRANTED`，界面 Snackbar 提示"已启用云端 AI"、但常驻状态块因 `shouldShowCloudStatusBlock` 双条件不满足（Key 仍为空）而不出现——用户被明确告知"已启用"，实际后续调用会因 `activeType==CLOUD` 但 Key 为空在 `CloudAiRuntime` 内静默回退规则。**违反透明准则"诚实不操纵"红线**（用户可感知的成功提示与实际生效状态不一致）。

**修复**：`AiSettingsScreen.kt:112-116` 的 `onReenableConsent`，按 `reenableKeyDraft` 的返回值分流——有 Key 才走同意面板（走 STEP-1 已修的路径），没 Key 就先引导去填 Key（复用既有 `KeyDialog`，填完后天然经 `routeOnSave` 判到 `FULL_CONSENT`，走正常首次同意路径，不新造分支）：

```kotlin
onReenableConsent = {
    val draft = reenableKeyDraft(state.keyByVendor, vm.selectedModel().vendor)
    if (draft.isBlank()) {
        // [AI修改] hotfix v1.1 E-L1-03兄弟场景：密钥已被删除，没有"重新启用"的资格，引导先补填 Key
        //   （填完后 KeyDialog.onConfirm 的 routeOnSave 会判到 FULL_CONSENT，走正常首次同意路径）。
        keyDialogOpen = true
    } else {
        pendingKeyDraft = draft
        consentPanelOpen = true
    }
},
```

完成形态判据：`grep -n "draft.isBlank()" androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiSettingsScreen.kt` 命中恰好 1 处。

### §8.2 STEP-5（对应复审 🟡#3 的纵深防御建议）：`grantConsent` 对空 Key 免疫，作为契约边界的最后一道防线

**理由**：STEP-4 从 UI 层堵住了当前唯一已知的空 Key 调用点，但 `grantConsent` 函数本身仍然"无条件相信调用方传来的 key"——INV-L1-05 的设计意图是"清空 Key 这个动作永远不该经过 `grantConsent`"，让函数体本身满足这条不变量比要求每一个未来的新调用点都自觉遵守更可靠（纵深防御，不是可有可无的锦上添花——这类同意/密钥语义的代码后续任何遗漏都直接对应"数据外发"红线）。

**文件**：`androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiSettingsViewModel.kt`

原文（:90）：
```kotlin
            onSaveVendorKey(vendor, key)
```

改为（**仅这一行**，函数其余部分不动）：
```kotlin
            if (key.isNotBlank()) onSaveVendorKey(vendor, key) // [AI修改] hotfix v1.1：空Key不覆盖已存Key（INV-L1-05 精神：清空动作不该经此函数发生）
```

**回归测试**：`androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/ai/AiSettingsViewModelConsentTest.kt`，沿用文件已有 `runVmTest`/`awaitUntil` 风格新增一个测试（放在 `T-L1-06b` 之后即可）：

```kotlin
@Test
fun `T-L1-03 grantConsent以空Key调用不得清空已保存Key`() = runVmTest { vm, config ->
    config.setVendorApiKey("zhipu", "sk-keep")
    vm.grantConsent("zhipu", "", ConsentSource.EXPLICIT_FIRST_ENABLE)
    awaitUntil { config.cloudAiConsent().status == ConsentStatus.GRANTED }
    assertEquals("sk-keep", config.vendorApiKey("zhipu"))
}
```

这个测试在 STEP-5 改动前应为**红**（验证它真的在测这次修复，不是重言测试——CODE 执行时请先跑一遍确认真红，再落地改动、再确认转绿，两次结果都写进 §7 Evidence）。

### §8.3 STEP-6（对应复审 🟡#4 文档-代码一致性）：勘误两处已被 STEP-2 推翻的旧注释 + 测试改名

**文件**：`androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiSettingsScreen.kt`

1. `:160` 附近（`vendorConfirmOpen` AlertDialog 上方注释）：
   - 原文含"…下钻只读面板，**不自动弹回**（§5.1 显式简化）"
   - 改为："…下钻只读面板，**叠在换厂商确认框之上**，关闭后外层原样可继续操作（§5.1"不自动弹回"简化已被 hotfix v1.1 E-L1-06 推翻，见主蓝图 §5.1 标注）"
2. `:224` 附近（`readonlyPanelOpen` 面板上方注释）：
   - 原文含"…"知道了"关闭后**不自动弹回**换厂商确认框（§5.1 显式简化）"
   - 改为："…只读面板本就叠在换厂商确认框之上（若从该入口打开），"知道了"只关自己，外层原样保留（hotfix v1.1 E-L1-06）"

**文件**：`androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/ai/CloudAiSaveRouteTest.kt`

- 测试名 `reenableKeyDraft 在已配置 Key 时返回该 Key` → 改名为 `T-L1-03a reenableKeyDraft在已配置Key时返回该Key`
- 测试名 `reenableKeyDraft 在该厂商未配置 Key 时返回空串` → 改名为 `T-L1-03b reenableKeyDraft在该厂商未配置Key时返回空串`
- 文件顶部 KDoc "`routeOnSave`/`shouldShowCloudStatusBlock` 纯函数单测" 补上 `reenableKeyDraft`
- `reenableKeyDraft` 函数自身的 KDoc（`CloudAiSaveRoute.kt:29`）末尾追加"（INV-L1-12，回应真机 E-L1-03）"

### §8.4 主蓝图 INV-L1-12 回填（ARCH 已直接执行，非 CODE 任务）

已由 ARCH 直接编辑 `L1_云端AI首启同意与合规免责_实施蓝图.md` 第 115 行 INV-L1-12，"必须结果"列补入"打开面板前必须先用 `reenableKeyDraft(keyByVendor, vendor)` 填充 `pendingKeyDraft`"，"禁止结果"列补入"以空 Key 直接进 `grantConsent`，覆盖/清空已保存 Key"，见该文件本次修订。CODE 执行 STEP-4/5/6 时不需要再碰主蓝图文件。
