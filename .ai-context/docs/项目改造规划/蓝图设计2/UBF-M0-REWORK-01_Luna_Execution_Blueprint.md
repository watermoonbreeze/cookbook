# UBF-M0-REWORK-01 — Truth Pack 修订与补充采集执行蓝图

> 文档身份：Luna Mechanical Execution Blueprint  
> 状态：`READY FOR EXECUTION`  
> 制定日期：2026-08-11  
> 目标仓库：`https://github.com/watermoonbreeze/cookbook.git`  
> Execution Parent：`169bb0a70524c513fd4d2fd1cc72e06cac3ee27d`  
> 目标分支：`master`

## 0. 给 Luna 的执行方式

用户将本文件交给 Luna 后，Luna 应在全新上下文中完整读取本文件，再开始执行。

本批明确授权 Luna：

- 在当前 CookBook 项目中修订既有 Truth Pack 仓库副本；
- 在同一目录新增一份 M0 补充证据文档；
- 仅暂存本批 allowlist 文件；
- 创建一个 Git commit；
- 将该 commit 正常推送到远程 `origin/master`；
- 返回 commit hash 与规定的验证证据。

本授权不包括修改协议、GC registry、Phase 3 状态、业务代码或其他项目文件。

## 1. 任务目的

修复 commit `169bb0a7` 所提交 Truth Pack 仓库副本中的已验证缺陷，并补齐 UBF-M0 进入 M1 前缺失的项目治理证据。

本批只处理以下六类问题：

1. 补采 canonical GC registry：  
   `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md`；
2. 补采治理索引：  
   `.ai-context/docs/experience/INDEX.md`、`.ai-context/project_graph/README.md`；
3. 明确 Phase 3A、Phase 3B、`GOV-BP-P3-01` 的文件声明状态；
4. 登记 `project.yaml` 与 Phase 3A 审计/验收文件之间的状态冲突；
5. 纠正原 Truth Pack 将相同 hash 误报为“不同”的错误；
6. 将公开仓库当前版本中的用户级全文和本地绝对路径改为脱敏审计记录。

本批不对上述状态冲突作架构裁决，也不进入 UBF-M1。

## 2. 固定事实与解释边界

以下事实可以直接使用，不得重新解释：

- `b7fc77e4d442364e6f5db790b374ece4c5da409d` 是原 Truth Pack 执行采集时的 Local HEAD；
- 原 Truth Pack 中的 Observed Remote Target 也是 `b7fc77e4d442364e6f5db790b374ece4c5da409d`；
- 因此二者在采集时是相同的，不是不同的；
- `169bb0a70524c513fd4d2fd1cc72e06cac3ee27d` 是用户后来授权将 Truth Pack 仓库副本提交、推送后形成的 commit；
- 原始导出先生成于用户 `Downloads`，之后由用户另行授权复制到项目中，以便架构审核；
- `Downloads → 项目目录 → commit` 是传输来源链，不属于 Luna 原始只读采集越界；
- 原采集开始前 working tree 已经是 `DIRTY`；该既有状态必须保留，不得清理、恢复或夹带提交；
- 原 Truth Pack 的 `COMPLETE` 结论不能保持，应改为 `PARTIAL / SUPERSEDED IN PART BY UBF-M0 SUPPLEMENT`；
- 本批只记录文件事实；不得推断 CookBook Legacy L7 对应哪个 Universal Level。

## 3. 修改 allowlist

本批只允许修改两个路径。

### 3.1 既有 Truth Pack 仓库副本

先运行：

```text
git diff-tree --no-commit-id --name-only -r 169bb0a70524c513fd4d2fd1cc72e06cac3ee27d
```

预期：commit `169bb0a7` 只新增一个 Markdown 文件。将该文件的仓库相对路径记为：

```text
<TRUTH_PACK_PATH>
```

若输出不是“恰好一个 Markdown 文件”，立即 `STOP`，不得自行选择。

### 3.2 新增补充证据文档

在 `<TRUTH_PACK_PATH>` 的同一目录创建：

```text
UBF-M0-Truth-Pack-Supplement-169bb0a7.md
```

其仓库相对路径记为：

