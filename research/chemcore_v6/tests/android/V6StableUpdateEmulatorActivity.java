package com.servoc.app.chemcore.library;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.content.SharedPreferences;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Disposable Android 12 CI Activity for genuine offline ChemCore Stable
 * transactions. The files are derived from the pinned HR069 research snapshot,
 * not downloaded or activated in any user's live application.
 *
 * It validates installer atomicity, downgrade restrictions, user measurement
 * blocking, tamper rejection and rollback before V6 APK release.
 */
public final class V6StableUpdateEmulatorActivity extends Activity {
    private int assertions=0;
    private void check(boolean ok,String message) {
        assertions++;
        if(!ok)throw new AssertionError("V6-STABLE-"+assertions+": "+message);
    }
    private File assetFile(String name)throws Exception {
        File out=new File(getCacheDir(),"test-"+name);
        try(InputStream source=getAssets().open("test_only/v6_stable/"+name);
            FileOutputStream sink=new FileOutputStream(out)){
            byte[] buf=new byte[8192];int n;
            while((n=source.read(buf))!=-1)if(n>0)sink.write(buf,0,n);
            sink.flush();sink.getFD().sync();
        }
        return out;
    }
    private ChemCoreLibraryManifest manifest(String name)throws Exception {
        try(InputStream in=getAssets().open("test_only/v6_stable/"+name)){
            byte[] all=new byte[16384],part=new byte[4096];int used=0,n;
            while((n=in.read(part))!=-1){
                if(n==0)continue;
                if(used+n>all.length)throw new IllegalArgumentException("fixture manifest too large");
                System.arraycopy(part,0,all,used,n);used+=n;
            }
            return ChemCoreLibraryManifest.fromJson(
                new JSONObject(new String(all,0,used,StandardCharsets.UTF_8)));
        }
    }
    private void runTest()throws Exception {
        final String appVersion="6.0";
        final ChemCoreLibraryStore store=new ChemCoreLibraryStore(this);
        final ChemCoreLibraryManifest m1=manifest("v1-manifest.json");
        final ChemCoreLibraryManifest m2=manifest("v2-manifest.json");
        final File payload=assetFile("chemcore-stable-snapshot.json");
        check(store.activePayload()==null,"fresh installation begins without an active remote snapshot");
        check(m1.validate().isEmpty()&&m2.validate().isEmpty(),
            "both research-source test Stable manifests conform to schema v2");
        check(!m1.libraryVersion.equals(m2.libraryVersion),
            "update test uses two unique versions");
        check(ChemCoreLibraryVerifier.verifyPayload(payload,m1,appVersion).accepted,
            "actual embedded snapshot validates against first source-qualified manifest");
        check(ChemCoreLibraryVerifier.verifyPayload(payload,m2,appVersion).accepted,
            "same research snapshot validates second test version without unsafe M4 promotion");
        check(!store.installVerifiedSnapshot(m1,payload,appVersion,true,false).installed,
            "measurement in progress forbids library activation");
        check(store.activePayload()==null,"blocked installation cannot activate a partial package");

        File tamper=new File(getCacheDir(),"tampered-source.json");
        Files.copy(payload.toPath(),tamper.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        try(FileOutputStream out=new FileOutputStream(tamper,true)){
            out.write(0x30);out.flush();out.getFD().sync();
        }
        check(!ChemCoreLibraryVerifier.verifyPayload(tamper,m1,appVersion).accepted,
            "tampered payload fails size and SHA256 checks");
        check(!store.installVerifiedSnapshot(m1,tamper,appVersion,false,false).installed,
            "tampered payload cannot be installed");
        check(store.activePayload()==null,"tamper rejection preserves original empty pointer");

        check(store.installVerifiedSnapshot(m1,payload,appVersion,false,false).installed,
            "first verified Stable package activates");
        check(m1.libraryVersion.equals(store.activeVersion()),
            "first version pointer matches signed-off manifest");
        check(m1.payloadSha256.equalsIgnoreCase(store.activeSha256()),
            "active package checksum is recorded");
        check(!store.installVerifiedSnapshot(m1,payload,appVersion,false,false).installed,
            "identical library replay is idempotent");
        check(store.installVerifiedSnapshot(m2,payload,appVersion,false,false).installed,
            "newer compatible manifest activates");
        check(m2.libraryVersion.equals(store.activeVersion()),
            "newer Stable becomes active");
        check(!store.installVerifiedSnapshot(m1,payload,appVersion,false,false).installed,
            "silent downgrade is refused");
        check(!store.rollbackToPrevious(true,appVersion).installed,
            "rollback blocked during active measurement");
        check(m2.libraryVersion.equals(store.activeVersion()),
            "blocked rollback retains latest pointer");

        SharedPreferences pointers=getSharedPreferences("chemcore_library_store_v2",
            Context.MODE_PRIVATE);
        File previous=new File(pointers.getString("previous_path",""));
        check(previous.isFile(),"previous package physically exists on disk");
        byte[] previousBytes=Files.readAllBytes(previous.toPath());
        try(FileOutputStream out=new FileOutputStream(previous,true)){
            out.write(0x31);out.flush();out.getFD().sync();
        }
        check(!store.rollbackToPrevious(false,appVersion).installed,
            "corrupt previous package fails checksum before pointer swap");
        check(m2.libraryVersion.equals(store.activeVersion()),
            "corrupt rollback target cannot replace active working Stable");
        try(FileOutputStream out=new FileOutputStream(previous,false)){
            out.write(previousBytes);out.flush();out.getFD().sync();
        }
        check(store.rollbackToPrevious(false,appVersion).installed,
            "verified previous Stable package restored atomically");
        check(m1.libraryVersion.equals(store.activeVersion()),
            "rollback selects previous version");
        check(store.activePayload()!=null&&store.activePayload().isFile(),
            "rollback target remains accessible and self-contained");
        check(store.rollbackToPrevious(false,appVersion).installed,
            "verified rollback to former newest snapshot also works");
        check(m2.libraryVersion.equals(store.activeVersion()),
            "rollback swaps old/new pointers correctly");

        java.net.URL allowed=new java.net.URL(
            "https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/updates/chemcore-stable/manifest.json");
        ChemCoreLibraryManager.validateGitHubManifestUrl(allowed);
        ChemCoreLibraryUpdateClient.requireSameApprovedGithubDirectory(
            allowed,new java.net.URL(
            "https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/updates/chemcore-stable/chemcore-stable-snapshot.json"));
        check(true,"official HazRob Stable manifest and sibling payload accepted");
        boolean outsideRejected=false;
        try{
            ChemCoreLibraryManager.validateGitHubManifestUrl(new java.net.URL(
                "https://attacker.example/HazRob/HazRob-Tank/main/updates/chemcore-stable/manifest.json"));
        }catch(IllegalArgumentException expected){outsideRejected=true;}
        check(outsideRejected,"third-party hostname cannot issue ChemCore updates");
        boolean traverseRejected=false;
        try{
            ChemCoreLibraryUpdateClient.requireSameApprovedGithubDirectory(
                allowed,new java.net.URL("https://raw.githubusercontent.com/HazRob/"+
                    "HazRob-Tank/main/updates/chemcore-stable/../secrets.json"));
        }catch(IllegalArgumentException expected){traverseRejected=true;}
        check(traverseRejected,"manifest sibling path cannot traverse out of source directory");

        JSONObject report=new JSONObject().put("schema","hazrob-tank-v6-stable-offline-android12/1")
            .put("status","PASS")
            .put("assertions",assertions)
            .put("original_research_priors",49)
            .put("canonical_molecular_identities",652)
            .put("active_version",store.activeVersion())
            .put("test_hardware_connected",false)
            .put("remote_stable_manifest_published",false)
            .put("apk_release_produced",false)
            .put("rollback_verified",true)
            .put("tampered_library_rejected",true)
            .put("measurement_active_update_rejected",true);
        save(report);
    }
    private void save(JSONObject report)throws Exception {
        try(FileOutputStream out=openFileOutput("v6_stable_update_e2e.json",MODE_PRIVATE)){
            out.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try{runTest();}
        catch(Throwable problem) {
            android.util.Log.e("TankStableE2E","Stable source test failed",problem);
            try{save(new JSONObject().put("status","FAIL").put("assertions",assertions)
                .put("error",problem.toString()));}catch(Exception ignored){}
        }
    }
}