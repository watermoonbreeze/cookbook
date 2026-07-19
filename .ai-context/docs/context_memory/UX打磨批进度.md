# UX 打磨批进度（2026-07-19 用户 review 期·无人值守推进）

> 用户 2026-07-19 边用边反馈一批菜品/食材/搜索/AI推荐/记一餐的打磨项 + 一期规划拍板。用户已授权**全权无人值守·凡需拍板用我推荐方案**。每项完成走：门禁+强测试+审查 → 保存上下文 → 经验 → **更新项目说明书** → 飞书 → commit+push。

## 已交付（本轮·已 push）
- ✅ **一期规划里程碑 + 两轮决策**（`7b300c0`/`cf65be0`）：3方案(首页推荐/账号统计/路线图)+商业汲取13项+决策清单。见 `feature/一期收官路线图与决策清单.md`。
- ✅ **GLM-4.5-Flash 超时修复**（`3fd18f0`）：关默认思考(thinking.disabled)+max_tokens=2048+读超时45s。
- ✅ **加餐日期提示 bug**（`5db9d4e`）：setDate 重选当前空日期时清残留 dateWarning。
- 🔄 **#1 菜品页餐次统称"加餐"**（构建中·待提交）：见下。

## 打磨批队列（用户反馈顺序）
1. ✅ **菜品页餐次统称"加餐"**（`aecfe8c`）：菜品页餐次筛选把上午/下午合并成统称"加餐"chip·**只菜品页筛选/搜索用**·不改 MealSlot/meal_type/记一餐。androidApp `DishSlotFilter` enum(SNACK={上午,下午})。餐次栏=全部/早餐/中餐/加餐/晚餐/宵夜。
2. ✅ **搜索按分类**（待提交·构建绿+审查无阻断）：菜品搜"家常菜"(菜系)出该菜系全部菜、搜"早餐/加餐"(餐次)出该餐次菜;食材搜"蔬菜类"(类目)出该类目及子类目全部食材。整词命中分类→按分类筛模式(头部提示+不显新建行)。菜品:DishesViewModel `_searchCuisine`+searchNow菜系整词命中(排"其他"泛词)·DishSearchOverlay 泛化 classifyTitle。食材:IngredientPickerVM setKeyword 命中类目名(dimension≠care)→expandCategoryIds+listByCategories·SearchResultsPanel 加 categoryName 头部。Google审查无阻断(采纳:排"其他"泛词+清searchResults同步清searchCategoryName)。
3. ✅ **AI 推荐页界面精简（方案A）**（待提交·构建绿+审查中）：RecommendControls 精简为只留"餐次"横滚 PrimaryTabRow(与一级模式实心 SegmentedControl 天然视觉区分)+右侧「筛选」入口(有非默认筛选显"筛选 ●");去重周期/风格/食养(药膳)三段搬进新 `RecommendFilterSheet`(ModalBottomSheet·复用同样即时回调·底部"重置为默认")。默认屏控件 ~264dp→~70dp、功能零丢失。VM 逻辑零改。落 `AiRecommendScreen.kt`。（**三模式统一**:库存/随机共用同一 RecommendControls+筛选壳;周期计划本就是独立 AiPlanBody·不塞餐次/筛选。）
4. ⬜ **菜品页餐次升为主分类**：餐次要和菜系并列成主分类(现餐次是全局二级栏、但菜系Tab有左侧栏让它看着像嵌套)·需 Apple-UX 出布局。

## 主线路线图（打磨批清完接着跑·`feature/一期收官路线图与决策清单.md`）
阶段1 换一换第二批(会诊已修正·忌口红线/关思考已提前做) → 阶段2 首页推荐下一餐 → 阶段3 账号匿名统计(**友盟**·用户定) → 阶段4 成员化红绿灯+病种切换视角 → 阶段5 中式库/营养补全+药膳一期+热量国标DRIs对照 → 阶段6 AI框架(放开限制+AI对话·默认关供测试·放开限制先会诊出方案) → 阶段7 其余P1。

## 长期开放决策(用户已定·`待拍板队列.md`)
- 孕期(care_pregnancy)/哺乳(care_lactation)不评热量·热量+活动档对照中国DRIs2023(联网核准不编造)·RANDOM崩溃必修(食材库将增大)·拍照/扫码系→二期(涉三方SDK+成本)。
</content>
