# Universal Blueprint Framework Architecture Review

## 0. 评估范围与结论

本评估以 `执行部分-9.md` 的第一性目标为输入，审查 Blueprint、Granularity、coder 能力画像、项目 Overlay 与现有协议的关系。

本批只做理论和架构评估：

- 不修改用户级 `blueprint_protocol`；
- 不新增 GC；
- 不建立独立的 Granularity 评分体系；
- 不预设必须扩展到 L8/L10；
- 不迁移或删除现有 CookBook L7、GC-01~GC-48。

结论：现有体系的主要方向正确，但 `Level` 目前同时承担“委托细度”“风险治理”“验收闭环”三类职责，导致跨项目不可比、文档膨胀和 coder 能力被误判。建议保留 `FULL/LITE`、单调 Level、GC 治理和历史兼容边界，先把它们重新归位，再设计兼容迁移。

## 1. Blueprint 的第一性定义

Blueprint 不是详细需求文档，也不是检查清单。它是 Sol 将任务中需要由 coder 自行判断的决策空间显式闭合后，交给 coder 执行的委托接口。

```text
原始任务
  → Sol 完成问题解释、边界判断和必要决策
  → Blueprint 固化可验证的委托合同
  → coder 在剩余决策空间内实现
  → 通过证据验证交付
```

因此 Blueprint 的最小闭环是：目标、范围、不可变约束、责任边界、可观察结果、验证证据。文档长度、章节数量和 GC 数量都不是 Blueprint 本体。

## 2. Granularity Level 应衡量什么

Granularity 应衡量 coder 在不获得额外架构协助时仍需自行解决的决策空间，而不是文字数量。

```text
Level 越低 → coder 剩余决策空间越大
Level 越高 → Sol 预先闭合的决策越多
```

可比较的 Level 必须描述“闭合了哪一类决策”，例如目标与边界、架构职责、数据/状态流、文件与符号、错误与并发、测试与证据。它不应描述某个项目的具体目录或某批 GC。

Level 的单调性可以保留，但应允许任务只声明实际需要的闭合层级；不能因为 FULL、风险或治理要求存在，就机械追加与委托无关的细节。

## 3. 现有三轴的保留与归位

| 现有概念 | 保留 | 应明确的职责 |
|---|---|---|
| `FULL/LITE` | 保留 | 工件集合与治理覆盖范围，不表示委托细度 |
| `L1~LN` | 保留为统一阶梯 | 只表示剩余决策空间的闭合程度 |
| `GC-01~GC-48` | 保留历史兼容 | 治理约束、审计和经验，不改变 Level 语义 |
| `Q/AF` | 保留 | 缺口与阻断反馈，不作为 Level 分数 |
| Self-Application | 保留 | 检查 Blueprint 是否遵守自己的边界，不增加新的颗粒度轴 |

推荐的任务描述形式是：

```text
Blueprint mode = FULL or LITE
Required closure = 某个统一 Level
Task profile = 任务类型与风险条件
Project overlay = 项目特殊约束
Governance = 适用的 GC / Q / AF
```

## 4. Global Core + Project Overlay

Global Core 只定义跨项目可比较的语义：每一级闭合什么决策、最低需要什么证据、何时必须停止并升级。Project Overlay 只定义本项目的真相源、构建命令、目录约束、平台风险和历史兼容规则。

Overlay 不得重新定义“L5 在本项目是什么意思”，只能补充“本项目中 L5 的通用闭合要求如何落地”。例如，KMP 的 shared/androidApp 边界是项目约束；它不是第二套 Level。

判断规则：若一条规则可以脱离 Cookbook 仍然成立，它候选进入 Global Core；若规则依赖本项目文件、工具或业务状态，则留在 Overlay；若只是一次任务的选择，则留在 Task profile。

## 5. 项目经验晋升机制

现有 GC 和经验文档不应直接改写为新的 Level。项目经验只有在满足以下条件后，才可提出 Global Core 候选：

1. 同类失败在至少两个独立任务或项目中重复出现；
2. 能抽象为与具体目录、模型和工具无关的决策缺口；
3. 能写成可执行的边界或证据要求；
4. 不与现有 Level、GC 或真相源冲突；
5. 经过独立审查后，才进入后续协议演进批次。

本评估不新增 GC，也不把现有 GC 自动晋升为 Universal Level。

## 6. Coder Capability Profile

