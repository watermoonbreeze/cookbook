# 🔖 SESSION 交接入口

> 覆盖式更新（2026-08-30 23:10 · E-NAV 底部导航真悬浮改造交付后）。本轮流水见 `2026-08-30_E-NAV底部导航真悬浮_会话快照.md`；上一轮见 `2026-08-30_E-IGE食材编辑大类联动修复_会话快照.md`。

## ⏭ 下一步（新 session 直接从这里开始）

**装新包验 E-NAV-01~08**（`真机验证清单-待验证_202608302259.md` 首节，8 项全是普通操作：滚动/滚到底/进详情/出库撤销/组成菜品/搜索覆盖层/切 Tab）。装包注意：Manifest 未变、热更装即可（本次纯 Compose 层改动）。用户填"验证结果/原因"后按迁移规则归档。
其余待验项不变：E-IGE-01~03（🔧）、E-HM-04~08、DEV-L2-04、OBS/OVN 8 项、E-B7F-05——优先级与路径见上一轮快照与清单汇总节。
后续可选打磨（本轮 ⚪ 非回归遗留，勿当 bug）：搜索覆盖层 imePadding、二级页 Snackbar 贴屏底。

## 一、先读清单（按序）

1. **本文件**（当前状态与 ⏭下一步）。
2. `.ai-context/PROJECT.md`（项目入口与真相优先级）。
3. `CLAUDE.md`「交付必做：真机待验证登记」与「踩坑红线」（本轮新增沉淀在 `experience/06` 2026-08-30 E-NAV 节：bottomBar 槽=伪悬浮、contentPadding 穿透语义、Dialog 继承宿主 CompositionLocal 须显式归零、双口径防双 inset、SnackbarHost 避让）。
4. 最新待验证清单 `docs/真机验证/真机验证清单-待验证_202608302259.md`（25 项+1 DEFERRED，含 E-NAV-01~08）。
5. 需要细节读两份快照（见顶部引用）。

## 二、工作规则（当前任务域）

- 真机清单：迁移规则/中文文案规则见 CLAUDE.md；批量迁移用 `temp/claude/migrate_batch.py`（用法 `python migrate_batch.py "<验证结果文案>" <编号...>`），迁移后核对行数守恒。
- 验证项设计三审：①判据真机可见/日志可查；②前置状态可自然达成；③判据与当前代码契约对照。
- 日志代验：用户导出 JSONL 后 AI 写脚本逐项核对（模式见 `experience/06`「方法论2」）。
- Git：用户明确要求才 commit/push（会话交接协议内提交除外）；提交前 `git status --short` 全查；多行提交信息用 `-F 文件`。

## 三、当前状态

- **分支**：master，`a0149cf1`（E-NAV 代码）+ 本笔 docs 提交，均未推送（再往前 8de5f7ac/d6c41530 亦未推送）。
- **本轮交付**：E-NAV 底部导航真悬浮（bottomBar 槽→Box 叠加层；四 Tab 页 contentPadding 统一避让；`BottomBarOverlay` 单一真相源+`LocalBottomNavReserved` 路由感知；Snackbar 避让；Dialog 显式归零）。构建+双单测两轮通过、google_quality_engineer 0 阻断（2 建议已随批修复复验）。范式沉淀 §9.44、经验入 experience/06、03 册/F-TOOLS/功能索引已回写。
- **真机验证**：待验证 25 项+1 DEFERRED（`202608302259`）：E-NAV-01~08（🔧 本次新增）+ E-IGE-01~03（🔧）+ E-HM-04~08 + DEV-L2-04 + OBS/OVN 8 项 + E-B7F-05。
- **遗留小项**：①上轮审查建议2——营养字段键名 `"kcal"…"purine"` 在 `applyGuess`/`clearGuessed` 逐字段重复，抽常量+键集守卫测试（加第 11 个营养字段时必做）；②本轮 ⚪ 两项可选打磨（见 ⏭）。

## 六、全景图新鲜度（每次交接必填）

**执行时间**：2026-08-30 23:08（03 册 E-NAV 回写后、交接提交前实跑）。

### 横轴（`review_freshness.py --md`）

| 册 | 页脚 sha | 之后提交数 | 判定 | 处置 |
|---|---|---|---|---|
| 01_架构与技术底座 | 29af225b | 0 | FRESH | — |
| 03_界面与交互 | a0149cf1 | 0 | FRESH | —（E-NAV 增量已回写） |
| 04_数据层 | 29af225b | 0 | FRESH | — |
| 20_健康与算法逻辑（专属） | 57cfbb87 | 0 | FRESH | — |
| 21_AI与网络请求策略（专属） | 57cfbb87 | 0 | FRESH | — |
| 22_预设与参考资料治理（专属） | 742611ce | 0 | FRESH | — |

### 纵轴（`feature_sync_check.py`）

- `--struct`：**[OK] 结构体检通过**。
- `--backlog`：**[OK] 无历史欠账**（F-TOOLS 落点已含 E-NAV，索引已重生成）。

**止损条件见 `08_决策记录.md` D-20（横轴）/D-25（纵轴）。下次交接重跑本命令覆盖本表。**
