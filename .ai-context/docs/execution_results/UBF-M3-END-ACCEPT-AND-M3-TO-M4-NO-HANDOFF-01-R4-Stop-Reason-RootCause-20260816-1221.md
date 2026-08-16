# UBF-M3 R4 执行停止原因与根因排查（2026-08-16）

## 一、执行结果

- 时间：2026-08-16 11:55 / 12:21（Asia/Shanghai），两次执行
- 执行包：`UBF-M3-END-ACCEPT-AND-M3-TO-M4-NO-HANDOFF-01/R4`（LOW-TOKEN R4 FILE-CAPTURE RECOVERY）
- 执行结果：`STOP_UNPUBLISHED`
- 阶段：`CLAIM_PUBLICATION`
- 错误码：`COMMAND_FAILED`
- 原始错误：`git: error: unable to read sha1 file of .ai-context/docs/context_memory/BLUEPRINT_STATE.md (080e416ef861dd5bc87731aca17a58d397322b47)`
- 执行器末行 JSON：`{"status":"STOP_UNPUBLISHED","package":"UBF-M3-END-ACCEPT-AND-M3-TO-M4-NO-HANDOFF-01/R4","claim":"24d2f27a40c337c51fb27c016a020df2d3eec7fc","failure":{"code":"COMMAND_FAILED","detail":"git: error: unable to read sha1 file of .ai-context/docs/context_memory/BLUEPRINT_STATE.md (080e416ef861dd5bc87731aca17a58d397322b47)","stage":"CLAIM_PUBLICATION","trace_tail":null,"attribution":"PENDING_REMOTE_ARCH_REVIEW_NONCAPABILITY_DEFAULT"},"publication_exception":true,"turn":"CODE_UNTIL_REMOTE_ARCH_OBSERVES_REF","stop":true}`
- 影响：claim 发布未完成，本地零新提交，远程零推送（`ahead 2, behind 87` 执行前后无变化，`origin/master` 仍为 `ed235024`）

## 二、处置过程

- 第一次执行（11:55）失败后，以非破坏性 `git fetch origin` 补齐本地缺失对象 `080e416e…` 与 handoff parent `ed235024…`（本地主仓库对象库现已完好可读，`cat-file` 验证通过）。
- 12:21 恢复性重跑一次，**同一错误复现**，确认失败与本地对象库状态无关。

## 三、根因排查（只读诊断，未修补任何内容）

1. 执行器 `adapters/execute.py::clone_isolated` 使用 `git clone --filter=blob:none --no-checkout --single-branch --branch master <remote>` 做 **partial clone**（只拉 commit/tree、不拉 blob）；primary 路径几乎总成功，fallback 完整 clone 不触发。
2. `publish_claim` 在隔离 clone 中执行 `git checkout <claim_source_commit> -- .ai-context/docs/context_memory/BLUEPRINT_STATE.md`，需读取该文件的历史 blob `080e416e…`（master 历史可达，但不在 HEAD `ed235024`；HEAD 该文件为 `7058af15…`），依赖 promisor **lazy fetch** 按需抓取。
3. **gitee 不支持对历史对象的 lazy fetch**（`allowAnySHA1InWant` 未开启）：干净对照实验证明——全新 partial clone 后 checkout HEAD 可达对象成功，checkout 历史 commit（如 `da1c5d70`）的对象必失败 `unable to read sha1 file`；bash 与 python 环境结果一致，排除环境差异。
4. 结论：**执行包设计缺陷**（`--filter=blob:none` 与 gitee 历史对象 lazy fetch 不兼容），非模型能力问题、非本地仓库问题。与 `CONTROL.json` 的 `recovery_attribution: R1_R3_ARCH_PACKAGE_EXECUTOR_OR_OBSERVABILITY_DEFECT_NONCAPABILITY` 吻合。
5. 修复方向（属 R5 执行包，**未执行**）：`clone_isolated` 移除 `--filter=blob:none`（改完整 clone，或改为从本地 `source_repo` 复用对象库），claim 发布即可读取历史 blob。

## 四、处置声明

- 未手工修补执行器、未绕过执行器推送、未 force-push、未修改业务代码。
- 诊断全程只读：在主仓库仅执行 `git fetch` / `git cat-file` / `git fsck` / `git rev-list` 等只读或补对象操作；对照实验在仓库外临时目录（`~/Desktop/temp`）隔离 clone 进行，未触碰主仓库工作区。
