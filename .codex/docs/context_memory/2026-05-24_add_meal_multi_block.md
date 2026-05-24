# 2026-05-24 添加餐食多模块改造记录

- 添加餐食页从“单餐保存”改为“一天多个餐食模块批量保存”。
- `AddMealViewModel` 新增 `MealBlockUiState`，每个模块含餐次、用餐时间、菜品列表和备注。
- `MealRecordRepository` 新增 `DayMealDraft` 与 `saveDayMeals`，逐条复用原 `save` 逻辑，保持菜品热度累加规则一致。
- 日期使用 Material3 `DatePickerDialog`，时间使用 `TimePicker`；固定餐次自动带出默认时间，`加餐` 为非固定餐次，UI 要求手动选择时间。
- `PresetDataSeeder` 会补充 `SNACK/加餐` 餐次，兼容已有数据库。
- 添加餐食页的菜品选择器开启“添加菜品”入口，跳到新建菜品页返回后刷新菜品库列表，便于选择刚创建的菜品。
- 食材选择器的新建食材弹框增加可选图片路径，食材卡片改为无外边距色块风格。
- 验证命令：`./gradlew :androidApp:assembleDebug` 与 `./gradlew :androidApp:compileDebugKotlin`，均通过。
