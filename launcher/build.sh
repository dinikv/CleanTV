#!/bin/sh
# Local development build. Generates an ignored local signing key; never publish it.
set -eu
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$SDK" ] || { echo 'Set ANDROID_HOME to your Android SDK directory.' >&2; exit 1; }
BT="$SDK/build-tools/${BUILD_TOOLS_VERSION:-35.0.0}"
ANDROID="$SDK/platforms/android-${ANDROID_API:-35}/android.jar"
for name in aapt d8 zipalign apksigner; do [ -x "$BT/$name" ] || { echo "Missing SDK tool: $name" >&2; exit 1; }; done
mkdir -p build/classes build/dex res/drawable-nodpi
index=0
for name in neon-city thor batman-red batman-blue; do
 cp "../wallpapers/$name.jpg" "res/drawable-nodpi/wallpaper_$index.jpg"
 index=$((index+1))
done
# Clean stale bytecode, but keep the private development signing key.
find build/classes build/dex -type f -delete
"$BT/aapt" package -0 jpg -f -m -J build -M AndroidManifest.xml -S res -I "$ANDROID"
javac --release 8 -classpath "$ANDROID" -d build/classes src/org/cleantv/launcher/*.java build/org/cleantv/launcher/R.java
jar cf build/classes.jar -C build/classes .
"$BT/d8" --lib "$ANDROID" --min-api 26 --output build/dex build/classes.jar
"$BT/aapt" package -0 jpg -f -M AndroidManifest.xml -S res -I "$ANDROID" -F build/unsigned.apk
(cd build/dex && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
if [ ! -f build/local-signing.jks ]; then
 keytool -genkeypair -keystore build/local-signing.jks -storepass localbuild -keypass localbuild -alias cleantv -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=CleanTV Local Development' >/dev/null 2>&1
fi
"$BT/apksigner" sign --ks build/local-signing.jks --ks-pass pass:localbuild --key-pass pass:localbuild --out build/CleanTV.apk build/aligned.apk
"$BT/apksigner" verify build/CleanTV.apk
printf '%s\n' 'Built launcher/build/CleanTV.apk (local development signature)'
