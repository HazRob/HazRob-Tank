#!/usr/bin/env bash
set -euo pipefail
mkdir -p /tmp/v6-n09-scientific-risk
adb install -r source/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.servoc.app/com.servoc.app.report.V6ScientificRiskDbFixtureActivity
ready=0
for i in $(seq 1 120);do
  if adb shell run-as com.servoc.app test -f files/n09_scientific_risk_db.json >/dev/null 2>&1;then ready=1;break;fi
  sleep 2
done
if [[ "$ready" != 1 ]];then
  adb logcat -d -t 2500 | grep -E 'N09V11|FATAL EXCEPTION|AndroidRuntime' | tail -120 || true
  exit 1
fi
adb exec-out run-as com.servoc.app cat files/n09_scientific_risk_db.json >/tmp/v6-n09-scientific-risk/n09_scientific_risk_db.json
adb exec-out run-as com.servoc.app cat files/n09_ghs_report.pdf >/tmp/v6-n09-scientific-risk/n09_ghs_report.pdf
python3 - <<'PY'
import json
p='/tmp/v6-n09-scientific-risk/n09_scientific_risk_db.json'
d=json.load(open(p));print(json.dumps(d,indent=2))
assert d['status']=='PASS',d
assert d['sqlite_version']==13
assert d['covered_profiles']==d['post_authority_import_materials']>=652
assert d['unified_chemical_menu_rows']==d['unique_master_uids']==d['covered_profiles']
assert d['original_144_members']==144
assert d['unified_view_columns']>=75
assert d['materials_with_lfl']>=50 and d['materials_with_ufl']>=50
assert d['hazard_source_classification_rows']>=7
assert d['chemcore_g3_techniques']==0
assert d['bundled_ghs_vector_assets']==9
assert d['ghs_pdf_page_count']==2
assert d['ghs_pdf_vector_raster_red_pixels']>40
assert d['coverage_fraction']==1
assert d['osha_z1_rows']>=500 and d['niosh_idlh_rows']>=300
assert d['co2_source_pel_ppm']==5000 and d['co2_early_warn_ppm']==4500
assert d['normal_ranges_with_source']>=1 and d['physical_tank_g3']==0
print('PASS N09 V13 AUTHORITY-SEEDED SQLITE CHEMICAL PROFILE COVERAGE; NOT GAS CALIBRATION')
PY
