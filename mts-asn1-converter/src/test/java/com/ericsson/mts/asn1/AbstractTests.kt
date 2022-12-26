/*
 * Copyright 2022 Andrea Mennillo, git at amennillo dot eu
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1

import com.fasterxml.jackson.databind.ObjectMapper
import net.javacrumbs.jsonunit.JsonAssert
import org.custommonkey.xmlunit.XMLUnit
import org.junit.jupiter.api.Assertions
import java.io.InputStream
import java.io.StringWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

open class AbstractTests {
    private val mapper = ObjectMapper()
    private val writer = mapper.writer()

    @Throws(Exception::class)
    fun test(type: String, textPath: String, expectedJsonPath: String, expectedXmlPath: String) {
        testConversion(type, textPath, expectedJsonPath, expectedXmlPath)
    }

    @Throws(Exception::class)
    fun testConversion(type: String, textPath: String, expectedJsonPath: String, expectedXmlPath: String) {
        //JSON conversion test
        run {
            val formatWriter = JSONFormatWriter()
            asn1Converter?.convert(type, getResourceAsText(textPath)!!, formatWriter)
            val actual = writer.writeValueAsString(formatWriter.jsonNode)
            val expected = getResourceAsText(expectedJsonPath)!!
            JsonAssert.assertJsonEquals(expected, actual)
        }

        //XML conversion test
        run {
            val formatWriter = XMLFormatWriter()
            asn1Converter?.convert(type, getResourceAsText(textPath)!!, formatWriter)
            val tf = TransformerFactory.newInstance()
            val transformer = tf.newTransformer()
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            val writer = StringWriter()
            transformer.transform(DOMSource(formatWriter.result), StreamResult(writer))
            val actual = writer.toString()
            val expected = getResourceAsText(expectedXmlPath)!!
            XMLUnit.setIgnoreWhitespace(true)
            val diff = XMLUnit.compareXML(expected, actual)
            Assertions.assertTrue(diff.similar(), "$diff\nActual document is :\n$actual")
        }
    }

    private fun getResourceAsText(path: String): String? =
        object {}.javaClass.getResource(path)?.readText()

    companion object {
        var asn1Converter: ASN1Converter? = null

        @JvmStatic
        internal fun getResourceAsStream(path: String): InputStream? =
            object {}.javaClass.getResourceAsStream(path)
    }
}