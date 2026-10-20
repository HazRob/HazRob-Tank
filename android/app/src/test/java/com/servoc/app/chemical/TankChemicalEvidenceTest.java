package com.servoc.app.chemical;

import org.junit.Test;
import static org.junit.Assert.*;

public final class TankChemicalEvidenceTest {
    @Test public void aPubchemIdentityDoesNotClaimQsprOrQuantitativePpm(){
        TankChemicalEvidence.Flags f=new TankChemicalEvidence.Flags(false,false,false,true);
        assertFalse(f.referenceAcceptedCurve);
        assertTrue(f.describe().contains("معرف PubChem"));
        assertTrue(f.describe().contains("لا يمنع الحساب"));
        assertFalse(f.hasLocalMethod());
    }
    @Test public void aStoredCurveWithoutValidationIsNotPpmProof(){
        TankChemicalEvidence.Flags f=new TankChemicalEvidence.Flags(true,true,false,false);
        assertTrue(f.describe().contains("منحنى المعايرة: متاح للحساب"));
        assertTrue(f.hasLocalMethod());
        assertTrue(f.describe().contains("لا يمنع الحساب"));
    }
    @Test public void formulaAloneIsAnAvailableComputationalPath(){
        TankChemicalEvidence.Flags f=new TankChemicalEvidence.Flags(false,false,false,false,true);
        assertTrue(f.hasLocalMethod());
        assertTrue(f.describe().contains("معادلة أو نموذج حسابي: متاح"));
    }
}
