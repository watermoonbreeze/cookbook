# K1「AI 快捷输入记餐」方案设计

> 2026-07-28 · 四角色会商（UX+产品+架构+算法）→ 架构师汇总
> 状态：📄 方案待审核 → 拍板后编码

---

## 一、总览

用户通过自然语言（文字/语音）一句话记一餐，AI 提取结构化信息 → 预览确认 → 入库。

```
用户输入（文字/语音）
  │
  ├─ [语音] Android SpeechRecognizer → 文字填入输入框（可编辑）
  └─ [文字] 直接在输入框打字
       │
       ▼ 点"发送"
  ┌─────────────┐     失败/无Key
  │ AI 解析(云端)│ ──────────────────┐
  └──────┬──────┘                   ▼
         │ 成功              ┌──────────────┐
         ▼                   │ 本地规则兜底   │
  ┌──────────────┐           │ (正则+关键词)  │
  │ JSON 校验     │◄─────────┤               │
  └──────┬───────┘           └──────────────┘
         ▼
  ┌──────────────┐
  │ 预览确认 Sheet │ ← 单餐次/T1=Snackbar直存
  └──────┬───────┘     多餐次/不完整=T2预览弹框
         ▼
  ┌──────────────┐
  │ 入库 + 撤销   │
  └──────────────┘
```

---

## 二、JSON Schema（AI 输出约定）

```kotlin
// shared/.../ai/meallog/AiMealInputSchema.kt

@Serializable
data class AiMealParseResult(
    val date_offset: Int = 0,           // -2=前天, -1=昨天, 0=今天, 1=明天
    val meals: List<AiParsedMeal>       // 至少 1 项
)

@Serializable
data class AiParsedMeal(
    val meal_type: String? = null,      // "breakfast"/"lunch"/"dinner"/"snack"；null=按时间推断
    val meal_time: String? = null,      // "HH:MM"；null=餐次默认时间
    val note: String = "",              // 整餐备注
    val dishes: List<AiParsedDish>      // 至少 1 项
)

@Serializable
data class AiParsedDish(
    val name: String,                   // 菜名，必填
    val quantity: Double = 1.0,         // 份数
    val quantity_unit: String = "份",   // "碗"/"盘"/"份"/"个"
    val eaten_ratio: Double? = null,    // null=吃完；0.5=一半；0.25=少量
    val cooking_methods: List<String> = emptyList(),
    val note: String = "",
    val ingredients: List<AiParsedIngredient> = emptyList()  // AI 推断的食材（可选）
)

@Serializable
data class AiParsedIngredient(
    val name: String,                   // 食材名
    val quantity: Double = 100.0,
    val unit: String = "g",
    val is_main: Boolean = true
)
```

**示例**：
```json
// "昨天中午吃了红烧肉和米饭，吃了一半，少放盐"
{"date_offset":-1, "meals":[{"meal_type":"lunch", "note":"少放盐",
  "dishes":[{"name":"红烧肉","eaten_ratio":0.5},{"name":"米饭","eaten_ratio":0.5}]}]}

// "刚吃了一碗牛肉面"
{"date_offset":0, "meals":[{"meal_type":null,
  "dishes":[{"name":"牛肉面","quantity":1,"quantity_unit":"碗","cooking_methods":["煮"]}]}]}
```

---

## 三、模块归属

```
shared/commonMain/.../ai/meallog/
├── AiMealInputSchema.kt     // @Serializable 数据类
├── AiMealPrompt.kt          // system + user prompt 构建（纯函数）
├── AiMealParser.kt          // JSON 解析 + 校验 + 本地兜底（纯函数）
└── AiMealRecorder.kt        // UseCase：解析→菜品匹配/创建→入库编排

androidApp/.../ui/ai/
├── AiMealInputSheet.kt      // 输入 + 预览 Composable
└── AiMealInputViewModel.kt  // 状态机：输入→解析中→预览→保存
```

| 决策 | 归属 | 理由 |
|------|------|------|
| Schema + Prompt + Parser | **shared** | 纯逻辑无平台依赖，可单测，iOS 未来可复用 |
| AiMealRecorder (入库编排) | **shared** | 调已有 Repo，Domain 层逻辑 |
| ViewModel + UI | **androidApp** | 平台原生，准则 B |

