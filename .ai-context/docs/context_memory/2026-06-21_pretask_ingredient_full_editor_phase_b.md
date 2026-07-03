# 任务前快照：食材阶段 B 完整编辑入口

- 时间：2026-06-21
- 用户需求：继续阶段 B，把新增/编辑食材入口升级为完整表单。
- 任务类型：Feature
- 执行深度：标准
- 角色分派：当前子代理工具要求用户显式授权才可调用，本轮主线程模拟 DEV_SA、DEV_ARCH、DEV_UI、DEV_CODE、DEV_TEST。

## 已知状态

- 阶段 A 已完成 `ingredient_detail`、`ingredient_care_rule` 和 Repository 数据接口。
- 食材浏览器已统一在 `IngredientPickerScreen`。
- 当前新增/编辑食材仍主要是简单 AlertDialog。
- 当前选择器已有顶部主分类、底部详情弹层、已选弹框、多级分类树。

## 本轮目标

- 将添加/编辑食材改为更完整的 Sheet/Dialog 表单。
- 支持基础信息：名称、别名、图片、默认单位。
- 支持常规/营养/调养分类选择。
- 支持调养规则等级与原因。
- 支持做法/处理/保存/注意事项等详情字段。
- 保持现有选择器和菜品选择流程可用。

## 风险与验证

- 风险：表单字段多，容易引入 Compose 状态混乱。
- 风险：编辑旧食材时需要正确加载已有分类、详情和调养规则。
- 验证：`./gradlew :shared:testDebugUnitTest`、`./gradlew :androidApp:assembleDebug`。
