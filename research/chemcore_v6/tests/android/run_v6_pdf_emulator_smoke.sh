#!/usr/bin/env bash
# The emulator-runner executes each YAML script line with /bin/sh -c.
# Run this standalone bash script as ONE command to preserve loops/heredocs.
set -euo pipefail
adb install -r source/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.servoc.app/com.servoc.app.report.V6ResearchPdfSmokeFixtureActivity
mkdir -p /tmp/v6-n06c-pdf
ready=0
for i in $(seq 1 55); do
  if adb shell run-as com.servoc.app test -f files/v6_pdf_status.json >/dev/null 2>&1; then
    ready=1
    break
  fi
  sleep 2
done
if [ "$ready" != 1 ]; then
  adb logcat -d -t 500 | tail -100 || :
  echo "FAIL: no test activity PDF status in 110 seconds"
  exit 1
fi
adb exec-out run-as com.servoc.app cat files/v6_pdf_status.json > /tmp/v6-n06c-pdf/v6_pdf_status.json
cat /tmp/v6-n06c-pdf/v6_pdf_status.json
python3 - <<'PY'
import json
p="/tmp/v6-n06c-pdf/v6_pdf_status.json"
d=json.load(open(p))
assert d["status"]=="PASS",d
assert d["pages_created"]==3 and d["pages_rasterized_with_android_pdf_renderer"]==3
assert d["reportable_ppm"] is False and d["physical_tank_g3"]==0
print("PASS ANDROID_EMULATOR_PDF_RENDERER THREE_PAGES")
PY
adb exec-out run-as com.servoc.app cat files/v6_pdf_fixture.pdf > /tmp/v6-n06c-pdf/v6_pdf_fixture.pdf
for i in 1 2 3; do
  adb exec-out run-as com.servoc.app cat files/v6_pdf_fixture_page_$i.png > /tmp/v6-n06c-pdf/v6_pdf_fixture_page_$i.png
done
echo "PASS V6 REAL Android PdfRenderer exported three PNGs and PDF fixture"