```text
<SUPPLEMENT_PATH>
```

### 3.3 唯一允许的 Git diff

最终由本任务产生的 diff 只能包含：

```text
M  <TRUTH_PACK_PATH>
A  <SUPPLEMENT_PATH>
```

如果需要修改第三个文件，必须 `STOP / Q`，不得扩大 allowlist。

## 4. 明确 denylist

禁止修改、创建、删除、移动或格式化以下对象：

- 用户级或项目级 `GLOBAL.md`；
- 用户级或项目级 `rules/blueprint_protocol.md`；
- `MODEL_ROUTING.md`；
- `PROJECT.md`；
- `BLUEPRINT_STATE.md`；
- `project.yaml`；
- `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md`；
- `.ai-context/docs/experience/INDEX.md`；
- `.ai-context/project_graph/README.md`；
- 所有 Phase 3 control、audit、blueprint、acceptance、handoff 文件；
- `PHASE2E_VIEW_DRIFT.md`；
- 任何 GC registry、experience、index 或 generated view；
- 任何源代码、测试、构建文件、配置文件；
- 本批 allowlist 之外的任何文件。

禁止执行：

- `checkout`、`reset`、`clean`、`stash`；
- `pull`、`merge`、`rebase`；
- force push；
- amend 或重写已有 commit；
- 删除或改写 Git 历史；
- 启动或继续 Phase 3；
- 修复发现的治理冲突；
- 调整 Universal Level、FULL/LITE 或 GC 语义。

## 5. Preflight

在写文件前执行并保存原始结果：

```text
git rev-parse --show-toplevel
git rev-parse HEAD
git branch --show-current
git remote get-url origin
git status --short --untracked-files=all
git diff --cached --name-status
git ls-remote origin refs/heads/master
git show --format=fuller --stat --name-status 169bb0a70524c513fd4d2fd1cc72e06cac3ee27d
git diff-tree --no-commit-id --name-only -r 169bb0a70524c513fd4d2fd1cc72e06cac3ee27d
```

必须同时满足：

- 当前仓库根目录属于 CookBook；
- `origin` 指向 `watermoonbreeze/cookbook.git`；
- 当前分支是 `master`；
- Local HEAD 等于 Execution Parent `169bb0a70524c513fd4d2fd1cc72e06cac3ee27d`；
- Remote `refs/heads/master` 等于同一 Execution Parent；
- index 中不存在任何预先 staged 的变更；
- `169bb0a7` 的文件列表能唯一解析出 `<TRUTH_PACK_PATH>`。
- `<TRUTH_PACK_PATH>` 在 preflight 没有既有 unstaged 修改；
- `<SUPPLEMENT_PATH>` 在 preflight 尚不存在，也没有同名未跟踪文件。

任一项不满足时：

- `STOP`；
- 原样回报差异；
- 不得 pull、checkout、reset、stash、merge、rebase 或修复。

working tree 中既有的 unstaged 或 untracked 项目不构成自动 STOP，但必须：

- 完整记录 preflight 状态；
- 不读取无关未跟踪文件内容；
- 不修改或删除它们；
- 不将它们加入本批 commit。

## 6. 只读证据输入

### 6.1 必须完整采集的项目级文件

下列三个固定路径必须存在；对每个文件记录仓库相对路径、SHA-256、行数、自述 Truth Role 和完整原文：

```text
.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md
.ai-context/docs/experience/INDEX.md
.ai-context/project_graph/README.md
```

如果任一文件不存在或不可读，标记 `STOP / REQUIRED TRUTH ABSENT`，不创建 commit。

### 6.2 Phase 与协议状态证据

只读定位所有以下 basename；存在多个候选时全部登记，不自行裁决：

```text
project.yaml
BLUEPRINT_STATE.md
PHASE3A_AUDIT.md
PHASE3A_BLUEPRINT.md
PHASE3_ARCHITECTURE_ACCEPT.md
```

另外只读定位：

- 明确声明 Phase 3 control/status 的文件；
- 明确声明 Phase 3B authorization/status 的文件；
- 明确声明 `GOV-BP-P3-01` 状态的文件；
- 当前用户级 `rules/blueprint_protocol.md`。

