#!/usr/bin/env bash
set -euo pipefail
adb install -r source/app/build/outputs/apk/debug/app-debug.apk
mkdir -p /tmp/v6-full-original-reports
adb shell am start -W -n com.servoc.app/com.servoc.app.report.V6AllOriginalPdfFixtureActivity
ready=0
for i in $(seq 1 90); do
  if adb shell run-as com.servoc.app test -f files/v6_all_original_pdf_status.json >/dev/null 2>&1; then ready=1;break;fi
  sleep 2
done
if [ "$ready" != 1 ]; then
  adb logcat -d -t 400 | tail -100 || :
  echo "V6 original report generator timeout";exit 1
fi
adb exec-out run-as com.servoc.app cat files/v6_all_original_pdf_status.json > /tmp/v6-full-original-reports/all_reports_status.json
cat /tmp/v6-full-original-reports/all_reports_status.json
python3 -c 'import json; p=json.load(open("/tmp/v6-full-original-reports/all_reports_status.json"));assert p["status"]=="PASS",p'
for name in 01_V6_Instant_ChemCore_Original.pdf 02_V6_Physical_Snapshot_Original.pdf 05_V6_Fruit_Freshness_Original.pdf 06_V6_Meat_Freshness_Original.pdf; do
  adb exec-out run-as com.servoc.app cat files/"$name" > /tmp/v6-full-original-reports/"$name"
done
adb shell am start -W -n com.servoc.app/com.servoc.app.report.V6ApprovedFieldE2EFixtureActivity
ready=0
for i in $(seq 1 90); do
  if adb shell run-as com.servoc.app test -f files/n06e_status.json >/dev/null 2>&1; then ready=1;break;fi
  sleep 2
done
if [ "$ready" != 1 ]; then echo "N06E full site report generator timeout";exit 1;fi
adb exec-out run-as com.servoc.app cat files/n06e_status.json > /tmp/v6-full-original-reports/field_status.json
cat /tmp/v6-full-original-reports/field_status.json
python3 -c 'import json; p=json.load(open("/tmp/v6-full-original-reports/field_status.json"));assert p["status"]=="PASS",p'
adb exec-out run-as com.servoc.app cat files/n06e_full_approved_field.pdf > /tmp/v6-full-original-reports/03_V6_Approved_Field_Session_Original.pdf
adb exec-out run-as com.servoc.app cat files/v6_qa_site.pdf > /tmp/v6-full-original-reports/04_V6_Site_Summary_Original.pdf
python3 - <<'PY'
from pathlib import Path
import json,hashlib
p=Path("/tmp/v6-full-original-reports")
for file in sorted(p.glob("*.pdf")):
    data=file.read_bytes()
    assert data.startswith(b"%PDF-") and len(data)>2500,(file,len(data))
    print(file.name,len(data),hashlib.sha256(data).hexdigest())
assert len(list(p.glob("*.pdf")))==6
(p/"QA_MANIFEST.json").write_text(json.dumps({
"schema":"hazrob-v6-authentic-original-android-pdf-fixture/1",
"source":"Production report generator classes copied unchanged from HazRob-Tank V6",
"origin":"Android emulator only; test-only fixture values; NOT measured data",
"not_evidence_of":"Tank physical G3, chemical ppm validity, occupational exposure compliance",
"files":[{"name":f.name,"bytes":f.stat().st_size,"sha256":hashlib.sha256(f.read_bytes()).hexdigest()}
 for f in sorted(p.glob("*.pdf"))]
},indent=2))
PY
