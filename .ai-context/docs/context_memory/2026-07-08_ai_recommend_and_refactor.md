# AI 推荐下一餐方案 + picker 重构 · 2026-07-08

[AI生成] 烹饪计时之后：清待办 → 做两个 picker 重构 → 定 AI 首场景落地方案。

## 一、picker 两个重构（已提交 5cfea4c）
- **维度集中化（原 DimensionSection，✅完成）**：`IngredientPickerCommon` 统一 `NUTRITION_DIMENSIONS`/`SEASON_DIMENSION`/`NUTRITION_TAB_DIMENSIONS`/`groupByDimension()`/`DimensionRows`。详情属性区 + VM 营养 Tab 维度集共用（消除 VM 与详情各写一份且不一致）。**完整三态(展示/筛选/编辑)组件暂缓**——无筛选面板消费方，避免过度设计。
- **selectionMode（🔄部分）**：抽出 `SelectionBottomBar` 命名组件消除最大一处散落。**完整两入口拆分暂缓**——高风险，picker 在菜品选食材关键流程，需真机验证。
- 纯重构不改行为，编译通过。

## 二、AI 首场景落地方案（已定，文档 `feature/AI推荐下一餐落地方案.md`）
**场景**：按在手食材推荐下一餐，3 组合、每餐 2~3 菜、附理由+做法。

**决策（用户 2026-07-08 拍板）**：
1. 云端**免费**优先：首选**智谱 GLM-4-Flash**，备选讯飞星火Lite/百度ERNIE-Lite；`AiRuntime` 抽象可换。
2. 云/端：**云端优先验证价值，端侧 P2 隐私版**（架构中立）。
3. 可做性=**主料(is_main=1)齐即可做**；辅料多少→模型给不同做法(cookingHint)。**复用现有 `dish_ingredient.is_main`，不改表**。
4. 入口**都要**：首页"下一餐"区 + 加餐页 AI 按钮。
5. 一餐 **2~3 菜**搭配。

**安全核心**：规则先筛(可做性/犯忌/去重，纯代码 `HealthRuleEngine`)→模型只在安全集里搭组合/讲理由→schema 校验+不足补齐+全失败纯规则兜底。忌口由代码强校验，模型无权绕过。

**分层**：`shared`(commonMain 可单测)放 DTO/`AiRuntime`接口/`RecommendationOrchestrator`/`HealthRuleEngine`/`PromptBuilder`+schema；`androidApp` 放 `CloudAiRuntime`(HTTP)+UI。

**分步**：S0 纯规则+取数+单测 → S1 AiRuntime抽象+Mock+全链路 → S2 接真实云端 → S3 UI(3候选卡/换一换/落地/回退) → S4(P2)端侧。

**编码前置**：①校验 20 预设菜 is_main 主辅料标注(数据质量，:shared:testDebugUnitTest)；②确认取"在手食材/健康忌口大类/最近N餐"query 齐备。

## 三、当前进度
方案定稿committed。**下一步：从 S0 起编码**（HealthRuleEngine + 取数 + 单测，纯 shared，不联网不花钱先验证纯规则推荐）。

## 关联
- 上位方案 `feature/端侧AI能力接入方案.md`（架构/安全边界/场景分级）
- 待办总览 `feature/待办总览.md`（AI 层标 🔄）
