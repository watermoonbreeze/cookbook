# F5 报告·膳食结构日历色块重新布局（Apple-UX 门禁产出）

## 色块定义(从代码确认)
每格=某天「膳食均衡度级别」FoodGroup.nutritionLevel(三支柱覆盖度)：0红/1橙单一/2琥珀尚可/3浅绿均衡/4绿优/-1没记灰。
数据源 DietReport.perDayLevels(逐日),月=自然月实际天数28-31(非固定30,横滑裁切造成"30天"观感)。

## 必须一并解决的一致性问题
报告内联 levelColor 带红色(红→绿),但首页色系墙/餐卡用 NutritionColor.nutritionWallColor(中性灰→琥珀→黄绿→绿·无红)。
→ 结构日历统一改用 nutritionWallColor(level)(去红色·全App同级同色·去焦虑符合"回顾非考核")；均衡度行方块+均值文字色改 nutritionAccent。守免责不写"达标/合格"。

## 上下布局(去横滑,一次看清)
把单行Row改成按周分行网格(Column堆多Row)，移除横向滚动。
- 格子:Modifier.weight(1f).aspectRatio(1f)(等分随屏宽)·圆角4dp·行列间距spacedBy(4dp)。360屏卡内可用~300dp→每格~39dp。
- 周视图:1行7格+weekday抬头(一二三四五六日·周一起)。
- 月视图:7列(周一→日)×5-6行日历网格·leadingBlanks=月1号相对周一偏移(占位对齐)·格内显小日号·空占位格透明无数字不可点·"没记"空心描边灰格(区别有记单一的实心琥珀)。5行高~211dp,整卡~315dp<半屏,零横滑。
- 新组件 StructureCalendarGrid(levels,leadingBlanks,showDayNumber,weekdayHeader)：拼List<Cell>(Blank/Day)→chunked(7)分行·普通Column+Row静态渲染(不用LazyVerticalGrid避嵌套滚动)。

## 图例(新增,放日历上方)
4个10dp圆角小方块+标签横排:绿均衡/黄绿尚可/琥珀单一/空心灰没记 + 一句"每个格子=那天吃到的食物种类齐不齐·仅供参考"(copywriter定稿·鼓励非责备)。

## 色块交互(建议做·价值高)
MVP:点有记格→轻量气泡/ActionSheet显"7月15日·均衡"+"吃到蛋白·主食·蔬菜"或"这天缺蔬菜"(复用structureGaps当日算)。没记格不误导。进阶(二期):点跳该日食历(需带LocalDate+onGoDay)。

## ViewModel补最小派生(纯派生·不改DB·不迁移)
weekStart=MONDAY；月视图 leadingBlanks=range.start.dayOfWeek.ordinal；(若点跳)带range.start。

## 复用vs新增
复用:SegmentedControl(周月切换)/InsetGroup(卡)/Divider/nutritionWallColor+nutritionAccent(弃内联levelColor去红)/LegendDot方块版。
新增:StructureCalendarGrid(纯Column+Row)/LegendSwatch。
风险=需真机:月网格1号对齐/整月一次看清/无红色/没记vs单一可区分/点按当日。沉淀§九+报告模块方案。
