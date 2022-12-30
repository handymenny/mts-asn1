/*
 * Copyright 2022 Andrea Mennillo, git at amennillo dot eu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1.nsg4

import com.ericsson.mts.asn1.ASN1Converter
import com.ericsson.mts.asn1.AbstractTests
import com.ericsson.mts.asn1.converter.ConverterNSG
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class Nsg4LTERRCTests : AbstractTests() {
    @Test
    @Throws(Exception::class)
    fun testUeEutraCapability() {
        test(
            "UE-EUTRA-Capability",
            "/data/NSG4/UEEutraCapability.txt",
            "/data/oracle/UeEutraCapability.json",
            "/data/oracle/UeEutraCapability.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUeEutraCapabilityBandCombinationAdd2() {
        test(
            "UE-EUTRA-Capability",
            "/data/NSG4/UeEutraCapabilityBandCombinationAdd2.txt",
            "/data/oracle/UeEutraCapabilityBandCombinationAdd2.json",
            "/data/oracle/UeEutraCapabilityBandCombinationAdd2.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilityRequestDiffFallback() {
        test(
            "UE-EUTRA-Capability",
            "/data/NSG4/UEEutraCapabilityRequestDiffFallback.txt",
            "/data/oracle/UEEutraCapabilityRequestDiffFallback.json",
            "/data/oracle/UEEutraCapabilityRequestDiffFallback.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUEEutraCapabilitySkipFallback() {
        test(
            "UE-EUTRA-Capability",
            "/data/NSG4/UEEutraCapabilitySkipFallback.txt",
            "/data/oracle/UEEutraCapabilitySkipFallback.json",
            "/data/oracle/UEEutraCapabilitySkipFallback.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUeEutraCapabilityKirin() {
        test(
            "UE-EUTRA-Capability",
            "/data/NSG4/UeEutraCapabilityKirin.txt",
            "/data/oracle/UeEutraCapabilityKirin.json",
            "/data/oracle/UeEutraCapabilityKirin.xml"
        )
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            try {
                asn1Converter = ASN1Converter(ConverterNSG(), listOf(
                    getResourceAsStream(
                        "/grammar/LTERRC/EUTRA-RRC-Definitions.asn")!!))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}