# 食材数据承载阶段 A 完成

- 时间：2026-06-21
- 类型：Feature / 数据结构补齐
- 流程说明：标准任务；子代理工具需用户显式授权，本轮主线程模拟 DEV 角色执行。

## 完成内容

- 新增 `ingredient_detail` 表，用于承载：
  - 常见做法
  - 处理建议
  - 食用注意
  - 保存建议
  - 通用健康说明
- 新增 `ingredient_care_rule` 表，用于承载通用调养规则：
  - 调养分类节点
  - 推荐/限量/避免
  - 原因说明
- 新增迁移 `shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/8.sqm`。
- 新增领域模型：
  - `IngredientDetail`
  - `IngredientCareRule`
- `IngredientRepository` 新增接口：
  - `listCategoryIds`
  - `replaceIngredientCategories`
  - `saveIngredientDetail`
  - `getIngredientDetail`
  - `replaceCareRules`
  - `listCareRules`
- 补充单元测试覆盖：
  - 多分类关系整体替换
  - 食材详情保存/读取
  - 调养规则整体替换/读取
- 更新文档：
  - `.ai-context/docs/feature/数据库设计方案.md`
  - `.ai-context/docs/feature/食材体系重构总方案.md`

## 验证

- `./gradlew :shared:testDebugUnitTest`：BUILD SUCCESSFUL
- `./gradlew :androidApp:assembleDebug`：BUILD SUCCESSFUL

## 下一步

- 开始阶段 B：把“添加/编辑食材”从简单弹框升级为完整全屏 Sheet 或独立页面。
- 新页面直接接入本轮新增的数据接口，支持多分类、调养规则和详情字段。
