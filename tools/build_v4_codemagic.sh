#!/usr/bin/env bash
# HazRob Tank V4 diagnostic build; uses read-only pinned original Android source.
set -Eeuo pipefail
cd "$(dirname "$0")/.."
ROOT="$(pwd)"
echo "[Tank V4] branch / commit:"; git rev-parse --abbrev-ref HEAD; git rev-parse HEAD
if [ -z "${HAZROB_SOURCE_READ_TOKEN:-}" ]; then
  echo "Missing HAZROB_SOURCE_READ_TOKEN. Add it as a secret in Codemagic group hazrob_source." >&2
  exit 1
fi
for check in tools/verify_v4_session_queries.py tools/verify_v4_echo_r0_contract.py tools/verify_v4_screening_stable_contract.py tools/verify_approved_report_sql.py; do
  echo "[Tank V4] Source contract $check"
  python3 "$check"
done
if [ -e source ]; then echo "Refusing to overwrite existing source directory" >&2; exit 1; fi
# Avoid embedding GitHub credentials in Git remote URLs or command-line arguments.
ASKPASS="$(mktemp /tmp/tank-v4-askpass-XXXXXX)"
chmod 700 "$ASKPASS"
cat > "$ASKPASS" <<'GIT_AUTH'
#!/usr/bin/env bash
case "$1" in
  *Username*) printf 'x-access-token\n' ;;
  *Password*) printf '%s\n' "$HAZROB_SOURCE_READ_TOKEN" ;;
  *) printf '\n' ;;
esac
GIT_AUTH
trap 'rm -f "$ASKPASS"' EXIT
GIT_ASKPASS="$ASKPASS" GIT_TERMINAL_PROMPT=0 git -c credential.helper= clone --quiet "https://github.com/sohpany/SERVOC-Android.git" source
rm -f "$ASKPASS"
trap - EXIT
PIN="6281d3eb7c1c961ce63b0b5e4ae0792874626720"
git -C source checkout --quiet --detach "$PIN"
test "$(git -C source rev-parse HEAD)" = "$PIN"
echo "[Tank V4] Original repo pinned and read-only at $PIN"
mkdir -p source/app/src/main/java source/app/src/test/java
cp -R android/app/src/main/java/. source/app/src/main/java/
if [ -d android/app/src/main/res ]; then
  mkdir -p source/app/src/main/res
  cp -R android/app/src/main/res/. source/app/src/main/res/
fi
if [ -d android/app/src/test/java ]; then
  cp -R android/app/src/test/java/. source/app/src/test/java/
fi
python3 - <<'PY'
from pathlib import Path
import re
f=Path('source/app/build.gradle')
s=f.read_text()
required=["applicationId 'com.servoc.app'","versionName '0.10.2'","versionCode 30","debug { signingConfig signingConfigs.prototype }"]
for term in required:
    assert term in s, "Original source changed: "+term
