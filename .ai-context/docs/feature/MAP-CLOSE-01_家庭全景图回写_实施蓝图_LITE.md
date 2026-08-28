# MAP-CLOSE-01 · F-FAMILY 全景图回写实施蓝图

> 颗粒度：L3（纯文档状态锚与已解决缺陷归档；L4~L7 N/A，理由见 §0.1）。

## 0. 任务卡与角色合同

目标：将已接受的 L2 与 Bug-2119 代码事实同步到 F-FAMILY 的唯一状态锚，消除 `feature_sync_check --backlog` 的历史欠账。

非目标：不改 Android/shared 产品代码、测试、schema、seed、真机项、运行时结论或现有 L2/Bug-2119 事实。

ROLE_CONTRACT：你（ARCH=sol）冻结合同与终审；你（CODE=luna）只做 §3 的文件操作和 §4 命令；你（REVIEW=旗舰模型）只按 ID 审计 diff 与证据。遇到 `c8a60ac7` 无法作为 F-FAMILY 唯一命中提交时，追加 `Q-MAP-01-01` 并停止；不得猜测或抬高 SHA。

### §0.1 颗粒度勾销表

| GC | 状态 | 落点 |
|---|---|---|
| L1 决策与范围 | 满足 | §0、§3 allowlist |
| L2 证据 | 满足 | §2、§4 |
| L3 真相源 | 满足 | §1：STATE.yml 是同步锚；40 表只容纳活跃缺陷 |
| L4 生命周期 | N/A：不改产品对象/异步任务/资源 |
| L5 投影 | N/A：不改集合或索引 |
| L6 可见副作用 | N/A：不改用户可见行为 |
| L7 脚本勾销 | N/A：纯文档批按项目 L3 降级；每项仍有 STEP/T 可核对 |

## 1. 事实地图与不变量

| ID | While / When | Do | Must not | Evidence |
|---|---|---|---|---|
| INV-MAP-01 | 全批 | `F-FAMILY/STATE.yml:synced_to` 唯一设为 `c8a60ac7` | 设为 HEAD 或未核验 SHA | T-MAP-01 |
| INV-MAP-02 | 回写实现 | `20_实现.md` 保留 L2 与 Bug-2119 的 `ARCH_ACCEPTED / AUTOMATED_GATES_PASS`，且设备项仍待验 | 写为真机通过 | T-MAP-02 |
| INV-MAP-03 | 归档缺陷 | 从活跃 `40_缺陷.md` 移出 Bug-2119，并在 `_archive/` 保留原 ID、修复 commit 与待验状态 | 删除历史或双写同一 ID | T-MAP-03 |
| INV-MAP-04 | 收口 | 本批只改 allowlist；脚本结构和 backlog 无欠账 | 改产品代码或真机清单 | T-MAP-04~05 |

## 2. 独立挑战台账

| 挑战项 | 裁决 |
|---|---|
| 是否把 `synced_to` 提到当前 HEAD | 驳回：同步锚必须指 F-FAMILY 最后命中产品提交 `c8a60ac7`，而非无关文档 HEAD。 |
| 是否将 Bug-2119 留在活跃缺陷表等待真机 | 驳回：自动化与 ARCH 已接受；设备待验是运行时证据，不是活跃代码缺陷。归档时保留待验。 |
| 是否修改真机清单状态 | 驳回：本批无设备操作；所有现有 `PENDING_DEVICE_VERIFICATION` 保持不变。 |

## 3. 文件 allowlist 与实施步骤

