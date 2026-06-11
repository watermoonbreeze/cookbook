# 应用启动图标设置结果

- 时间：2026-06-10
- 用户需求：使用 `/Users/sxd/Downloads/ico_cookbook_logo.png` 设置 App 启动图标。
- 源图信息：PNG，1024x1024，RGBA。
- 实现：生成 `mipmap-mdpi`、`mipmap-hdpi`、`mipmap-xhdpi`、`mipmap-xxhdpi`、`mipmap-xxxhdpi` 下的 `ic_launcher.png`、`ic_launcher_round.png`、`ic_launcher_foreground.png`。
- 实现：新增 `mipmap-anydpi-v26/ic_launcher.xml` 和 `ic_launcher_round.xml`，支持 Android 8+ adaptive icon。
- 实现：新增 `values/colors.xml` 中的 `ic_launcher_background`，作为 adaptive icon 背景色。
- 实现：`AndroidManifest.xml` 的 `application` 增加 `android:icon="@mipmap/ic_launcher"` 和 `android:roundIcon="@mipmap/ic_launcher_round"`。
- 验证：`./gradlew :androidApp:assembleDebug` 成功。
- 备注：本次仅配置 Android 启动图标；未修改 iOS AppIcon。
