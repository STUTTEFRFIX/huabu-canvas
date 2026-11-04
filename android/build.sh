#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
APP_ROOT="$(cd "$ROOT/.." && pwd)"
BUILD="$ROOT/build"
VERSION="${VERSION:-0.3.0}"
APK_NAME="huabu-${VERSION}.apk"
KEYSTORE="${DEBUG_KEYSTORE:-$ROOT/debug.keystore}"

SDK="${ANDROID_SDK:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}}"
if [ -z "$SDK" ] || [ ! -d "$SDK" ]; then
  echo "请设置 ANDROID_SDK 指向 Android SDK 根目录（包含 platforms/ 和 build-tools/）。" >&2
  exit 1
fi

ANDROID_JAR="$SDK/platforms/android-36/android.jar"
if [ ! -f "$ANDROID_JAR" ]; then
  ANDROID_JAR="$(find "$SDK/platforms" -maxdepth 2 -name android.jar 2>/dev/null | sort -V | tail -1)"
fi
if [ -z "${ANDROID_JAR:-}" ] || [ ! -f "$ANDROID_JAR" ]; then
  echo "找不到 android.jar，请安装 Android SDK Platform。" >&2
  exit 1
fi

find_tool() {
  local name="$1"
  if command -v "$name" >/dev/null 2>&1; then
    command -v "$name"
    return 0
  fi
  local candidate
  for candidate in "$SDK"/build-tools/*/"$name"; do
    if [ -x "$candidate" ]; then
      echo "$candidate"
      return 0
    fi
  done
  return 1
}

AAPT2="$(find_tool aapt2 || true)"
D8="$(find_tool d8 || true)"
APKSIGNER="$(find_tool apksigner || true)"
ZIPALIGN="$(find_tool zipalign || true)"

for tool in AAPT2 D8 APKSIGNER; do
  if [ -z "${!tool:-}" ]; then
    echo "缺少构建工具：${tool,,}" >&2
    exit 1
  fi
done

rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/dex" "$ROOT/assets"
cp "$APP_ROOT/index.html" "$ROOT/assets/index.html"

echo "==> 编译资源"
"$AAPT2" compile --dir "$ROOT/res" -o "$BUILD/res.zip"

echo "==> 链接 APK"
"$AAPT2" link \
  -o "$BUILD/app-unsigned.apk" \
  -I "$ANDROID_JAR" \
  --manifest "$ROOT/AndroidManifest.xml" \
  -R "$BUILD/res.zip" \
  -A "$ROOT/assets" \
  --auto-add-overlay

echo "==> 编译 Java"
javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options \
  -classpath "$ANDROID_JAR" \
  -d "$BUILD/classes" \
  "$ROOT/src/io/github/stuttefrfix/huabu/MainActivity.java"

echo "==> 转换为 dex"
find "$BUILD/classes" -name '*.class' -print0 | xargs -0 "$D8" \
  --lib "$ANDROID_JAR" --min-api 26 --output "$BUILD/dex"

echo "==> 打包 classes.dex"
(cd "$BUILD" && zip -q -j app-unsigned.apk dex/classes.dex)

INPUT="$BUILD/app-unsigned.apk"
if [ -n "$ZIPALIGN" ] && "$ZIPALIGN" -h >/dev/null 2>&1; then
  echo "==> 对齐"
  "$ZIPALIGN" -f 4 "$INPUT" "$BUILD/app-aligned.apk"
  INPUT="$BUILD/app-aligned.apk"
else
  echo "==> 跳过 zipalign（当前工具不可执行）"
fi

if [ ! -f "$KEYSTORE" ]; then
  echo "==> 生成 debug keystore"
  keytool -genkeypair -noprompt \
    -keystore "$KEYSTORE" \
    -storepass android -keypass android \
    -alias androiddebugkey \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US" >/dev/null
fi

echo "==> 签名"
"$APKSIGNER" sign \
  --ks "$KEYSTORE" \
  --ks-key-alias androiddebugkey \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "$BUILD/$APK_NAME" \
  "$INPUT"

echo "==> 校验"
"$APKSIGNER" verify --print-certs "$BUILD/$APK_NAME"

echo "APK: $BUILD/$APK_NAME"
