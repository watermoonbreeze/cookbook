# 端侧 AI 能力接入方案

> 时间：2026-06-12。[AI生成]
> 目标：基于当前菜单 App 的记录、复用、食材、健康档案和慢病规则能力，规划近期可落地的端侧 AI 能力。[AI生成]

## 一、结论

- 推荐主线：Android 优先采用 `LiteRT-LM + 小参数中文指令模型`，以 `Qwen2.5-0.5B-Instruct` 作为首轮 POC 模型候选；如果 LiteRT-LM 转换、性能或分发验证不稳定，则切换到 Google 官方 LiteRT-LM/Gemma 生态模型做 Android 首发。[AI生成]
- 不建议把 Gemini Nano/AICore 作为唯一方案：它体验好、免模型分发，但设备覆盖和 API 可控性不足，适合作为“支持设备上的优先运行通道”。[AI生成]
- 不建议首轮用在线大模型做核心闭环：慢病、体检、用餐记录属于敏感数据，近期应优先端侧推理；复杂营养方案可后续做用户授权后的云端兜底。[AI生成]
- iOS 后续单独规划：可优先评估 Apple Foundation Models；若要保持 Android/iOS 同一模型行为，再评估 LiteRT-LM Swift 或 llama.cpp/ExecuTorch。[AI生成]

## 二、模型与运行时选型

### 2.1 推荐组合

| 层级 | 方案 | 用途 | 判断 |
|---|---|---|---|
| 主线运行时 | LiteRT-LM | Android 端本地 LLM 推理、结构化输出、工具调用 | 2026 年官方推荐方向，支持 Android/iOS/Web/Desktop，支持工具调用和 constrained decoding。[AI生成] |
| 首轮 POC 模型 | Qwen2.5-0.5B-Instruct 量化版 | 中文自然语言理解、JSON 草案、菜品/食材语义整理 | 中文和结构化输出更贴合本项目；体积与速度适合先验证价值。[AI生成] |
| 官方模型备选 | Gemma 4 / Gemma 3n 小模型 | LiteRT-LM 官方链路、性能和多模态验证 | 若 Qwen 转换成本高，先用官方模型把工程闭环跑通。[AI生成] |
| 设备系统通道 | Gemini Nano / ML Kit GenAI | 支持设备上的摘要、重写、图像描述、语音转文字等 | 可作为增强通道，不作为唯一依赖。[AI生成] |
| 低层备选 | llama.cpp / ONNX Runtime GenAI / ExecuTorch | 实验、特殊模型、未来跨平台优化 | 首轮集成成本和碎片化风险更高，不建议先做主线。[AI生成] |

### 2.2 与讨论文档的修正

- `temp/端侧模型加载.md` 中推荐 ONNX Runtime 的方向可作为备选，但当前 Google AI Edge 已将 MediaPipe LLM Inference 标为仍可用但建议迁移 LiteRT-LM；首轮应优先跟随 LiteRT-LM。[AI生成]
- 文档中“模型直接生成 SQL”风险偏高；本项目应禁止任意 SQL，只允许模型输出白名单工具调用，由 Repository/UseCase 执行。[AI生成]
- “食材分量、营养成分要准确”不能交给小模型保证；准确计算必须由本地营养/慢病规则库完成，模型只生成候选和解释草案。[AI生成]

## 三、适合当前 App 的 AI 场景

### 3.1 近期可做

| 场景 | 用户入口 | 模型任务 | App 代码任务 | 风险 |
|---|---|---|---|---|
| AI 快速记一餐 | `+号` 或首页搜索 | 把“中午吃了鸡腿饭、番茄蛋汤”解析为餐次、菜品、时间、备注 | 搜索/匹配本地菜品，生成待确认餐食草案 | 低 |
| AI 新建菜品草案 | `NewDishScreen` | 根据菜名推断标签、烹饪方式、常见食材清单 | 用户确认后保存菜品，食材不存在时引导选择/创建 | 低 |
| AI 食材风险解释 | 食材选择器/菜品详情 | 把 crowd_ingredient 的 recommend/limit/avoid 规则解释成人话 | 风险等级仍由规则库计算 | 低 |
| AI 菜品整理 | 菜品库管理 | 识别重复菜品、建议标签、建议别名/拼音搜索词 | 只生成建议，不自动合并或删除 | 中 |
| AI 下一餐建议 | 首页/添加餐食 | 基于健康档案、最近食材、历史菜品给 3 个候选组合 | 过滤禁忌、去重、落到 DishPicker 选择态 | 中 |

### 3.2 一期后做

| 场景 | 前置条件 | 模型任务 | App 代码任务 |
|---|---|---|---|
| 一周菜单生成 | 健康档案、慢病规则、足够菜品库 | 生成按天/餐次的候选计划 | 规则过滤、重复度控制、用户确认后写入计划 |
| 营养师方案录入助手 | 营养方案表或配置模型 | 从自然语言方案提取热量、三餐比例、限制条件 | 存储为结构化方案并参与校验 |
| 饮食周报 | 历史餐食足够 | 总结偏好、重复、风险点和改善建议 | 统计指标由 SQL/代码生成，模型只做摘要表达 |
| 智能换一换 | 推荐体系稳定 | 在约束内替换某个菜品或食材 | 校验健康规则和本地可用食材 |

