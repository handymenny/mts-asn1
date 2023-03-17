/*
 * Copyright 2023 Andrea Mennillo, git at amennillo dot eu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.ericsson.mts.asn1.translator;

import com.ericsson.mts.asn1.ASN1Parser;
import com.ericsson.mts.asn1.BitArray;
import com.ericsson.mts.asn1.BitInputStream;
import com.ericsson.mts.asn1.TranslatorContext;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;

import java.util.List;
import java.util.Map;

public class TranslatorWrapper extends AbstractTranslator {
  private AbstractTranslator translator;

  public TranslatorWrapper() {

  }

  public TranslatorWrapper(AbstractTranslator translator) {
    this.translator = translator;
  }

  public AbstractTranslator getTranslator() {
    return translator;
  }

  public void setTranslator(AbstractTranslator translator) {
    this.translator = translator;
  }

  public String getName() {
    if (translator != null) {
      return translator.getName();
    }
    return null;
  }

  public void setName(String name) {
    if (translator != null) {
      translator.setName(name);
    }
  }

  protected void addParameters(ASN1Parser.ParameterListContext parameterListContext) {
    if (translator != null) {
      translator.addParameters(parameterListContext);
    }
  }

  public List<String> getParameters() {
    if (translator != null) {
      return translator.getParameters();
    }
    return null;
  }

  public void encode(String name, BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> parameters) throws Exception {
    if (translator != null) {
      translator.encode(name, s, reader, translatorContext, parameters);
    }
  }

  public void decode(String name, BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, List<String> parameters) throws Exception {
    if (translator != null) {
      translator.decode(name, s, writer, translatorContext, parameters);
    }
  }

  protected Map<String, String> getRegister(List<String> values) {
    if (translator != null) {
      return translator.getRegister(values);
    }
    return null;
  }
}