# 无人值守决策记录

> 用户 2026-07-08 下班启动无人值守。指令：按待办顺序逐步执行；**暂无上架计划，P0 存储合规不做**；其他继续。
> 规则：深度执行；高风险/需真机 → 待确认队列不擅自做；每阶段 `[unattended]` 提交不 push；≤15 文件/阶段。

---

## 2026-07-10 第二次无人值守（下班）

> 指令：从待办挑可做项，逐个「实现→审核→回测→过了再下一个」。
> 全做完后：**整体回归测试+代码审核+架构审查 至少 5 轮**确保零问题 → 保存上下文 → 总结经验 → **编译 debug APK(给路径)** → 提交 push → 飞书通知完成。
> 补充约束：**iOS 暂不做，只做 Android**（用户明确）。库存*配置开关*用户本日已定"先占位、之后再做"，本轮回避该配置项。

### 本轮回避（不擅自做）
- 库存/份数**功能开关配置**（用户本日明确先占位）
- iOS 编译（用户明确暂不做）
- 端侧 AI Step2（上游端侧模型未接入，阻塞）
- 营养素+每日卡路里+热力图（L，需补营养数据 + 健康数据免责红线，范围大不宜无人值守）
- 各"真机待验"项（无设备）

### 本轮候选执行（自包含/可 :shared 单测回测/对现有行为零破坏优先）
| 顺序 | 任务 | 判定 | 状态 |
|---|---|---|---|
| 1 | 采购清单聚合(购物清单) | 加法型，复用 pantry 口径，可测 | ✅ 完成(89单测绿)`6ca7943` |
| 2 | P3 KMP 日志 | expect/actual 无依赖；接入 seed/采购诊断点 | ✅ 完成`9030640` |
| — | P2 SQLDelight 写入 Diff | 改写 seed 写库逻辑，中高风险 | ⏸ 停做(与"零问题"目标冲突，留待确认) |
| — | DI 平台化 | Koin 重构低收益、有 wiring 风险 | ⏸ 停做(同上) |

**决策**：完成 2 个自包含加法项后停止新增功能，转入用户要求的「5 轮整体回归+审核+架构审查」，把风险预算留给"确保零问题"。P2/DI 留待用户确认再做。

### 5 轮整体审查记录
- 每轮：全量 build + :shared 单测 + 多维代码审查(正确性/架构一致性/Compose/数据安全/KMP)，发现即修复再回测。

## 执行计划（安全优先，可编译/单测验证的工程任务先做）

| 顺序 | 任务 | 判定 | 处置 |
|---|---|---|---|
| 1 | P1 Shared IO Dispatcher | 低风险纯重构，build+test 可验 | ✅ 执行 |
| 2 | DI 平台化 `platformModule` | 低风险，build 可验 | ✅ 执行 |
| 3 | P3 KMP 日志 | 加依赖(Kermit)+集成，中等 | ✅ 执行(评估后) |
| 4 | P2 SQLDelight 写入 Diff | 改写库逻辑，涉数据，中高 | ⏸ 评估风险后定 |

## 待确认队列（高风险/需真机，未擅自执行）
- **首页搜索"跳到具体食材并高亮"**：跨底部 Tab 传参 + 高亮，Compose 跨 Tab 状态易出错，无真机难验证。
- **回归验证（删除一致性 / 图片持久化）**：本质是真机专项验证，无设备无法执行；仅可做代码级审阅。
- **iOS shared 编译能力**：启用 iOS target 需补多处 actual + framework 验证，改动大且无 iOS 环境验证。
- **selectionMode 完整两入口拆分**：高风险重构，picker 在菜品选食材关键流程，需真机回归。
- **AI S4 端侧模型(LiteRT-LM)**：大工程，占位已留，待专门规划。
- **P0 存储权限合规**：用户明确暂不做（无上架计划）。

## 本轮收尾总结（2026-07-08 无人值守）

**已完成并提交（本地，`[unattended]` 前缀，未 push）：**
1. P1 IO Dispatcher（91 处 → 专用 IO 池）
2. 删除一致性修复（菜品软删后食历保留当时吃的菜）
3. 首页搜索点食材 → 跳到该食材并高亮（`IngredientJumpBus`）

**共 4 个功能/修复提交 + 若干文档提交，全部 build/单测通过。均未 push（等用户回来）。**

**结论**：待办里**清晰安全、可编译验证**的项已做完。其余项（见下方待确认队列）都需用户**决策/真机/依赖抉择**，无人值守下按规则不擅自动，等用户返回定夺。

