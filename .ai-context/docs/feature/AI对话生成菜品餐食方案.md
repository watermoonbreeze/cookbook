# 对话式 AI 交流 → 生成菜品/餐食 → 确认入库 · 方案

> [AI生成] 2026-07-19 ai_engineer 会商。用户诉求：点"对话"按钮→带家庭成员+忌口基础信息→AI给引导→用户问(如夏天清热食材+营养)→AI列食材+按健康/忌口加建议→选食材出做法成菜品/生成一天几天餐食→确认入库。仅方案不改代码。

## 0. 定位与关键结论
**本对话功能 = "放开AI推荐限制·云端档"的会话式外壳**：把已定的"云端只给健康约束+忌口·食材/菜自由推·缺的自动建库"从一次性问答升级为多轮对话+挑选+确认入库。**复用**现有自动建库链(`DishNameIngredientGuesser`+`createUserIngredient`+`NutritionGuesser`+`saveDish`/`saveDayMeals`),不重造。
- **🔴前置基建缺口**：现 `AiRuntime.complete(system,user)` 是单轮双消息,多轮对话必须先扩 `LlmRequest` 加 `history: List<ChatTurn>` + `GlmProtocol` messages 拼 `[system]+history+[user]`(向后兼容·单轮推荐 history空)。
- **三条红线(代码校验层·非提示)**：①入库前必过忌口校验 ②用户显式确认才写库 ③新食材营养走本地 `NutritionGuesser` 估算标"待核"·**模型编的营养数值一律丢弃不采信**。

## 1. 入口与基础信息注入
- 入口(P1)：**AI 推荐页顶**"和 AI 聊聊吃什么"(不新增Tab·与"AI推荐下一餐"同心智共用取数)。
- 注入(守红线·敏感明细不上云)：给模型=家庭规模+年龄段粗标签(有老人/儿童·不传具体年龄身高体重)+健康关注粗标签("关注:高血压"·不传血压值)+忌口食材名清单+做法层限量("少盐少油")；**不给模型**=精确 avoidIds(本地校验用)+BodyMetrics明细。新增 `ChatContextBuilder`(shared纯函数·复用 RecommendationDataSource 口径)产出"给模型粗标签串+本地校验集"。

## 2. AI 引导(进对话先教会用·本地渲染不调云端)
欢迎卡=角色说明+**示例问法芯片**(点击填入·"夏天清热解暑食材""用鸡蛋番茄做什么菜""排明天三餐""痛风能吃的家常菜"动态替换关注病种)+免责一行+已注入信息摘要("已带上:3人·关注高血压·忌口五花肉")。P2 文案需 copywriter。

## 3. 多轮对话架构
- 3.1🔴基建:AiRuntime 多轮 messages(见§0)。
- 3.2 会话状态机 `ChatSessionState`:阶段 CHAT(讲解)/INGREDIENTS(列食材芯片可选)/DISH(菜品预览卡→加入库CTA)/PLAN(餐食计划预览→记入CTA)。
- 3.3 结构化输出(P3)：**一期用 JSON强约束+intent字段路由**(GLM-4.5-flash 已支持 json_object·`supportsJsonMode`本轮已接·比 function-calling 省)。模型每轮输出统一信封 `{intent:chat/ingredients/dish/plan, reply, ingredients[], dish{}, plan{}}`。新增 `ChatResponseParser`(仿 RecommendationParser 容错·缺字段降级只显 reply·intent非法当chat)。
- 3.4 system prompt:角色前置+**硬约束前置**(忌口"绝不含"·医疗红线"禁降压/治疗/达标·不承诺疗效"·免责)+能力说明+输出格式+自由度("食材菜不限本地库")。
- 3.5 token预算:历史裁剪最近3~4轮+rolling summary(本地拼)·选食材不回灌整列表·system每轮带但精简。

## 4. 生成→确认→入库(复用现有链)
- 菜品(dish):模型给名+食材名+做法→本地按名对齐库存食材(有用id·无标"新食材将自动创建")→**预览卡**(忌口命中标红)→用户点"加入菜品库"→库外食材 `createUserIngredient`+`NutritionGuesser` 估算标待核·菜 `saveDish`·**归一名去重**(已有同名提示)。
- 餐食计划(plan·二期):相对日期"明天"本地转 LocalDate(不让模型算)·每菜先入库拿 dishId·按天餐次预览→确认→`saveDayMeals(bumpPreference=false)`(P4:AI排的餐≠吃过·不抬喜爱度)。
- 预览卡=提交前编辑态(可删菜/改份数·复用 MiniStepper/ActionSheet·防幻觉入库最后人工闸)。

## 5. 红线(代码校验层·非提示)
①忌口:入库前过 HealthRuleEngine 忌口校验·命中标红+阻止直接入库(二次确认放行·告知不隐藏) ②不编造营养:新食材营养只走 NutritionGuesser·模型营养数值丢弃·营养讲解仅对话展示不写库+本地拼免责 ③用户确认才入库(全程不静默写) ④非医疗断言:prompt硬约束+**本地后置敏感词扫描** `MedicalClaimGuard`(降压/降糖/治疗/达标…命中拦截/附免责·可反哺推荐链) ⑤成本:欢迎/免责本地渲染·历史裁剪·纯本地操作不调云端·云端失败降级"AI暂不可用·试规则推荐"不硬崩。

## 6. 分阶段(MVP→增强)
- **MVP**(先验证价值+红线):①🔴基建多轮 messages ②单入口对话页+本地欢迎/示例/免责 ③CHAT+INGREDIENTS(问营养/列食材) ④**DISH生成+确认入库**(忌口校验+自动建库·红线①②③) ⑤云端失败降级+埋点。**MVP不做PLAN**(多天最复杂红线面最大·二期)。
- 二期:PLAN多天餐食生成批量记入·多轮上下文优化·对话结果打通AI推荐候选池。
- 三期:语音输入·对话历史持久化回看·常聊菜沉淀"AI菜谱本"。

## 7. 与"放开AI推荐限制"关系
本对话=放开限制·云端档最完整形态。内核(约束注入切分+自动建库链)复用。差异:现"AI推荐下一餐"一次性无交互放开·对话多轮可挑选可编辑可确认。**基建共建(ChatContextBuilder+多轮messages+自动建库+红线校验共用)·UI分两形态**(推荐用 RecommendationOrchestrator从候选选·对话用新 ChatOrchestrator自由生成)。

## 拍板点
P1入口(荐AI推荐页顶)·P2欢迎/示例文案(copywriter)·P3一期JSON-intent(荐)vs function-calling·P4 AI排餐不抬喜爱度(荐)·忌口命中"标红+二次确认放行"(荐)。**对话页整体交互需 Apple-UX 先出交互稿**(气泡/示例芯片/食材多选卡/菜品预览卡/确认CTA+Snackbar·复用§九)。

## 新增模块(均 shared 纯函数可测)
`ChatContextBuilder`(约束注入切分)·`ChatOrchestrator`(对话编排)·`ChatResponseParser`(信封解析容错)·`MedicalClaimGuard`(敏感词后置扫描)。基建改 AiRuntime(LlmRequest+history)/GlmProtocol(messages)。入库复用 DishNameIngredientGuesser/NutritionGuesser/createUserIngredient/saveDish/saveDayMeals·忌口复用 HealthRuleEngine。
