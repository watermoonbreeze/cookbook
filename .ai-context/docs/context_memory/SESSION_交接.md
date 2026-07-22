# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落档+**覆盖**本文件+git 提交。
> **维护约定（省 token）**：只保留当前状态·每次**全覆盖**·不堆历史明细（历史靠 git log + 同目录 `SESSION_交接_历史.md`）·目标 ≤1 屏。
> 更新时间：**2026-07-22 · Bug 修复 session（3 真机 bug 全修+push·`c920973`）+ 待办分类重整。剩 off-type 各单开 session。**

## 本 session 交付（Bug 修复类·全 push origin/master·构建+单测绿·过 Google 门禁无阻断）
- ✅ **待办分类重整**：`待办总览.md` 顶部加「按 session 类型」分类导航层（Bug/UI/数据健康/功能算法/工程/会商）+ 登记本轮用户反馈 5 项（I 表）。盘点：✅完成 57 · 未完成 ~57（多数🔨🔄为"核心已交付·剩真机验尾巴"）。
- ✅ **BUG1 今日卡"吃了多少"弹层**：点档位不刷新/退出重进才生效/不能滑动。根因=`todayCards` combine 透传 `observeTimelineWindow` 旧 cards、eaten_ratio 令牌不重建(stateIn去重白并)。修=令牌**下沉进 observeTimelineWindow**重跑 buildDayMealCards + sheet 加 verticalScroll + 删死代码令牌方法。
- ✅ **BUG2 编辑加菜后"少量"消失**：单测证实**数据没丢**(saveDayMeals 快照回填正确)，是加菜后整餐混合态、整餐档不高亮。修=`saveDayMeals` 新加菜**继承本餐统一吃完度**(仅统一态·混合/默认1.0不继承)。⚠️此为拍的产品语义，用户若要"新菜恒默认吃完"可改回。
- ✅ **BUG3 食材"100.0个"+营养按错单位**：加食材单位字典未就绪→`unit_id` 存 NULL。修=`DishRepository.saveDish` 空单位**回填"克"**(`di.unitId ?: gramUnitId`)。顺带修小剂量调料(盐3g)营养被当"3个×60=180g"放大。
- **测试**：MealRecordRepositoryTest +3、DishRepositoryTest +3(含"营养不放大"回归网)，全绿。**红线沉淀 2 条**(combine令牌透传反模式 / 存菜unit_id别留NULL)入 CLAUDE.md + `experience/06`。

## ⏭ 下一步（本 Bug session 主线已清·以下均 off-type·各单开 session）
- 【**真机待验**·用户做·设备 GCL 有数据】3 bug 各自验点见 `待办总览.md` I 表 + A 表(单位/剂量行)：①弹层即时刷新+可滑 ②整餐少量→加菜仍少量 ③加食材显"100克"、老菜重存自动补。
- 【**UI**·单开 UI session + Apple-UX 门禁】"我的"页重新归类(高频前/弹框合并) + "关于·已为你开启"突出 + 两编辑页统一 + 营养走势三线 + 首页推荐 v2 + 家族化 P2-P4。
- 【**数据/健康**·需 greenlight+联网核准】营养计算规则单独成页 + 四项数据待核 + 忌口补漏 + GI/纤维覆盖 + 菜品加食材智能默认剂量。
- 【**会商**】餐状态机("计划记了没吃"显式态·独立大改造)。

## 先读清单
1. 本文件 + `feature/待办总览.md`(顶部**分类导航**=按 session 类型选活·I 表=本轮 5 项)。
2. `CLAUDE.md` 踩坑红线(尤其**combine令牌透传反模式 / 存菜unit_id别留NULL / 食用比例/两处热量同源**)+架构准则。
3. `experience/06_问题与踩坑.md` 顶部本轮 3 笔(含"单测复现取真相"元经验)。

## 工作规则（用户已定·稳定）
1. 🔴 **一个 session 只做一类**·off-type 进待办不当场做·切类型=换 session。
2. 中文·深度·全权推进·快速做完不反复问确认。每批过门禁(界面→apple_ux_designer/App自动行为→apple_software_behavior/文案→copywriter/Android代码→google_quality_engineer+/code-review)+构建+单测。
3. 🔴 构建看输出别信 exit code(grep `BUILD SUCCESSFUL`)。**数据/计算 bug 先写 shared 单测在内存库复现取真相**(可证伪假设·比 adb pull 快·连无数据设备也能推进·复现测留作回归);真机数据 bug 用 GCL 设备 `adb pull`(外部路径·`MSYS_NO_PATHCONV=1`·多设备`-s`·列名 energy_kcal)。**temp/ 未 gitignore→提交必显式 add 指定文件、绝不 `git add -A`**。
4. 用户在场默认不自动 commit；用户要求才 commit+push origin/master。真机验证用户做(设备 GCL 有 app+数据，FCQNU 无数据)。
5. 红线：健康免责·透明分级告知·联网核准列数据来源·AI生成内容落库·热量默认开可关守免责·抽共享防漂移·SQLDelight 改表必加 `.sqm`。
