# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-25 · 数据健康 session · J15修复 + 数据生产线 P0搭建 + 4项拍板落地(satFat/忌口guideline/nlc/USDA·无人值守)。**

## 本次(07-25 无人值守)追加交付 · 数据生产线 4项拍板全落地(逐项提交·均本地未push)
- ✅**①satFat 5真错重采样**(`4711be7`)：按 USDA satFat/fat比例×我方fat 口径校准(海带0.04/沙丁鱼0.28/田螺0.05/羊排1.40·培根清空)·satFat>fat归零。
- ✅**②忌口补guideline**(`c05bfc6`)：`condition_guidelines.json` 配置驱动·573回填→772/772 care规则100%可溯源·仅胃炎1条verified=0待核。
- ✅**③nlc常规化**(`e7840b4`)：端点http308→https·别名感知精确匹配·报告落盘+抽样+cron就绪。
- ✅**④USDA采集P1**(`2bc8c20`)：`collect_usda.py` 本地全库零key·补5项零脂satFat(→66%)·**真瓶颈=cn_en_map缺170映射**(禁无验证批量自动映射·错配=satFat根源)。
- 全程 roundtrip仍0无损·shared单测绿·seed改动仅10项(均安全可解释)。详见 `unattended_decisions.md`。


## 本 session 交付（数据健康类·构建单测绿·守健康免责红线）
- ✅ **J15 今日营养"缺主食"误报**（`456c3d4`）：真机 adb pull 坐实根因=营养大类判定只取 `is_main` 主料·问题菜(大排饭/炒饭)全 is_main=0→漏判。修=`FoodGroup.classificationNames(main,all)=main.ifEmpty{all}`(主料优先·空则回退全食材·纯纠错)·DishMini 加 allIngredientNames·今日卡/色系墙/per-meal 三处同步·mains 保留给痛风GI·3 复现单测全绿。⚠️色系墙历史有 empty-mains 菜的日子颜色更准(建议 UX 扫一眼)。遗留 partial-mains 靠 J14 补录。
- ✅ **07-24 反馈 8 项登记 J14–J21**（`456c3d4`）：J14 菜名解析(加调料/补大米)· J15✅ · J16 采购入口移库存 · J17 一周计划营养统一 · J18 复制日期锁死bug · J19 开源壁垒讨论 · J20 盐调养口径 · J21 吃了多少 affordance。
- ✅ **数据生产线 P0 基础搭建**（无人值守·未提交待办已记）：`data-pipeline/` 六脚本+schema+白名单+首校体检报告。**roundtrip diff=0**(链路无损)。体检揪 **🔴5 真错**(饱脂>总脂·培根/沙丁鱼/海带/田螺/羊排·混源口径)+🟡章鱼+低置信41+缺guideline573(结构性)+离群223(启发式)。**只诊断不改 seed**。
- 📌 **数据脚本方案已相当落地**：`scripts/data/`(能量自检 nutri_selfcheck.py/nlc_cross.py/USDA映射)·7-22 三源全量交叉跑过(真错仅虾仁已修)·今日 505 复检无新真错。

## ⏭ 下一步（本 session 可续 = 数据健康类）
- 【P1 核心·门禁】**扩 cn_en_map 中英映射(170项)** 填满 satFat(66%→~95%)：须逐条可信(LLM兜底/人工·§七)·**禁无验证批量自动映射**(错配=satFat>fat根源)。填后跑 `collect_usda.py` 出提案→人工把关→改seed。
- 【发布链路 P4·未做】staging→预制db(assets)+增量seed(指纹重跑)·guideline富化导回seed(现仅staging层)。condition配置驱动加病种。
- 【常规化接线】nlc/USDA cron 月度体检(工具就绪·全量502留cron·不硬刷)。
- 【待核·等真实数据再调】玫瑰花/章鱼/年糕/生蚝 + 培根fat=9(用户定:等真实数据一起调) · 胃炎guideline待联网核原文。
- 【会商·换 session】J17 一周计划营养线统一(与 AiPlan 抽共享·亮点)· J2 成员维度摄入建模② · J3 快照vs实时 · J10 多档案综合。
- 【UI/文案·单开 session·off-type】J14 菜名解析 · J16 采购入口 · J18 复制日期bug · J20 盐口径 · J21 affordance · J4/J5/J6/J7/J9/J11 · "我的"归类(I表)。
- 【待确认】Q2/Q3 · J19 开源壁垒战略 · J1 图片不刷新(Bug session)。

## 先读清单
1. 本文件 + `feature/待办总览.md`（**J 表=本轮 12 项 J1–J13** · 顶部分类导航按 session 类型选活；J8 已✅、J12 🔨诊断完）
2. `feature/预设食材数据生产线方案.md` + `_详细设计.md`（冻结·P0 待开工）+ `feature/食用比例进度条_成员维度摄入方案.md`（J2 选②·核心是建模落地）
3. `CLAUDE.md` 踩坑红线 + `context_memory/unattended_decisions.md`（本轮存档 + 待确认 Q1–Q4）

## 工作规则（用户已定·稳定）
1. 🔴 **一个 session 只做一类**·off-type 进待办·切类型=换 session。
2. 中文·深度·快速做完不反复问确认。每批过门禁（界面→apple_ux/文案→copywriter/Android→google_quality+/code-review）+构建+单测。
3. 🔴 构建看输出别信 exit code（grep `BUILD SUCCESSFUL`）。数据/计算 bug 先写 shared 单测内存库复现取真相。**temp/ 未 gitignore→提交必显式 add 指定文件、绝不 `git add -A`**。
4. 用户要求才 commit+push origin/master。真机验证用户做（设备 GCL 有数据）。
5. 🔴 健康数据红线：免责·联网核准列数据来源·**联网只少量验证·大批量用脚本**·忌口=规则非数据（指南原文人工核）·营养数值不采信AI·预制db不毁老用户增量·抽共享防漂移·SQLDelight 改表必加 `.sqm`。
