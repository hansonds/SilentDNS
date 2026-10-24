#!/usr/bin/env bash
# Builds SilentDNS without Gradle.
# Needs: JDK 17+, an Android SDK (build-tools 34 + platform 34).
# Usage: KS=path/to.jks KS_PASS=... KS_ALIAS=... tools/build.sh
set -euo pipefail
cd "$(dirname "$0")/../app"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:?set ANDROID_HOME}}"
BT="$SDK/build-tools/34.0.0"; JAR="$SDK/platforms/android-34/android.jar"
rm -rf build && mkdir -p build/gen build/classes
"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/base.apk -I "$JAR" --manifest AndroidManifest.xml \
  --java build/gen --min-sdk-version 21 --target-sdk-version 34 build/res.zip
javac -nowarn -source 8 -target 8 -bootclasspath "$JAR" -d build/classes build/gen/com/hanson/silentdns/R.java src/com/hanson/silentdns/*.java
"$BT/d8" --min-api 21 --lib "$JAR" --output build $(find build/classes -name '*.class')
cp build/base.apk build/unsigned.apk && (cd build && zip -qj unsigned.apk classes.dex)
"$BT/zipalign" -f 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$KS_PASS" --ks-key-alias "$KS_ALIAS" \
  --out build/SilentDNS.apk build/aligned.apk
echo "Built app/build/SilentDNS.apk"
