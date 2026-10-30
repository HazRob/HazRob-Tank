#!/usr/bin/env bash
set -euo pipefail
OUT=/tmp/v6-clean-air-instant
mkdir -p "$OUT"
adb install -r source/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.servoc.app/com.servoc.app.report.V6CleanAirInstantFixtureActivity
ready=0
for i in $(seq 1 65); do
  if adb shell run-as com.servoc.app test -f files/v6_clean_air_instant_status.json >/dev/null 2>&1; then
    ready=1;break
  fi
  sleep 2
done
if [[ "$ready" != 1 ]]; then echo "V6_CLEAN_AIR_INSTANT_STATUS_MISSING"; exit 1; fi
adb exec-out run-as com.servoc.app cat files/v6_clean_air_instant_status.json > "$OUT/status.json"
cat "$OUT/status.json"
python3 - <<'PY'
import json
d=json.load(open('/tmp/v6-clean-air-instant/status.json'))
assert d['status']=='PASS',d
assert d['observed_mq']==8 and d['ratio_ready']==8
assert d['source_curves']==0 and d['ui_candidates']==0
assert d['diagnostic_curves']==49 and d['diagnostic_chemical_groups']==13
assert d['response_gate_state']=='WITHIN_CLEAN_AIR_REFERENCE_BAND'
assert d['distinct_chemical_groups']==0
assert d['unqualified_r0_operator_candidates']==0
assert d['manual_unverified_candidates']==0
assert d['firmware_without_samples_candidates']==0
assert d['opposite_direction_candidates']==0
assert d['reported_identity']=='UNKNOWN' and not d['certified_ppm']
assert d['pdf_pages']==1 and not d['genuine_tank_exposure']
print('PASS actual Android TankInstantAnalyzer and TankInstantReportPdf for SAME CLEAN AIR R0')
PY
adb exec-out run-as com.servoc.app cat files/v6_clean_air_instant.pdf > "$OUT/v6_clean_air_instant.pdf"
adb exec-out run-as com.servoc.app cat files/v6_clean_air_instant_result.json > "$OUT/v6_clean_air_instant_result.json"
