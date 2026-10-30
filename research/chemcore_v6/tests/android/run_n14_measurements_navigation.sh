#!/usr/bin/env bash
set -euo pipefail
mkdir -p /tmp/v6-n14
adb install -r source/app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n com.servoc.app/com.servoc.app.ui.V6MeasurementsNavigationFixtureActivity
ready=0
for i in $(seq 1 45);do
  if adb shell run-as com.servoc.app test -f files/n14_measurements_navigation.json >/dev/null 2>&1;then
    ready=1;break
  fi
  sleep 2
done
if [[ "$ready" != 1 ]];then
  adb logcat -d -t 1200 | grep -E 'N14|FATAL EXCEPTION|AndroidRuntime' | tail -120 || true
  exit 1
fi
adb exec-out run-as com.servoc.app cat files/n14_measurements_navigation.json >/tmp/v6-n14/navigation.json
python3 - <<'PY'
import json
x=json.load(open('/tmp/v6-n14/navigation.json'))
print(json.dumps(x,indent=2,ensure_ascii=False))
assert x['status']=='PASS',x
assert x['main_navigation_tabs']==5 and x['food_protocols']==2,x
assert x['assertions']>=14,x
PY
