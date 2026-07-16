#!/usr/bin/env bash
# =====================================================================
# [AI生成] CLI 双轨构建入口（macOS/Linux）
# 用途：不依赖全局 ~/.gradle/gradle.properties 与 IDE 设置，
#       显式用 JDK 17 启动 Gradle（AGP 8.2.2 要求 JDK 17 运行）。
# 用法：./scripts/build-cli.sh :androidApp:assembleDebug
# 换机时如自动探测失败，修改下方 CLI_JDK_HOME 为本机 JDK 17 路径。
# =====================================================================
set -e

# 换机时如自动探测都失败，把 CLI_JDK_HOME 手填为本机 JDK 17 根目录（含 bin/java）。
CLI_JDK_HOME=""

# ① macOS 官方探测：任意厂商(Temurin/Zulu/Oracle/Corretto)注册到系统的 JDK 17 都能找到，多个取最新。
if [ -z "$CLI_JDK_HOME" ] && command -v /usr/libexec/java_home >/dev/null 2>&1; then
    CLI_JDK_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
fi

# ② macOS 兜底：java_home 探不到时（如装在非标准位置）扫常见安装目录里的 *-17*。
if [ -z "$CLI_JDK_HOME" ]; then
    for d in /Library/Java/JavaVirtualMachines/*17*/Contents/Home \
             "$HOME"/Library/Java/JavaVirtualMachines/*17*/Contents/Home \
             /opt/homebrew/opt/openjdk@17 /usr/local/opt/openjdk@17; do
        if [ -x "$d/bin/java" ]; then CLI_JDK_HOME="$d"; break; fi
    done
fi

# ③ Linux 兜底：扫常见 JDK 17 安装目录。
if [ -z "$CLI_JDK_HOME" ]; then
    for d in /usr/lib/jvm/*17*/ /usr/lib/jvm/java-17-*/; do
        if [ -x "$d/bin/java" ]; then CLI_JDK_HOME="${d%/}"; break; fi
    done
fi

if [ -n "$CLI_JDK_HOME" ] && [ -x "$CLI_JDK_HOME/bin/java" ]; then
    export JAVA_HOME="$CLI_JDK_HOME"
    echo "[build-cli] JAVA_HOME=$JAVA_HOME"
else
    echo "[build-cli] 警告: 未自动找到 JDK 17，回退使用当前 JAVA_HOME=${JAVA_HOME:-未设置}"
    echo "[build-cli]       如构建报 JVM 版本错，请在脚本顶部把 CLI_JDK_HOME 手填为本机 JDK 17 路径。"
fi

cd "$(dirname "$0")/.."
exec ./gradlew "$@"
