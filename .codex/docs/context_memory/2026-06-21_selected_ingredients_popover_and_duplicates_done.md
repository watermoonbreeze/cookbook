# 已选食材弹框与重复食材处理完成

- 时间：2026-06-21
- 类型：Feature / BugFix
- 流程说明：按标准任务执行；当前会话没有真实 `multi_agent_v1.spawn_agent`，已降级为主线程模拟 DEV 角色。

## 完成内容

- 底部“已选 X 项”支持点击打开左下角跟随弹框。
- 跟随弹框竖直展示所有已选择食材，点击外部自动关闭。
- 点击跟随弹框内食材会打开同一个底部食材详情弹层。
- 食材详情弹层左侧按钮从“取消”改为“关闭”。
- 详情弹层右侧按钮按选择状态显示：
  - 未选择：`选择`
  - 已选择：`取消选择`
- 点击“取消选择”会立即从已选列表删除该食材。
- 菜品页选择食材后，如果包含已存在食材，会弹出确认框提示重复食材名称。
- 确认后不会重复添加已有食材，只追加本次选择中的新增食材。

## 涉及文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/newdish/NewDishScreen.kt`

## 验证

- 已执行 `./gradlew :androidApp:assembleDebug`
- 结果：BUILD SUCCESSFUL
