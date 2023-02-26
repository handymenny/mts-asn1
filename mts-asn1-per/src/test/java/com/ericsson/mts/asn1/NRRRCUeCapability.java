package com.ericsson.mts.asn1;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class NRRRCUeCapability extends AbstractTests {

    @BeforeAll
    static void init() {
        try {
            asn1Translator = new ASN1Translator(new PERTranslatorFactory(false), Collections.singletonList(S1APAuthWSyncFailure.class.getResourceAsStream("/grammar/NRRRC/NR-RRC-Definitions.asn")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testUeMrdcCapability() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-MRDC-Capability", "/data/NRRRC/UeMrdcCapability.bin", "/data/NRRRC/UeMrdcCapability.json", "/data/NRRRC/UeMrdcCapability.xml");
    }

    @Test
    void testUeMrdcCapabilityKirin() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-MRDC-Capability", "/data/NRRRC/UeMrdcCapabilityKirin.bin", "/data/NRRRC/UeMrdcCapabilityKirin.json", "/data/NRRRC/UeMrdcCapabilityKirin.xml");
    }

    @Test
    void testUeMrdcCapabilityMediatek() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-MRDC-Capability", "/data/NRRRC/UeMrdcCapabilityMediatek.bin", "/data/NRRRC/UeMrdcCapabilityMediatek.json", "/data/NRRRC/UeMrdcCapabilityMediatek.xml");
    }

    @Test
    void testUeMrdcCapabilityExynos() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-MRDC-Capability", "/data/NRRRC/UeMrdcCapabilityExynos.bin", "/data/NRRRC/UeMrdcCapabilityExynos.json", "/data/NRRRC/UeMrdcCapabilityExynos.xml");
    }

    @Test
    void testUeMrdcCapabilityIpad() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-MRDC-Capability", "/data/NRRRC/UeMrdcCapabilityIpad.bin", "/data/NRRRC/UeMrdcCapabilityIpad.json", "/data/NRRRC/UeMrdcCapabilityIpad.xml");
    }

    @Test
    void testUeMrdcCapabilityFR2() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-MRDC-Capability", "/data/NRRRC/UeMrdcCapabilityFR2.bin", "/data/NRRRC/UeMrdcCapabilityFR2.json", "/data/NRRRC/UeMrdcCapabilityFR2.xml");
    }

    @Test
    void testUeNrCapability() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-NR-Capability", "/data/NRRRC/UeNrCapability.bin", "/data/NRRRC/UeNrCapability.json", "/data/NRRRC/UeNrCapability.xml");
    }

    @Test
    void testUeNrCapabilityKirin() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-NR-Capability", "/data/NRRRC/UeNrCapabilityKirin.bin", "/data/NRRRC/UeNrCapabilityKirin.json", "/data/NRRRC/UeNrCapabilityKirin.xml");
    }

    @Test
    void testUeNrCapabilityMediatek() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-NR-Capability", "/data/NRRRC/UeNrCapabilityMediatek.bin", "/data/NRRRC/UeNrCapabilityMediatek.json", "/data/NRRRC/UeNrCapabilityMediatek.xml");
    }

    @Test
    void testUeNrCapabilityExynos() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-NR-Capability", "/data/NRRRC/UeNrCapabilityExynos.bin", "/data/NRRRC/UeNrCapabilityExynos.json", "/data/NRRRC/UeNrCapabilityExynos.xml");
    }

    @Test
    void testUeNrCapability3CC() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-NR-Capability", "/data/NRRRC/UeNrCapability3CC.bin", "/data/NRRRC/UeNrCapability3CC.json", "/data/NRRRC/UeNrCapability3CC.xml");
    }
}