允许使用 `rg --files`、`rg -n`、hash 和行数统计命令。不得修改任何输入文件。

### 6.3 用户级输入的公开边界

用户级 `GLOBAL.md`、`blueprint_protocol.md`、`BLUEPRINT_STATE.md` 等材料在本批只允许记录：

- 脱敏路径；
- Scope；
- SHA-256；
- 行数；
- 文件自述身份/Truth Role；
- 与 Phase 3 或 `GOV-BP-P3-01` 状态直接相关的最小原文摘录和行号。

不得再次把用户级文件全文写入项目或 supplement。

## 7. 修订 `<TRUTH_PACK_PATH>`

只执行下列机械修订，不重排其他项目证据，不用新的总结覆盖原始命令输出。

### 7.1 增加仓库副本说明

在文档开头 metadata 后加入醒目标注，必须表达：

```text
Repository Transport Copy

该文件最初作为 UBF-M0-EXPORT-01 的仓库外 Truth Pack 生成于用户 Downloads。
用户随后另行授权将其复制到 CookBook 项目并以 commit 169bb0a7 推送，供架构审核。
当前仓库版本是经过脱敏的传输副本，不是新的采集运行。
```

不要写入真实用户主目录或原始绝对路径。

### 7.2 修正采集状态

将该文档的总体采集结论明确改为：

```text
PARTIAL / SUPERSEDED IN PART BY UBF-M0-Truth-Pack-Supplement-169bb0a7.md
```

并注明原因仅包括：

- 漏采 canonical GC registry；
- 漏采两个治理索引；
- Phase 3A、Phase 3B、`GOV-BP-P3-01` 状态没有明确展开；
- 未登记 `project.yaml` 与后续 Phase 3A 文件的状态冲突；
- Local HEAD comparison 有一处事实错误。

### 7.3 修正 hash 比较错误

找到所有将采集时 Local HEAD 与 Observed Remote Target 描述为不同、不一致或 mismatch 的句子，改为：

```text
At capture time, Local HEAD and Observed Remote Target were identical:
b7fc77e4d442364e6f5db790b374ece4c5da409d.
```

不得把后来产生的 `169bb0a7` 倒填为原采集时 HEAD。

### 7.4 脱敏绝对路径

在仓库当前版本中：

- CookBook 仓库根绝对路径统一替换为 `<COOKBOOK_REPO>`；
- 用户主目录前缀统一替换为 `<USER_HOME>`；
- 原始 Downloads 路径统一表达为 `<USER_HOME>/Downloads/<filename>`；
- 仓库内路径保留仓库相对路径；
- 不改变文件 hash、行数与 Git 命令输出所表达的实质事实；
- 不新增用户名、设备名或其他本地标识。

### 7.5 移除用户级文件全文

对 `Complete File Contents` 中 Scope 为 `USER-LEVEL` 的每个全文代码块：

- 保留材料名称、脱敏路径、Scope、Truth Role、SHA-256、行数；
- 将全文代码块替换成：

```text
CONTENT OMITTED FROM REPOSITORY TRANSPORT COPY
Reason: user-level governance content is outside the CookBook project publication boundary.
Integrity evidence is preserved by SHA-256 and line count.
```

只移除用户级全文。不得删除项目级文件的既有内容或证据。

### 7.6 增加勘误指针

在原文档末尾加入 `Errata and Supplement` 小节，指向：

```text
UBF-M0-Truth-Pack-Supplement-169bb0a7.md
```

并说明补充文档拥有“补缺和勘误证据”角色，但不覆盖原始采集命令输出，也不裁决 canonical 状态冲突。

## 8. 创建 `<SUPPLEMENT_PATH>`

新增文档必须严格采用以下结构。

### 8.1 Header

```text
# UBF-M0 Truth Pack Supplement — 169bb0a7

Document Role: Supplemental Evidence and Errata
Status: COMPLETE 或 PARTIAL（必须由实际结果决定）
Execution Parent: 169bb0a70524c513fd4d2fd1cc72e06cac3ee27d
Original Capture HEAD: b7fc77e4d442364e6f5db790b374ece4c5da409d
Task ID: UBF-M0-REWORK-01
```

