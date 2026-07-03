# 任务前快照：食材数据承载阶段 A

- 时间：2026-06-21
- 用户需求：按照“阶段 A：补齐食材数据承载”继续开发。
- 任务类型：Feature / 数据结构补齐
- 执行深度：标准
- 角色分派：当前子代理工具要求用户显式授权才可调用，因此本次主线程模拟 DEV_SA、DEV_ARCH、DEV_DB、DEV_CODE、DEV_TEST、DEV_REVIEW。

## 已知状态

- 食材浏览器已统一到 `IngredientPickerScreen`。
- 顶部主分类、底部详情弹层、已选弹框、重复选择提示、多级分类树已完成。
- 当前 schema 已有：
  - `ingredient`
  - `food_category`
  - `ingredient_category`
  - `crowd_ingredient`
- 当前缺少：
  - 食材详情承载表
  - 食材分类关系批量保存/查询能力
  - 通用调养规则表或对 `crowd_ingredient` 的抽象扩展

## 本轮目标

- 优先做不影响现有 UI 的数据底座。
- 新增食材详情表和 Repository 模型/接口。
- 补充食材分类关系查询与重设能力，为后续新增/编辑食材全屏 Sheet 做准备。
- 评估是否本轮加入 `ingredient_care_rule`；若迁移风险可控则一并补齐空表和接口。

## 验证计划

- 如果修改 SQLDelight schema，执行 `./gradlew :shared:testDebugUnitTest`。
- 执行 `./gradlew :androidApp:assembleDebug` 确认 Android 集成。
