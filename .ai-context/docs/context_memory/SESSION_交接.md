# 🔖 SESSION 交接入口

> 更新时间：**2026-08-05 15:20**
> 当前状态：AI 记一餐 V2 追加验证通过；周期记 + NDJSON 流式大改的 B1/B2 均已八审通过，B3 可开始。

---

## 一、当前已完成

### 1. AI 记一餐 V2 追加验证

用户已确认：**V2 追加整体没问题，可以了**。2026-08-05 补充：V2-1～V2-17 整组真机验证通过，清单已标记。

已推送提交：

- `b903a900 chore: add AI meal debug raw logs`
- `6930b5a8 fix: polish AI meal validation fixes`

关键修复：

- 解析失败详情里“查看原始返回”可滚动。
- 确认页去掉重复“重新输入”，只保留底部“修改”。
- 日期锚点修复：重新选择日期后 AI 快捷记按新日期注入。
- 菜品家庭分类口径修复：`source != preset` 均归入家庭分类，含自建、AI、后续来源。
- AI 记餐长输入/周期记/NDJSON/流式输出方案已写入全景图、ADR、诊断地图、功能路径索引和待办。

### 2. 本轮最新小优化（待提交/推送）

用户要求：AI 快捷记只有修改日期才清空；同一日期误关重开要保留内容，防止手快点错。

已实现：

- `AddDayFoodScreen` 的 AI Sheet key 改为“日期切换/保存完成才换 key”。
- 同一日期未保存会话重开保留输入/预览状态。
- 切换日期后重开清空，并以新日期为锚点。
- 保存成功后同日再打开是新空会话，避免复用 DONE 状态闪退。
- 真机清单追加 V2-17，文件已更新时间戳：`feature/真机待验证清单_202608051156.md`。
- 本地构建已通过：`scripts\build-cli.bat :androidApp:assembleDebug` → `BUILD SUCCESSFUL`。

### 3. 跨模型项目上下文收敛

- 新增 `.ai-context/PROJECT.md`：所有模型的首读顺序、事实优先级、当前任务和资料分层。
- 重写 `docs/AI-交接文档.md`，并同步 `AGENTS.md`、`CLAUDE.md`、`.ai-context/README.md`、项目地图和待办入口。
- 根 `docs/` 的 5 份历史资料迁入 `feature/_archive/legacy_root_docs/`；根目录不再保留项目知识双源。
- 旧无时间戳真机清单已归档；当前唯一清单是 `feature/真机待验证清单_202608051156.md`。

---

## 二、下个 session 的主任务

### 任务：AI 记一餐大改 · 周期记 + NDJSON 流式解析

> 2026-08-05 架构基线已补齐：实施必须先读 `feature/AI记一餐_周期记_NDJSON流式开发规范.md`，按 B1 至 B6 分批交付；当前无老版本，禁止旧协议兼容、数据迁移与双轨状态。`b37ace6f` 已以注入阻塞 `HttpURLConnection` 覆盖 `disconnect→IOException→CancellationException`，B1/B2 八审通过；下一步只可按 B3 会话状态机范围实施，不得回退或重构已通过的 transport/parser 契约。
> 跨模型上下文已审计：首次接手先读 `.ai-context/PROJECT.md`；根 `docs/` 历史资料已迁入 `feature/_archive/legacy_root_docs/`，不得作为当前依据；真机只认时间戳最新的唯一清单。

先读：

1. `.ai-context/PROJECT.md`
2. `feature/AI记一餐_周期记_NDJSON流式开发规范.md`
3. `feature/AI记一餐_周期记_NDJSON流式改造落地方案.md`
4. `projectReview/21_AI与网络请求策略（专属）.md`
5. `projectReview/08_决策记录.md` 的 D-13～D-16
6. `projectReview/05_诊断地图.md` 的 AI 记餐条目
7. `.ai-context/docs/功能路径索引.md` 的 AI 快捷输入记餐行
8. 最新真机清单：`feature/真机待验证清单_202608051156.md`

### 已拍板目标

