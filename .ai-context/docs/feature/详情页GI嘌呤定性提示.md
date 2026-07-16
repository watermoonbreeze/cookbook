# 菜品详情页 GI/嘌呤定性提示（part2 慢病维度）

> 2026-07-16 无人值守落地。经**多角色验证门禁**（临床营养/产品 + 架构/UX 一致性双角色）+ Google 代码审查门禁。守健康免责红线。

## 一、目标与问题
承接今日卡的 GI(糖尿病)/嘌呤(痛风)聚合提示——把慢病定性下沉到**菜品详情页**（用户"选这道菜"的**决策时点**，事前干预 > 今日卡事后回顾）。难点：详情页已有"逐食材 care 忌口"(avoid/limit/recommend)，聚合 GI/嘌呤是否重叠冲突？→ **多角色验证**裁决。

## 二、多角色验证结论（收敛）
两角色（临床营养/产品 + 架构/UX）结论高度一致：**该做，但只作为 care 忌口的 data-driven 补充**。
- **两套来源天然互补**：care = `care_category` 人工策展清单（avoid/limit/recommend，带医学建议色彩）；GI/嘌呤 = data-driven（GI≥70 查库值 / 嘌呤 WS/T560 关键词，食物客观属性）。**GI/嘌呤只补 care 没策展到的漏网食材**。
- **去重**：GI/嘌呤命中排除已在 `avoidNames∪limitNames` 的食材（care 更强定性优先），用户不会看到同一食材两处。
- **gate 病种**：详情页沿用**全家 enabled 健康档案**口径（`fromCareName(crowdName)`），与本页 avoid/limit 同源自洽。与今日卡按"关注成员"的差异属**视角不同**（详情=菜品对全家档案、今日卡=关注成员当日摄入），合理，非 bug。
- **只算主料**(isMain)，与 care 同口径（避免辅料失真 + 告警疲劳）。
- **措辞规避 GL 陷阱**：不说"这道菜升糖快/危险"（GI 高但份量小时 GL 低会误导），只**成分陈述**"含X等高GI食物 · 糖尿病可换低GI或控量"（"控量"呼应 GL）、"偏高：X · 痛风建议避免"。不引入嘌呤数值分级（无国标）、不引入 GL 计算（数据不足更不可靠）。

## 三、实现落点
- `NutritionLevel.kt`：新增纯函数 `dishQualitativeHits(mainNames, conditions, giByName, alreadyFlagged): Pair<高GI名, 高嘌呤名>`——gate 病种 + 复用 `matchHighGiFoods`/`matchHighPurineFoods` + `filterNot{in alreadyFlagged}` 去重。**聚合去重逻辑下沉 shared 纯函数**以覆盖 androidApp 无 VM 测台的盲区。
- `DishDetailViewModel.computeInsights`：avoid/limit/recommendNames 提为 val；解析 conditions；gate DIABETES 才 `nutritionRepo.giByName()`（省无谓全表查）；调纯函数；DishInsights 加 `highGiNames`/`highPurineNames`。
- `DishDetailScreen`：care `when` 块后加两独立 `InsightLine`（GI/嘌呤是独立维度，可与忌口并存），复用现有组件与免责行，零新组件。
- 单测：`dishQualitativeHits` 覆盖去重(care 覆盖的排除)、gate(未登记病种空)、只登记糖尿病不算嘌呤。

## 四、状态
- ✅ `:shared:testDebugUnitTest` + `:androidApp:assembleDebug` 通过。
- ✅ 多角色验证门禁（临床营养/产品 + 架构/UX）。
- ✅ Google 代码审查门禁：**无阻断**。采纳建议1（去重两侧统一 trim 归一比对，消除隐含前提）+ 建议4（VM 全限定名改 import 短名）；建议2/3/5/6 为确认类（RECOMMEND 不纳入去重=有意、口径差异已注释、边界已覆盖）无需改。
- **真机待验**：登记糖尿病/痛风档案 + 含高GI主食/高嘌呤主料的菜，详情页正确显示补充行且不与 care 忌口重复。
- **剩余风险**：`HIGH_PURINE_KEYWORDS` 的"肝/胰"单字泛词已知风险（新增含字食材需复核）由复用匹配器继承；GI 值覆盖率决定命中率（部分菜 gi 缺则不命中，属向后兼容不误报）。

## 五、关联
三慢病维度定性提示现覆盖：**今日卡（聚合，关注成员视角）+ 菜品详情页（单菜，全家档案视角）**。见 `营养数据治理方案.md`、[[cookbook-phase1-goal]]。