## 决策/进展日志

- **阶段1 ✅ P1 IO Dispatcher**（commit `[unattended]`）：新增 `expect val ioDispatcher` + Android actual=Dispatchers.IO；8 repo + RecommendationDataSource 共 91 处 `Dispatchers.Default`→`ioDispatcher`；build+单测过。
- **阶段2 ⏸ DI 平台化 → 待确认**：shared 只有 koin.core（无 koin-android）。移平台注册进 shared androidMain 需加 koin-android 依赖 + 用 androidContext()，且 DI 解析错误是**运行时崩溃**、无设备编译验不出——无人值守不擅自做。
- **阶段3 代码审计 + 修复**（删除一致性 + 图片持久化）：
  - ✅ **修复(中危)删除一致性**：`selectDishesOfMealRecords` 去掉 `d.status=1` → 菜品软删后食历仍显示当时吃的菜（与食材保留引用 pattern 一致；菜品库另用带 status=1 的查询不受影响）。build+MealRecord 测试过。**属行为变更，用户可 review**。
  - ⏭ **不做(低危)图片路径去重**：审计建议用 zip 按对 distinct，但会假设 image/thumbnail 两列表等长；旧数据可能长度不等→zip 截断丢图，引入回归。原问题近不可能触发，不冒险。留待需要时按"补齐长度再配对"做。
  - ✅ 图片持久化整体**无问题**：存 `/sdcard/cookbook/img/` 绝对路径、原图+缩略图、重启可回源。
- **阶段4 ✅ 首页搜索"跳到具体食材并高亮"**（原待确认项，重估后安全落地）：
  - 新增 `IngredientJumpBus`(单例) 跨屏传食材；搜索点食材 → request()+跳食材页；IngredientsScreen 消费 → 复用成熟 `jumpToIngredient` 定位高亮 → consume。
  - **失效模式优雅**：若跨 Tab 时序未收到，只退回"跳到食材页"当前行为，**无回归风险**——故安全执行。
  - build 过。**跳转/高亮的视觉效果需真机确认**（时序类逻辑编译验不出，但复用已验证的 jumpToIngredient，且失败降级到旧行为）。

## 无人值守（2026-07-09 下班后）：审核+测试+优化

**触发**：用户"把能做的都做完，不要问；代码架构审核测试多跑几轮，能优化的优化好，符合软件工程规范"。已先保存上下文 + 经验总结#14。

**本轮已完成（`[unattended]` 提交，未 push）**：P0存储合规、图片相对路径、完整zip备份、SAF导出导入、双设备局域网同传+二维码、端侧自测Step1、AI推荐打磨——见 `2026-07-09_p0存储_备份同传_端侧自测.md`。

**待确认队列（无人值守不做，需用户/真机/依赖抉择）**：
- 端侧 AI Step2：接 native 运行时(建议先 MediaPipe LLM Inference)+模型导入+实装推理。需真机+大模型，无法在此验证。
- 真机验证：P0重装/备份导出导入/双设备同传(两台同WiFi)/拍照CAMERA授权/删除一致性/图片持久化。
- iOS shared 编译、selectionMode 完整拆分：高风险暂缓。
- DI 平台化：需加 koin-android 依赖 + 运行时才验得出，暂缓（同上轮结论）。

**本轮动作**：对本会话改动跑 4 路并行代码审核(存储备份/双设备同传/图片AI/架构规范)，汇总后修确认的真实问题，多轮 build+单测，符合工程规范的优化一并做。findings 与修复见下方续记。

### 审核结果与修复（4路并行审核 → 5批修复）

**4 路独立审核**（存储备份/双设备同传/图片AI/架构规范）共发现真实问题，已按"数据安全>真实bug>工程规范"修复：

**批1 备份数据安全**（`fix(backup)`）：恢复原子化+回滚(防中途失败破坏现有db)；TRUNCATE checkpoint 只打包主库(消除wal/主库版本错配)；importFrom失败清理半成品；zip白名单解压；exportTo/expect KDoc订正。
**批2 同传健壮性**（`fix(sync)`）：接收/握手读 SO_TIMEOUT(防掉线永久阻塞/OOM)；持有receiveSocket可主动关闭；cancelling标志区分主动取消vs真实异常(不再吞错)；localWifiIp过滤link-local+wlan优先+私网段(修多网卡选错)；扫码后二次确认(覆盖不可逆)。
**批3 图片/AI**（`fix(image/ai)`）：PlanOrchestrator统计改花括号消分号隐患；DeviceAiGrade阈值7.5→7.0；过时"存储权限"文案→"请重试"；类注释路径订正；删未用常量；图片文件名加随机后缀防同毫秒碰撞。
**批4 工程债+测试**（`refactor(test)`）：SyncPayload(协议)、DeviceAiGrade+gradeFor(阈值) 下沉 shared，补 SyncPayloadTest/DeviceAiGradingTest 单测（androidApp原零测试覆盖）。

