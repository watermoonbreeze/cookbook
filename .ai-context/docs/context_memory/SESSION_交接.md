# 🔖 SESSION 交接入口

> 覆盖式更新（2026-09-04 · **L4-P1 分享链接解析完整交付**）。蓝图=`docs/feature/分享链接解析_实施蓝图.md`（BLUEPRINT_READY）；审核台账=`docs/arch_evidence/L4-P1_REVIEW_LEDGER.md`（六批编码审+终审 3 阻断+4 建议全消）。

## ⏭ 下一步（新 session 直接从这里开始）

**主线 = L4-P1 编码已全部完成，进入真机验证阶段**（代码+测试+终审+全景图全收口）。

1. **装最新包验真机**：`真机验证清单-待验证_202609040918.md` 的 **E-L4-01~11**（分享接收+隐私弹窗/解析+存为菜品全链/列表四组/横幅红点/失败重试/新食材管线/图片预下载/做法推断锁/步骤图/单位映射/离线重建+去重）。**本批改了 Manifest（新增 ShareReceiverActivity）——必须重装 APK，热更/增量装不生效。**
2. 用户填「验证结果/原因」后：通过项迁移归档（`temp/claude/migrate_batch.py` 或手工），失败项定位（调试包 logcat 查 Tag `LinkRecv`/`LinkParse`，正式包无这些日志）。
3. **平行待办（非编码）**：①J3 四合一研究批；②横轴 3 册 STALE(1)（01 架构/20 健康/22 预设·软信号 DEFER·到期批次处理）；③小尾巴（L1 总免责引言/S-5 回填断言/合成行透传测试——若仍待办）。

## 一、先读清单（按序）

1. **本文件**。
2. `docs/arch_evidence/L4-P1_REVIEW_LEDGER.md`（**2026-09-04 批F 验证与交付节+终审修复表**）。
3. `docs/feature/分享链接解析_实施蓝图.md`（§9 交付台账已回填·§7/§8 编码合同）。
4. `CLAUDE.md`「踩坑红线」。

## 二、工作规则（当前任务域）

- 编码=子智能体（haiku 轻量模型）+主会话审核（用户 2026-09-03 指示）；真机验证阶段无编码，主会话直接读反馈定位。
- Git：不 push（除非用户明确要求）；提交前 `git status --short` 全查；多行提交信息用 `-F 文件`。
- 真机清单：迁移规则/中文文案规则见 CLAUDE.md。

## 三、当前状态

- **分支**：master。**已提交 3 笔**（未 push）：`4f1df5f2`（feat: L4-P1 完整落地+终审修复·代码+测试+台账+真机清单）、`45f7a202`（docs: 纵轴回写 F-DISH/F-RECOMMEND）、`9c7eea0a`（docs: 全景图新鲜度自检·横轴锚点+纵轴 5 功能域）。**工作区干净**。
- **交付内容**（L4-P1 分享链接解析·下厨房单源）：
  - **shared 解析引擎**：ParseConfig/ParsedRecipe/ExtractedPage（extractPageFromJson 双层解码+loadParseConfig）/RecipeParser（行配对+锚定截断+isMain 折算≥100g）/LinkPrefillMapper（单位映射/extractUrl/横幅四态）；xiachufang.json v3 + 基准样本。
  - **数据层**：share_link（含 step_images）+parse_dictionary 两表+33.sqm+ShareLinkRepository（markShareLinkSaved 按 dish 活性判断·S-1）+seedParseDictionary。
  - **采集与下载**：WebViewTextExtractor（once CAS 单发+三路 destroy+mainHandler lazy）/RemoteImageSaver（connectionFactory/dirProvider 注入）。
  - **接收与解析 UI**：ShareReceiverActivity（双入口+T2 隐私弹窗+去重+死菜兜底）/ParseViewModel（startCollect/rebuildFrom 双流+prefetch Deferred+空结果判失败）/ParseSheet（三态+步骤图渐进披露）/LinkList 四组/LinkBadge 红点/LinkPendingBanner 横幅。
  - **预填链路**：NewDishPrefill 量纲载体（List<DishIngredient>）+NewDishViewModel applyPrefill 整体化+addPrefilledIngredient+markSaved 回写+适量 null 折默认+做法三态提示行。
  - **入口接线**：DishesScreen 链接图标红点/HomeScreen 横幅（一次性冷读）/MainActivity EXTRA_OPEN_NEWDISH/MainScaffold LINK_LIST。
  - **测试**：shared 16+androidApp 11 用例全绿（夹具生产同路径）。
- **终审**：google_quality_engineer 3 阻断（R-1 extractNote 越界/R-2 空结果判失败/R-3 适量 null 一致）+4 建议（S-1 死菜重存回写/S-2 await Deferred/S-3 勺类折算/S-4 NEW_TASK）**全修复+复验通过**（台账有表）。
- **三命令全绿**：`:shared:testDebugUnitTest` + `:androidApp:testDebugUnitTest` + `:androidApp:assembleDebug`。
- **真机清单**：E-L4-01~11 已登记（`202609040918`）。
- **全景图**：横轴 03/04 已修 FRESH、21 FRESH；**3 册 STALE(1) DEFER**（01 架构/20 健康/22 预设·软信号锚点未变·到期批次处理）。纵轴 `--backlog` 无历史欠账、`--struct` 通过。

## 六、全景图新鲜度（2026-09-04 实跑·已提交）

### 横轴（`review_freshness.py --md`）

| 册 | 判定 | 处置 |
|---|---|---|
| 01_架构与技术底座 | STALE(1) | DEFER（软信号·锚点未变·到期批次） |
| 03_界面与交互 | FRESH | ✅ 已修（screens=36·4f1df5f2） |
| 04_数据层 | FRESH | ✅ 已修（seed_files=14·4f1df5f2） |
| 20_健康与算法逻辑 | STALE(1) | DEFER |
| 21_AI与网络请求策略 | FRESH | — |
| 22_预设与参考资料治理 | STALE(1) | DEFER |

### 纵轴（`feature_sync_check.py`）

- `--struct`：**[OK] 结构体检通过**。
- `--backlog`：**[OK] 无历史欠账**（F-DISH/F-RECOMMEND + F-AI-MEAL/F-HEALTH/F-INGREDIENT/F-MEAL/F-TOOLS 共 7 功能域 synced_to 全回写 4f1df5f2）。
- `--emit-index`：未重跑（L4-P1 新增 ui/link 组件/解析引擎符号——下次收口批随功能路径索引重生成）。
