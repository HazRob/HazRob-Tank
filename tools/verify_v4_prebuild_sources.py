#!/usr/bin/env python3
"""Fast, source-only HazRob Tank V4 prebuild gate. Does not call Gradle or build APK."""
from pathlib import Path

root=Path(__file__).resolve().parent.parent
tests=root/"android/app/src/test/java"
source=root/"android/app/src/main/java"
required=[
    "com/servoc/app/ui/FieldSessionsTab.java",
    "com/servoc/app/ui/ReportsTab.java",
    "com/servoc/app/ui/DeveloperR0Tab.java",
    "com/servoc/app/ui/DeveloperExperimentsTab.java",
    "com/servoc/app/communication/RobotApi.java",
    "com/servoc/app/calibration/FirmwareR0Contract.java",
    "com/servoc/app/chemcore/ChemCoreLabQuality.java",
]
for p in required:
    assert (source/p).is_file(),f"Missing Tank source overlay: {p}"

java_test_files=list(tests.rglob("*.java"))
assert len(java_test_files)>=25,f"Unexpected V4 test suite shrinkage: {len(java_test_files)}"
for p in java_test_files:
    t=p.read_text(encoding="utf-8")
    assert "Files.readString(" not in t,f"Java source API mismatch in {p.relative_to(root)}"
    assert "Thread.sleep(360000)" not in t,f"Blocking calibration test in {p.relative_to(root)}"

field=(source/"com/servoc/app/ui/FieldSessionsTab.java").read_text(encoding="utf-8")
assert "host.io().execute(()->{" in field
assert "repo.verifySessionSchema()" in field
assert "if(disposed){refreshing=false;return;}" in field

r0=(source/"com/servoc/app/ui/DeveloperR0Tab.java").read_text(encoding="utf-8")
for x in ("calibrationStart(300,60)","FirmwareR0Contract.committedValues(response)",
          "ManualR0Calibration.saveFirmware("):
    assert x in r0,x

report=(source/"com/servoc/app/ui/ReportsTab.java").read_text(encoding="utf-8")
for x in ("fieldSessions.approvedSessionsForSite(effectiveSite)",
          "if(released||chartGeneration!=request||selectedId!=experimentId)"):
    assert x in report,x

build=(root/".github/workflows/build-tank-android-v4.yml").read_text(encoding="utf-8")
assert "workflow_dispatch:" in build
assert "assert hashlib.sha256(payload).hexdigest()==manifest['payload_sha256']" in build

print(f"PASS: V4 source-only prebuild checks, {len(java_test_files)} test overlays inspected; NO APK BUILT")
