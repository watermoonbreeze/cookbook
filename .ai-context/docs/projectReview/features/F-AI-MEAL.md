# F-AI-MEAL · AI 快捷记一餐

> 全景图纵轴：单功能深度详情。跨功能机制/算法/架构/约定见横轴册（`01/02/04/06/20/21/22`），本文件只放该功能独有内容，跨功能内容放摘要+链接，不重复权威内容。
> Project Graph 对应：`.ai-context/project_graph/features/F-AI-MEAL.yaml`（`lifecycle: active`，Project Graph 的 PoC 样例功能，全实体演示）
> **当前项目主线之一**：本功能是"AI 与网络请求策略"（`21`）册的主要案例来源。

## 目标
AI 主导解析用户输入（文字/语音）生成餐食记录，双阶段：阶段一 AI 主导宽松结构化解析、本地规范化入库；阶段二经单次同意生成健康建议，只在确认页展示、不持久化（`08_决策记录.md` D-13/D-14）。

## 现状
- ✅ **AI 记餐长输入改造（NDJSON 流式）已实现，K1g 真机验证中**：快速记 ≤200 字；周期记按周/日期分段；云端 max token 按场景放大覆盖一周；确认页发送后即进入生成页 + NDJSON/JSONL 流式解析 + 已合法内容可确认；整体 JSON 作为同一协议的兼容输入规范化。代码见 `StreamingMealParser.kt`/`StreamingMealSession.kt`。验收门禁见 `feature/AI记一餐_周期记_NDJSON流式开发规范.md` B1–B6（→`21`）。B3–B6 均已交付并 ACCEPTED（`.ai-context/docs/功能路径索引.md` 此前"B4-B6未开始"描述过期，2026-08-18 已订正）。
- ✅ **L1 云端 AI 首启同意 + 合规免责**已落地（同意状态模型 + 运行时闸门 + 设置页政策披露，`ad1c5878`），ARCH 独立复核通过，真机待验 `E-L1-01~12`。
- ✅ **K1i AI 流式渐进展示**已落地（`SwitchableAiRuntime.stream()` 真实委托，复用 L1 同意闸门，`d7240d6f`），ARCH 独立复核通过，真机待验 `E-K1I-01`（阻断性）/`E-K1I-02`。
- ✅ **B7：CREATE 路径做法/烹饪方式/标签/描述/特别说明丢失 bug 已修复（2026-08-18）**：根因 `DishAutoGenerator.preview()` 未把 `SemanticDish` 已解析字段透传进 `DishPreview`，`commit()` 又对 `dishRepo.saveDish()` 硬编码空值——AI 解析出的做法从未真正落库。改动：`AutoGenModels.kt`（`SemanticDish`/`DishPreview` 补字段）+ `DishAutoGenerator.kt`（preview/commit 透传）+ `MultiDayRecorder.kt`（补 `steps` 映射），回归测试 `T-B7-01`，653 个 shared 单测全绿，经 `google_quality_engineer` 审查无阻断项。**尚未提交 git**。此前"讨论中的方案"里"份量、做法持久化…后续完成"这条的确切根因即此。

## 讨论中的方案
- 🔄 **核心实体能力层统一**：手工建菜/食材库/自由搭配仍有路径绕过 `IngredientAutoGenerator`，目标是统一的食材、营养、菜品语义能力入口（跨 F-DISH/F-INGREDIENT）。
- K1b（膳食健康评价逐成员化）蓝图仍 `DRAFT·PARKED`，等本线真机验证彻底收尾后再拾起。
- K1i-2（AI 推荐/周计划健康建议流式化）仅登记名字，未设计。

