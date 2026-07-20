# 🔖 SESSION 交接入口（新会话先读这里）

> 会话交接唯一固定入口（每次交接覆盖，历史流水在 git）。
> 触发词：「查看session继续/会话继续」→读本文件按"先读清单"补上下文、按"⏭下一步"接着干；「交接/保存session」→落地文档+覆盖本文件+git 提交。
> 更新时间：**2026-07-20 交接（无人值守·超长会话·~20 commit 全 push origin/master）**。shared 单测 + androidApp BUILD SUCCESSFUL 全绿；修复包多次装真机 GCL0220212004523 验证。**用户授权持续无人值守推进**。

## 一、先按序读（进入状态）
1. **本文件** + `feature/App操作基调_设计系统.md`（★本会话核心产出·全App统一操作逻辑基调）。
2. `feature/待办总览.md`（**H节=2真机bug** / **G节=架构透明治理** / 家族化 XL 项 / F节真机反馈批）。
3. `CLAUDE.md`（**新增：透明准则章节 + 数据来源/加食材走忌口/导航时序/移分类reconcile 4条红线**）+ `.ai-context/rules/通用规则.md`。
4. `experience/06`(本会话+5坑) + `App行为透明清单.md`(17行为分级) + `参考内容数据驱动架构方案.md`。

## 二、工作规则（用户已定·稳定·重要）
1. 中文；**无人值守·全权推进·凡需拍板用推荐方案·快速全做完·不反复问确认**。先 Explore/读代码摸现状再动·别过度设计·动手前验证别改已正确的。
2. **每功能走全套**：门禁（Apple-UX 界面 / apple_software_behavior 行为 / copywriter 文案 / Google 代码审查·阻断必修）→存上下文→经验→待办→飞书(4段)→commit(`[unattended]`)→push。
3. 🔴构建看输出别信 exit code：`scripts\build-cli.bat <task> *> log; Select-String "BUILD SUCCESSFUL"`。
4. **透明准则(用户确立)**：App所有行为让用户知道·分级透明(T0可查/T1留痕/T2弹框+changelog/T3硬同意)·避免弹框疲劳；行为规范由 apple_ux_designer+apple_software_behavior 总负责。
5. **健康数据红线**：不编造·联网核准ref+review·**加食材/菜品必走忌口系统核对(数值判绿≠临床可食)·联网核准必列"我的·数据来源"页**·嘌呤GI惯例阈值标非国标·忌口菜不进推荐。
6. 真机 GCL0220212004523；临时文件 temp/claude/；只交付 Android。

## 三、本会话交付全景（~20 commit·全 push·master 绿）
- ✅ 50道日常家常菜(658→708) ✅ **华为A12倒计时闪退根因修复(导航图未挂navigate·真机验证通过)** ✅ F#4酒水顶层分类+酒酿归谷薯 ✅ **可乐重复分类修复(seeder补齐式只加不删→reconcile+SEED v8·真机验证)** ✅ **酒类入库+临床忌口(啤酒对痛风显可食→35条care规则+红绿灯avoid显慎选·真机验证)** ✅ F#6报告记一餐带日期 ✅ 食材最近Tab去字母条
- ✅ **透明准则+分级透明确立**(CLAUDE.md章节+新建apple_software_behavior角色) ✅ **数据来源必列规则固化** ✅ 参考内容数据驱动架构会商定案 ✅ **《App操作基调·设计系统》**(5设计师会诊·全App家族化基调) ✅ 经验总结(手册第37次+2红线)
- 记忆新增：透明准则/联网核准列来源/(已有)加食材走忌口系统等。

## 四、⏭ 下一步（专注批·按优先级·无人值守推进）
1. 🔴🔴 **Bug 2 热量计算(健康关键)**：12000/290/400三处不一致(H节)。根因已析：①resolveGrams兜底 quantity×60 把100克当100个×60g→6000g ②今日卡combine基于餐记录流不随菜配料编辑重触发。**必按红线先 adb pull 拉库→python模拟SQL证实→再改**(resolveGrams兜底防大quantity+根治保存配料带g单位/今日卡刷新触发)。
2. 🔴 **家族化 P1 菜品编辑页**(首个基调参考实现)：按 `App操作基调_设计系统.md §三`+`temp/claude/design_edit_*.md`重构(封面前移§9.31高频低频分层+保存下移底部+质量5阻断修复)。§9.30 P2高风险·需真机全回归 新建/编辑/导入/回传savedDishId 四链路。
3. 🟡 **Bug 1 推荐图片不符**(H节)：推荐卡图优先取 dish.imagePath·无图才默认。
4. **家族 Lead agent 补跑**(6:10pm 额度重置后)：跨5页(餐食/菜品/食材编辑+菜品/食材选择)一致性走查·完善家族统一规范。
5. 家族化 P2食材编辑页→P3选择页统一+餐食编辑→P4全App逐批(每批Apple-UX过·Google审·真机验)。

> 接手：工作区仅 temp/claude·全 push·master 绿。可乐/酒忌口用户已真机验证通过。**下一步首推 Bug 2(健康关键·需DB pull) 或 家族P1菜品编辑页**。构建务必读输出确认 BUILD SUCCESSFUL。设计稿在 temp/claude/design_edit_*.md（视觉/质量/菜品UX/食材UX 4份完整·家族Lead待补跑）。
