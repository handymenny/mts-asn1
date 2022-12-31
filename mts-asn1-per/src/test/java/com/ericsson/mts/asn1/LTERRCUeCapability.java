package com.ericsson.mts.asn1;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class LTERRCUeCapability extends AbstractTests {

    @BeforeAll
    static void init() {
        try {
            asn1Translator = new ASN1Translator(new PERTranslatorFactory(false), Collections.singletonList(S1APAuthWSyncFailure.class.getResourceAsStream("/grammar/LTERRC/EUTRA-RRC-Definitions.asn")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testUeCapabilityInformation() throws Exception {
        test("UL-DCCH-Message", "/data/LTERRC/UL_DCCH/UeCapabilityInformation.bin", "/data/LTERRC/UL_DCCH/UeCapabilityInformation.json", "/data/LTERRC/UL_DCCH/UeCapabilityInformation.xml");
    }

    @Test
    void testUeEutraCapability() throws Exception {
        test("UE-EUTRA-Capability", "/data/LTERRC/UL_DCCH/UeEutraCapability.bin", "/data/LTERRC/UL_DCCH/UeEutraCapability.json", "/data/LTERRC/UL_DCCH/UeEutraCapability.xml");
    }

    @Test
    void testUeEutraCapabilityBandCombinationAdd() throws Exception {
        test("UE-EUTRA-Capability", "/data/LTERRC/UL_DCCH/UeEutraCapabilityBandCombinationAdd.bin", "/data/LTERRC/UL_DCCH/UeEutraCapabilityBandCombinationAdd.json", "/data/LTERRC/UL_DCCH/UeEutraCapabilityBandCombinationAdd.xml");
    }

    @Test
    void testUeEutraCapabilityBandCombinationAdd2() throws Exception {
        test("UE-EUTRA-Capability", "/data/LTERRC/UL_DCCH/UeEutraCapabilityBandCombinationAdd2.bin", "/data/LTERRC/UL_DCCH/UeEutraCapabilityBandCombinationAdd2.json", "/data/LTERRC/UL_DCCH/UeEutraCapabilityBandCombinationAdd2.xml");
    }

    @Test
    void testUeEutraCapabilityBandCombinationReduced() throws Exception {
        test("UE-EUTRA-Capability", "/data/LTERRC/UL_DCCH/UeEutraCapabilityBandCombinationReduced.bin", "/data/LTERRC/UL_DCCH/UeEutraCapabilityBandCombinationReduced.json", "/data/LTERRC/UL_DCCH/UeEutraCapabilityBandCombinationReduced.xml");
    }

    @Test
    void testUEEutraCapabilityRequestDiffFallback() throws Exception {
        test("UE-EUTRA-Capability", "/data/LTERRC/UL_DCCH/UeEutraCapabilityRequestDiffFallback.bin", "/data/LTERRC/UL_DCCH/UeEutraCapabilityRequestDiffFallback.json", "/data/LTERRC/UL_DCCH/UeEutraCapabilityRequestDiffFallback.xml");
    }

    @Test
    void testUEEutraCapabilitySkipFallback() throws Exception {
        test("UE-EUTRA-Capability", "/data/LTERRC/UL_DCCH/UeEutraCapabilitySkipFallback.bin", "/data/LTERRC/UL_DCCH/UeEutraCapabilitySkipFallback.json", "/data/LTERRC/UL_DCCH/UeEutraCapabilitySkipFallback.xml");
    }

    @Test
    void testUeEutraCapabilityKirin() throws Exception {
        test("UE-EUTRA-Capability", "/data/LTERRC/UL_DCCH/UeEutraCapabilityKirin.bin", "/data/LTERRC/UL_DCCH/UeEutraCapabilityKirin.json", "/data/LTERRC/UL_DCCH/UeEutraCapabilityKirin.xml");
    }

    @Test
    void testUeEutraCapabilityMediatek() throws Exception {
        test("UE-EUTRA-Capability", "/data/LTERRC/UL_DCCH/UeEutraCapabilityMediatek.bin", "/data/LTERRC/UL_DCCH/UeEutraCapabilityMediatek.json", "/data/LTERRC/UL_DCCH/UeEutraCapabilityMediatek.xml");
    }

    @Test
    void testUeEutraCapabilityIntel() throws Exception {
        test("UE-EUTRA-Capability", "/data/LTERRC/UL_DCCH/UeEutraCapabilityIntel.bin", "/data/LTERRC/UL_DCCH/UeEutraCapabilityIntel.json", "/data/LTERRC/UL_DCCH/UeEutraCapabilityIntel.xml");
    }

    @Test
    void testUeEutraCapabilityIntel2() throws Exception {
        test("UE-EUTRA-Capability", "/data/LTERRC/UL_DCCH/UeEutraCapabilityIntel2.bin", "/data/LTERRC/UL_DCCH/UeEutraCapabilityIntel2.json", "/data/LTERRC/UL_DCCH/UeEutraCapabilityIntel2.xml");
    }


}