## 已知问题
- ⚠ **AI 日志隐私风险**：云端 Runtime 仍可能记录完整请求/响应，须脱敏后才能宣称符合隐私口径。
- ⚠ **`AppLogger.e/w/i` 无 debug 门禁，release 包也持久化写盘（2026-08-18 排查发现）**：`androidApp/.../util/AppLogger.kt` 的 `e/w/i` 不经 `isDebuggable()` 判断，release 下仍把日志写入 app 专属外部目录 `/sdcard/cookbook/log/yyyy-MM-dd.log`；对比同文件 `debugLong()`（专供"AI 原始请求/响应"用，明确注释"只允许 debuggable 包输出"）——项目已意识到 AI 原始载荷需要门禁，但没推广到 `e/w/i`，是执行不一致。已排查 `AiMealInputViewModel.kt:732/832` 两处 `AppLogger.e()` 本身不拼接用户原始饮食文本（只拼 `e.message`），但 `DishAutoGenerator.kt:147`/`IngredientAutoGenerator.kt:123` 两处防御性 `error("...for ${preview.inputName}")` 会把具体菜名/食材名（用户饮食片段）写进这份持久化日志（触发概率低，属不该发生的边界断言）。严重度中低，暴露面限于物理接触设备/adb pull，不构成远程泄漏。修复建议见待办。
- 🐛 **S1：AI 标签写库未清洗（2026-08-18 code review 发现，B7 修复引入的新 sink）**：`DishRepository.kt:534-538` 的 `tagNames.distinct().forEach { q.insertDishTag(...) }` 无 `trim()`/空值过滤（对比同文件 `ensureCookingMethodIds` 有守卫）。B7 修复后 AI 标签首次接入这条写入点，AI 吐出空白/带前后空格标签会在 `dish_tag` 字典留脏数据（空名标签排序靠前、`" 下饭菜"` 与 `"下饭菜"` 分裂成两条）。

## 待办
- 真机验证详单见 `真机验证/真机待验证清单_<最新>.md`（`E-L1-01~12`/`E-K1I-01~02`/`E-B4/B5/B6-*` 等 ~30+ 项），本文件不重复摘抄。
- **B7 后续 S1**：`DishRepository.kt:534` 收口 `tagNames.map{it.trim()}.filter{it.isNotBlank()}.distinct()`。
- **B7 后续 · 日志门禁**：①`AiMealInputViewModel.kt:732/832` 改用 `AppLogger.e("AiMealInput","preview failed",e)` 不手动拼 `e.message`；②`AppLogger.e/w/i` 补齐与 `debugLong()` 一致的 release 门禁（至少持久化写文件这步区分 debug/release）；③`DishAutoGenerator.kt:147`/`IngredientAutoGenerator.kt:123` 防御性 `error()` 去掉具体菜名/食材名。
- **B7 后续 S3**：补两个回归测试——① REUSE 不会被同名新内容覆盖（本次修复安全前提，目前只有代码逻辑保证没有测试钉住）；② `MultiDayRecorder→DB` 的 `steps` 端到端测试（目前只有 `DishAutoGenerator` 单测覆盖，`MultiDayRecorder.toSemanticDay` 那行映射本身无测试）。
- **B7 后续 S2（决策项，非纯 bug）**：`DishJson.cuisine`/`meal_slots`/`note` 同样在 `MultiDayRecorder.toSemanticDay` 里未透传进 `SemanticDish`。`cuisine` 有坑——schema 默认值"家常菜"、AI 未被要求填此字段，直接透传等于给所有 AI 建的菜盖章"家常菜"，需要先决定要不要收，不能照搬 B7 做法。
- **B7 后续 S4**：遗留文件 `AiMealRecorder.kt`（`resolveDish()`，功能路径索引标"遗留未接入"）里有 B7 同一个 bug 的另一份拷贝（硬编码 `tagNames=emptyList()`/`description=""`/`steps=emptyList()`）。当前不在实际调用路径上，但仍在 DI 注册，建议确认后直接删除，否则是"哪天被复用就复发"的雷。
- **B7 后续 · 透明准则交叉项**：确认页 `AiMealInputSheet.kt` 目前只渲染菜名+「新」标+营养行，B7 修复后 AI 生成的做法/标签/描述会跟着确认动作落库，但用户确认时看不到这些内容。按项目 Tiered Transparency 准则可能需要 Apple UX 视角评估是否要在「新」标菜下露出"含 N 步做法"之类摘要。

## 关联横轴
- `21_AI与网络请求策略（专属）.md`（本功能的 AI 调用/隐私/重试策略权威定义，本功能是该册主案例）
- `20_健康与算法逻辑（专属）.md`（阶段二健康建议算法）
- `08_决策记录.md` D-13/D-14/D-15/D-16/D-17（AI 显式语义优先、双阶段、日期锚点、NDJSON 流式、冻结蓝图门禁）

---
最后更新：2026-08-18 · 来源：本次代码级排查 + B7 修复批次实核，B7 之前的条目来自 `07_项目现状.md` 能力成熟度表重组迁移未逐条重新核实，后续随真实开发批次继续校准。
