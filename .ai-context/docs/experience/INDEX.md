# AI智能体经验手册 - 索引

> 本手册由 /zongjie 指令自动维护，记录项目开发过程中的关键经验和知识。
> 拆分为多个主题文件，便于检索和维护。

## 元信息

- 上次总结会话点：2026-07-14下午/晚 营养素体系(ingredient_nutrition表+measurement_unit.grams克当量19.sqm+NutritionCalculator/Repository/Balance+专用seed文件ingredient_nutrition.json 65食材+菜品详情营养卡+餐食当天热量) + 增长型本地推荐(RecommendationWeights/Style综合/偏熟悉/偏新鲜/偏营养+HealthRuleEngine因子化+DataSource派生画像(偏好/主料重复/营养互补)+PeriodPlanner style+AI推荐页/周期计划风格选择器) + 色系墙固定本公历年/定位今天居中(snapshotFlow等布局)/往年平均色行 + 沉浸式insets(全屏页navigationBarsPadding/底栏去clip) + 标签烹饪库选择器(LibraryPickerDialog软删仅user) + 加餐默认日期(最后餐食+1/时段) + UX设计师5轮审查修2必修; 关键坑=沉浸式全屏页遮挡+底部双重padding、scrollToItem同帧layoutInfo旧、mapResult重建丢粘性字段、软删已选未同步复活
- 上次总结会话点：2026-07-14晚2 每日卡路里目标+推荐收尾+营养数据(BodyMetrics/CalorieTarget BMR×活动系数 存偏好JSON免迁移+功能设置录入+餐食达标+色系墙评级结合热量combine+mapLatest; 推荐prompt加风格画像+逐菜理由常做/补营养; 今日营养卡热量进度+宏量占比条; ingredient_nutrition 65→100; 5轮审核修录入写回竞态/数字过滤/图例) 关键坑=偏好JSON免迁移、表单多字段写回竞态用本地态单一真相源、数字过滤单小数点、combine+mapLatest批量异步
- 上次总结会话点：2026-07-15 家庭档案体系(family_member/member_care/day_absentee表20-21.sqm+FamilyRepository默认成员迁移/忌口并集/关注成员达标/饭量系数份额/缺席按天+成员管理页+膳食统计页+缺席微调持久化+健康膳食设置分组+色系墙与成员/热量解耦)+A1自定义食材归类(food_group列22.sqm+营养大类必选预选+映射分类树+classify显式覆盖)+常用单位英文化(克→g等,23.sqm rename保FK)+私人菜单数据补全(食材279→440/菜品147→516/营养100%/842用量)+修食材单位下拉为空bug(selectMainTab旧快照copy)+D10旧快照排查。关键坑=多init并发+旧快照copy写回丢字段、迁移rename保FK带守卫、加列升级无损、表无code按名映射、真机logcat+埋点定位UI数据问题
- 上次总结会话点：2026-07-15 数据核对轮(食材归类核准 classify尾词优先+NAME_OVERRIDE54项特例 + 全量440食材营养从权威平台nlc.chinanutri.cn/USDA/悉尼GI/2024痛风指南 15组agent联网核准 覆盖升级式合并verified278/pending162 + 食材营养表功能combine5筛选排序VM/横滚可排序表/数据来源弹窗 + 数据来源.md记档)。关键坑=中文食材分类看尾词head-noun非前缀关键词、健康数据禁编造出处必标ref+verified/pending、覆盖升级式合并保留auth未覆盖字段、agent联网分片核准数据模式
- 上次总结会话点：2026-07-15 无人值守UX深挖轮(4个Apple-UX agent深挖食材/菜品/餐食/健康膳食→方案→主线程落地15项安全改进+高风险入待确认队列 + 营养表体验行高亮/搜索进标题栏/横竖屏悬浮按钮/上划收筛选栏)。关键坑=字典英文化后按旧名硬编码查(gramUnit找"克"恒null丢unitId)、健康提示守免责红线(如实非医嘱nutritionGaps)、combine多源suspend可内联(别用list+cast)、无人值守研究agent产方案+主线程落地安全项模式(安全vs需拍板判定)
- 上次总结会话点：2026-07-15深夜 无人值守三评审+bug大轮(用户报bug:凉皮0千卡=seedDishes已存在即跳过致配料关联永久残缺→补齐式重挂+SEED_LOGIC_VERSION盐;随机推荐翻忌口菜=rotate随机翻到全忌口末批;膳食统计没吃回不来=togglePresent读冻结的stateIn.value + 3专家agent深挖算法/架构/创建流程,落地avoid50→5/onHandMain封顶/seed缺失项告警/deleteDay落日志/resolveGrams防负/编辑未保存守卫/详情记这道菜CTA/首页计划上移/备注折叠/存为菜品,需拍板项入待办E节)。关键坑=seed已存在即跳过致关联永久残缺(补齐式)、stateIn(WhileSubscribed)无订阅者value冻结致toggle失效、rotate随机翻罚分末批、打分硬约束巨值靠分层排序冗余、专家agent产方案+主线程落安全项
- 上次总结会话点：2026-07-16 体验修复轮(小米8拍照90°旋转=EXIF方向未应用加androidx.exifinterface读+Matrix摆正、主食角标改苹果式贴角小圆、个人健康档案整合进家庭档案=与成员"我"同套care分类纯UI去重取消独立入口用户卡取我的健康状态数据层保留、导航栏色跟随主题背景=透明系统栏在app深色但系统浅色时露android默认白windowBackground改SideEffect设navigationBarColor=colorScheme.background)。关键坑=拍照EXIF方向需读取+Matrix摆正(minSdk21用androidx.exifinterface)、透明系统栏≠跟随界面色需显式设Compose主题背景、健康档案与家庭成员同套care分类整合纯UI去重
- 上次总结会话点：2026-07-16 无人值守健康参考轮(排骨海带汤等预设菜0千卡新根因=补齐只补缺失关联忽略已关联NULL用量→fillDishIngredientQuantityIfNull回填空用量+SEED v3;软删菜reseed重插带新数据、未删走补齐分支被跳过;华为真机adb拉库python模拟368菜838行验证 + MMR批内多样性diversify主料Jaccard贪心只重排正常菜层四风格全开含单测 + 膳食参考依据页DietaryReference静态数据6分类12来源4片agent联网核准钠糖GI嘌呤脂肪+关于链接 + 营养级别评级方案只出文档 + 食材表单低频折叠+新建返回守卫 + build-cli.sh JDK17自动探测兼容Mac)。关键坑=0千卡补齐要回填空用量非只补缺失关联、Git Bash adb访问/sdcard需MSYS_NO_PATHCONV=1、db在getExternalFilesDir/cookbook/db非databases/、嘌呤三级分级非国标(WS/T560只定性)、GI 55/70是FAO/WHO非WS/T652、真机拉库python模拟SQL先证修复再改代码
- 上次总结会话点：2026-07-16 下半场(调料默认克数SeasoningDefaults只对分类调料缩小油菜仍100g + 数据来源页/参考资料组ReferenceScaffold复用 + 功能介绍页Apple欢迎范式 + 配色切换7套AppPalette/Palettes向iOS系统色调校鲜亮有活力+我的外观选择器 + 今日卡宏量渐变条段中心平滑过渡+慢病提示个人视角concerns琥珀点色系墙保持纯结构决策A + 组合部分选 + 交互模式库Apple-UX审阅§九增补9.12-9.17+交互组件复用指南+CLAUDE门禁 + B-1餐食页守卫UnsavedGuard复用件)。关键坑=Material3-1.1.2无HorizontalDivider用Divider、配色改走AppPalette+Palettes别硬编码散落、色系墙不关联热量慢病(热量个人放今日卡)、调料默认克数只对分类调料缩小、宏量渐变别实块糊接缝改段中心平滑过渡、未保存守卫用rememberUnsavedGuard非包裹式、UI交互先Apple-UX过再编码+做UI先查交互组件复用指南
- 上次总结会话点：2026-07-16 收尾(B批UX B-1~B-5 双门禁完成:UnsavedGuard复用件+餐食页守卫、AppSnackbar统一宿主、库存Tab MiniStepper就地加减、食历卡三图标收ActionSheet、删整天软删撤销snapshotDay+showUndo；渲染宏量渐变回退分段实色；Google代码审查阻断项=撤销saveDayMeals重复抬喜爱度→bumpPreference开关+回归单测；CLAUDE加代码质量门禁=Google工程师审查agent)。关键坑=删/还原走save路径别重复抬统计(saveDayMeals bumpPreference=false)、可逆删除软删+撤销别硬确认(快照读失败别照删/Snackbar Long/单job串行化)、未保存守卫非包裹式rememberUnsavedGuard、卡片能力用可选footer槽、CompositionLocal宿主null默认挂MainScaffold、两门禁(UI先Apple-UX后Google审查)
- 总结次数：35

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
