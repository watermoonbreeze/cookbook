# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-31 · K2 AI快捷记餐专项重构·Phase1+2完成**

## 本 session 交付

- ✅ **K2 Phase1 shared基础设施**（7新文件）：`UnifiedMealSchema`(四级+FlatMealJson)、`TextNormalizer`、`TextSegmenter`(方言全覆盖)、`RuleMealParser`(多餐分段+备注移除避坑)、`IngredientNameExtractor`(重构自DishNameIngredientGuesser)、`SchemaValidator`、`SchemaMigration`、`MultiDayRecorder`(3种MergeMode)
- ✅ **K2 Phase2 接入+语音修复**（4文件）：`VoiceRecognizer`(SpeechRecognizer封装+长按+波形)、`AiMealInputSheet`(新布局·去Tab+粘贴+(i)说明+语音波形)、`AiMealInputViewModel`(RuleMealParser兜底+语音状态)、`AiMealPrompt`(重写·FlatMealJson示例+多场景+食材估量)
- ✅ **AI角色→家庭健康营养师** + Manifest RECORD_AUDIO权限
- ✅ **构建双绿·502单测全绿** + Git push远程
- ✅ **专项方案文档** `20260729_AI快捷记餐专项重构方案.md`（含7点用户反馈补充）

## ⏭ 下一步（K1 系列·同一任务）

| 编号 | 项 | 状态 | 要点 |
|------|-----|------|------|
| **K1a** | 预览页展示营养素+热量 | 🔄 | 复用 `DayMealCardView`+`DishNutritionLine` |
| **K1b** | AI带入家庭健康档案做评价 | 🔄 | 脱敏摘要入参→逐成员评价→免责红线 |
| **K1c** | 规则引擎星期偏移推算 | 🔄 | `TextSegmenter.weekdayToIso()`已有接口，待推算逻辑 |
| K1d | Schema客户端/服务端双端兼容 | ⬜ | JSON Schema标准文件+入库流程文档 |
| K1e | AI调用点语义→AI专用结构转换 | ⬜ | 3个调用点紧凑JSON省50-70% token |
| K1f | AI食材入库轻量别名归一 | ⬜ | `ingredient_aliases.json` 50-100条映射·用户已批准 |

**建议优先做 K1a（UI·可感）+ K1b（AI核心价值），再做 K1c（算法补全）。**

## 设计决策（已确立·后续沿用）

1. **AI 交互原则**：食材库不给AI（自主推断→入库校验新建）·健康档案给脱敏摘要（成员年龄/慢病/生命阶段）
2. **AI Prompt 结构**：给AI的数据用紧凑短key JSON（省token），App内部用完整结构——`DomainStruct → AiStructCompact` 单向转换
3. **入库校验**：`createUserIngredient` UNIQUE防重 + `source="ai"` 可编辑/删除 + 轻量别名归一
4. **JSON Schema**：`FlatMealJson` 扁平格式为AI+服务端双端交换格式，`MultiDayJson`嵌套格式为App内部格式
5. **透明准则**：语音T3系统权限·AI解析T2文字告知·自动创建T1 Snackbar撤销·粘贴T0

## 先读清单

1. 本文件
2. `CLAUDE.md`（规范/门禁/踩坑红线）
3. `功能路径索引.md`（定位先查）
4. `待办总览.md`（K1a-K1f 当前进度）
5. `20260729_AI快捷记餐专项重构方案.md`（完整方案·含§十一用户反馈补充）
6. `真机待验证清单.md`（V1-V7 K2语音修复待验）

## 工作规则（延续）

1. 🔴 权威方法论优先 · 数据来源真实 · 营养免责非医嘱。
2. 🔴 一个 session 聚焦一个内聚任务 · off-type 进待办。
3. 🔴 定位先查功能路径索引 · 增删改名文件同 commit 同步索引。
4. 🔴 每功能/bug修复必登记真机待验证清单（含分步操作步骤）。
5. 🔴 色系墙只看膳食结构 · 热量数字默认开可关 · 健康文案守免责。
