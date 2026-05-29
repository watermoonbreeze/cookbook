# 2026-05-24 图片展示与食材长按菜单记录

- 新增 `StoredImage` 通用组件：读取 `image_path` 中 `|` 分隔的第一张图片 URI，真实渲染图片；失败或无图时显示默认 emoji/文字。
- 菜品卡片、菜品列表行、菜品详情、食材选择项、图片选择区均接入真实图片展示。
- 菜品/食材多图仍使用现有 `image_path` 字段，第一张作为默认展示图。
- 食材选择项增加可见来源提示：
  - `source=user`：显示“自建 · 长按编辑”，长按弹出编辑/删除菜单。
  - `source=preset`：显示“预设”，不可编辑、不可删除，也不响应长按菜单。
- Repository 和 SQL 层均限制：只有 `source='user'` 的食材允许编辑/删除。
- 默认预设食材无拍摄图片时展示 emoji 图标；拍摄/选择照片后展示真实图片。
- 验证命令：`./gradlew :androidApp:compileDebugKotlin`、`./gradlew :androidApp:assembleDebug`，均通过。
