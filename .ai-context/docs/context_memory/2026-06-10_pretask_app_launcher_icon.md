# 任务前上下文快照：设置应用启动图标

- 时间：2026-06-10
- 用户需求：将 `/Users/sxd/Downloads/ico_cookbook_logo.png` 加入项目合适位置，并设置为 App 启动图标。
- 任务类型：Feature / 资源配置。
- 执行深度：轻量。
- 交互模式：常规。
- 计划角色：主线程模拟 DEV_UI 处理 Android 图标资源，DEV_TEST 构建验证，DEV_REVIEW 检查资源引用。
- 已知项目状态：KMP 项目，Android App 位于 `androidApp`，启动图标应通过 Android `mipmap`/adaptive icon 与 Manifest 配置。
- 预计涉及文件：`androidApp/src/main/AndroidManifest.xml`、`androidApp/src/main/res/mipmap*`、可能的 `drawable` 或 `values` 资源。
- 主要风险：PNG 尺寸/透明边距不适配 launcher；直接覆盖现有资源可能影响不同密度显示；manifest 当前可能已有 label/icon 配置。
- 待验证项：资源能被 AAPT 识别，`./gradlew :androidApp:assembleDebug` 通过。
