package com.servoc.app.chemical;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TankChemicalEvidenceTest {
    @Test public void aPubchemIdentityDoesNotClaimQsprOrQuantitativePpm(){
        TankChemicalEvidence.Flags f=new TankChemicalEvidence.Flags(false,false,false,true);
        assertFalse(f.referenceAcceptedCurve);
        assertTrue(f.describe().contains("لا يثبت وجود نموذج QSPR"));
        assertTrue(f.describe().contains("PPM مرجعي: غير مثبت"));
        assertTrue(f.concise().contains("PPM غير مثبت"));
    }
    @Test public void aStoredCurveWithoutValidationIsNotPpmProof(){
        TankChemicalEvidence.Flags f=new TankChemicalEvidence.Flags(true,true,false,false);
        assertTrue(f.describe().contains("منحنى المعايرة: محفوظ"));
        assertTrue(f.describe().contains("دليل PPM مرجعي: غير مثبت"));
    }
}
