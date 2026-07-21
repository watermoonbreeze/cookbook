# 新 session 待办补充（用户诉求·本 session 工具异常故单独记此文件）

> [AI生成] 2026-07-21 深夜。本 session 后期工具层损坏（Bash 虚假回显、Read 重复行、ToolSearch 洪泛），
> 无法可靠 commit/构建/跑数据。本文件记用户明确的后续诉求，新 session 恢复后按此推进。主交接见 `SESSION_交接.md`。

## 一、恢复顺序（新 session 先做·详见 SESSION_交接.md）
先核实工作区真实改动 → 真构建验证营养展示 → 核实/重跑第一批数据合并 → 分批 commit+push（本 session 一个都没真提交·HEAD 仍在 `eab92b1`）→ 重建 nlc 库 → 再推进下面的待办。

## 二、脚本方案后续待办（按顺序·无人值守）
1. **落实脚本化数据方案**（`.ai-context/rules/数据获取维护规则.md`）：nlc 建库+匹配脚本纳入 `scripts/data/`、补 USDA(satFat)/GI/嘌呤(purine) CSV 脚本。
2. **第二批数据补缺**：155 条(gi46/satFat82/purine56)·分片重生成 `temp/claude/gen_fill_shards.py`·**改脚本方式**(USDA API + 本地 CSV·别再逐条丢 agent)。
3. **🌟全数据源脚本交叉核对（用户重点提议）**：脚本方案落实后，用脚本把**所有数据源对现有全量食材交叉跑一遍**（零 token·全量非抽样），很可能有新发现：
   - 数值录错（现有 vs nlc/USDA 权威 >10% 差异全量标红）
   - 口径不一致（干/鲜、带壳/可食部、生/熟系统性排查）
   - 字段遗漏（nlc 有我们没录的营养素）
   - 多源冲突（nlc vs USDA vs 原值三方不一致清单）
   - 能量交叉离群（全量 kcal≈P×4+F×9+C×4 揪录入错误）
   - 落地：`nlc_match.py` 全库比对→差异/冲突/离群三清单→人工只看这些→脚本批量修正→过四道关+来源入页。
4. 其他 backlog（见 `feature/待办总览.md`）依次推进。

## 三、汇报方式（用户2026-07-21明确要求）
- **无人值守推进**：每完成一个待办 → 保存上下文到 `context_memory/` + **飞书通知**（4 段结构：做了什么/还有哪些/接下来/要验证）。
- **脚本方案（含数据来源）有成果** → 飞书通知。
- **全部完成后** → 飞书发两份清单：① **真机可验证项清单**（用户统一真机验）② **脚本成果清单**（建了哪些脚本/数据资产/核对发现）。
- **飞书走 `mcp__feishu-notify__send_feishu_text`**（独立 HTTP·本 session 验证过可靠·返回正常 message id）；**别用 cc-connect(Bash) 发**（Bash 本 session 曾损坏·新 session 若正常可用但优先 MCP）。

## 四、本 session 教训（写给新 session）
- 长 session 工具会造假：**关键操作（git/构建/数据）务必核实真实结果**，别信单条回显；Bash 报成功要用 `git log`/文件内容二次验证。
- Edit/Write 落盘可靠、Bash 执行不可靠时：用 Read/文件内容判断真实性，不信 Bash stdout。
