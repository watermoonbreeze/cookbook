# 2026-06-03 任务前快照：修复11 菜品刷新、编辑加载、DB/图片外部存储

## 用户最新需求
- 添加菜品后 `菜品Screen` 没有及时刷新，菜品 tab 下列表增加下拉刷新。
- 编辑菜品标题是“编辑菜品”，但内容为空，说明没有按 id 加载菜品信息。
- 数据库迁移到 sdCard 根目录 `cookbook/db/`。
- 拍摄照片默认存储到 `cookbook/img/`，文件名 `yyyyMMddHHmmssS.jpg`。
- 同时生成缩略图 `yyyyMMddHHmmssS_thum.jpg`，保持图片尺寸，压缩到 5K 左右且不超过 10K。
- 菜品和食材表需要增加缩略图字段。
- 列表/默认展示使用缩略图；点击放大时优先加载缩略图，再加载原图，实现模糊到清晰。

## 执行模式
- 深度级 BugFix + 数据/存储迁移。
- 原因：跨 UI 刷新、导航参数、SQLDelight schema/migration、Android 外部存储和图片显示策略。

## 计划分派
- DEV_SA：梳理菜品新增返回刷新、编辑路由参数、ViewModel 状态复用链路。
- DEV_DB：梳理 SQLDelight 表字段、迁移文件、数据库 driver 路径配置。
- DEV_CODE/DEV_UI：主线程落地修复和 UI 下拉刷新。
- DEV_TEST：执行 SQLDelight 生成、Android 编译、assemble 与测试任务。

## 当前已知状态
- DB 版本已设置为 2，对应 `1.sqm` v1->v2。
- 菜品编辑页之前已新增 `NewDishViewModel.start(editingDishId, importDishId)`，但仍有空内容问题，可能是路由 id、查询或状态重置竞态。
- 图片目前已有压缩/展示组件，需要检查 `StoredImage`、`ImagePickerButton`、Repository 保存字段。

## 预计涉及文件
- `androidApp`：菜品列表、菜品编辑、导航、图片组件、Android driver/provider。
- `shared`：`Cookbook.sq`、`.sqm` migration、Dish/Ingredient model、Repository。

## 风险
- 外部存储路径在 Android 10+ scoped storage 下可能需要用 app-specific external files 目录或权限兼容。
- DB 文件迁移不能丢旧数据，需要从旧内部 DB 拷贝到新目录后再打开。
- 新增缩略图字段需要提升 SQLDelight version，并保留旧字段兼容。

## 待验证
- 新增菜品返回后菜品页刷新。
- 编辑菜品能加载已有数据。
- SQLDelight 生成和迁移编译通过。
- Android 打包通过。
