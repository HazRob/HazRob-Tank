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
adb exec-out run-as com.servoc.app cat files/v6_chemical_catalog_coverage.csv >/tmp/v6-n09-scientific-risk/v6_chemical_catalog_coverage.csv
adb exec-out run-as com.servoc.app cat files/n09_ghs_report.pdf >/tmp/v6-n09-scientific-risk/n09_ghs_report.pdf
python3 - <<'PY'
import json
p='/tmp/v6-n09-scientific-risk/n09_scientific_risk_db.json'
d=json.load(open(p));print(json.dumps(d,indent=2))
assert d['status']=='PASS',d
assert d['sqlite_version']==14
assert d['covered_profiles']==d['post_authority_import_materials']>=652
assert d['unified_chemical_menu_rows']==d['unique_master_uids']==d['covered_profiles']
assert d['original_144_members']==144
assert d['unified_view_columns']>=75
assert d['materials_with_lfl']>=50 and d['materials_with_ufl']>=50
assert d['hazard_source_classification_rows']>=7
assert d['chemcore_g3_techniques']==0
assert d['v14_master_uid_rows']==d['post_authority_import_materials']==734
assert d['v14_unified_columns']>=96
assert d['v14_linked_source_curves']==49
assert d['v14_unverified_source_r0_curves']==49
assert d['v14_actual_tank_r0_samples']==0
assert d['v14_simulator_only_r0_channels']==8
assert d['bundled_ghs_vector_assets']==9
assert d['ghs_pdf_page_count']==1
assert d['ghs_pdf_vector_raster_red_pixels']==0
assert d['coverage_fraction']==1
coverage=d['material_field_coverage_audit']
assert coverage['materials']==coverage['unique_uids']==734,coverage
assert coverage['duplicate_uids']==0,coverage
assert coverage['materials_with_name_en']==734,coverage
assert not coverage['scientific_fields_exhaustive'],coverage
assert coverage['missing_values_policy']=='NULL_NOT_ZERO_NO_SAFETY_INFERENCE'
assert coverage['materials_with_cas']==698,coverage
assert coverage['materials_with_molar_mass']==637,coverage
assert d['nist_27_cas_identity_idempotence'].find('matched=27')>=0,d
assert d['nist_27_cas_identity_idempotence'].find('cas_filled=0')>=0,d
assert d['nist_27_cas_identity_idempotence'].find('rejected=0')>=0,d
assert coverage['materials_with_niosh_cas_equivalence']==27,coverage
assert coverage['materials_with_direct_or_linked_cas']==725,coverage
assert coverage['materials_without_direct_or_linked_cas']==9,coverage
assert coverage['non_single_substance_labels']==9,coverage
assert coverage['individual_materials_without_linked_cas']==0,coverage
import csv
with open('/tmp/v6-n09-scientific-risk/v6_chemical_catalog_coverage.csv',newline='') as stream:
    records=list(csv.DictReader(stream))
assert len(records)==734,len(records)
assert len(set(row['uid'] for row in records))==734
assert all(row['name_en'] for row in records)
assert all(row['mq_curve_count'].isdigit() for row in records)
et=next(x for x in records if x['uid']=='104')
assert not et['cas'] and et['canonical_uid_niosh']=='499'
h2=next(x for x in records if x['uid']=='76')
assert h2['cas']=='1333-74-0' and float(h2['molar_mass'])>2.0
assert next(x for x in records if x['uid']=='131')['cas']=='754-12-1'
assert next(x for x in records if x['uid']=='132')['cas']=='29118-24-9'
assert et['niosh_equivalent_cas']=='64-17-5'
assert next(x for x in records if x['uid']=='143')['chemical_class']=='MEASUREMENT_INDEX_NOT_CHEMICAL'
print('V14 734 master UID coverage fields:',json.dumps(coverage,sort_keys=True))
assert d['osha_z1_rows']>=500 and d['niosh_idlh_rows']>=300
assert d['co2_source_pel_ppm']==5000 and d['co2_early_warn_ppm']==4500
assert d['normal_ranges_with_source']>=1 and d['physical_tank_g3']==0
print('PASS N09 V14 AUTHORITY-SEEDED SQLITE CHEMICAL PROFILE COVERAGE; NOT GAS CALIBRATION')
PY
