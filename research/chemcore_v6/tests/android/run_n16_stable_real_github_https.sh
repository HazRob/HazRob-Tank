#!/usr/bin/env bash
set -euo pipefail
mkdir -p /tmp/v6-n16
adb install -r source/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.hazrob.tank/com.servoc.app.chemcore.library.V6StableOnlineGithubActivity
present=0
for i in $(seq 1 110); do
  if adb shell run-as com.hazrob.tank test -f files/v6_stable_online_e2e.json >/dev/null 2>&1; then
    present=1;break
  fi
  sleep 2
done
if [[ "$present" != 1 ]];then
  adb logcat -d -t 4000 | grep -E 'TankOnlineN16|AndroidRuntime|FATAL EXCEPTION' | tail -160 || true
  exit 1
fi
adb exec-out run-as com.hazrob.tank cat files/v6_stable_online_e2e.json > /tmp/v6-n16/online_github_stable_e2e.json
python3 - <<'PY'
import json
from pathlib import Path
r=json.loads(Path('/tmp/v6-n16/online_github_stable_e2e.json').read_text())
print(json.dumps(r,indent=2,ensure_ascii=False))
assert r['status']=='PASS',r
assert r['assertions']>=14,r
assert r['branch'].startswith('qa/v6-stable-online-'),r
assert r['installed_version']=='1.0.1-hr069r1',r
assert r['payload_sha256']=='94f41ba754a1c239122b82081a91e9656012503cebda0eae6473ed8a4e45d7cf',r
assert r['actual_https_download'] is True,r
assert r['production_main_modified'] is False,r
assert r['release_apk_delivered'] is False,r
assert r['real_tank_g3_qualified'] is False,r
print('PASS N16 Android12 actual HTTPS Stable GitHub QA download, SHA256 activation, no main publication')
PY
