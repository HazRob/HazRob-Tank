package com.servoc.app.chemcore.library;

import java.net.URL;
import org.junit.Test;
import static org.junit.Assert.*;

/** Pure URL boundary tests; no network request or installed Stable mutation. */
public final class TankV5GithubSourceBoundaryTest {
    private static URL u(String s)throws Exception{return new URL(s);}
    private static void rejected(String manifest,String payload)throws Exception{
        try{ChemCoreLibraryUpdateClient.requireSameApprovedGithubDirectory(u(manifest),u(payload));
            fail("Untrusted or redirected Stable payload source was accepted");}
        catch(IllegalArgumentException expected){}
    }
    @Test public void exactSameGithubRepositoryDirectoryIsAllowed()throws Exception{
        ChemCoreLibraryUpdateClient.requireSameApprovedGithubDirectory(
            u("https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/manifest.json"),
            u("https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/snapshot.json"));
    }
    @Test public void nonGithubHttpsAndOtherOrganizationsAreRejected()throws Exception{
        rejected("https://example.org/HazRob/repo/stable/manifest.json",
                 "https://example.org/HazRob/repo/stable/snapshot.json");
        rejected("https://raw.githubusercontent.com/Imposter/HazRob-Tank/main/manifest.json",
                 "https://raw.githubusercontent.com/Imposter/HazRob-Tank/main/snapshot.json");
    }
    @Test public void payloadMustStayInSameDirectoryAndHost()throws Exception{
        String m="https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/manifest.json";
        rejected(m,"https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/other/snapshot.json");
        rejected(m,"https://github.com/HazRob/HazRob-Tank/main/stable/snapshot.json");
        rejected(m,"https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/sub/snapshot.json");
        rejected(m,"https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/%2e%2e/snapshot.json");
        rejected(m,"https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/snapshot.json?download=1");
        rejected(m,"https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/snapshot.json#fragment");
    }
    @Test public void rejectSourceQueryAndEmbeddedCredentials()throws Exception{
        String m="https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/manifest.json";
        rejected(m+"?ref=main","https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/snapshot.json");
        rejected("https://person@raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/manifest.json",
                 "https://raw.githubusercontent.com/HazRob/HazRob-Tank/main/stable/snapshot.json");
    }
}
