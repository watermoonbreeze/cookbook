# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-24 · 数据健康 session · 交接文件对齐真实状态（此前 af78497「交接落盘」漏更本文件，现修正）。**

## 本 session 交付（数据健康类·构建单测绿·守健康免责红线）
- ✅ **J8 钠阈值版本一致性**（`b275c1b`）：据《中国高血压防治指南2024修订版》，`SODIUM_HYPERTENSION_MG` 2400→2000（高血压钠<2g/d≈5g盐·旧2400/6g过时）+ 6 处文案（膳食参考依据/营养怎么算/健康状态参考/NutritionInterpreter/DishNutritionLine 偏咸阈值800→667）+ NutritionLevelEvaluator/InterpreterTest 占比重算·全绿。
- ✅ **Q1 藻类加固**（`b275c1b`）：紫菜/海带存疑从严排除植物豁免（2024指南植物豁免主针对豆/菜/菌菇）·干紫菜高嘌呤仍判红·+单测。
- 🔨 **J12 今日"还缺"诊断**（`b275c1b`·未完）：单测证实 **classify 逻辑正确**（蛋/豆/菜全判对）→**根因在数据层**（今日"还缺"只取 `is_main` 主料太脆弱·简单菜/自建菜 is_main 标注让 mains 取不到能识别蛋白/蔬菜的名字→误报缺）。复现单测已留作回归。
- 📋 **J13 登记**（月度营养数据交叉对比脚本·纯脚本脱离AI·挂靠数据生产线 P4）· **J2 已选②**（成员维度摄入建模方案二）。

## ⏭ 下一步（本 session 可续 = 数据健康类）
- 【J12 收尾·在型可编码】`mains` fallback 改进：主料空→用**菜名/全部食材** classify 兜底，修"吃了蛋/豆/菜仍报缺"。**前置**：涉色系墙"还缺"口径，需**算法+UX确认**后再改；真机 adb pull 佐证 is_main 由用户做。改完补复现单测回归。
- 【数据生产线】P0 开工（不依赖 USDA key）：建 `data-pipeline/`+schema+导入+首校脚本→现有 seed 数据体检单（本期只诊断不修）。方案已冻结。
- 【会商·换 session】J2 成员维度摄入建模②落地设计（算法+DB+UI·五方会商）· J3 快照vs实时 · J10 多档案综合评估。
- 【UI/文案·单开 session·off-type】J4 食历时间轴+月折叠 · J5 日期 · J6"往年"→"历年" · J7 非家庭用餐 · J9 忌口宜口配色 · J11 搜索二级名。
- 【待确认】Q2 鱼油recommend/Q3 鸡毛蛋preschool（守红线未做·待定）· J1 图片不刷新（Bug session）。

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
