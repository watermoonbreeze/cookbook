# 食材阶段 B 完整编辑入口完成

- 时间：2026-06-21
- 类型：Feature
- 流程说明：标准任务；当前子代理工具需用户显式授权，本轮主线程模拟 DEV 角色执行。

## 完成内容

- 新增/编辑食材入口由简单 AlertDialog 升级为全屏 Dialog 表单。
- 表单支持基础信息：
  - 食材名称
  - 别名
  - 图片
  - 默认单位
- 表单支持分类归属：
  - 常规分类多选
  - 营养标签多选
- 表单支持调养建议：
  - 调养分类
  - 推荐/限量/避免
  - 原因说明
- 表单支持详情说明：
  - 常见做法
  - 处理建议
  - 食用注意
  - 保存建议
  - 健康说明
- 编辑已有食材时会加载已有分类、详情和调养规则。
- 保存时会写入阶段 A 新增的数据底座：
  - `ingredient_category`
  - `ingredient_detail`
  - `ingredient_care_rule`
- 创建新食材后仍会自动加入当前已选食材，保持菜品选择流程不变。
- 已更新 `.codex/docs/feature/食材体系重构总方案.md` 阶段 3 状态。

## 涉及文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerViewModel.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/data/repository/IngredientRepository.kt`
- `shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/Cookbook.sq`
- `.codex/docs/feature/食材体系重构总方案.md`

## 验证

- `./gradlew :shared:testDebugUnitTest`：BUILD SUCCESSFUL
- `./gradlew :androidApp:assembleDebug`：BUILD SUCCESSFUL

## 后续

- 阶段 C：升级食材详情展示，读取并展示分类、详情说明和调养建议。
- 阶段 D：结构化 seed，把 `日常食材维度.md` 逐步转为可维护数据。
