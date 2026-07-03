# CLI 构建使用说明

> [AI生成] 2026-07-03。本项目统一用命令行构建（AS Hedgehog 打开工程报模块实体错误，IDE 不作为构建路径）。
> 原理：AGP 8.2.2 要求 Gradle 跑在 JDK 17 上，`build-cli` 脚本会显式设置 JDK 17 后再调用 gradlew，不依赖全局配置。

## 常用命令（在项目根目录执行）

| 目的 | 命令 |
|---|---|
| **打 Debug APK（最常用）** | `scripts\build-cli.bat :androidApp:assembleDebug` |
| 打 Release APK（当前未配签名，产物未签名） | `scripts\build-cli.bat :androidApp:assembleRelease` |
| 跑 shared 单元测试 | `scripts\build-cli.bat :shared:testDebugUnitTest` |
| 构建 shared 模块 | `scripts\build-cli.bat :shared:build` |
| 清理构建产物 | `scripts\build-cli.bat clean` |
| 查看 Gradle/JVM 版本（验证环境） | `scripts\build-cli.bat --version`（JVM 应显示 17.x） |

macOS/Linux 把 `scripts\build-cli.bat` 换成 `./scripts/build-cli.sh`，任务名相同。

## 产物位置

- Debug APK：`androidApp\build\outputs\apk\debug\androidApp-debug.apk`
- Release APK：`androidApp\build\outputs\apk\release\`
- 单元测试报告：`shared\build\reports\tests\testDebugUnitTest\index.html`

## 装到手机

```bat
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

## 换机 / 环境排查

1. 脚本顶部 `CLI_JDK_HOME` 写死了本机 JDK 17 路径（`C:\Program Files\Java\jdk-17`），换机后如路径不同改这一行即可；找不到时脚本会警告并回退用当前 `JAVA_HOME`。
2. 构建报 JVM/版本类错误时，先跑 `scripts\build-cli.bat --version` 确认 JVM 是 17.x。
3. 构建行为怪异（卡住、缓存错乱）时：`scripts\build-cli.bat --stop` 停掉 daemon 后重试，必要时再 `clean`。
4. 不得为"适配环境"升级 Gradle/AGP/Kotlin 版本（见 `.ai-context/rules/通用规则.md` 第八节）。