- 快速记：最大 200 字。
- 周期记：按周/日期分段，每天最大 200 字。
- 云端 max token 按场景放大，至少覆盖一周餐食。
- 优先 NDJSON/JSONL 输出，每行事件可独立校验、边收边展示。
- NDJSON 事件必须携带归属键：`segment_id` 标识输入分段，`meal_id={date}|{slot}` 标识餐次，`dish_id={meal_id}|d{index}` 标识菜品；本地以父子键归组，不以行顺序作为唯一依据。
- 整体 JSON 兼容：能提取菜名就规范化进入确认页，不因“非扁平格式”直接拒绝。
- 发送后立即进入生成确认页；已合法内容可查看、可确认记下。
- 健康建议仍只展示、不持久化。
- 所有 AI 输出类能力都按流式/渐进显示兼容设计。

### 推荐分批

1. 协议与 max token：`AiMealPrompt`、`CloudAiRuntime`、`AiMealParser`，补 NDJSON/整体 JSON/截断测试。
2. 流式 Runtime：新增流式抽象或扩展 `AiRuntime`；不支持流式时整体响应模拟事件。
3. ViewModel 渐进状态：增加生成态、事件缓冲、局部合法 preview、诊断。
4. 输入 UI：快速记/周期记两种输入；每段 200 字限制。
5. 确认页流式展示：增量餐食、警告、失败尾部、最终重排。
6. 真机验证：DeepSeek 一周菜单、截断、非扁平 JSON、日期锚点、同日误关重开、切日期清空。

### 不做/单独待办

- 菜品/食材自动添加成熟算法联网调研：已登记在 `待办_功能算法.md`，下个大改不强依赖，可单开调研。
- 健康建议持久化：明确不做。
- 旧版本兼容：不做，用户明确“版本还没发，所有都是新版本”。

### 文档挂钩规则

- 方案型细节不能只写在 feature 文档里；必须挂到全景图对应功能分册。
- 本次大改已挂：`projectReview/21`、`projectReview/08 D-16`、`projectReview/05`、`功能路径索引`、`feature/AI记一餐_周期记_NDJSON流式改造落地方案.md`。
- 下个 session 每完成一批都同步更新对应全景图节点和唯一真机清单。

---

## 三、关键红线

- AI 显式结构化食材/调料/做法优先，本地不要再按菜名规则覆盖。
- `finish_reason=length` 必须明确提示“模型输出被截断”，且保留截断前已完整解析内容。
- 日期锚点按 D-15：绝对日期 > 所选日期；星期 = 所选日期所在周；无日期 = 所选日期。
- 流式生成中不写库；用户确认后才写库。
- Release 禁止完整 prompt、原始饮食文本、完整模型响应和 Key。
- 同一日期误关重开保留内容；切换日期或保存成功后新会话。
- NDJSON 缺 `meal_id/dish_id` 时只能进入待确认/诊断，不能静默挂到最近餐次或最近菜品；防止周一早餐被归到周二晚餐。

---

## 四、当前真机待验

最新清单：

- `feature/真机待验证清单_202608051156.md`

本轮新增重点：

- V2-17：同一日期误关重开保留输入/预览；切换日期清空；保存后同日再开为空会话。
- V2 追加整组：已通过，不需要下个 session 重复验证。

---

## 五、提交前状态提醒

本轮应提交/推送的相关文件：

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal/AddDayFoodScreen.kt`
- `feature/真机待验证清单_202608051156.md`（由 202608051122 rename）
- `feature/AI记一餐_周期记_NDJSON流式改造落地方案.md`
- `context_memory/SESSION_交接.md`
- `context_memory/SESSION_交接_历史.md`
- experience 相关总结文件
- `.ai-context/PROJECT.md`、`docs/AI-交接文档.md`、`.ai-context/README.md`
- 项目地图、待办/经验入口和 `feature/_archive/legacy_root_docs/` 的本轮文档收敛变更

仍存在既有无关未提交文件，不要混入：

- `temp/claude/chatlog.md`
- `temp/db/cookbook.db`
- `temp/pic/`
- 若干 2026-08-04/08-05 旧未跟踪 context_memory 文件
