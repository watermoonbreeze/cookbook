# 任务前快照：已选食材弹框与重复食材处理

- 时间：2026-06-21
- 用户需求：
  - 点击底部“已选 X 项”时，在左下角弹出竖直跟随弹框，展示所有已选择食材；点击弹框外关闭；点击弹框内食材打开详情。
  - 食材详情弹框左侧“取消”改为“关闭”；右侧按钮根据选择状态显示“选择”或“取消选择”，取消选择后从已选中移除。
  - 菜品界面选择食材后，如存在已添加食材，弹出确认提示；确定后去重，仅新增未重复食材。
- 任务类型：Feature / BugFix
- 执行深度：标准
- 角色分派：因当前会话没有真实 multi_agent_v1.spawn_agent 工具，主线程模拟 DEV_SA、DEV_ARCH、DEV_UI、DEV_CODE、DEV_TEST、DEV_REVIEW。

## 已知项目状态

- 食材浏览与菜品选择食材已统一使用 `IngredientPickerScreen`。
- 详情已经是底部 65% 弹层。
- 底部已选栏仅在选择模式且已选非空时出现。

## 预计涉及文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerViewModel.kt`
- 菜品新增/编辑页中打开食材选择器并接收结果的文件，待定位。

## 风险与验证

- 风险：重复处理要避免改变已有菜品食材用量、备注等编辑信息。
- 风险：弹框与详情弹层同时存在时需要关闭顺序明确，避免遮挡或状态残留。
- 验证：执行 `./gradlew :androidApp:assembleDebug`。
