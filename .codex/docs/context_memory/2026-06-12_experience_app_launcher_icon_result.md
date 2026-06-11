# 应用启动图标经验总结结果

- 时间：2026-06-12
- 用户需求：总结本次为 Android App 添加启动图标的经验。
- 写入位置：`.codex/docs/experience/05_UI组件.md`。
- 新增章节：`Android 启动图标配置`。
- 经验要点：图标资源应放在 `androidApp/src/main/res/mipmap-*`；多密度普通图标、圆形图标和 Android 8+ adaptive icon XML 需要配套。
- 经验要点：`application` 需要配置 `android:icon` 与 `android:roundIcon`。
- 经验要点：配置后至少运行 `./gradlew :androidApp:assembleDebug` 验证资源和 Manifest 引用。
- 边界说明：本经验只覆盖 Android；iOS AppIcon 需要单独维护。
