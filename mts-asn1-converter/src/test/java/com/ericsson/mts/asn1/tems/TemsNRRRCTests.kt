/*
 * Copyright 2023 Andrea Mennillo, git at amennillo dot eu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1.tems

import com.ericsson.mts.asn1.ASN1Converter
import com.ericsson.mts.asn1.AbstractTests
import com.ericsson.mts.asn1.converter.ConverterTems
import com.ericsson.mts.asn1.converter.ConverterWireshark
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class TemsNRRRCTests : AbstractTests() {
    @Test
    @Throws(Exception::class)
    fun testUeNrCapability3() {
        test(
            "UE-NR-Capability",
            "/data/tems/UeNrCapability3.txt",
            "/data/oracle/UeNrCapability3.json",
            "/data/oracle/UeNrCapability3.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUeNrCapability4() {
        test(
            "UE-NR-Capability",
            "/data/tems/UeNrCapability4.txt",
            "/data/oracle/UeNrCapability4.json",
            "/data/oracle/UeNrCapability4.xml"
        )
    }


    @Test
    @Throws(Exception::class)
    fun testUeMrdcCapability3() {
        test(
            "UE-MRDC-Capability",
            "/data/tems/UeMrdcCapability3.txt",
            "/data/oracle/UeMrdcCapability3.json",
            "/data/oracle/UeMrdcCapability3.xml"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testUeMrdcCapability4() {
        test(
            "UE-MRDC-Capability",
            "/data/tems/UeMrdcCapability4.txt",
            "/data/oracle/UeMrdcCapability4.json",
            "/data/oracle/UeMrdcCapability4.xml"
        )
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun init() {
            try {
                asn1Converter = ASN1Converter(ConverterTems(), listOf(
                    getResourceAsStream(
                        "/grammar/NRRRC/NR-RRC-Definitions.asn")!!))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