### 8.2 A. Provenance and Scope

记录：

- 原 Truth Pack 的仓库相对路径和 SHA-256（修订前与修订后分别记录）；
- 原始采集、Downloads 导出、用户授权复制、commit `169bb0a7` 的先后关系；
- 本补充文档的 allowlist、denylist；
- 本文仅补缺与勘误，不改变输入文件；
- 所有路径均使用仓库相对路径或 `<USER_HOME>`、`<COOKBOOK_REPO>` 占位符。

### 8.3 B. Corrected Observation Register

使用表格逐项记录：

| ID | Original Observation | Corrected Fact | Evidence | Effect |
|---|---|---|---|---|

至少包含：

- `ERR-01`：采集时 Local HEAD 与 Observed Remote Target 实际相同；
- `ERR-02`：原 `COMPLETE` 应为 `PARTIAL`；
- `ERR-03`：三个必需项目治理文件漏采；
- `ERR-04`：Phase 3A、Phase 3B、`GOV-BP-P3-01` 状态未明确展开；
- `ERR-05`：`project.yaml` 与后续 Phase 3A 文件存在状态声明冲突；
- `NOTE-01`：Downloads 与仓库路径差异是用户授权后的传输链，不是原采集越界；
- `NOTE-02`：原工作区在采集前已为 DIRTY，本任务不得把既有状态归因于自己。

每一项必须引用实际文件、commit 或命令证据；没有证据时标 `UNKNOWN`，不得推断。

### 8.4 C. Canonical Governance Inventory Supplement

对以下三个文件逐一建立元数据表：

```text
.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md
.ai-context/docs/experience/INDEX.md
.ai-context/project_graph/README.md
```

字段必须包含：

- Requested Material；
- Repository-relative Path；
- Scope；
- Truth Role（只引用文件自述）；
- Status；
- SHA-256；
- Line Count；
- Notes。

### 8.5 D. Complete Project-level File Contents

完整收录上述三个项目级文件的原文：

- 保持字节解码后的原始顺序；
- 不摘要；
- 不改写；
- 不修复；
- 每份原文前重复记录 path、SHA-256、line count；
- 放入适当语言标记的代码块。

如果文件中发现疑似凭据或秘密值：

- 不写入具体值；
- 在对应位置写 `SENSITIVE_CONTENT_DETECTED / VALUE OMITTED`；
- 记录文件和行号；
- 整批标记 `STOP / SENSITIVE CONTENT`；
- 不 commit、不 push。

### 8.6 E. Phase and Protocol State Evidence

建立一张状态证据表，每个“来源文件中的声明”单独一行：

| Subject | Source Path | Source Role | Exact Status Text | Line(s) | SHA-256 | Observation |
|---|---|---|---|---|---|---|

必须覆盖：

- Phase 3 总状态；
- Phase 3A 当前状态；
- Phase 3B 当前状态或授权情况；
- `GOV-BP-P3-01` 当前状态；
- 当前 `blueprint_protocol.md` 的脱敏路径、hash、自述身份；
- 哪些文件自述为 canonical truth；
- 哪些内容属于 lifecycle state；
- 哪些属于 acceptance snapshot；
- 哪些属于 generated view；
- 无法由文件确认的内容写 `UNKNOWN`。

`Exact Status Text` 只摘录证明状态所需的最小原文，不复制用户级文件全文。

### 8.7 F. Conflict and Absence Register

必须显式登记：

- `project.yaml` 声明 Phase 3 `AUTHORIZED / NOT STARTED`，而后续 Phase 3A 审计/验收材料声明 Phase 3A 已执行或待独立审核；
- 对冲突两侧分别给出文件路径、hash、行号、最小原文；
- 标记为 `UNRESOLVED AUTHORITY / LIFECYCLE CONFLICT`；
- 不决定哪一方应被修改；
- 不更新任何状态文件；
- Phase 3B 与 `GOV-BP-P3-01` 若存在多来源差异，同样逐项登记；
- 所有 ABSENT、多候选、无法读取材料。

### 8.8 G. Working Tree Preservation Evidence

记录：

