#!/usr/bin/env bash
set -euo pipefail
OUT=/tmp/v6-n06g-approved
mkdir -p "$OUT"
adb install -r source/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.servoc.app/com.servoc.app.report.V6MultiSessionSiteFixtureActivity
ready=0
for i in $(seq 1 90); do
  if adb shell run-as com.servoc.app test -f files/n06g_status.json >/dev/null 2>&1; then
    ready=1;break
  fi
  sleep 2
done
if [[ "$ready" != 1 ]]; then
  adb logcat -d -t 2000 | grep -E 'N06G|FATAL EXCEPTION|AndroidRuntime' | tail -100 || true
  echo "N06G_ANDROID_FULL_EXPORT_STATUS_MISSING"
  exit 1
fi
adb exec-out run-as com.servoc.app cat files/n06g_status.json > "$OUT/n06g_status.json"
cat "$OUT/n06g_status.json"
python3 - <<'PY'
import json
p='/tmp/v6-n06g-approved/n06g_status.json'
d=json.load(open(p))
assert d['status']=='PASS',d
assert d['pdf_pages']==6 and d['xlsx_sheet_count']==14,d
assert d['approved_gas_cycles']==1
assert d['independent_source_sensor_models']==8
assert d['sqlite_original_approved_source_sha_before_after_equal']
assert d['archive_digest_before']==d['archive_digest_after']
assert d['reported_gas_identity']=='UNKNOWN' and d['ppm_reportable'] is False
assert d['tank_g3']==0 and not d['user_data_used']
assert d['site_approved_independent_sessions']==2 and d['site_pdf_pages']==5
assert d['site_second_session_tamper_rejected'] and not d['site_reportable_ppm']
assert d['xlsx_v6_measurements_sheet'] and d['xlsx_v6_sources_sheet']
print("PASS N06G FULL FIELD_SESSION_REPORT_EXPORTER PDF+XLSX E2E",d['assertions'])
PY
adb exec-out run-as com.servoc.app cat files/n06g_full_approved_field.pdf > "$OUT/n06g_full_approved_field.pdf"
adb exec-out run-as com.servoc.app cat files/n06g_full_approved_field.xlsx > "$OUT/n06g_full_approved_field.xlsx"

adb exec-out run-as com.servoc.app cat files/n06g_site_two_approved.pdf > "$OUT/n06g_site_two_approved.pdf"
