# 🔖 SESSION 交接入口

> 覆盖式更新（2026-09-04 · L4-P1 编码收尾批F·终审修复中间态）。蓝图=`docs/feature/分享链接解析_实施蓝图.md`（BLUEPRINT_READY·已冻结 `0d3dee0e`）；审核台账=`docs/arch_evidence/L4-P1_REVIEW_LEDGER.md`。编码模式（用户 2026-09-03 起沿用）：**开子智能体用轻量模型（haiku）编码、主会话审核**。

## ⏭ 下一步（新 session 直接从这里开始）

**主线 = L4-P1 批F 收尾**（编码已完成，剩终审修复收口+交付）。剩余步骤（按序）：
1. **补 R-1 断言**：`shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/data/parser/RecipeParserTest.kt` 加一个用例——作者行与用料行**直接相邻**（作者值行缺失）时 parse 不崩（extractNote 越界根修验证）。构造 innerText 形如 `"标题\n作者：\n用料\n土豆\n500克\nxx的做法步骤\n步骤 1\n切好\n菜谱创建时间：x"`，断言 parse 正常返回且不抛异常。
2. **重跑三命令**（终审 R-1/R-2/R-3/S-1~S-4 修复后验证）：`scripts\build-cli.bat :shared:testDebugUnitTest` + `:androidApp:testDebugUnitTest` + `:androidApp:assembleDebug`，全 BUILD SUCCESSFUL 为止。新增测试：`ParseViewModelBehaviorTest`（R-2）、`NewDishPrefillBehaviorTest` 的 R-3 用例、`LinkPrefillMapperTest` 的 S-3 用例、`RecipeParserTest` 的 R-1 用例。
3. **纵轴 backlog 回写**（SESSION_交接 六节已登记待办）：`projectReview/features/F-DISH/STATE.yml` + `F-RECOMMEND/STATE.yml` 的 `synced_to` 改为最终提交 sha；两文件夹 `20_实现.md` 各补一条 L4-P1 链接导入落点（NewDishViewModel.applyPrefill 整体化/NewDishScreen 做法三态提示行/LinkListScreen 四组）。
4. **终审复验**：确认 R-1/R-2/R-3 三阻断已修（可 grep 关键字：`from > endIdx` / `parse_empty_result` / `gramUnitForSave`），台账补「终审修复+复验」段。
5. **git 提交**（无人值守）：多行提交信息用 `-F temp/claude/l4p1_commit_msg.txt`（先 Write 该文件），前缀 `feat:`（或 `docs:`）。提交前 `git status --short` 全查暂存区。**不 push**（用户明确要求才 push）。
6. **覆盖式更新本文件**（第六节全景图新鲜度实跑）+ 视需要 `python .ai-context/tools/review_freshness.py --md` 修锚。

**平行待办（非编码·勿混入本次提交）**：①装最新包验真机（E-L4-01~11 已登记·清单 `真机验证清单-待验证_202609040918.md`）；②J3 四合一研究批。

## 一、先读清单（按序）

1. **本文件**。
2. `docs/arch_evidence/L4-P1_REVIEW_LEDGER.md`（**2026-09-04 编码批审节+批F 验证节**·含终审 3 阻断+4 建议原文与修复裁定）。
3. `docs/feature/分享链接解析_实施蓝图.md`（§7 STEP/§8 测试矩阵/§9 交付台账已回填）。
4. `CLAUDE.md`「踩坑红线」。

## 二、工作规则（当前任务域）

- **编码=子智能体（haiku 轻量模型）+主会话审核**：每批给蓝图对应 STEP 精确合同（冻结值/红线/「不确定就报告不许猜」）；回来后主会话审核=编译+grep 判据+红线抽查+Q 报告逐条裁决。审核台账**边收边写**。
- Git：不 push（除非用户明确要求）；提交前 `git status --short` 全查；多行提交信息用 `-F 文件`。
- 真机清单：迁移规则/中文文案规则见 CLAUDE.md；清单文件 `真机验证清单-待验证_202609040918.md`。

## 三、当前状态

- **分支**：master。WIP 提交链：`0d3dee0e`(蓝图)→`a3dda27b`(编码 WIP)→`5bfbb476`(交接)。当前工作区 **39 项未提交改动**（全部 L4-P1 编码+终审修复+台账+真机清单+蓝图台账）。
- **编码进度（全完成）**：
  - ✅ **批C**（STEP-L4-4/5）：NewDishPrefill 扩字段+ingredients→List<DishIngredient>、NewDishViewModel applyPrefill 整体化+addPrefilledIngredient+addCookingMethod(markUserTouched)+save() source 参数化+markSaved 回写、NewDishScreen 消费行+做法三态提示行、IngredientsScreen:64 生产点适配、AndroidModule 注册。
  - ✅ **批A**（STEP-L4-2/3+9.1+12.1）：ParseConfig/ParsedRecipe/ExtractedPage(extractPageFromJson 双层解码+loadParseConfig)/RecipeParser/LinkPrefillMapper/两 json+基准样本/seedParseDictionary/Preference 两 key/Cookbook.sq insertParseDictionary。
  - ✅ **批B**（§8 shared 测试）：16 用例全绿。
  - ✅ **批D**（WIP 期）：RemoteImageSaver/WebViewTextExtractor（WebViewTextExtractor 的 mainHandler 已 lazy 化供纯 JVM 测试构造）。
  - ✅ **批E**（STEP-L4-8~13）：ShareReceiverActivity(双入口+去重+T2)/ParseViewModel/ParseSheet/LinkList 四组/LinkBadge/LinkPendingBanner/DishesScreen 红点/HomeScreen 横幅(冷读)/MainActivity EXTRA_OPEN_NEWDISH/MainScaffold 接线/IngredientPicker isLinkNutritionPending/DataSourceReference/androidApp 测试(9+新 2 用例)。
- **批F 进度**：三命令验证曾全绿（修 Home* 两套件构造变更）；真机清单 E-L4-01~11 已登记；蓝图 §9 台账已回填；**终审（google_quality_engineer）3 阻断+4 建议，修复已全部落地**（R-1 extractNote 越界根修+parseWith runCatching 纵深/R-2 空结果判失败/R-3 quantity=null 显示适量+save 折默认/S-1 markShareLinkSaved 死菜重写/S-2 await Deferred 不重下/S-3 勺类数量折算/S-4 NEW_TASK）；补断言 ParseViewModelBehaviorTest+NewDishPrefill R-3+LinkPrefillMapper S-3 已落盘。**未完成**：R-1 断言、修复后重跑三命令、纵轴回写、提交、全景图。
- **终审 3 阻断+4 建议原文**见台账 2026-09-04 批F 节（含 R/S 编号与最小修复，已全部按方案落地）。

## 六、全景图新鲜度（下次交接重跑覆盖）

**执行时间**：2026-09-04 交接时**未重跑**（终审修复中间态·收尾提交后按 `review_freshness.py --md` + `feature_sync_check.py --struct/--backlog/--emit-index` 实跑覆盖本表）。纵轴已知 backlog：F-DISH（`0d3dee0e` 起）、F-RECOMMEND（`57cfbb87` 起）两处黄，收尾批回写 synced_to。
