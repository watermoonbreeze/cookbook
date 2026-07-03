#!/usr/bin/env bash
# =====================================================================
# [AI生成] CLI 双轨构建入口（macOS/Linux）
# 用途：不依赖全局 ~/.gradle/gradle.properties 与 IDE 设置，
#       显式用 JDK 17 启动 Gradle（AGP 8.2.2 要求 JDK 17 运行）。
# 用法：./scripts/build-cli.sh :androidApp:assembleDebug
# 换机时如自动探测失败，修改下方 CLI_JDK_HOME 为本机 JDK 17 路径。
# =====================================================================
set -e

CLI_JDK_HOME=""

# macOS 自动探测 JDK 17
if [ -z "$CLI_JDK_HOME" ] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
    CLI_JDK_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
fi

if [ -n "$CLI_JDK_HOME" ] && [ -x "$CLI_JDK_HOME/bin/java" ]; then
    export JAVA_HOME="$CLI_JDK_HOME"
else
    echo "[build-cli] 警告: 未找到 JDK 17，回退使用当前 JAVA_HOME=${JAVA_HOME:-未设置}"
fi

cd "$(dirname "$0")/.."
exec ./gradlew "$@"
