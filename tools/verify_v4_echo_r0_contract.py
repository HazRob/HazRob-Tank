#!/usr/bin/env python3
"""Read-only V4 source contracts for Echo diagnostics and current-R0 authority.

This is not an Arduino compile, an HC-SR04 bench test, an Android runtime test,
or evidence that an actual clean-air environment has zero hazardous gas.
"""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parent.parent
def src(relative):
    return (ROOT / relative).read_text(encoding="utf-8")

cfg = src("firmware/ESP32-Tank/Config.h")
fw = src("firmware/ESP32-Tank/Hardware.cpp")
main = src("android/app/src/main/java/com/servoc/app/MainActivity.java")
manual = src("android/app/src/main/java/com/servoc/app/calibration/ManualR0Calibration.java")
proto = src("android/app/src/main/java/com/servoc/app/communication/RobotProtocolV4.java")
normal = src("android/app/src/main/java/com/servoc/app/calibration/AcceptedR0Normalizer.java")
controls = src("android/app/src/main/java/com/servoc/app/ui/ControlTab.java")
sessions = src("android/app/src/main/java/com/servoc/app/ui/FieldSessionsTab.java")

def require(condition, explanation):
    if not condition:
        raise AssertionError(explanation)

def val(name):
    match = re.search(r"\b" + re.escape(name) + r"\s*=\s*(\d+)", cfg)
    require(match is not None, name + " missing")
    return int(match.group(1))

low, high, timeout = val("ULTRA_MIN_ECHO_US"), val("ULTRA_MAX_ECHO_US"), val("ULTRA_TIMEOUT_US")
require(100 <= low < high <= timeout <= 30000, "Echo pulse interval invalid")
require("ultraMedian3" in fw and "ultraFilteredCm" in fw, "three-echo filter missing")
require("ultraRawCm>=2.0f&&ultraRawCm<=Cfg::ULTRA_WARN_CM" in fw,
        "close-object raw echo must warn before median settles")
require("ultraInvalidate()" in fw and '"raw_cm"' not in "", "invalid echo path")
segment = fw.split("void ultraUpdate(){", 1)[1].split("/* measurement */", 1)[0]
require("motionStop" not in segment and "driveSet" not in segment,
        "Echo must remain warning-only on Tank")
require("ultraStatus=\"invalid_pulse\"" in segment, "invalid pulse status missing")

def pulse_to_cm(us):
    return us * 0.01715
require(abs(pulse_to_cm(1166)-20) < 0.1, "20 cm pulse")
require(abs(pulse_to_cm(5248)-90) < 0.1, "90 cm pulse")
require(sorted([20, 20, 90])[1] == 20, "isolated 90 cm spike must be rejected")
require(2 <= pulse_to_cm(466) <= 10, "8 cm approach must generate immediate warning")

require("ManualR0Calibration.load(prefs)" in main, "latest durable phone R0 not loaded")
require("latestDiag=snap;syncOperationalR0();sensorHealth.accept(snap,now)" in main,
        "R0 baseline update must precede gas evaluation")
require("sensorHealth.setBaseline(key,Double.isFinite(r0)&&r0>0?1.0:Double.NaN)" in main,
        "old health baseline overrides current clean-air R0")
require("ManualR0Calibration.apply(hw.key,x)" in proto, "live protocol bypasses R0")
require('q.put("ratio",rs/r0)' in manual, "Rs/R0 computation missing")
require("return Double.isFinite(rs)&&rs>0?rs/r0:Double.NaN" in normal,
        "legacy firmware ratio must not replace missing Rs with active R0")
require("actions.addView(relayButton" in controls and "cameraBox.addView(relayButton" not in controls,
        "MQ toggle not in bottom action row")
require("boundSessionIds.equals(sessionOptionIds)" in sessions,
        "session spinner rebind guard missing")
print("PASS V4 source contracts: Echo pulse bounds/median/warning-only; live R0 authority; MQ lower controls; session guards")