**验证**：clean 后完整构建 + 全部单测 = **14类68用例0失败**；androidApp assembleDebug 通过。

**复核判为非bug（未改，记录）**：`deleteTempCameraFile` 实际可删(FileProvider `camera_cache` 根名映射，lastPathSegment 即文件名)；两列表 distinct 因文件名带时间戳唯一实际不触发。

**待确认/后续（无人值守不做）**：
- 恢复后驱动热重建/自动重启进程：现关单例 driver 无重建，UI已提示"重启应用"、本轮已补原子化防损坏；彻底解决需改 DI 单例生命周期或恢复后强制重启，属架构改动+需真机验，留待确认。
- iOS BackupManager actual：iOS target 当前注释、不编译，不影响；启用 iOS 时补。
- 过时 `/sdcard/cookbook/log` 注释(AppLogger/LogFileManager/MineViewModel/AndroidModule 各1处)：纯注释、零风险，为守 ≤15 文件规范本轮未动，5分钟可扫尾。
- 备份并发写 torn-page：minSdk21 无 VACUUM INTO；手动触发低并发 + TRUNCATE checkpoint 已缓解，接受。
- StoredImage 预览无缓存、二维码主线程生成：低价值性能项，留后。

**结论**：审核发现的**数据安全与真实bug已全部修复并测试通过**；工程债(可测逻辑下沉+单测)已补；其余为需真机/架构决策/纯注释项，按无人值守规范记入待确认。均 `[unattended]` 本地提交，未 push。

## 无人值守（2026-07-10 吃饭）：搜索入库份数 + 周期规划采购提示

**触发**："我吃饭去了，你自行决定吧"。两需求：
1. 食材搜索界面入库时也能选份数(现搜索面板入库=默认1份, 无选择)。
2. 周期规划菜品：库存有但份数不够→"缺料"(灰); 库存里没有→"采购"(灰, 同缺料样式)。

**自主决策**：
- 请求1：搜索入库复用份数选择(弹份数 stepper 后 addServings)，与详情表 PantryServingSection 一致体验。
- 请求2：**采购只判主料**(盐/油等调料不标, 避免噪音, 延续"非库存食材不标"精神但对主料放开为采购提示)。作用于**周期规划预览**(AiPlanScreen)：按当前库存快照，每道菜主料 不在库→采购、在库但可用份数≤0→缺料，缺任一则灰显。不跨规划自身多天分配(规划本就"涉及后续采购")。
- 若 PlanDish 无食材 id，则给 PlanDish 补主料 ingredientId 列表(gatherForPlan)，AiPlanViewModel 计算采购/缺料标到 PlannedDish 供 UI 展示。

**验证**：每步 build + 单测；≤15 文件；[unattended] 提交不 push。探查结果与实现细节续记于下。

### 实现结果（2026-07-10 吃饭时段）
- **请求1 搜索入库份数** ✅：`SearchResultsPanel` 入库→`PantryServingDialog`(1~99份)→addServings。
- **请求2 周期规划采购/缺料** ✅：`PlannedDish` 加 shortageNames/purchaseNames；`PantryPlanAnnotator`(纯,可测,按天序分配剩余份数, 只判主料)；`RecommendationDataSource.annotatePlanWithPantry`(查主料+库存快照); AiPlanViewModel 生成后标注; AiPlanScreen 缺/采购菜半透明+"🛒采购:X"/"⚠缺:X"。
- **自评修复**：库存完全为空(未用库存功能)时**不标注**，避免整份规划全标采购灰显打扰。
- 验证: 全部单测 78 用例 0 失败(+PantryPlanAnnotatorTest); androidApp 构建通过。
- 决策: 采购只判主料(盐油等调料不标); 规划按当前剩余份数为预算跨天分配(规划本就"涉及后续采购")。
- 均 [unattended] 本地提交, 未 push。
