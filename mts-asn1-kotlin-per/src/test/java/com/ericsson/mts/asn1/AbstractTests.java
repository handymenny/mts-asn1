// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AbstractTests {
  static ASN1Translator asn1Translator;
  protected Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

  void test(String type, String binaryPath, String expectedJsonPath, String expectedXmlPath) throws Exception {
    testEncode(type, binaryPath, expectedJsonPath, expectedXmlPath);
    testDecode(type, binaryPath, expectedJsonPath, expectedXmlPath);

//        updateDataformatFileTest(type, binaryPath, expectedJsonPath, expectedXmlPath);
  }

  protected void testEncode(String type, String binaryPath, String expectedJsonPath, String expectedXmlPath) throws Exception {
    //JSON encode test
    {

      KotlinJsonFormatReader jsonFormatReader = new KotlinJsonFormatReader(this.getClass().getResourceAsStream(expectedJsonPath), type);
      BitArray bitArray = new BitArray();
      asn1Translator.encode(type, bitArray, jsonFormatReader);


      InputStream expectedInputStream = this.getClass().getResourceAsStream(binaryPath);
      BitArray bitArray1 = new BitArray();
      while (expectedInputStream.available() != 0) {
        bitArray1.write(expectedInputStream.read());
      }

      assertEquals(bitArray1.getBinaryMessage(), bitArray.getBinaryMessage());
    }

    //XML encode test
    {
      XMLFormatReader xmlFormatReader = new XMLFormatReader(this.getClass().getResourceAsStream(expectedXmlPath), type);
      BitArray bitArray = new BitArray();
      asn1Translator.encode(type, bitArray, xmlFormatReader);

      InputStream expectedInputStream = this.getClass().getResourceAsStream(binaryPath);
      BitArray bitArray1 = new BitArray();
      while (expectedInputStream.available() != 0) {
        bitArray1.write(expectedInputStream.read());
      }
      assertEquals(bitArray1.getBinaryMessage(), bitArray.getBinaryMessage());
    }
  }

  void testDecode(String type, String binaryPath, String expectedJsonPath, String expectedXmlPath) throws Exception {
    //JSON decode test
    {
      KotlinJsonFormatWriter formatWriter = new KotlinJsonFormatWriter();
      asn1Translator.decode(type, this.getClass().getResourceAsStream(binaryPath), formatWriter);
      String actual = formatWriter.getJsonNode().toString();
      String expected = IOUtils.toString(this.getClass().getResourceAsStream(expectedJsonPath), StandardCharsets.UTF_8);
      assertJsonEquals(expected, actual);
    }

    //XML decode test
    {
      XMLFormatWriter formatWriter = new XMLFormatWriter();
      asn1Translator.decode(type, this.getClass().getResourceAsStream(binaryPath), formatWriter);
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(formatWriter.getResult()), new StreamResult(writer));


      String actual = writer.toString();
      String expected = IOUtils.toString(this.getClass().getResourceAsStream(expectedXmlPath), StandardCharsets.UTF_8);
      XMLUnit.setIgnoreWhitespace(true);
      Diff diff = XMLUnit.compareXML(expected, actual);

      assertTrue(diff.similar(), diff + "\nActual document is :\n" + actual);
    }
  }

  /**
   * Update xml and json test file in the target folder.
   * <p>
   * WARNING : Be sure that encoding and decoding are correct before using this method.
   *
   * @param type           Entry point for a given protocol (protocol dependant)
   * @param binaryPath     File binary path
   * @param targetJsonPath Target JSON file
   * @param targetXmlPath  Target XML file
   * @throws Exception Encoding exception
   */
  void updateDataformatFileTest(String type, String binaryPath, String targetJsonPath, String targetXmlPath) throws Exception {
    //JSON decode test
    {
      KotlinJsonFormatWriter formatWriter = new KotlinJsonFormatWriter();
      asn1Translator.decode(type, this.getClass().getResourceAsStream(binaryPath), formatWriter);
      PrintWriter printWriter = new PrintWriter(new File(this.getClass().getResource(targetJsonPath).getPath()));
      printWriter.print(formatWriter.getJsonNode().toString());
      printWriter.close();
    }

    //XML decode test
    {
      XMLFormatWriter formatWriter = new XMLFormatWriter();
      asn1Translator.decode(type, this.getClass().getResourceAsStream(binaryPath), formatWriter);
      TransformerFactory tf = TransformerFactory.newInstance();
      Transformer transformer = tf.newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(formatWriter.getResult()), new StreamResult(writer));
      PrintWriter printWriter = new PrintWriter(new File(this.getClass().getResource(targetXmlPath).getPath()));
      printWriter.print(writer);
      printWriter.close();
    }
  }

  void testParsingANTLR() {
    asn1Translator.parseTranslators();
  }
}