- 本批 preflight HEAD；
- preflight staged 状态；
- preflight unstaged/untracked 状态；
- allowlist 文件写入后的状态；
- 除 `<TRUTH_PACK_PATH>` 与 `<SUPPLEMENT_PATH>` 外，原有工作区状态是否逐项保持；
- 本任务是否修改、删除、暂存任何既有 dirty 项。

禁止把工作区“变干净”作为验收要求。验收要求是：无关状态保持不变且不进入 commit。

### 8.9 H. Completion Assessment

只有在以下条件全部满足时，Supplement 才可写 `COMPLETE`：

- 三个缺失项目治理文件全部收录；
- Phase 3A、Phase 3B、`GOV-BP-P3-01` 均有明确证据或明确 `UNKNOWN/ABSENT`；
- 冲突 register 完整；
- hash 比较错误已纠正；
- 用户级全文未进入 supplement；
- 没有发现敏感值；
- 无关 working tree 状态保持不变。

否则写 `PARTIAL` 并列出全部 STOP/Q；不得为了得到 `COMPLETE` 而猜测或修改输入。

### 8.10 I. Integrity

记录：

- `<TRUTH_PACK_PATH>` 修订后 SHA-256 和行数；
- `<SUPPLEMENT_PATH>` 文件名、仓库相对路径、文件大小、行数；
- supplement 自身 hash 字段写 `SELF_SHA256_REPORTED_EXTERNALLY`；
- 本批预期提交文件清单；
- 未解决 Q/STOP。

准确 supplement SHA-256 只在最终聊天回报中提供，避免自指 hash 变化。

## 9. 内容与隐私检查

在暂存前对两个 allowlist 文件执行检查：

- Markdown fence 是否成对闭合；
- 是否仍包含真实用户主目录、用户名或 Downloads 绝对路径；
- 是否含用户级 `GLOBAL.md` 或 `blueprint_protocol.md` 全文；
- 是否出现 API key、access token、private key、cookie、credential 等疑似秘密；
- `git diff --check -- <TRUTH_PACK_PATH> <SUPPLEMENT_PATH>`；
- 读取修订后的关键段落，确认没有把 `169bb0a7` 错写为原采集 HEAD；
- 确认 `b7fc77e4` 与 `b7fc77e4` 比较结果为 identical；
- 确认 supplement 对三个固定项目文件均有 metadata 和完整原文。

若发现疑似敏感内容：

- `STOP`；
- 不暂存、不 commit、不 push；
- 只回报文件和行号，不输出敏感值。

## 10. Diff 隔离验证

写入完成后，先保存：

```text
git status --short --untracked-files=all
git diff --name-status
git diff --stat
git diff --check
```

将此结果与 preflight 比较。必须证明：

- 本任务新增的变化只有 `<TRUTH_PACK_PATH>` 和 `<SUPPLEMENT_PATH>`；
- preflight 已存在的其他 unstaged/untracked 项仍保持原状态；
- 没有删除、恢复、格式化或改写其他文件。

如果无法区分“本任务变化”与“原有 dirty 状态”，立即 `STOP / WORKTREE ATTRIBUTION UNCLEAR`，不得提交。

## 11. 暂存、提交与推送

只有前述检查全部通过且 Supplement 结果为 `COMPLETE` 时，才允许继续。

### 11.1 暂存

只暂存两个明确路径：

```text
git add -- <TRUTH_PACK_PATH> <SUPPLEMENT_PATH>
git diff --cached --name-status
git diff --cached --stat
git diff --cached --check
```

`git diff --cached --name-status` 必须恰好是：

```text
M  <TRUTH_PACK_PATH>
A  <SUPPLEMENT_PATH>
```

若出现其他文件：

- 立即取消本批两个路径的暂存；
- 不触碰原有文件；
- `STOP / STAGING SCOPE VIOLATION`；
- 不 commit、不 push。

### 11.2 Commit

提交信息固定为：

```text
docs(governance): complete UBF M0 truth evidence
```

创建 commit 后验证：

```text
git show --format=fuller --stat --name-status HEAD
git diff-tree --no-commit-id --name-status -r HEAD
git diff-tree --check HEAD^ HEAD
```

新 commit 必须：

