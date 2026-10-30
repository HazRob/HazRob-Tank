package com.servoc.app.chemcore.library;

import android.app.Activity;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/**
 * Network-backed CI-only Android 12 Stable channel integration. The branch
 * is ephemeral and NEVER named 'main', the app update host is unmodified.
 */
public final class V6StableOnlineGithubActivity extends Activity {
    private static final String URL_SOURCE=
        "https://raw.githubusercontent.com/HazRob/HazRob-Tank/__QA_BRANCH__/updates/chemcore-stable/manifest.json";
    private int assertions;
    private void require(boolean condition,String message){
        assertions++;
        if(!condition)throw new AssertionError("N16-"+assertions+": "+message);
    }
    private void save(JSONObject result)throws Exception{
        try(FileOutputStream out=openFileOutput("v6_stable_online_e2e.json",MODE_PRIVATE)){
            out.write(result.toString(2).getBytes(StandardCharsets.UTF_8));
            out.flush();out.getFD().sync();
        }
    }
    private void verify()throws Exception{
        final String appVersion="6.0";
        final String qaBranch="__QA_BRANCH__";
        require(!qaBranch.equals("main")&&qaBranch.startsWith("qa/v6-stable-online-"),
            "ephemeral test-only branch, no production channel");
        ChemCoreLibraryStore store=new ChemCoreLibraryStore(this);
        require(store.activePayload()==null,"clean emulated ChemCore library store");
        ChemCoreLibraryManager manager=new ChemCoreLibraryManager(this);
        manager.setManifestUrl(URL_SOURCE);
        require(manager.usingDeveloperManifestOverride(),
            "test-only endpoint is explicit isolated override");
        URL u=new URL(URL_SOURCE);
        ChemCoreLibraryManager.validateGitHubManifestUrl(u);
        ChemCoreLibraryUpdateClient client=new ChemCoreLibraryUpdateClient();
        ChemCoreLibraryUpdateClient.CheckResult manifest=client.check(u);
        require(manifest.manifest.libraryVersion.equals("1.0.1-hr069r1"),
            "external temporary manifest has test-only newer version");
        require(manifest.manifest.chemicalCount==652 &&
                manifest.manifest.calibrationCount==0 &&
                manifest.manifest.modelCount==0,
            "research provenance cannot promote M4 evidence");
        require(ChemCoreLibraryVerifier.verifyManifestOnly(manifest.manifest,appVersion,
                ChemCoreLibraryManager.REQUIRED_SAMPLING_PROFILE).accepted,
            "network manifest compatible with V6 real sampling contract");
        ChemCoreLibraryManager.CheckSummary available=manager.check(appVersion);
        require(available.updateAvailable,"actual remote GitHub branch discovered");
        require(available.remote.manifest.payloadSha256.equals(
                manifest.manifest.payloadSha256),"repeat HTTPS manifest same checksum");
        ChemCoreLibraryStore.InstallResult install=manager.install(available,appVersion,
                ()->false);
        require(install.installed,"real HTTPS 1.7 MB payload downloaded SHA-verified and activated");
        require(store.activePayload()!=null&&store.activePayload().isFile(),
            "real HTTPS package physically installed under private app files");
        require(store.activeSha256().equalsIgnoreCase(manifest.manifest.payloadSha256),
            "activated payload SHA-256 matches published test manifest");
        require(ChemCoreLibraryVerifier.verifyPayload(store.activePayload(),
                manifest.manifest,appVersion).accepted,
            "installed remote bytes pass complete semantic verifier");
        require(!manager.check(appVersion).updateAvailable,
            "same remote test snapshot is not offered again");
        require(!manager.install(manager.check(appVersion),appVersion,
                ()->true).installed,
            "measurement-active check cannot install another same snapshot");
        require(!store.rollbackToPrevious(false,appVersion).installed,
            "cannot roll back before a previous verified revision exists");
        JSONObject result=new JSONObject().put("status","PASS")
            .put("schema","hazrob-tank-v6-n16-real-https-github-android12/1")
            .put("assertions",assertions).put("branch",qaBranch)
            .put("installed_version",store.activeVersion())
            .put("payload_sha256",store.activeSha256())
            .put("manifest_source",URL_SOURCE)
            .put("actual_https_download",true)
            .put("production_main_modified",false)
            .put("release_apk_delivered",false).put("real_tank_g3_qualified",false);
        save(result);
    }
    @Override public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        // CI-only executable test Activity: avoid doing network I/O on the UI
        // thread, while writing an auditable result for adb run-as.
        new Thread(()->{
            try{verify();}
            catch(Throwable e){
                android.util.Log.e("TankOnlineN16","online Stable check failed",e);
                try{save(new JSONObject().put("status","FAIL")
                    .put("assertions",assertions).put("error",e.toString()));}
                catch(Exception ignored){}
            }
        },"v6-stable-online-e2e").start();
    }
}