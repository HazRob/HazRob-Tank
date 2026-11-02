#!/usr/bin/env python3
"""Source/flow security contract; Android execution remains separately gated."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[3]
BASE=ROOT/"android/app/src/main/java/com/servoc/app"
def read(path):return (BASE/path).read_text(encoding="utf-8")
class FeedbackPath(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ui=read("MainActivity.java")
        cls.measure=read("ui/MeasurementsTab.java")
        cls.queue=read("chemcore/nextintegration/NextFeedbackQueue.java")
        cls.sender=read("chemcore/nextintegration/FeedbackGithubUploader.java")
        cls.auth=read("chemcore/nextintegration/FeedbackGithubCredential.java")
        cls.settings=read("ui/SettingsTab.java")
    def test_floating_opposite_refresh_hidden_until_decision(self):
        self.assertIn('Gravity.TOP|Gravity.LEFT',self.ui)
        self.assertIn('Gravity.TOP|Gravity.RIGHT',self.ui)
        self.assertIn('withRefresh(measurementsTab.view(),54,true)',self.ui)
        self.assertIn('currentFeedbackDecisionUid()',self.ui)
        self.assertIn('feedbackAction.setVisibility(selectedTab==1&&!uid.isEmpty()',self.ui)
        self.assertIn('new android.app.AlertDialog.Builder(this)',self.ui)
        self.assertIn('setNegativeButton("إلغاء"',self.ui)
        self.assertIn('setPositiveButton("تأكيد"',self.ui)
        self.assertIn('note.getText().toString().trim()',self.ui)
        self.assertIn('finished.analysis.optString("decision_uid"',self.measure.replace("result.analysis","finished.analysis") if False else self.measure.replace("result.analysis","finished.analysis"))
    def test_no_live_snapshot_or_old_legacy_reanalysis_in_feedback(self):
        callback=self.ui.split('private void openCurrentInstantFeedback(){',1)[1].split('private void queueDriveSend(){',1)[0]
        self.assertIn('NextDecisionStore(db)',callback)
        self.assertIn('.byId(uid)',callback)
        self.assertIn('NextFeedbackQueue',callback)
        self.assertNotIn('TankInstantAnalyzer.analyze(',callback)
        self.assertNotIn('latestDiag',callback)
    def test_local_json_preserves_original_and_consents_explicitly(self):
        for expected in ('"original_analysis"','"original_mq_measurements"','"app_version"',
                         '"model_version"','"decision_uid"','"snapshot_sha256"',
                         '"operator_classification"','"public_share_consent"'):
            self.assertIn(expected,self.queue)
        self.assertIn('publicShareConsent?PENDING_AUTH:LOCAL_ONLY',self.queue)
        self.assertIn("publicShareConsent?1:0",self.queue)
        self.assertIn("CREATE TRIGGER feedback_original_immutable",self.queue)
        self.assertIn("CREATE TRIGGER feedback_no_delete",self.queue)
    def test_upload_only_after_auth_and_actual_github_receipt(self):
        self.assertIn("isConfigured(context)",self.sender)
        self.assertIn('Authorization","Bearer "+token',self.sender)
        self.assertIn("NET_CAPABILITY_VALIDATED",self.sender)
        self.assertIn("feedback/records/",self.sender)
        self.assertIn('if(status!=201)',self.sender)
        self.assertIn("remoteReceiptIfSame",self.sender)
        self.assertIn("confirmRemoteReceipt",self.sender)
        self.assertIn('new String(Base64.decode(content',self.sender)
        self.assertIn("expectedSha.equals(sha(received))",self.sender)
        self.assertIn('BRANCH="main"',self.sender)
    def test_no_embedded_credentials_encrypted_token_settings_only(self):
        self.assertIn('"AndroidKeyStore"',self.auth)
        self.assertIn("AES/GCM/NoPadding",self.auth)
        self.assertIn('KeyProperties.PURPOSE_ENCRYPT',self.auth)
        self.assertIn('KeyProperties.PURPOSE_DECRYPT',self.auth)
        self.assertIn('token.startsWith("github_pat_")',self.auth)
        self.assertIn("FeedbackGithubCredential.clear",self.settings)
        self.assertIn("openFeedbackCredentialDialog",self.settings)
        self.assertIn("startFeedbackConnectivityMonitor()",self.ui)
        self.assertIn("stopFeedbackConnectivityMonitor()",self.ui)
        for source in (self.ui,self.measure,self.queue,self.sender,self.auth,self.settings):
            self.assertNotIn("Skwhw_1166",source)
    def test_no_scientific_training_or_g3_claim(self):
        self.assertIn('"physical_g3_qualified",false',self.queue)
        self.assertIn('"ppm_certified",false',self.queue)
        self.assertIn('"training_source_promotion_authorized",false',self.queue)
if __name__=="__main__":unittest.main(verbosity=2)
