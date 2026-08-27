# L2 · 非酒精性脂肪肝 App 入口实施蓝图

> 颗粒度：L7 · BLUEPRINT-FULL · 勾销表见 §0.1。

## 0. 任务卡与角色合同

目标：使已经 seed 的 `care_fatty_liver` 可被用户理解并以既有 category-id 链路验证；补透明参考页。非目标：不创建脂肪肝数值算法或新持久化能力。

ROLE_CONTRACT：你（CODE）只按 allowlist 实施、测试、更新台账；你（REVIEW）只按本合同审查。遇到既有 category-id 管道不能完成 T-L2-04，追加 `Q-L2-01` 并停工，禁止扩展 `HealthCondition`。

### §0.1 颗粒度勾销表

| GC | 状态 | 落点 |
|---|---|---|
| L1 决策范围 | 满足 | §0、§1、§2 |
| L2 证据 | 满足 | §5、§6 |
| L3 真相源 | 满足 | §3 |
| L4 所有权/生命周期 | 满足 | §3、INV-L2-04 |
| L5 集合投影 | 满足 | §3 category-id→member_care→并集 |
| L6 可见副作用 | 满足 | §4、INV-L2-06 |
| L7 脚本勾销 | 满足 | §5、§6、§8 |

## 1. 不变量

| ID | Owner | While/When | Do | Must not | Evidence |
|---|---|---|---|---|---|
| INV-L2-01 | CODE | 全批 | 保持 `HealthCondition`/`fromCareName` 不变 | 新增 FATTY_LIVER | diff/审查 |
| INV-L2-02 | CODE | 全批 | 只验证既有 category-id care | 改 NutritionLevel、Interpreter、AI/推荐算法 | diff/测试 |
| INV-L2-03 | CODE | 全批 | 复用现有 seed/reseed | 改 schema、迁移、seed JSON/版本 | diff/T-01~02 |
| INV-L2-04 | CODE | member 保存/回读 | 使用既有动态 crowd type、member_care 与并集 | 硬编码 Family UI 或改变合并规则 | T-03 |
| INV-L2-05 | CODE | 规则消费 | 断言既有 avoid/limit/recommend | 生成专属评级、诊断或治疗规则 | T-04 |
| INV-L2-06 | CODE | 参考页渲染 | 标“非酒精性脂肪肝”“仅供参考、非医嘱、遵医嘱” | 给数值阈值、疗效或处方承诺 | T-05 |
| INV-L2-07 | CODE | 交付 | 真机项均登记 PENDING | 声称真机 PASS | T-07 |

## 2. 冻结决策

`care_fatty_liver` 是现有 `food_category`（`crowd`）而不是新增 `HealthCondition`。Family UI 已动态读取分类，生产代码唯一改动为参考页新增一个静态 `InsetGroup`。其正文只说明 App 依现有食材级规则给出 avoid/limit/recommend 参考；来源表述限定为“具体食材规则来源请以条目为准”，不得暗示全部 18 条均由任一单一指南逐条背书；不复制任何 seed 的饮食指令或原因文本。

## 3. 数据流与唯一真相

`food_categories.json:care_fatty_liver` → `PresetDataSeeder` → `HealthProfileRepository.listAllCrowdTypes()` → Family 通用多选 → `member_care` → `FamilyRepository.allEnabledCareIds()` → 既有规则消费者。分类、成员选择与规则分别以 seed、`member_care` 和 `ingredient_care_rules` 为唯一真相；参考页不参与计算。

## 4. 文件 allowlist

```allowlist
allow:
androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/reference/HealthConditionReferenceScreen.kt | 新增一个脂肪肝透明说明 InsetGroup
shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/data/seed/PresetDataSeederTest.kt | T-L2-01~02
shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/data/repository/FamilyRepositoryTest.kt | T-L2-03
shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/ai/RecommendationDataSourceTest.kt | T-L2-04，必须从真实 `gatherConstraints()` 断言三档规则消费
.ai-context/docs/feature/L2_脂肪肝App入口_实施蓝图.md | 合同、挑战与审查台账
.ai-context/docs/context_memory/BLUEPRINT_STATE.md | 当前批握手
.ai-context/docs/context_memory/SESSION_交接.md | 最新交接
.ai-context/docs/context_memory/2026-08-27_L2脂肪肝入口_任务前快照.md | 阶段结论
.ai-context/docs/projectReview/features/F-FAMILY/20_实现.md | 已实现事实回写
.ai-context/docs/projectReview/features/F-FAMILY/30_待办.md | L2/J22 关闭回写
.ai-context/docs/projectReview/features/F-FAMILY/STATE.yml | F-FAMILY 状态源与生成视图输入
.ai-context/docs/projectReview/20_健康与算法逻辑（专属）.md | category-id边界回写
.ai-context/docs/projectReview/07_项目现状.md | 脚本生成的全项目状态视图
.ai-context/docs/projectReview/features/_INDEX.md | 脚本生成的功能目录视图
.ai-context/docs/真机验证/真机待验证清单_202608271041.md → 真机待验证清单_202608272229.md | 同一受控清单改名并只新增 PENDING 项
forbidden:
shared/src/commonMain/resources/seed/* | 既有受控数据，不改
shared/src/commonMain/sqldelight/* | 禁止schema/迁移
shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/NutritionLevel.kt | 禁止新增HealthCondition
androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/family/* | 通用动态UI已正确，禁止硬编码
androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav/* | 既有路由正确，禁止改动
```

