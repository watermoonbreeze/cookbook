# AI智能体经验手册 - 索引

> 本手册由 /zongjie 指令自动维护，记录项目开发过程中的关键经验和知识。
> 拆分为多个主题文件，便于检索和维护。

## 元信息

- 上次总结会话点：2026-07-14下午/晚 营养素体系(ingredient_nutrition表+measurement_unit.grams克当量19.sqm+NutritionCalculator/Repository/Balance+专用seed文件ingredient_nutrition.json 65食材+菜品详情营养卡+餐食当天热量) + 增长型本地推荐(RecommendationWeights/Style综合/偏熟悉/偏新鲜/偏营养+HealthRuleEngine因子化+DataSource派生画像(偏好/主料重复/营养互补)+PeriodPlanner style+AI推荐页/周期计划风格选择器) + 色系墙固定本公历年/定位今天居中(snapshotFlow等布局)/往年平均色行 + 沉浸式insets(全屏页navigationBarsPadding/底栏去clip) + 标签烹饪库选择器(LibraryPickerDialog软删仅user) + 加餐默认日期(最后餐食+1/时段) + UX设计师5轮审查修2必修; 关键坑=沉浸式全屏页遮挡+底部双重padding、scrollToItem同帧layoutInfo旧、mapResult重建丢粘性字段、软删已选未同步复活
- 上次总结会话点：2026-07-14晚2 每日卡路里目标+推荐收尾+营养数据(BodyMetrics/CalorieTarget BMR×活动系数 存偏好JSON免迁移+功能设置录入+餐食达标+色系墙评级结合热量combine+mapLatest; 推荐prompt加风格画像+逐菜理由常做/补营养; 今日营养卡热量进度+宏量占比条; ingredient_nutrition 65→100; 5轮审核修录入写回竞态/数字过滤/图例) 关键坑=偏好JSON免迁移、表单多字段写回竞态用本地态单一真相源、数字过滤单小数点、combine+mapLatest批量异步
- 上次总结会话点：2026-07-15 家庭档案体系(family_member/member_care/day_absentee表20-21.sqm+FamilyRepository默认成员迁移/忌口并集/关注成员达标/饭量系数份额/缺席按天+成员管理页+膳食统计页+缺席微调持久化+健康膳食设置分组+色系墙与成员/热量解耦)+A1自定义食材归类(food_group列22.sqm+营养大类必选预选+映射分类树+classify显式覆盖)+常用单位英文化(克→g等,23.sqm rename保FK)+私人菜单数据补全(食材279→440/菜品147→516/营养100%/842用量)+修食材单位下拉为空bug(selectMainTab旧快照copy)+D10旧快照排查。关键坑=多init并发+旧快照copy写回丢字段、迁移rename保FK带守卫、加列升级无损、表无code按名映射、真机logcat+埋点定位UI数据问题
- 总结次数：28

## 文件索引

| 文件 | 内容概述 |
|---|---|
| [01_项目基础.md](01_项目基础.md) | 项目信息、模块结构、构建配置 |
| [02_架构规范.md](02_架构规范.md) | MVVM 规范、DataSource、数据流、页面跳转 |
| [03_数据库.md](03_数据库.md) | 数据库集成、表结构、升级记录、配置同步 |
| [04_业务功能.md](04_业务功能.md) | 登录、注册、打印等业务知识 |
| [05_UI组件.md](05_UI组件.md) | 自定义控件、布局规范、drawable |
| [06_问题与踩坑.md](06_问题与踩坑.md) | Bug 排查经验 + 注意事项与踩坑记录 |
| [07_操作记录.md](07_操作记录.md) | 关键操作记录（按时间倒序） |
| [08_用户习惯.md](08_用户习惯.md) | 用户工作习惯、代码要求、沟通风格 |
| [09_工程统一规范.md](09_工程统一规范.md) | KMP 架构、Android/iOS UI、数据库、代码风格与开发流程规范 |
