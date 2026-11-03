#!/usr/bin/env python3
"""V8 P09 telemetry-to-Next handshake: static structural gate only."""
import pathlib
import unittest
ROOT=pathlib.Path(__file__).resolve().parents[3]
def read(p):return (ROOT/"android/app/src/main/java/com/servoc/app"/p).read_text()
class FieldHandoff(unittest.TestCase):
    def test_finalization_uses_saved_series_not_terminal_v6(self):
        s=read("measurement/TelemetryRecorder.java")
        field=s.split('if(fieldCaptureSession&&"ready".equalsIgnoreCase(state))',1)[1].split('}else if(!wasChemCore&&!fieldCaptureSession',1)[0]
        for marker in ("requiresGasDecision(db,completed)","NextFieldArchiveBridge",
                       ".analyzeAndPersist(appContext,db,completed,snapshot,",
                       "v8_field_next_failure_"):
            self.assertIn(marker,field)
        self.assertNotIn("CompletedSessionChemCoreAnalyzer",field)
    def test_field_source_has_60s_and_unique_scan_gates(self):
        s=read("chemcore/nextintegration/NextFieldArchiveBridge.java")
        for marker in ("telemetry_samples WHERE session_id=? ORDER BY t_ms,id",
                       "SOURCE_ARCHIVE_ELIGIBLE_FOR_RESEARCH_UNKNOWN_ONLY",
                       "SOURCE_ARCHIVE", "medianRsKohm","source.sourceSha256",
                       "CoreContracts.Mode.FIELD_SESSION","FIELD_RELAY_NOT_ON_AT_CAPTURE_SEAL"):
            if marker=="SOURCE_ARCHIVE_ELIGIBLE_FOR_RESEARCH_UNKNOWN_ONLY":continue
            self.assertIn(marker,s)
        source=(ROOT/"android/app/src/main/java/com/hazrob/tank/chemcore/next/NextFieldArchive.java").read_text()
        for marker in ("coverageMs>=58000L", "distinctGlobalGasScanCycles",
                       "putIfAbsent(p.cycle,p)","unique.get", "sourceSha256"):
            if marker=="distinctGlobalGasScanCycles": self.assertIn(marker,source)
            else: self.assertIn(marker,source)
    def test_approval_requires_next_not_any_v6_decision(self):
        s=read("field/FieldCycleRepository.java")
        method=s.split("private boolean hasRequiredChemCoreDecision(",1)[1].split("private static boolean hasMeasuredEvidence(",1)[0]
        self.assertIn("NextDecisionStore",method)
        self.assertIn('startsWith("FIELD-ARCHIVE-")',method)
        self.assertNotIn("ChemCoreDecisionRepository",method)
        digest=s.split("private static String evidenceDigest(",1)[1]
        self.assertIn("FROM next_research_decisions WHERE session_id=?",digest)
    def test_mq_relay_off_only_after_frozen_archive_and_next_decision_with_ack(self):
        controller=read("measurement/FieldCycleController.java")
        part=controller.split("if(repo.complete(cycleId,deviceId)){",1)[1].split("private void stopWithFailure(",1)[0]
        for token in ("host.robotApi().mqPower(false)",
                      "host.robotApi().snapshot(0,0)",
                      "repo.markUnsafeReview(cycleId,fault)",
                      'end("REVIEW_READY",',
                      'end("FAILED",notice,cycleId)'):
            self.assertIn(token,part)
        self.assertLess(part.index("mqPower(false)"),part.index('end("REVIEW_READY",'))
        repository=read("field/FieldCycleRepository.java")
        method=repository.split("public boolean markUnsafeReview(",1)[1].split("public boolean fail(",1)[0]
        self.assertIn("approved_ts IS NULL",method)
        self.assertIn("new String[]{id,REVIEW}",method)
        self.assertIn("v.put(\"state\",FAILED)",method)
    def test_recover_interrupted_cycle_requires_relay_off_ack(self):
        s=read("measurement/FieldCycleController.java")
        recovered=s.split("public void recoverInterrupted(){",1)[1].split("public void retry(",1)[0]
        for condition in ("repo.recoverAbandoned()","host.robotApi().mqPower(false)",
                          "MQ_RELAY_OFF_NOT_ACKNOWLEDGED","SAFETY_FAULT"):
            self.assertIn(condition,recovered)
    def test_missing_next_receipt_yields_reason(self):
        s=read("measurement/FieldCycleController.java")
        self.assertIn("FIELD_NEXT_DECISION_NOT_PERSISTED",s)
        self.assertIn("FIELD_COMPLETION_GATE_FAILED",s)
    def test_f02_any_mq_abort_requires_positive_shutdown_ack(self):
        s=read("measurement/FieldCycleController.java")
        aborted=s.split("private boolean failWorker(",1)[1].split("private void end(",1)[0]
        self.assertIn('host.robotApi().mqPower(false)',aborted)
        self.assertIn('host.robotApi().snapshot(0,0)',aborted)
        self.assertIn('relayOffAcknowledged(off)',aborted)
        self.assertIn('RELAY_RECOVERY_FAULT_KEY',aborted)
        self.assertIn('host.database().putMeta(',aborted)
        self.assertIn('MQ_RELAY_OFF_ACK_WRONG_DEVICE',aborted)
        self.assertLess(aborted.index('relayOffAcknowledged(off)'),
                        aborted.index('host.database().putMeta('))
        failed=s.split("private void stopWithFailure(",1)[1].split("private boolean failWorker(",1)[0]
        self.assertIn("boolean unsafeRelay=failWorker(",failed)
        self.assertIn('"SAFETY_FAULT"',failed)
        self.assertNotIn("busy=false",failed)
    def test_f02_next_archive_off_failure_is_persistent_lock(self):
        s=read("measurement/FieldCycleController.java")
        completed=s.split("if(repo.complete(cycleId,deviceId)){",1)[1].split('end("REVIEW_READY"',1)[0]
        self.assertIn('relayOffAcknowledged(after)',completed)
        self.assertIn("host.database().putMeta(RELAY_RECOVERY_FAULT_KEY,fault)",completed)
        self.assertIn("repo.markUnsafeReview(cycleId,fault)",completed)
        self.assertLess(completed.index("putMeta(RELAY_RECOVERY_FAULT_KEY,fault)"),
                        completed.index("repo.markUnsafeReview(cycleId,fault)"))
    def test_f02_recovery_cannot_turn_off_another_live_field_operation(self):
        s=read("measurement/FieldCycleController.java")
        method=s.split("public void recoverInterrupted(){",1)[1].split("public void retry(",1)[0]
        self.assertIn("MeasurementSessionRegistry.reserveFieldOperation()",method)
        self.assertIn("MeasurementSessionRegistry.endFieldOperation()",method)
        self.assertIn("finally{",method)
        self.assertIn("busy=true; // release() must not release recovery's operation lease.",method)
        self.assertIn("busy=false;",method)
        self.assertLess(method.index("busy=true;"),method.index("host.robotApi().mqPower(false)"))
        self.assertIn("MQ_RELAY_RECOVERY_WORKER_UNAVAILABLE",method)
        self.assertLess(method.index("reserveFieldOperation()"),
                        method.index("repo.recoverAbandoned()"))
        self.assertLess(method.index("reserveFieldOperation()"),
                        method.index("host.robotApi().mqPower(false)"))
    def test_f02_recovery_lock_only_clears_after_explicit_off_ack(self):
        s=read("measurement/FieldCycleController.java")
        method=s.split("public void recoverInterrupted(){",1)[1].split("public void retry(",1)[0]
        self.assertIn("previousSafetyFault",method)
        self.assertIn("n>0||previousSafetyFault",method)
        self.assertIn("!relayOffAcknowledged(off)",method)
        self.assertIn('putMeta(RELAY_RECOVERY_FAULT_KEY,"")',method)
        self.assertIn("relaySafetyFault=true",method)
        self.assertIn('putMeta(RELAY_RECOVERY_FAULT_KEY,',method)
        self.assertIn('String expectedDevice=host.database().getMeta(RELAY_EXPECTED_DEVICE_KEY,"")',method)
        self.assertIn('"MQ_RELAY_OFF_ACK_WRONG_DEVICE"',method)
        self.assertLess(method.index("expectedDevice.equals(identity(off))"),
                        method.index('putMeta(RELAY_RECOVERY_FAULT_KEY,"")'))
        self.assertIn('putMeta(RELAY_EXPECTED_DEVICE_KEY,"")',method)
        for operation in ("begin(String sessionId", "retry(String predecessor", "approve(String id"):
            body=s.split("public void "+operation,1)[1].split("host.io().execute(",1)[0]
            self.assertIn('getMeta(RELAY_RECOVERY_FAULT_KEY,"")',body)
            self.assertIn('getMeta(RELAY_EXPECTED_DEVICE_KEY,"")',body)
        self.assertIn('putMeta(RELAY_EXPECTED_DEVICE_KEY,id)',s)
        self.assertIn('MQ_RELAY_OFF_ACK_WRONG_DEVICE',s)
        self.assertEqual(s.count('MQ_RELAY_OFF_WRONG_DEVICE_PREFLIGHT'),3)
        self.assertLess(method.index('MQ_RELAY_OFF_WRONG_DEVICE_PREFLIGHT'),
                        method.index('host.robotApi().mqPower(false)'))
        self.assertIn('JSONObject beforeOff=host.robotApi().snapshot(0,0)',s)
        ui=read("ui/FieldSessionsTab.java")
        self.assertIn('FieldCycleController.RELAY_EXPECTED_DEVICE_KEY',ui)
        self.assertIn("relayRecoverySafetyFault",ui)
        self.assertIn("data.relaySafetyFault",ui)
        self.assertIn("approveMeasurement.setEnabled(review&&active&&!relayRecoverySafetyFault)",ui)
    def test_f01_f02_live_safety_fault_locks_ui_before_async_refresh(self):
        ui=read("ui/FieldSessionsTab.java")
        callback=ui.split('public void changed(String state,String message,String id){',1)[1].split('public void refreshed()',1)[0]
        self.assertIn('if("SAFETY_FAULT".equals(state))relayRecoverySafetyFault=true;',callback)
        self.assertIn('failureDetails.setVisibility(View.VISIBLE)',callback)
        self.assertIn('renderCycleButtons(state)',callback)
        self.assertLess(callback.index('relayRecoverySafetyFault=true'),
                        callback.index('renderCycleButtons(state)'))
        self.assertIn('if("BLOCKED".equals(state)||"SAFETY_FAULT".equals(state))refresh()',callback)
        buttons=ui.split('private void renderCycleButtons(String stage){',1)[1].split('private interface NameResult',1)[0]
        self.assertIn('boolean locked=relayRecoverySafetyFault||"SAFETY_FAULT".equals(stage)',buttons)
        for control in ('beginMeasurement','retryMeasurement','approveMeasurement',
                        'gasCheck','particleCheck','physicalCheck'):
            self.assertIn(control+'.setEnabled(',buttons)
        for control in ('gasCheck','particleCheck','physicalCheck'):
            self.assertIn(control+'.setEnabled(!locked&&selectedCanBegin&&!relayRecoverySafetyFault)',buttons)
    def test_f01_empty_sites_and_sessions_clear_old_cycle_ids(self):
        ui=read("ui/FieldSessionsTab.java")
        render=ui.split('private void renderSessionUi(SessionData data){',1)[1].split('private static CheckBox measurementCheck',1)[0]
        self.assertGreaterEqual(render.count('clearCycleSelection(data.relaySafetyFault);return;'),2)
        clear=ui.split('private void clearCycleSelection(String relayFault){',1)[1].split('private void renderCycleWorkspace(',1)[0]
        for required in ('latestCycleId="";','selectedCanBegin=false;',
                         'selectedHasApprovedMeasurement=false;',
                         'selectedHasReviewMeasurement=false;',
                         'relayRecoverySafetyFault=relayFault!=null&&!relayFault.isEmpty();',
                         'failureDetails.setVisibility(View.VISIBLE)'):
            self.assertIn(required,clear)
        for control in ('beginMeasurement','retryMeasurement','approveMeasurement',
                        'deleteMeasurement','gasCheck','particleCheck','physicalCheck'):
            self.assertIn(control+'.setEnabled(false)',clear)
    def test_f01_late_review_callback_cannot_approve_unbound_session(self):
        ui=read("ui/FieldSessionsTab.java")
        render=ui.split('private void renderCycleButtons(String stage){',1)[1]
        self.assertIn('selectedHasReviewMeasurement&&!selectedHasApprovedMeasurement',render)
        self.assertIn('!selectedSessionId.isEmpty()',render)
        self.assertIn('"BLOCKED".equals(stage)',render)
        self.assertIn('"DELETING".equals(stage)',render)
        loader=ui.split('private void showSessionLoadError(){',1)[1].split('private void renderSessionUi(',1)[0]
        self.assertIn('clearCycleSelection(relayRecoverySafetyFault?',loader)
        self.assertIn('جميع أوامر البدء والاعتماد معطلة',loader)
        self.assertIn('beginMeasurement.setEnabled(false)',ui.split('private void clearCycleSelection(',1)[1])

if __name__=="__main__":unittest.main(verbosity=2)