### 3.3 后期再做

| 场景 | 原因 |
|---|---|
| 拍照识别菜品 | 多模态模型、相机、图像质量和食材识别误差较大，适合在文本 AI 稳定后做。 |
| 食品包装/OCR 识别营养表 | 需要 OCR、营养字段映射和错误校验，价值高但工程面更大。 |
| 复杂多病种营养处方 | 医疗风险高，应以规则库和专业来源为主，小模型只能做辅助解释。 |
| 自动执行数据库写入 | 风险高，必须保留用户确认，不做无确认自动写库。 |

## 四、推荐架构

```text
UI 层
  AI 快速记录 / AI 菜品草案 / AI 解释 / AI 推荐入口
        |
AI Orchestrator
  任务路由、Prompt 构造、模型调用、JSON Schema 校验、重试
        |
Tool Registry 白名单
  search_dishes
  search_ingredients
  list_recent_meals
  list_recent_ingredients
  get_health_profile
  get_crowd_rules
  create_meal_draft
  create_dish_draft
        |
shared Repository / UseCase / SQLDelight
  真正读写数据库、统计、规则计算、慢病校验
```

- `shared` 放 AI 任务 DTO、工具调用协议、规则校验、草案模型和 UseCase；不要放 Android/iOS 运行时实现。[AI生成]
- `androidApp` 放 LiteRT-LM/Gemini Nano 适配器、模型下载、权限/存储、推理日志和 UI 入口。[AI生成]
- 模型输出必须是 JSON 或工具调用，先通过 schema 校验，再转为 `MealDraft`、`DishDraft`、`RecommendationDraft` 等业务对象。[AI生成]
- 所有写操作必须用户确认；模型不能直接执行 SQL，不能直接删除、合并、覆盖用户数据。[AI生成]

## 五、近期实施路线

### 第 0 步：桌面 POC

- 准备 20-50 条真实中文用例：快速记餐、生成菜品草案、解释风险、推荐下一餐。[AI生成]
- 比较 `Qwen2.5-0.5B-Instruct`、`Qwen2.5-1.5B-Instruct`、官方 Gemma 小模型在 JSON 稳定性、中文菜名、慢病解释上的表现。[AI生成]
- 验收指标：JSON 可解析率 > 95%；禁忌食材不应主动推荐；输出可被 App 规则层过滤。[AI生成]

### 第 1 步：Android 工程闭环

- 新增 `AiRuntime` 抽象和 Android LiteRT-LM 适配器。[AI生成]
- 模型不直接打进 APK，优先放到 `/sdcard/cookbook/model/` 或应用私有目录，首次使用时引导下载/导入。[AI生成]
- 先只做“AI 新建菜品草案”和“AI 快速记一餐”，这两项最贴近当前 MVP，且失败可回退为手动填写。[AI生成]

### 第 2 步：规则 + 推荐

- 建立 `HealthRuleEngine`：基于 `crowd_ingredient`、健康档案、食材分类输出硬规则结果。[AI生成]
- AI 推荐只生成候选，规则层负责剔除不合格项，并给出可解释原因。[AI生成]
- 加入“换一换”，但只在本地已有菜品/食材范围内替换，避免模型凭空生成不可选项。[AI生成]

### 第 3 步：多模态与跨平台

- Android 可评估 Gemini Nano/ML Kit 的图像描述、语音识别能力，作为拍照/OCR/语音输入试点。[AI生成]
- iOS 重启后评估 Apple Foundation Models 或 LiteRT-LM Swift；保持 shared 层协议一致，平台只替换 runtime。[AI生成]

## 六、安全与产品边界

- 慢病建议不能表现为诊断或医疗处方，文案应定位为“饮食记录与风险提示辅助”。[AI生成]
- 健康规则、过敏、禁忌、营养阈值必须由代码强校验，小模型输出不能作为最终依据。[AI生成]
- 本地日志只记录任务类型、耗时、模型版本、JSON 解析结果和错误摘要，不记录完整健康档案、完整对话和敏感体检数据。[AI生成]
- 模型下载需展示体积、网络/Wi-Fi 提示和删除入口；低端设备应允许关闭 AI 功能。[AI生成]
- 端侧推理失败时必须可回退到现有手动流程，不能阻塞记录一餐的 MVP 核心链路。[AI生成]

## 七、资料来源

- Google AI Edge LiteRT-LM Overview：https://developers.google.com/edge/litert-lm/overview
- Google AI Edge MediaPipe LLM Inference Android Guide：https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android
- Android Gemini Nano：https://developer.android.com/ai/gemini-nano
- ML Kit GenAI APIs：https://developers.google.com/ml-kit/genai
- Apple Foundation Models：https://developer.apple.com/documentation/FoundationModels
- ONNX Runtime GenAI：https://github.com/microsoft/onnxruntime-genai
- Qwen2.5-0.5B-Instruct：https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct
