# RECOMMEND-REPRODUCIBILITY-01 · 实施蓝图

> 状态：**BLUEPRINT_READY**；R5-A；基线：`d8244693`。ARCH/SOL 冻结与复核；CODE/Luna 只按本文件实施。

## 目标与范围

以固定 fixture 锁定本地规则推荐的候选顺序、当前批切片和 fallback suggestion；仅允许补同层同分的 `dish.id` 稳定排序。不得改变权重、风格、MMR、健康阈值、DataSource、云端、schema 或 UI。

## Allowlist

- `shared/.../ai/HealthRuleEngine.kt`：只在现有 `sortedWith` 末尾加入同层同分 `dish.id` 升序 tie-break；前三个分层 key 和 score 均不得改变。
- `shared/src/androidUnitTest/.../ai/RecommendationOrchestratorTest.kt`：新增 T-RR-01~05。
- `shared/src/androidUnitTest/.../ai/HealthRuleEngineTest.kt`：仅在需要直接断言 comparator 时新增 T-RR-06。
- `F-RECOMMEND/20_实现.md`、`30_待办.md`、本文件台账、`BLUEPRINT_STATE.md`、功能完成交接：仅事实回写。

其余所有文件禁止修改，尤其 `RecommendationStyle.kt`、`RecommendationDataSource.kt`、`RecommendationPrompt.kt`、Android UI、SQLDelight。

## 不变量与步骤

1. 同一 `RecommendationInput/style/mealCount/rotation` 使用空 `MockAiRuntime()` 重复执行，候选 id 顺序和 suggestion dishIds 必须完全相同。
2. 排序层级保持“正常 → 最近 → 忌口”；仅层内同分按 id 升序，绝不跨层。
3. 25 菜、rotation 0..3 的当前 DISPLAY_BATCH/循环与不重叠行为不退化。
4. 四个 style 分别前后稳定；不要求、也不允许人为让不同 style 相同。
5. 先添加测试；仅当 T-RR-02 证明同分依赖输入顺序时，才做允许的 comparator 最小改动。旧断言若改变，停止并登记 `Q-RR-01`。

## 测试矩阵

| ID | 夹具 | 精确断言 |
|---|---|---|
| T-RR-01 | 8 菜固定输入，重复 20 次 | candidates 和首条 suggestion 均等于首轮 |
| T-RR-02 | 同层同分、乱序输入 | id 升序，且三层分组不变 |
| T-RR-03 | 25 菜，rotation 0..3 各重复 5 次 | 每批稳定，3 回到 0，批间语义不退化 |
| T-RR-04 | 四个 style | 每个 style 自身稳定 |
| T-RR-05 | 同时含 avoid/recent | 正常、最近、忌口顺序稳定 |
| T-RR-06 | 直接 Engine fixture（若需要） | tie-break 不跨层 |

必跑：`RecommendationOrchestratorTest`、`RecommendationDataSourceTest`、`:shared:testDebugUnitTest`、`:androidApp:assembleDebug`。本批无新真机项；完成后单独交接。

## 台账

| ID | 事实 | 状态 |
|---|---|---|
| RV-RR-01 | `HealthRuleEngine` 当前 comparator 只有 avoid/recent/-score，没有显式同分 id key。 | ARCH_CONFIRMED |
| C-RR-01 | 零权重/云端/schema/UI 改动，只补确定性证据和必要 tie-break。 | FROZEN |
