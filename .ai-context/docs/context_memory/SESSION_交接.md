# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + 同目录 `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-22 · 功能 session（是否吃完 + 三开关默认开 + 口径修复，全部已 push 并真机验证通过）。本 session 收尾·剩 off-type 各单开 session。**

## 本 session 交付（功能类·深度·全部构建绿+真机验证通过+已 push）
- ✅ **是否吃完(食用比例) per-dish 全功能**：`meal_record_dish.eaten_ratio`(31.sqm·默认1.0零回归) + `IntakeCalculator`(个人摄入=Σ整份×eatenRatio×share·单一真相源) + 今日卡/报告接入 + `observeEatenRatioChanges` 令牌(解改B表不刷今日卡红线) + `EatenAdjustSheet`(今日卡入口·四档整餐+分菜折叠·实时可逆) + `saveDayMeals` 快照回填(防编辑当天丢吃完度)。红线:eatenRatio 只缩个人摄入总量不缩定性提示。会商方案 `feature/食用比例吃完度_摄入会商方案.md`。
- ✅ **三开关默认关→默认开**(用户决策·透明opt-out)：热量数值/营养色系/分步执行·`PreferenceKeys.DEFAULT_*` 集中防漂移·首启 `FeatureGuideScreen` 透明告知一屏·旧"热量个人概念默认关"红线更新为"默认开·可关·守免责"。
- ✅ **口径一致性修复**(用户报"今日营养 vs 今日餐食热量不对应")：`DayMealCardView` 由 `ids.distinct()` 少算同菜多餐次 → 对齐今日营养卡(逐实例+IntakeCalculator)·两处同源。**真机 07-22(午晚同3菜)验证通过**。
- ✅ **透明/测试**：报告"营养按你的饭量估算"夸大文案修·App行为透明清单#18/19/20·`IntakeCalculatorTest`+`DietReportAggregatorTest`折算用例+`MealRecordRepositoryTest`2条(数据保全/整餐设置往返)。
- **门禁**：google_quality_engineer 终审无阻断(3🟡+2⚪全处理)+/code-review 无新发现+`:shared`单测绿+`:androidApp`绿。
- **真机验证**：设备 GCL(version32)迁移无损(eaten_ratio列+1098行全1.0)+口径修复UI确认。
- **commits(已push origin/master)**：`9c9f101`(主功能)·`bcf835c`(报告用例)·本次交接commit(口径修复+DB测试+docs)。

## ⏭ 下一步（本功能 session 主线已清·以下均 off-type·各单开 session）
- 【**会商→拍板留待办**】**餐状态机**："计划记了但没吃"显式态(补计划餐"date≤today自动当吃完"语义漏洞)·独立大改造·单开 session 会商。
- 【**UI**·须单开 UI session + Apple-UX 门禁】①两编辑页统一(加食材/加菜品·抽 FormField/FormSection)②营养走势三线同显(蛋白/脂肪/碳水)+周/月视图统一评估。
- 【**数据/健康**·需你 greenlight+联网核准】四项数据待核(玫瑰花/章鱼/年糕/生蚝)、忌口补漏边界(鸡毛蛋/鱼油/植物高嘌呤)、GI/纤维覆盖。

## 先读清单
1. 本文件 + `feature/待办总览.md`(是否吃完行=🔨已实现待审→本次真机验过·off-type队列见上)。
2. `feature/食用比例吃完度_摄入会商方案.md`(会商结论+拍板+落地)。
3. `CLAUDE.md` 踩坑红线(尤其**食用比例/两处热量同源/三开关默认值集中**新增条)+架构准则+`~/.claude/workflow_auto_orchestration.md`。

## 工作规则（用户已定·稳定）
1. 🔴 **一个 session 只做一类**·off-type 进待办不当场做·切类型=换 session。
2. 中文·深度·全权推进·快速做完不反复问确认。每功能过门禁(界面→apple_ux_designer/App自动行为→apple_software_behavior/文案→copywriter/Android代码→google_quality_engineer+/code-review)+构建+单测。
3. 🔴 构建看输出别信 exit code(grep `BUILD SUCCESSFUL`)。数据 bug 先 `adb pull` 真机库(外部路径·`MSYS_NO_PATHCONV=1`·多设备`-s`·列名 energy_kcal)证实再改。**temp/ 未 gitignore→提交必显式 add 指定文件、绝不 `git add -A`**。
4. 用户在场默认不自动 commit；用户要求才 commit+push origin/master。真机验证用户做(设备 GCL0220212004523 有 app，FCQNU 无)。
5. 红线：健康免责·透明分级告知·联网核准列数据来源·AI生成内容落库·**热量默认开·可关·守免责(旧默认关红线已更新)**·抽共享防漂移·SQLDelight 改表必加 `.sqm`。