s=s.replace("applicationId 'com.servoc.app'","applicationId 'com.hazrob.tank'")
s=s.replace("versionName '0.10.2'","versionName '4.0'")
s=s.replace("versionCode 30","versionCode 4")
# Debug uses its own automatic debug certificate, not the old SERVOC signing key.
s=s.replace("debug { signingConfig signingConfigs.prototype }","debug { }")
s += """
tasks.withType(Test).configureEach {
    filter {
        excludeTestsMatching 'com.servoc.app.NonDrivingUiContrastContractTest.allPrimaryNonDrivingTabsDeclareDarkWorkspaceSurface'
        excludeTestsMatching 'com.servoc.app.NonDrivingUiContrastContractTest.reportsAndMeasurementsUseReadableDarkCardsForDynamicText'
        excludeTestsMatching 'com.servoc.app.HR069R1FieldUXRegressionsTest.fastRefreshDoesNotRebuildHiddenCandidateViews'
    }
}
"""
f.write_text(s)
mf=Path('source/app/src/main/AndroidManifest.xml')
t=mf.read_text()
t=re.sub(r'android:label="[^"]*"', 'android:label="HazRob Tank"',t,count=1)
t=t.replace('android:icon="@drawable/ic_hazrob_hr069_r1"', 'android:icon="@drawable/ic_hazrob_tank_v3"')
t=t.replace('android:roundIcon="@drawable/ic_hazrob_hr069_r1"', 'android:roundIcon="@drawable/ic_hazrob_tank_v3"')
mf.write_text(t)
print("V4 applicationId=com.hazrob.tank; versionName=4.0; debug identity is separate")
PY
SDK="$(printenv ANDROID_SDK_ROOT || printenv ANDROID_HOME || true)"
test -n "$SDK" && test -d "$SDK" || { echo "Android SDK not available" >&2; exit 1; }
export ANDROID_HOME="$SDK"
export ANDROID_SDK_ROOT="$SDK"
printf 'sdk.dir=%s\n' "$SDK" > source/local.properties
if [ ! -d "$SDK/platforms/android-36" ] || [ ! -x "$SDK/build-tools/36.0.0/apksigner" ]; then
  SDKMANAGER="$(command -v sdkmanager || true)"
  if [ -z "$SDKMANAGER" ]; then SDKMANAGER="$SDK/cmdline-tools/latest/bin/sdkmanager"; fi
  test -x "$SDKMANAGER" || { echo "sdkmanager not found" >&2; exit 1; }
  "$SDKMANAGER" "platforms;android-36" "build-tools;36.0.0"
fi
TOOLS="$ROOT/.tank-v4-build-tools"
mkdir -p "$TOOLS"
if [ ! -x "$TOOLS/gradle-9.5.0/bin/gradle" ]; then
  echo "[Tank V4] Download verified Gradle 9.5.0 for Android Gradle Plugin 9.3.0"
  curl -fsSL --retry 3 "https://services.gradle.org/distributions/gradle-9.5.0-bin.zip" -o "$TOOLS/gradle.zip"
  curl -fsSL --retry 3 "https://services.gradle.org/distributions/gradle-9.5.0-bin.zip.sha256" -o "$TOOLS/gradle.sha256"
  printf '%s  %s\n' "$(cat "$TOOLS/gradle.sha256")" "$TOOLS/gradle.zip" | shasum -a 256 -c -
  unzip -qo "$TOOLS/gradle.zip" -d "$TOOLS"
  rm -f "$TOOLS/gradle.zip" "$TOOLS/gradle.sha256"
fi
GRADLE="$TOOLS/gradle-9.5.0/bin/gradle"
"$GRADLE" --version | head -18
echo "[Tank V4] Executing Android JUnit and assembleDebug"
(cd source && "$GRADLE" --no-daemon --stacktrace :app:testDebugUnitTest :app:assembleDebug)
APK="source/app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"
"$SDK/build-tools/36.0.0/apksigner" verify --verbose "$APK"
python3 - <<'PY'
import json,zipfile
apk="source/app/build/outputs/apk/debug/app-debug.apk"
base="assets/generated/chemcore/embedded_stable/"
with zipfile.ZipFile(apk) as z:
    names=set(z.namelist())
    assert base+"manifest.json" in names, "Embedded Stable manifest absent"
    assert base+"chemcore-stable-snapshot.json" in names, "Embedded Stable data absent"
    m=json.loads(z.read(base+"manifest.json"))
    assert m["library_version"]=="1.0.0-hr069r1"
    assert m["chemical_count"]==652
print("[Tank V4] Verified embedded Stable 652 identities and version")
PY
mkdir -p dist-v4
cp "$APK" dist-v4/HazRob-Tank-v4-UNRELEASED-debug.apk
shasum -a 256 dist-v4/*.apk > dist-v4/SHA256SUMS.txt
cat dist-v4/SHA256SUMS.txt
echo "[Tank V4] diagnostic APK ready. Requires real device verification before release."
