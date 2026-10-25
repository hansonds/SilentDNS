#!/usr/bin/env bash
# Builds SilentDNS without Gradle: a signed APK (sideload / GitHub) and a signed AAB (Google Play).
# Needs: JDK 17+, Android SDK with build-tools + platform, and bundletool.jar.
# Usage: KS=path/to.jks KS_PASS=... KS_ALIAS=... BUNDLETOOL=bundletool.jar tools/build.sh
set -euo pipefail
cd "$(dirname "$0")/../app"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:?set ANDROID_HOME}}"
PLATFORM="${PLATFORM:-36}"
BT="$SDK/build-tools/${BUILD_TOOLS:-36.0.0}"
JAR="$SDK/platforms/android-$PLATFORM/android.jar"
LINK_FLAGS=(-I "$JAR" --manifest AndroidManifest.xml --min-sdk-version 24 --target-sdk-version "$PLATFORM")

rm -rf build && mkdir -p build/gen build/classes
"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/base.apk "${LINK_FLAGS[@]}" --java build/gen build/res.zip

javac -nowarn -source 8 -target 8 -bootclasspath "$JAR" -d build/classes \
  build/gen/com/hanson/silentdns/R.java src/com/hanson/silentdns/*.java
"$BT/d8" --min-api 24 --release --lib "$JAR" --output build $(find build/classes -name '*.class')

# ---- APK ----
cp build/base.apk build/unsigned.apk && (cd build && zip -qj unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$KS_PASS" --ks-key-alias "$KS_ALIAS" \
  --out build/SilentDNS.apk build/aligned.apk
"$BT/apksigner" verify build/SilentDNS.apk
echo "Built app/build/SilentDNS.apk"

# ---- AAB (Google Play) ----
if [ -n "${BUNDLETOOL:-}" ]; then
  "$BT/aapt2" link --proto-format -o build/proto.apk "${LINK_FLAGS[@]}" build/res.zip
  rm -rf build/aab && mkdir -p build/aab/base/manifest build/aab/base/dex
  (cd build/aab && unzip -q ../proto.apk -d proto)
  mv build/aab/proto/AndroidManifest.xml build/aab/base/manifest/
  mv build/aab/proto/resources.pb build/aab/base/
  mv build/aab/proto/res build/aab/base/
  cp build/classes.dex build/aab/base/dex/
  (cd build/aab/base && zip -qr ../base.zip .)
  java -jar "$BUNDLETOOL" build-bundle --modules=build/aab/base.zip --output=build/SilentDNS.aab --overwrite
  jarsigner -sigalg SHA256withRSA -digestalg SHA-256 -keystore "$KS" -storepass "$KS_PASS" \
    build/SilentDNS.aab "$KS_ALIAS" > /dev/null
  java -jar "$BUNDLETOOL" validate --bundle=build/SilentDNS.aab > /dev/null
  echo "Built app/build/SilentDNS.aab"
fi