**复用清单**：
- AI 调用 → 复用 `SwitchableAiRuntime.complete()`，不新建
- 建菜品 → 复用 `DishRepository.saveDish()`
- 建食材 → 复用 `IngredientRepository.createUserIngredient()`（同名自动复用）
- 营养推断 → 复用 `NutritionGuesser`（三级：同名Match → 大类Group → 不填）
- 记餐入库 → 复用 `MealRecordRepository.saveDayMeals()`
- 撤销 → 复用 `deleteDayMeals()` + 快照还原（§9.12）

---

## 四、AI Prompt 设计（算法产出）

### System Prompt 核心规则

```
你是家庭饮食记录助手。从自然语言提取结构化 JSON。

规则：
1. 日期偏移：未提→0；昨天→-1；前天→-2；明天→1
2. 餐次：早餐/早饭→breakfast；午餐/中饭/中午→lunch；
   晚餐/晚饭→dinner；加餐/宵夜/零食→snack；未提→null
3. 菜品拆分：分隔符 "和" "跟" "还有" "+" "、" "，" → 多道菜
   "三个鸡蛋" = 1道菜×3个（非3道菜）
4. 份量：一碗→quantity=1/unit=碗；两盘→2/盘
5. 食用比例：吃了=默认吃完；一半→0.5；大半→0.75；少量→0.25
6. 烹饪方式：煮/蒸/炒/炸/煎/烤/炖/拌
7. 备注：整餐通用归 meal.note；单菜专属归 dish.note
8. 只输出 JSON，不要多余文字
```

### 调用参数

| 参数 | 值 | 理由 |
|------|-----|------|
| temperature | 0.2 | 确定性抽取任务 |
| jsonMode | true | 强制 JSON 输出 |
| maxTokens | 1024 | 一餐足够 |
| thinking | disabled | 不浪费 token 预算 |

### User Prompt 模板

```
用户说：${input}
当前日期：${today}（${weekday}）
当前时间：${nowTime}
```

---

## 五、本地规则兜底（算法产出）

当 AI 不可用（无Key/网络失败/JSON非法）时走本地解析器 `AiMealParser.localFallback()`。

### 解析步骤

1. **日期**：正则匹配"昨天/昨"→-1，"前天"→-2，其他→0
2. **餐次**：正则匹配关键词表 → meal_type
3. **菜品分隔**：按 `、` `，` `,` `+` 硬分隔；按 `和` `跟` `还有` 软分隔（两端都能独立成菜名则拆）
4. **份量**：`(\d+)\s*(个|碗|份|盘|碟|只|根|片|块|勺)` → quantity + unit
5. **食用比例**：正则匹配 → eaten_ratio
6. **备注**：剩余文本作为 note

### 覆盖率估算

典型家庭输入中 **65-75%** 可由本地规则覆盖（"中午吃了红烧肉""鸡蛋、米饭""三个包子"等短句）。长句/复合句（"昨天中午在公司食堂吃了红烧牛肉面，下午又加了两个橘子"）需 AI 处理。

---

## 六、自动创建策略（产品 + 算法共识）

### 决策矩阵

| 场景 | 策略 | 告知方式 |
|------|------|---------|
| AI 提取的菜名在库中存在 | 直接用已有 Dish | 无额外告知 |
| AI 提取的菜名在库中**不存在** | 自动创建 Dish（含 AI 拆的食材），source="ai" | Snackbar "已创建「X」，营养为估算" + 撤销 |
| AI 拆的食材在库中**不存在** | 自动创建 Ingredient（source="ai"，归入家庭分类），营养走 NutritionGuesser 大类均值 | 同上 |
| AI 未返回食材（本地规则/无食材信息） | Dish 仅 name（空壳），营养标"待完善" | 同上 |

### ✅ 决策结论（2026-07-28 用户拍板）

- **D1**：AI 自动创建食材 → `source="ai"`，归入家庭分类，和自建功能一样只是来源标识不同
- **D2**：始终预览 Sheet（即使单餐次全命中），用户看菜品对不对 + 营养元素
- **D3**：菜品整菜记录（如"牛肉面"），食材拆分为牛肉+面条按正常分量设g数
- **D4**：同一餐次再输入追加合并到已有块

**营养推断优先级**：
1. AI 提供了营养值 → 用 AI 值，标"AI估算"
2. NutritionGuesser 同名命中已有食材 → 用已有值，标"参考{refName}"
3. NutritionGuesser 大类均值兜底 → 用均值，标"按{大类}粗略估算·请核对"
4. 都不确定 → 不填，标"营养待完善"

---

## 七、交互设计（UX 产出）

### 入口

