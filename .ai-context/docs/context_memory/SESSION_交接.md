# 🔖 SESSION 交接入口（新会话先读这里）

> 会话交接唯一固定入口（每次交接覆盖，历史流水在 git）。
> 触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落地文档+覆盖本文件+git 提交。
> 更新时间：**2026-07-20 交接（无人值守·超长会话·~28 commit 全 push origin/master）**。shared 单测 + androidApp BUILD SUCCESSFUL 全绿；工作区仅 temp/claude。**用户授权持续无人值守推进**。用户设备暂断→**远程 git 验证**。

## 一、先按序读（进入状态）
1. **本文件** + `feature/App操作基调_设计系统.md`（★全App家族化统一操作逻辑基调·新页面/改页面按此走）。
2. `feature/待办总览.md`（**H节真机bug** / G节架构透明治理 / **家族化XL项(P1菜品编辑已交付·P2起待做)** / F节真机反馈批）。
3. `CLAUDE.md`（**新增：透明准则章节 + 分级透明 + 6条踩坑红线**：数据来源/加食材走忌口/导航时序/移分类reconcile/营养折算防天价/派生汇总观察源表）。
4. `experience/06`(本会话多坑) + `App行为透明清单.md` + `参考内容数据驱动架构方案.md`。

## 二、工作规则（用户已定·稳定·重要）
1. 中文；**无人值守·全权推进·凡需拍板用推荐方案·快速全做完·不反复问确认**。先 Explore/读代码摸现状再动·别过度设计·动手前验证别改已正确的。
2. **每功能走全套**：门禁（Apple-UX界面 / apple_software_behavior行为 / copywriter文案 / Google代码审查·阻断必修）→存上下文→经验→待办→飞书→commit(`[unattended]`)→push。
3. 🔴构建看输出别信 exit code。数据bug先 python 拉真机库证实再改。
4. **透明准则**：App所有行为让用户知道·分级透明(T0可查/T1留痕/T2弹框+changelog/T3硬同意)·避免弹框疲劳。
5. **健康数据红线**：不编造·联网核准ref+review·**加食材必走忌口系统核对(数值判绿≠临床可食)·联网核准必列"我的·数据来源"页**·营养折算防天价·嘌呤GI惯例阈值标非国标·忌口菜不进推荐。
6. 用户设备暂断→**推 git 用户远程电脑验证**；临时文件 temp/claude/；只交付 Android。

## 三、本会话交付全景（~28 commit·全 push·master 绿）
**真机验证过**：华为A12倒计时闪退根因修复✅ · 酒类临床忌口(啤酒对痛风改慎选)✅ · 可乐重复分类修复✅
**本会话交付**：50道家常菜(658→708) · F#4酒水分类+酒酿归谷薯 · F#6报告带日期 · 食材最近去字母条 · **透明准则+分级透明+apple_software_behavior角色** · 数据来源必列规则 · 参考内容数据驱动架构会商 · **《App操作基调·设计系统》(5设计师会诊·全App家族化)** · 经验总结(手册38次)
**下半程(待用户远程验)**：
- ✅ **热量bug修复**(用户给 cookbook.db·python证实)：12000根因=配料unit_id空+resolveGrams兜底×piece→按克直取防天价(`470f2e6`)；290停旧值=今日卡不随dish编辑刷新→observeDishContentChanges并进combine(`ed0b2f8`)。shared单测绿。
- ✅ **菜品编辑页P1家族化**(`0d0f6dd`)：封面coverStyle前移引导拍照+保存下移FormBottomBar+步骤/烹饪/标签下沉§9.31折叠+食材前移。Google审查无阻断。**真机待验四链路(新建/编辑/导入/回传savedDishId)**。

## 四、⏭ 下一步（专注批·无人值守推进）
1. 🟡 **Bug 1 首页推荐图片不符**(H节)：推荐卡图优先取 dish.imagePath·无图才默认。落点 `AiRecommendScreen`/推荐卡取图逻辑。
2. 🔴 **家族化 P2 食材编辑页**：按 `App操作基调_设计系统.md §四`+`temp/claude/design_edit_ingredient_page.md`。P0拆4折叠段(营养数值/更多信息/做法说明/调养建议)+单件克重条件显+**去营养大类必选**(现Dialog内纯版式·安全)；P1视觉InsetGroup/AppTopBar/rememberUnsavedGuard；P2载体全屏Dialog→路由页接统一Snackbar。**建议先真机验P1菜品编辑参考实现再铺P2-P4**(避免未验证基调铺满5页返工)。
3. 家族化 P3两选择页统一SelectionSummaryBar(F#3)+餐食编辑 → P4全App逐批(每批Apple-UX过·Google审·真机验)。
4. **家族Lead补跑**(会话额度重置后)：跨5页一致性走查。
5. 家族质量走查5阻断项(design_edit_quality.md·保存失败提示看不见/进程被杀草稿等) + 视觉/质量建议归UI打磨批。

> 接手：工作区仅 temp/claude·全 push·master 绿。设计稿在 temp/claude/design_edit_*.md(菜品UX/食材UX/视觉/质量 4份完整·家族Lead待补)。构建务必读输出确认 BUILD SUCCESSFUL。用户远程 git 验证热量修复+菜品编辑P1。
