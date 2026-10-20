package com.servoc.app.field;
import org.junit.Test;
import static org.junit.Assert.*;
public final class FieldSessionApprovalPolicyTest {
    @Test public void acceptsReadyCapture(){
        assertTrue(FieldSessionApprovalPolicy.completedSuccessfully(
            "{\"terminal_state\":\"ready\",\"sample_count\":10}","{\"measurement\":{\"state\":\"ready\"}}"));
        assertTrue(FieldSessionApprovalPolicy.completedSuccessfully(
            null,"{\"measurement\":{\"state\":\"ready\"}}"));
    }
    @Test public void blocksCancelledErrorAndConflictingEvidence(){
        for(String state:new String[]{"error","idle","cancelled","warmup","measuring"}){
            assertFalse(FieldSessionApprovalPolicy.completedSuccessfully("{\"terminal_state\":\""+state+"\"}","{\"measurement\":{\"state\":\""+state+"\"}}"));
            assertFalse(FieldSessionApprovalPolicy.completedSuccessfully("{\"terminal_state\":\""+state+"\"}","{\"measurement\":{\"state\":\"ready\"}}"));
        }
        assertFalse(FieldSessionApprovalPolicy.completedSuccessfully(null,null));
        assertFalse(FieldSessionApprovalPolicy.completedSuccessfully("{bad}","{\"measurement\":{\"state\":\"ready\"}}"));
    }
}