`AddDayFoodScreen` 顶栏加 `✨ AI快捷记` 按钮（`Icons.AutoAwesome` + 文字，actions 槽）。始终可见，不因 Key 未配隐藏。

### 输入 Sheet

底部 `ModalBottomSheet`，占屏 ~0.75：

- 标题"AI 快捷记一餐"
- `SegmentedControl` 切换 文字/语音
- 文字模式：多行输入框 + 引导示例 + "发送" CapsuleButton
- 语音模式：圆形 mic 按钮（点按模式：点→开始→再点→结束→自动填入文字框→可编辑→发送）

### 解析中

三个圆点动画 + "AI 正在理解你的输入..." + 取消按钮。>15s 超时自动降级。

### 预览确认

解析完成 → Sheet 切换为预览：
- 日期行（可点改）
- 每餐次一卡片：餐次名+时间 + 菜品列表（MiniStepper 调份数）+ 备注
- 自动新建提示条（如有新菜/食材）
- "确认记下" CapsuleButton + "重新输入" TextButton

### 透明分级

| 场景 | Tier | 方式 |
|------|------|------|
| 解析成功（含单餐次） | T2 | **始终预览 Sheet**——用户看菜品对不对 + 营养元素，确认后保存 |
| 解析失败 | T1 | Toast + 输入框保留文字，可修改重试 |

---

## 八、入库流程（架构产出）

### AiMealRecorder.record()

```
Phase 0: date = today + result.date_offset

Phase 1: 食材解析
  for each dish:
    if AI gave ingredients → use them
    else → DishNameIngredientGuesser.guessDetailed(name)
    for each ingredient:
      id = ingredientRepo.createUserIngredient(name)  // 同名复用
      nutrition = NutritionGuesser.guess(name, candidates, classify(name))
      → write nutrition (if any)

Phase 2: 菜品解析
  for each dish:
    existingId = dishRepo.dishIdByName(name)
    if exists → reuse
    else → dishRepo.saveDish(name, ingredients, source="ai")

Phase 3: 组装 Drafts → mealRepo.saveDayMeals(date, drafts)

Phase 4: 回填 eaten_ratio（非默认值）
```

### 新增依赖注入

```kotlin
// SharedModule
single { AiMealRecorder(get(), get(), get(), get()) }

// AndroidModule
viewModel { (initialText: String) -> AiMealInputViewModel(initialText, get(), get()) }
```

---

## 九、新增文件清单

| 文件 | 位置 | 职责 |
|------|------|------|
| `AiMealInputSchema.kt` | shared/.../ai/meallog/ | Schema 数据类 |
| `AiMealPrompt.kt` | shared/.../ai/meallog/ | System + User Prompt 构建 |
| `AiMealParser.kt` | shared/.../ai/meallog/ | JSON 解析 + 校验 + 本地兜底 |
| `AiMealRecorder.kt` | shared/.../ai/meallog/ | 入库编排 UseCase |
| `AiMealInputViewModel.kt` | androidApp/.../ui/ai/ | 状态机 VM |
| `AiMealInputSheet.kt` | androidApp/.../ui/ai/ | 输入+预览 Composable |

---

## 十、决策结论（2026-07-28 用户拍板 ✅）

| # | 问题 | 结论 | 实现要点 |
|---|------|------|---------|
| D1 | AI 自动创建食材？ | ✅ **创建，标 ai 源** | 食材归入"家庭"分类，`source="ai"`，和用户自建功能一样只是来源标识不同 |
| D2 | 单餐次是否预览？ | ✅ **始终预览** | 预览目的：①看菜品对不对 ②看营养元素。即使单餐次也要弹预览 Sheet |
| D3 | "牛肉面"拆不拆？ | ✅ **整菜 + 拆食材** | 菜品名="牛肉面"（整菜），食材拆为牛肉+面条按正常分量设g数 |
| D4 | 同一餐次再输入？ | ✅ **追加合并** | AI 快捷输入后该餐次已有记录→新菜追加到已有块，不新建第二条 |

---

## 十一、测试要点

- **AiMealParser 单测**：合法 JSON → 正确解析；截断/非法 JSON → 兜底；空响应 → null
- **localFallback 单测**：覆盖"中午吃了红烧肉""鸡蛋、米饭""红烧肉和青椒炒肉""三个鸡蛋""一大碗面""少盐番茄炒蛋"等典型输入
- **AiMealRecorder 单测**：食材匹配命中/新建、菜品复用/新建、营养推断链路
- **全链路 Mock**：MockAiRuntime → 成功路径/失败→兜底路径 → 入库验证
