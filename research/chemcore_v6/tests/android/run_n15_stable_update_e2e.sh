#!/usr/bin/env bash
set -euo pipefail
mkdir -p /tmp/v6-n15
adb install -r source/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.hazrob.tank/com.servoc.app.chemcore.library.V6StableUpdateEmulatorActivity
ready=0
for i in $(seq 1 90);do
  if adb shell run-as com.hazrob.tank test -f files/v6_stable_update_e2e.json >/dev/null 2>&1;then
    ready=1;break
  fi
  sleep 2
done
if [[ "$ready" != 1 ]];then
  adb logcat -d -t 3500 | grep -E 'TankStableE2E|FATAL EXCEPTION|AndroidRuntime' | tail -140 || true
  exit 1
fi
adb exec-out run-as com.hazrob.tank cat files/v6_stable_update_e2e.json   > /tmp/v6-n15/stable_update_e2e.json
python3 - <<'PY'
import json
from pathlib import Path
p=Path('/tmp/v6-n15/stable_update_e2e.json')
r=json.loads(p.read_text())
print(json.dumps(r,indent=2,ensure_ascii=False))
assert r['status']=='PASS',r
assert r['assertions']>=25,r
assert r['original_research_priors']==49,r
assert r['canonical_molecular_identities']==652,r
assert r['active_version']=='1.0.2-hr069r1',r
assert r['rollback_verified'] and r['tampered_library_rejected'],r
assert r['measurement_active_update_rejected'],r
assert not r['test_hardware_connected'] and not r['remote_stable_manifest_published'],r
assert not r['apk_release_produced'],r
print('PASS V6 N15 genuine Android 12 Stable install/rollback/tamper/read-only source testing — no production APK')
PY