coder 能力不应压缩成一个永久数字。建议按任务类型记录最低可靠闭合层级，并同时记录证据质量：

```text
coder capability:
  task_type: bugfix / feature / refactor / migration / concurrency / test
  observed_minimum_level: 任务在该 Level 下稳定完成的最低值
  sample_count: 有效样本数
  rework_count: 返工次数
  scope_violation_count: 越界次数
  evidence_completeness: 证据完整度
```

单次成功不能升级画像。至少需要同类任务的重复样本，并区分“coder 能力不足”“Blueprint 缺口”“环境失败”和“需求变化”。

## 7. 同一任务的能力测试

能力测试应使用同一任务的等价 Blueprint 变体：只改变预先闭合的决策范围，不改变目标、验收标准和输入数据。比较指标包括：

- 首次通过率；
- 返工次数；
- 越界或擅自决策次数；
- Q/AF 数量及归因；
- 测试和证据完整度；
- 完成时间与额外澄清次数。

若低 Level 失败、高 Level 成功，只能说明该 coder 在该任务类型上的最低可靠闭合层级尚未确定；还必须排除 Blueprint 缺口和环境因素。

## 8. 最低安全 Level 的选择

不建立第二套评分系统。Level 选择应是已有统一 Level 阶梯上的取最大值：

```text
required level = max(
  coder/task-type minimum,
  task risk floor,
  project governance floor
)
```

这里的三个输入不是三个新等级体系：

- coder/task-type minimum：能力画像中的历史观察值；
- task risk floor：任务本身对闭合程度的最低要求；
- project governance floor：项目既有规则规定的最低门槛。

如果输入无法由证据确定，默认选择更保守的既有 Level，并记录不确定性，而不是临时创造新分数。

## 9. 如何避免高 Level 退化成清单堆叠

每增加一层，必须回答“减少了 coder 哪类决策”。如果只是增加格式、审计栏或重复说明，而没有减少实际决策空间，就不应提升 Level。

Blueprint 应以最小充分闭合为目标：

```text
足够详细 = coder 能在边界内独立完成并给出证据
过度设计 = Sol 已替 coder 完成实现选择，coder 只剩抄写
```

高 Level 允许接近机械执行，但不应成为默认选项；只有任务风险、coder 画像或重复失败证据支持时才使用。

## 10. 对当前用户级协议的 Gap Analysis

### 已经符合第一性目标的部分

- 区分 `FULL/LITE` 与 Level；
- 强调蓝图缺口不能由 coder 自行发挥；
- 保留 Q/AF、审计和 Self-Application 反馈闭环；
- 要求测试、证据和真相源；
- 支持项目级特殊规则和历史 GC。

### 需要后续重审的结构性偏差

1. Level 语义与项目治理条款耦合，跨项目可比性不足；
2. FULL/LITE、Level、GC、审计字段可能重复表达同一约束；
3. 当前规则更擅长规定“蓝图必须包含什么”，但较少规定“替 coder 消除了哪类决策”；
4. coder 能力、任务风险和项目最低门槛尚未形成清晰的输入关系；
5. Q/AF 反馈已有闭环，但尚未稳定区分 coder 失败、蓝图缺陷和环境失败。

这些是后续重构候选，不是本批对用户级协议的修改。

## 11. 兼容迁移建议

迁移必须分阶段进行：

1. 冻结现有 L7 与 GC-01~GC-48 的历史含义，建立映射说明；
2. 在不改协议的前提下，为新任务增加“委托闭合说明”字段的试运行样例；
3. 用真实任务验证哪些 Level 描述具有跨项目稳定性；
4. 只把重复、可泛化的缺口提交为后续 Global Core 变更候选；
5. 最后才讨论协议字段或用户级规则迁移。

任何迁移批次都必须保持旧蓝图可审计、旧 GC 可反查、旧项目文档可读取。不得通过批量改名、删除历史记录或重写既有 GC 来制造“迁移完成”。

## 12. 决策与后续入口

本评估支持继续研究 Universal Blueprint Framework，但不支持现在直接重构用户级协议。下一批若获得明确授权，应先选择少量真实任务做对照试验，验证“Level = 剩余决策空间闭合程度”是否比现有描述更能预测 coder 的可靠性，再决定是否进入协议迁移设计。

本文件不改变运行时代码、数据库、测试 fixture、用户级规则或项目当前治理状态。