## 5. 实施与测试矩阵

| Step/Test | 机械动作与断言 |
|---|---|
| STEP-L2-01 | 在参考页高血脂后新增一个 `InsetGroup`；固定标题为“非酒精性脂肪肝 · 食材级饮食参考”，说明只按已登记状态消费食材级规则，含免责声明与来源。 |
| T-L2-01 | 新库 seed 后存在唯一 `care_fatty_liver`、名称/维度正确；18 条规则中燕麦=RECOMMEND、大米=LIMIT、白糖和啤酒=AVOID。 |
| T-L2-02 | 同 DB 重跑 `seedIfNeeded()` 后类别/规则数不重复。 |
| T-L2-03 | 创建、更新、回读成员的 care ids；清空该成员后仅移除其 ID，其他成员/legacy 并集保留。 |
| T-L2-04 | 用真实全家 care-id 输入既有规则消费者，断言上述四种样本保持既有三档；不出现 `HealthCondition.FATTY_LIVER`。 |
| T-L2-05 | 源码守卫标题、`仅供参考`、`非医嘱` 与 `医生/遵医嘱`；禁止新增数字阈值/治疗宣称。 |
| T-L2-06 | `:shared:testDebugUnitTest`、`:androidApp:testDebugUnitTest`、`:androidApp:assembleDebug`。 |
| T-L2-07 | 真机清单新增 L2-DEV-01~04，全部 PENDING_DEVICE_VERIFICATION。 |

## 6. 验收与停机

自动项全部通过、allowlist 无越界、Feature 回写完成后才可 `SELF_CHECKED`。真机：已有安装 reseed 可见、选择/回读、多病种规则并集、参考页可读四项只登记待验。任一项无法由现有 category-id 链路证明，停机并追加 Q-L2-01。

## 7. 独立挑战台账（GC-37）

| 挑战方 | 项 | 裁决 |
|---|---|---|
| `dev-solution-architect` | `HealthCondition` 是否必须新增 | 不采纳：category-id 规则链直接消费；枚举会扩散至未冻结数值域。 |
| `dev-solution-architect` | Family UI 是否需要硬编码 | 不采纳：动态 crowd 分类已承接，硬编码将破坏可扩展性。 |
| `dev-solution-architect` | 参考文案是否越过医疗边界或夸大单一指南覆盖 | 采纳限制：只说明食材级参考与免责，提示逐条来源；不复制处方性 seed 文案，也不暗示 18 条均有同一指南背书。 |
| `dev-solution-architect` | T-L2-04 是否可用纯规则单测替代 | 不采纳：必须使用 `RecommendationDataSource.gatherConstraints()` 验证真实 category-id 消费。 |

## 8. 审查台账

| 轮次 | 结论 | AF | 归因 |
|---|---|---|---|
| R1 | 通过 | AF-L2-01~03 已关闭 | 独立架构审查确认 category-id 链路、范围和医疗边界；AF-L2-01 改为原始规则数断言，AF-L2-02 补成员→并集断言，AF-L2-03 改为逐条来源口径。 |

## 9. 验收证据

| ID | 结果 | 证据 |
|---|---|---|
| T-L2-01~04 | PASS | 新增 seed/reseed、成员回读/全家并集、真实 `RecommendationDataSource.gatherConstraints()` 测试通过。 |
| T-L2-05 | PASS | 静态守卫确认标题和“仅供参考、非医嘱、遵医嘱”；页面未新增脂肪肝数值阈值或治疗宣称。 |
| T-L2-06 | PASS | `:shared:testDebugUnitTest`、`:androidApp:testDebugUnitTest`、`:androidApp:assembleDebug` 均 `BUILD SUCCESSFUL`。非缓存重跑曾因生成型 `R.jar` 文件锁中断；停止 Gradle 守护进程后其重新执行产出无失败 XML/Debug APK，随后三命令再次成功结束。 |
| T-L2-07 | PENDING_DEVICE_VERIFICATION | `DEV-L2-01~04` 已写入唯一清单 `真机待验证清单_202608272229.md`，未声明真机通过。 |
