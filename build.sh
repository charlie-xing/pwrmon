#!/bin/bash
# Gradle-free build: aapt2 -> javac -> d8 -> zipalign -> apksigner.
# Usage: ./build.sh [install]
set -euo pipefail
cd "$(dirname "$0")"

SDK="${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}"
BT="$SDK/build-tools/36.0.0"
JAR="$SDK/platforms/android-36/android.jar"
PKG=com.xcl.pwrmon
OUT=build

rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex"

"$BT/aapt2" compile --dir res -o "$OUT/res.zip"
"$BT/aapt2" link -o "$OUT/unsigned.apk" -I "$JAR" --manifest AndroidManifest.xml \
    --version-code 1 --version-name 0.1 "$OUT/res.zip"

javac -source 17 -target 17 -Xlint:-options -XDstringConcat=inline \
    -classpath "$JAR" -d "$OUT/classes" $(find src -name '*.java')
"$BT/d8" --lib "$JAR" --min-api 30 --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
(cd "$OUT/dex" && zip -q -j ../unsigned.apk classes.dex)

"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

KS="$HOME/.android/debug.keystore"
if [ ! -f "$KS" ]; then
    mkdir -p "$HOME/.android"
    keytool -genkeypair -keystore "$KS" -storepass android -keypass android -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass pass:android --key-pass pass:android \
    --ks-key-alias androiddebugkey --out "$OUT/pwrmon.apk" "$OUT/aligned.apk"
echo "built $OUT/pwrmon.apk"

if [ "${1:-}" = "install" ]; then
    adb install -r "$OUT/pwrmon.apk"
    adb shell am start -n "$PKG/.MainActivity"
fi
