#!/usr/bin/env python3
"""Static-only HazRob Tank V4 operator/laboratory integration checks.

This script never invokes Gradle, packages APKs, changes application data, or
declares physical sensor calibration. Run on the Tank source checkout.
"""
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
checks=[]

def read(path):
    p=ROOT/path
    if not p.is_file():
        raise AssertionError(f"Required source absent: {path}")
    return p.read_text(encoding="utf-8")

def require(text,needle,label):
    if needle not in text:
        raise AssertionError(f"{label}: required source marker absent: {needle}")
    checks.append(label)

sessions=read("android/app/src/main/java/com/servoc/app/ui/FieldSessionsTab.java")
require(sessions,"private static final class SessionData","snapshot-immutable-by-structure")
require(sessions,"host.io().execute(()->{","field-sessions-background-queries")
require(sessions,"repo.verifySessionSchema()","migrated-DB-schema-guard")
require(sessions,"if(refreshing){refreshQueued=true;return;}","coalesced-UI-refresh")
require(sessions,"if(!requestedSite.equals(selectedSiteId))","discard-stale-site-result")
require(sessions,'SiteSummaryExportActivity.start(scroll.getContext(),selectedSiteId,"pdf")',"site-PDF-route")
require(sessions,'SiteSummaryExportActivity.start(scroll.getContext(),selectedSiteId,"xlsx")',"site-XLSX-route")
require(sessions,'host.requestFieldSessionExport("pdf",q.id)',"exact-session-PDF-route")
require(sessions,'host.requestFieldSessionExport("xlsx",q.id)',"exact-session-XLSX-route")
assert "selectingScope||refreshing||" not in sessions, "user interaction must not be dropped during refresh"
checks.append("site-selection-during-refresh")

activity=read("android/app/src/main/java/com/servoc/app/MainActivity.java")
require(activity,"pendingFieldSessionPicker=true","no-overlapping-report-picker")
require(activity,"selected=new FieldSessionRepository(db).fieldSessionById(fs)","report-lookup-in-worker")
require(activity,"if(!selectedRow.approved())","approved-session-before-picker")
require(activity,'pendingFieldSessionPicker=false;pendingFieldSessionId="";',"report-picker-reset-on-cancel")
require(activity,"FieldSessionReportExporter.writePdf(out,db,fieldId,chemicals)","session-pdf-exact-id")
require(activity,"FieldSessionReportExporter.writeXlsx(out,db,fieldId,chemicals)","session-xlsx-exact-id")

reports=read("android/app/src/main/java/com/servoc/app/ui/ReportsTab.java")
require(reports,"fieldSessions.approvedSessionsForSite(chosenSiteId)","approved-site-session-scope")
require(reports,"if(rebindingScope||position!=fieldSelector.getSelectedItemPosition())return","spinner-guard")
require(reports,"UiKit.lightOptions(scroll.getContext(),labels.toArray(new String[0]))","outlined-black-text-dropdown")
require(reports,'else host.requestFieldSessionExport(format,chosenSessionId)',"reports-exact-session-route")

experiments=read("android/app/src/main/java/com/servoc/app/ui/DeveloperExperimentsTab.java")
require(experiments,"qualifiedReferenceChannels(","reference-8-channel-validation")
require(experiments,"TankReferenceR0Gate.qualifiedEight(channels,controllerId)","reference-gate-centralization")
gate=read("android/app/src/main/java/com/servoc/app/calibration/TankReferenceR0Gate.java")
require(gate,'!controllerId.equals(q.optString("r0_device_id",""))',"eight-channel-source-device-binding")
require(gate,'!q.optBoolean("r0_provenance_complete",false)',"reject-reference-without-R0-evidence")
require(experiments,"recorded_r0_provenance","reference-frozen-r0-provenance")
require(experiments,"ManualR0Calibration.getDeviceId(key)","reference-device-binding")
assert "private void openR0Maintenance()" not in experiments,"Duplicate R0 editor must not remain"
checks.append("single-authority-R0-editor")

calibration=read("android/app/src/main/java/com/servoc/app/ui/DeveloperR0Tab.java")
require(calibration,"host.robotApi().calibrationStart(300,60)","firmware-owned-300+60")
require(calibration,"FirmwareR0Contract.committedValues(response)","committed-R0-only")
require(calibration,"ManualR0Calibration.saveFirmware(","durable-phone-R0-mirror")
require(calibration,"تأكيد التجاوز","explicit-manual-calibration-override")
r0=read("android/app/src/main/java/com/servoc/app/calibration/FirmwareR0Contract.java")
require(r0,'SCHEMA="tank-r0-fw-v4/1"',"firmware-R0-schema-version")
require(r0,"p.counts.get(key)<10","eight-channel-R0-min-samples")

quality=read("android/app/src/main/java/com/servoc/app/chemcore/ChemCoreLabQuality.java")
require(quality,"controllerId.equals(sensorDeviceId)","laboratory-cross-robot-block")
require(quality,"referenceFromDb>=0?referenceFromDb:0","M4-from-curated-DB-not-telemetry")
require(quality,'channel.put("device_id",sensorDeviceId)',"lab-calibration-provenance")

firmware=read("firmware/ESP32-Tank/ESP32-Tank.ino")
require(firmware,'web.on("/r0"',"ESP32-firmware-calibration-route")

stable=read("tools/prepare_v4_stable_source.py")
require(stable,"version='4.0'","stable-android-version-compatible")
require(stable,'"calibration_count":0',"stable-non-M4-evidence-gate")
workflow=read(".github/workflows/build-tank-android-v4.yml")
require(workflow,"workflow_dispatch:","manual-build-only")
require(workflow,"assert hashlib.sha256(payload).hexdigest()==manifest['payload_sha256']","stable-payload-hash-gate")
require(workflow,"assert root+'chemcore-stable-snapshot.json' in names","stable-inside-APK-gate")

print(f"PASS: {len(checks)} static source invariants; NO Android build or hardware test executed")
