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
    void testUeNrCapability() throws Exception {
        /* This message contains extensions, the encoding test may fail if a specification other than "3GPP TS 38.331 V17.2.0" is used */
        test("UE-NR-Capability", "/data/NRRRC/UeNrCapability.bin", "/data/NRRRC/UeNrCapability.json", "/data/NRRRC/UeNrCapability.xml");
    }
}
