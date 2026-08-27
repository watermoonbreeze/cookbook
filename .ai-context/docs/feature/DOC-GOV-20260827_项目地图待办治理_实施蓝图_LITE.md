# DOC-GOV-20260827 · 项目地图与待办治理实施蓝图

> 颗粒度：L3（纯文档/生成视图批次；无产品代码、运行时或数据变更）·勾销表见 §0.1。

## 0. 任务卡与角色合同

目标：以代码、Feature Project Truth 与生成脚本为唯一事实源，清除横轴文档过期锚点、生成视图配置错误和 Feature 历史回写欠账。

非目标：不实现任何产品待办；不改产品代码、schema、依赖、真机项、运行时状态、蓝图协议或历史提交。

ROLE_CONTRACT：你（ARCH）只冻结此纯文档合同并复核证据；你（CODE）仅修改 allowlist 内文档/状态锚并运行指定脚本；你（REVIEW）仅按本蓝图核对 diff 与命令输出。任何无法从代码/现有 Project Truth 唯一确定的描述都记录 `Q-DOCGOV-01`，不自行补写。

### §0.1 颗粒度勾销表

| GC | 状态 | 落点 |
|---|---|---|
| L1 决策与范围闭合 | 满足 | 本节任务卡、非目标、ROLE_CONTRACT |
| L2 证据闭合 | 满足 | §4 命令与验收矩阵 |
| L3 真相源闭合 | 满足 | §1 来源与 §2 不变量 |
| L4+ 所有权/索引/副作用/脚本勾销 | N/A：本批不改产品类型、数据流、运行时状态或用户可见副作用 | 纯文档与既有脚本生成视图 |

## 1. 事实来源

- Project Truth：`.ai-context/docs/projectReview/features/<F-ID>/STATE.yml` 与同目录 `20_实现.md`。
- Runtime/产品事实：对应已提交代码；不得用 SESSION 或待办替代。
- 生成视图：`.ai-context/tools/feature_sync_check.py --emit-index --write` 唯一生成 `07_项目现状.md`、`features/_INDEX.md` 与 `功能路径索引.md` 的生成段。
- 横轴锚点：`01/03/04/20/21` 页脚及正文，按已提交代码/Project Truth 逐项刷新；只在已核验的事实范围内清除 STALE。

## 2. 不变量

| ID | While/When | Do | Must not | Evidence |
|---|---|---|---|---|
| INV-DOCGOV-01 | 任何生成视图刷新时 | 仅运行 `--emit-index --write` 生成段 | 手工修改 GENERATED 段 | T-01、git diff |
| INV-DOCGOV-02 | 更新 Feature 回写时 | 同步 `STATE.yml:synced_to` 与受影响的 `20_实现.md`，内容可回查提交 | 只抬 SHA 或凭待办猜测实现 | T-02、`--backlog` |
| INV-DOCGOV-03 | 更新横轴时 | 保留未核验范围的 STALE/限制描述 | 把文档或静态检查写成真机 PASS | T-03、diff 审核 |
| INV-DOCGOV-04 | 全批 | 仅改 allowlist | 改产品代码、真机清单状态、蓝图协议 | T-04、allowlist diff |

## 3. 实施脚本

```allowlist
allow:
.ai-context/docs/projectReview/01_架构与技术底座.md | 基于已核验代码刷新陈旧事实锚
.ai-context/docs/projectReview/03_界面与交互.md | 基于已核验代码刷新陈旧事实锚
.ai-context/docs/projectReview/04_数据层.md | 基于已核验 schema/迁移刷新陈旧事实锚
.ai-context/docs/projectReview/20_健康与算法逻辑（专属）.md | 基于已核验代码刷新陈旧事实锚
.ai-context/docs/projectReview/21_AI与网络请求策略（专属）.md | 基于已核验代码刷新陈旧事实锚
.ai-context/docs/projectReview/features/*/20_实现.md | 回写已发生的受影响代码落点
.ai-context/docs/projectReview/features/*/STATE.yml | 将已回写功能同步锚定到当前 HEAD
.ai-context/docs/projectReview/07_项目现状.md | 仅由生成脚本写入生成段
.ai-context/docs/projectReview/features/_INDEX.md | 仅由生成脚本写入生成段
.ai-context/docs/projectReview/功能路径索引.md | 仅由生成脚本写入生成段
.ai-context/docs/context_memory/2026-08-27_待办分类与文档治理.md | 记录本批结论
.ai-context/docs/context_memory/SESSION_交接.md | 覆盖式维护下一会话启动卡
.ai-context/docs/context_memory/BLUEPRINT_STATE.md | 更新本批握手状态
.ai-context/docs/feature/DOC-GOV-20260827_项目地图待办治理_实施蓝图_LITE.md | 冻结本批合同与审核返工记录
.ai-context/tools/review_freshness.py | 仅将 D-25 指定的 Tier B 册纳入页脚检查，排除生成视图
.ai-context/tools/feature_sync_check.py | C6 同时校验三个生成视图与当前 Feature Truth 同步
forbidden:
shared/** | 禁止改产品代码
androidApp/** | 禁止改产品代码
.ai-context/docs/真机验证/** | 禁止改变真机验证状态
.ai-context/rules/** | 禁止改用户级/项目协议
```

顺序：先修复生成视图被页脚检查误判的配置冲突；再审计每个 backlog 功能对应提交与 `20_实现.md`，再更新 STATE；随后刷新横轴；最后运行生成器与结构/回写检查。遇到无法唯一核验的横轴陈旧项，保留 STALE 并写 `Q-DOCGOV-01`。

### Q-DOCGOV-01（已闭合）

定位：`review_freshness.py` 无差别扫描根目录 Markdown，导致 D-25 规定的生成视图 `07_项目现状.md` 与功能路径索引被要求具有 Tier B 页脚。

唯一修复：定义显式 `TIER_B_VOLUMES = {01,03,04,20,21,22}`，主循环仅检查此集合；`07` 继续仅由 `feature_sync_check.py --emit-index --write` 与 C6 保障，功能路径索引继续由 C6 保障。禁止向生成物添加手工页脚。

## 4. 测试矩阵与交付

| ID | 操作 | 预期 |
|---|---|---|
| T-01 | `python .ai-context/tools/feature_sync_check.py --emit-index --write` | 成功，生成段由脚本写入 |
| T-02 | `python .ai-context/tools/feature_sync_check.py --backlog` | 仅允许无历史回写欠账；若非零，逐项回到 §3 |
| T-03 | `python .ai-context/tools/feature_sync_check.py --struct` | 通过 |
| T-04 | `git diff --name-only` | 仅命中 allowlist |

交付台账：报告更新的事实锚、仍保留的陈旧项、每条命令结果；不声称真机通过。
