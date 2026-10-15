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
 
---
 
## 2026-10-15 · 构建排查与软件著作权材料交接
 
### 本次已完成
 
- 已从远程 `origin/master` 快进拉取到 `c9083a0b`，工作区原有代码无冲突。
- 执行 `scripts\build-cli.bat clean` 成功。
- 首次执行 `scripts\build-cli.bat :androidApp:assembleDebug` 时，失败于 `:androidApp:checkDebugAarMetadata`：`androidx.emoji2:emoji2-views-helper:1.4.0` 回退访问 `dl.google.com` 时 TLS 握手失败；不是 Kotlin 或业务源码错误。
- 使用依赖刷新后补齐本地缓存，再次执行同一构建命令成功，生成 `androidApp\build\outputs\apk\debug\androidApp-debug.apk`。构建过程存在 Kotlin/Compose 警告，但无编译错误。
- 全景图检查：`feature_sync_check.py --struct` 通过；`--emit-index` 预览基线为 `c9083a0b`；`review_freshness.py` 报 `01`、`20`、`22` 各 `STALE(1)`；`architecture_quality_check.py` 因真机验证清单未被脚本识别而失败。
- 已建立软件著作权材料草案目录：`.ai-context/docs/software_copyright/`。
 
### 首期申报范围
 
纳入 Android 非 AI 家庭膳食管理闭环：家庭/健康档案、食材库、菜品库、记一餐、食历、营养分析、慢病饮食参考、库存/采购、手动周计划、厨房步骤与计时、搜索、备份同步、引导和设置。
 
暂不纳入：AI 记餐、云端模型推荐、AI 周计划、AI 健康建议、AI 语音、本地 AI 占位能力。
 
### 尚未关闭事项
 
- 补申请人主体、著作权人、最终软件名称、版本号、开发完成日期、首次发表情况、权属声明和签章材料。
- 采集首期主要页面截图，生成用户手册和设计说明书的最终排版稿。
- 按登记机构要求生成去敏后的连续源程序鉴别页，并锁定申报版本。
- 完成家庭成员编辑/病种切换、库存迁移/开关、备份同步、周计划和营养病种切换等真机待验项。
- 处理或确认全景图横轴 `STALE(1)`，并让架构质量检查识别唯一真机验证清单。
- 后续换机或清缓存构建时，确保阿里云镜像/代理可访问，避免再次回退到 `dl.google.com` TLS 失败。
 
### 交接入口
 
- 软著材料总入口：`.ai-context/docs/software_copyright/README.md`
- 未达成项：`.ai-context/docs/software_copyright/申报前未达成项清单.md`
- 构建失败日志：`temp/codex/assembleDebug-20261015.log`
- 构建重试日志：`temp/codex/assembleDebug-20261015-retry.log`
- Debug APK：`androidApp/build/outputs/apk/debug/androidApp-debug.apk`
 
### 本次交付状态
 
- 代码：未修改。
- 文档：新增软件著作权申请材料草案及本交接记录。
- 待执行：提交本次文档并推送 `origin/master`。

