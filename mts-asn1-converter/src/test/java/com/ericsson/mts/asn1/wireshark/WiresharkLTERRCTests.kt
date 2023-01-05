/*
 * Copyright 2022 Andrea Mennillo, git at amennillo dot eu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1.wireshark

import com.ericsson.mts.asn1.ASN1Converter
import com.ericsson.mts.asn1.AbstractTests
import com.ericsson.mts.asn1.converter.ConverterWireshark
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class WiresharkLTERRCTests : AbstractTests() {
    @Test
    @Throws(Exception::class)
    fun testUeEutraCapability() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapability.txt",
            "/data/oracle/UeEutraCapability.json",
            "/data/oracle/UeEutraCapability.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUeEutraCapabilityBandCombinationAdd() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityBandCombinationAdd.txt",
            "/data/oracle/UeEutraCapabilityBandCombinationAdd.json",
            "/data/oracle/UeEutraCapabilityBandCombinationAdd.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUeEutraCapabilityBandCombinationAdd2() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityBandCombinationAdd2.txt",
            "/data/oracle/UeEutraCapabilityBandCombinationAdd2.json",
            "/data/oracle/UeEutraCapabilityBandCombinationAdd2.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUeEutraCapabilityBandCombinationReduced() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityBandCombinationReduced.txt",
            "/data/oracle/UeEutraCapabilityBandCombinationReduced.json",
            "/data/oracle/UeEutraCapabilityBandCombinationReduced.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilityRequestDiffFallback() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityRequestDiffFallback.txt",
            "/data/oracle/UeEutraCapabilityRequestDiffFallback.json",
            "/data/oracle/UeEutraCapabilityRequestDiffFallback.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilitySkipFallback() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilitySkipFallback.txt",
            "/data/oracle/UeEutraCapabilitySkipFallback.json",
            "/data/oracle/UeEutraCapabilitySkipFallback.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilityKirin() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityKirin.txt",
            "/data/oracle/UeEutraCapabilityKirin.json",
            "/data/oracle/UeEutraCapabilityKirin.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilityMediatek() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityMediatek.txt",
            "/data/oracle/UeEutraCapabilityMediatek.json",
            "/data/oracle/UeEutraCapabilityMediatek.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilityIntel() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityIntel.txt",
            "/data/oracle/UeEutraCapabilityIntel.json",
            "/data/oracle/UeEutraCapabilityIntel.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilityIntel2() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityIntel2.txt",
            "/data/oracle/UeEutraCapabilityIntel2.json",
            "/data/oracle/UeEutraCapabilityIntel2.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilityExynos() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityExynos.txt",
            "/data/oracle/UeEutraCapabilityExynos.json",
            "/data/oracle/UeEutraCapabilityExynos.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilityGCT() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityGCT.txt",
            "/data/oracle/UeEutraCapabilityGCT.json",
            "/data/oracle/UeEutraCapabilityGCT.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilityBandCombination1024qam() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityBandCombination1024qam.txt",
            "/data/oracle/UeEutraCapabilityBandCombination1024qam.json",
            "/data/oracle/UeEutraCapabilityBandCombination1024qam.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilityBandCombinationReduced1024qam() {
        test(
            "UE-EUTRA-Capability",
            "/data/Wireshark/UeEutraCapabilityBandCombinationReduced1024qam.txt",
            "/data/oracle/UeEutraCapabilityBandCombinationReduced1024qam.json",
            "/data/oracle/UeEutraCapabilityBandCombinationReduced1024qam.xml"
        )
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            try {
                asn1Converter = ASN1Converter(ConverterWireshark(), listOf(
                    getResourceAsStream(
                        "/grammar/LTERRC/EUTRA-RRC-Definitions.asn")!!))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}