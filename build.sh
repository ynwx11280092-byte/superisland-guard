#!/usr/bin/env bash
set -euo pipefail

# 一键构建 SuperIslandGuard APK（需联网，自动装 JDK17 + Gradle + Android SDK）
# 用法： ./build.sh

if [ "$(id -u)" = "0" ]; then SUDO=""; else SUDO="sudo"; fi

SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
GRADLE_VER="8.7"
BUILD_TOOLS="36.0.0"
PLATFORM="android-36"
CMD_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VER}-bin.zip"

log(){ printf '\n\033[1;36m==>\033[0m %s\n' "$*"; }

# 1) JDK
if ! command -v java >/dev/null 2>&1; then
  log "安装 JDK 17..."
  $SUDO apt-get update -y
  $SUDO apt-get install -y openjdk-17-jdk-headless
fi
java -version

# 2) Gradle
if ! command -v gradle >/dev/null 2>&1; then
  log "下载 Gradle ${GRADLE_VER}..."
  curl -fL "$GRADLE_URL" -o /tmp/gradle.zip
  rm -rf "$HOME/gradle-${GRADLE_VER}"
  unzip -q /tmp/gradle.zip -d "$HOME"
  export PATH="$HOME/gradle-${GRADLE_VER}/bin:$PATH"
fi
gradle --version

# 3) Android SDK
if [ ! -d "$SDK_ROOT/cmdline-tools" ]; then
  log "下载 Android cmdline-tools..."
  mkdir -p "$SDK_ROOT/cmdline-tools"
  curl -fL "$CMD_URL" -o /tmp/cmdline.zip
  unzip -q /tmp/cmdline.zip -d "$SDK_ROOT/cmdline-tools"
  mv "$SDK_ROOT/cmdline-tools/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
fi
export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
yes | "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
"$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
  "platform-tools" "platforms;${PLATFORM}" "build-tools;${BUILD_TOOLS}" >/dev/null

# 4) 构建（debug 签名，可直接安装）
log "构建 APK..."
cd "$(dirname "$0")"
gradle :app:assembleDebug

APK="app/build/outputs/apk/debug/app-debug.apk"
log "完成：$(pwd)/$APK"
ls -lh "$APK"
