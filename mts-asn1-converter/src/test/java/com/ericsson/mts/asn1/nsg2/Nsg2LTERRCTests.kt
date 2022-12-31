/*
 * Copyright 2022 Andrea Mennillo, git at amennillo dot eu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1.nsg2

import com.ericsson.mts.asn1.ASN1Converter
import com.ericsson.mts.asn1.AbstractTests
import com.ericsson.mts.asn1.converter.ConverterNSG
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class Nsg2LTERRCTests : AbstractTests() {
    @Test
    @Throws(Exception::class)
    fun testUeEutraCapability2() {
        test(
            "UE-EUTRA-Capability",
            "/data/NSG2/UeEutraCapability2.txt",
            "/data/oracle/UeEutraCapability2.json",
            "/data/oracle/UeEutraCapability2.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUeEutraCapability3() {
        test(
            "UE-EUTRA-Capability",
            "/data/NSG2/UeEutraCapability3.txt",
            "/data/oracle/UeEutraCapability3.json",
            "/data/oracle/UeEutraCapability3.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUeEutraCapability4() {
        test(
            "UE-EUTRA-Capability",
            "/data/NSG2/UeEutraCapability4.txt",
            "/data/oracle/UeEutraCapability4.json",
            "/data/oracle/UeEutraCapability4.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUeEutraCapability5() {
        test(
            "UE-EUTRA-Capability",
            "/data/NSG2/UeEutraCapability5.txt",
            "/data/oracle/UeEutraCapability5.json",
            "/data/oracle/UeEutraCapability5.xml"
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