```allowlist
allow:
.ai-context/docs/projectReview/features/F-FAMILY/STATE.yml | STEP-MAP-01：只替换 synced_to
.ai-context/docs/projectReview/features/F-FAMILY/20_实现.md | STEP-MAP-02：更新回写日期与证据定位
.ai-context/docs/projectReview/features/F-FAMILY/40_缺陷.md | STEP-MAP-03：移除已解决 Bug-2119 行，保留“暂无活跃缺陷”
.ai-context/docs/projectReview/features/F-FAMILY/_archive/缺陷_已解决_20260828.md | STEP-MAP-03：新增归档事实
.ai-context/docs/context_memory/SESSION_交接.md | STEP-MAP-04：记录收口命令结果
.ai-context/docs/context_memory/BLUEPRINT_STATE.md | STEP-MAP-05：仅更新本批状态/交付证据/TURN
.ai-context/docs/feature/MAP-CLOSE-01_家庭全景图回写_实施蓝图_LITE.md | 本合同、Q/AF/审查台账
forbidden:
androidApp/** | 禁止产品代码
shared/** | 禁止产品代码和测试
.ai-context/docs/真机验证/** | 禁止改真机验证状态
.ai-context/docs/projectReview/features/F-FAMILY/30_待办.md | 不改未完成待办
```

1. `STEP-MAP-01`：将 `STATE.yml` 的 `synced_to: de9f50b0` 替换为 `synced_to: c8a60ac7`，其余字段字节语义不变。
2. `STEP-MAP-02`：将 `20_实现.md` 尾部“最后更新”替换为 2026-08-28，并明确 L2=`c8a60ac7`、Bug-2119=`76a6fa41`，两者设备验证待验。
3. `STEP-MAP-03`：从 `40_缺陷.md` 移除 Bug-2119 行；新增归档表一行，注明自动化/ARCH 通过、`DEV-2119-01=PENDING_DEVICE_VERIFICATION`。
4. `STEP-MAP-04`：依次执行 §4 命令，将实际结果写入 SESSION。
5. `STEP-MAP-05`：仅在所有 T 通过后改本批为 `CODE_COMPLETE / PENDING ARCH REVIEW`、`TURN=REVIEW`；不得写 ACCEPTED。

## 4. 测试矩阵与交付

| ID | 命令/检查 | 精确通过条件 |
|---|---|---|
| T-MAP-01 | `rg -n "synced_to: c8a60ac7" .../F-FAMILY/STATE.yml` | 仅 1 个匹配 |
| T-MAP-02 | `rg -n "c8a60ac7|76a6fa41|PENDING_DEVICE_VERIFICATION" .../F-FAMILY/20_实现.md` | 三种事实均存在 |
| T-MAP-03 | `rg -n "Bug修复-2119" .../F-FAMILY` | 活跃 40 文件零匹配；archive 恰 1 匹配 |
| T-MAP-04 | `python .ai-context/tools/feature_sync_check.py --struct` | `[OK]` |
| T-MAP-05 | `python .ai-context/tools/feature_sync_check.py --backlog` | 不输出 `[BACKLOG] F-FAMILY` |
| T-MAP-06 | `git diff --check` + `blueprint_check.py --allowlist` | 无格式问题、无 allowlist 越界 |

设备：无新增 DEV；不得改动唯一清单。CODE 交付时附 `STEP → 文件:行 → T → commit` 映射，随后转旗舰 REVIEW。

## 5. Q / AF / 审查台账

| ID | 状态 | 内容 |
|---|---|---|
| Q-MAP-01-01 | OPEN | 无法唯一确认 F-FAMILY 命中提交时使用；当前未触发。 |
| AF-MAP-01-01 | CLOSED@review | 初次交付的蓝图未随实现 commit 可得，状态文件却已引用它；作为交付完整性问题，已在本次审查提交中纳入版本控制。未发现产品代码、测试、真机清单或 F-FAMILY 未完成待办越界。 |

## 6. 旗舰审查台账

| 轮次 | 结论 | 证据 | 归因与处置 |
|---|---|---|---|
| R1 | 通过 | `blueprint_check --allowlist --range 712a51b1..9e87f6ee` 全部匹配；`feature_sync_check --struct` 与 `--backlog` 均 `[OK]`；`review_freshness --md` 均 FRESH；`git diff --check` 无错误 | AF-MAP-01-01 为 `EXEC/INTEGRITY`：交付可得性遗漏。以本提交纳入蓝图，未改变产品范围；不提升颗粒度。 |