- 父提交为 `169bb0a70524c513fd4d2fd1cc72e06cac3ee27d`；
- 只包含两个 allowlist 文件；
- 不包含任何 preflight 既有 dirty 项。

### 11.3 Push

允许执行一次正常 fast-forward push：

```text
git push origin HEAD:master
```

禁止 force push。

如果 push 因远端前进、权限、网络或任何原因失败：

- `STOP / PUSH FAILED`；
- 保留本地 commit；
- 不 pull、不 rebase、不 merge、不重试扩大权限；
- 原样回报失败信息和本地 commit hash，等待用户决定。

push 成功后只读验证：

```text
git rev-parse HEAD
git ls-remote origin refs/heads/master
git status --short --untracked-files=all
```

Local HEAD 与 Remote `master` 必须等于新 commit。

## 12. 验收条件

本批只有同时满足以下条件才可回报 `COMPLETE`：

- Execution Parent 与远程 parent 均为 `169bb0a7...`；
- 既有 Truth Pack 当前分支版本已完成状态纠正、来源说明和脱敏；
- supplement 完整补齐三个项目治理文件；
- Phase 3A、Phase 3B、`GOV-BP-P3-01` 状态有逐来源证据；
- `project.yaml` 与 Phase 3A 文件冲突已登记但未裁决；
- 用户级全文和真实本地绝对路径未出现在两个目标文件中；
- 本批 commit 仅包含两个 allowlist 文件；
- 原有 dirty working tree 未被修改、清理或夹带；
- commit 创建成功；
- push 成功；
- 远程 `master` 指向新 commit。

本批 `COMPLETE` 只表示 `UBF-M0-REWORK-01` 执行完成，不等于 UBF-M0 已被架构审核 `ACCEPT`，也不授权 M1。

## 13. STOP / Q 条件

出现以下任一情况必须停止：

- Local HEAD 或 Remote `master` 不是 Execution Parent；
- index 在 preflight 已有 staged 变更；
- 无法唯一确定原 Truth Pack 路径；
- 原 Truth Pack 在本批开始前已有未提交修改，或 supplement 目标路径已存在；
- 三个固定项目治理文件任一缺失；
- 输入文件含疑似敏感值；
- 无法保护原有 dirty working tree；
- 需要修改 allowlist 外文件；
- 无法基于文件原文确认状态且模板要求被迫猜测；
- staged diff 包含其他文件；
- commit 父提交不正确；
- push 失败或远端发生前进。

STOP 后不得自行修复、回滚用户改动或扩大范围。

## 14. Luna 最终回报格式

完成后只回报：

```text
Task: UBF-M0-REWORK-01
Result: COMPLETE | STOPPED | PARTIAL
Execution Parent: 169bb0a70524c513fd4d2fd1cc72e06cac3ee27d
Preflight Local HEAD:
Preflight Remote master:
Preflight Branch:
Preflight staged status:
Preflight working tree status:

Truth Pack path:
Truth Pack SHA-256 before:
Truth Pack SHA-256 after:
Truth Pack line count after:

Supplement path:
Supplement SHA-256:
Supplement line count:
Supplement result: COMPLETE | PARTIAL

Required governance files captured: 3/3 | other
Phase 3A evidence: FOUND | ABSENT | MULTIPLE
Phase 3B evidence: FOUND | ABSENT | MULTIPLE
GOV-BP-P3-01 evidence: FOUND | ABSENT | MULTIPLE
Conflict entries:
Sensitive content detected: YES | NO
Real local absolute paths remaining in target files: YES | NO
User-level full contents remaining in target files: YES | NO

Commit created: YES | NO
Commit hash:
Commit parent:
Committed file list:
Push performed: YES | NO
Push result:
Remote master after push:

Post-task working tree status:
Unrelated pre-existing changes preserved: YES | NO | UNCLEAR
Repository files outside allowlist changed by this task: YES | NO
Phase/protocol/GC/source files modified: YES | NO
STOP/Q items:
```

不要在聊天中粘贴两份长文档全文。用户只需将 commit hash 发给架构审核方。

完成后等待审核，不启动 M1，不修改 `rules/blueprint_protocol.md`。
