#!/usr/bin/env bash
set -euo pipefail
adb install -r source/app/build/outputs/apk/debug/app-debug.apk
mkdir -p /tmp/v7-r01-pdf
adb shell am start -W -n com.servoc.app/com.servoc.app.report.V7R01PdfFixtureActivity
ready=0
for i in $(seq 1 90); do
  if adb shell run-as com.servoc.app test -f files/v7_r01_pdf_status.json >/dev/null 2>&1; then ready=1;break;fi
  sleep 2
done
if test "$ready" != 1; then echo "QA fixture timeout";adb logcat -d -t 90 || :; exit 1;fi
adb exec-out run-as com.servoc.app cat files/v7_r01_pdf_status.json > /tmp/v7-r01-pdf/status.json
cat /tmp/v7-r01-pdf/status.json
python3 -c 'import json;p=json.load(open("/tmp/v7-r01-pdf/status.json"));assert p["status"]=="PASS",p'
adb exec-out run-as com.servoc.app cat files/V7_R01_Instant_ChemCore_Approved_Layout_Test.pdf > /tmp/v7-r01-pdf/HazRob_Tank_V7_R01_Actual_Android_Report.pdf
