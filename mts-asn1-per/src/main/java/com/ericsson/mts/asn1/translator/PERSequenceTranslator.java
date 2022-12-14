/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1.translator;

import com.ericsson.mts.asn1.BitArray;
import com.ericsson.mts.asn1.BitInputStream;
import com.ericsson.mts.asn1.PERTranscoder;
import com.ericsson.mts.asn1.TranslatorContext;
import com.ericsson.mts.asn1.exception.InvalidParameterException;
import com.ericsson.mts.asn1.exception.NotHandledCaseException;
import com.ericsson.mts.asn1.factory.FormatReader;
import com.ericsson.mts.asn1.factory.FormatWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PERSequenceTranslator extends AbstractSequenceTranslator {

    private PERTranscoder perTranscoder;

    public PERSequenceTranslator(PERTranscoder perTranscoder) {
        this.perTranscoder = perTranscoder;
    }

    @Override
    public void doEncode(BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> inputFieldList, Map<String, String> registry) throws Exception {
        logger.trace("Enter {} encoder, name {}, with input fields {}", this.getClass().getSimpleName(), this.name, inputFieldList);
        boolean[] extensionBitmap = new boolean[additionnalFieldList.size()];
        boolean extensionPresent = false;
        if (hasEllipsis || optionalExtensionMarker || (extensionAndException != -1)) {
            for (int i = 0; i < additionnalFieldList.size(); i++) {
                AdditionalField addField = additionnalFieldList.get(i);
                if (addField.isExtensionAdditionGroup()) {
                    for (Field field: ((FieldGroup)addField).getChildren()) {
                        if(inputFieldList.contains(field.getName())) {
                            extensionBitmap[i] = true;
                            extensionPresent = true;
                            break;
                        }
                    }
                } else if (inputFieldList.contains(((Field)addField).getName())) {
                    extensionBitmap[i] = true;
                    extensionPresent = true;
                }
            }
            s.writeBit(extensionPresent ? 1 : 0);
        }

        encodeSequenceFields(s, reader, translatorContext, inputFieldList, registry, fieldList);
        if (extensionPresent) {
            // Write extension bitmap
            perTranscoder.encodeNormallySmallWholeNumber(s, BigInteger.valueOf(additionnalFieldList.size() -1 ));
            for(int i = 0; i < additionnalFieldList.size(); i++) {
                s.writeBit(extensionBitmap[i] ? 1 : 0);
            }
            if (perTranscoder.isAligned()) {
                perTranscoder.skipAlignedBits(s);
            }

            for (int i = 0; i < additionnalFieldList.size(); i++) {
                if (extensionBitmap[i]) {
                    AdditionalField addField = additionnalFieldList.get(i);
                    BitArray bitArray = new BitArray();

                    if (addField.isExtensionAdditionGroup()) {
                        // Extension Addition Group are encoded like Sequence
                        encodeSequenceFields(bitArray, reader, translatorContext, inputFieldList, registry,
                                ((FieldGroup) addField).getChildren());
                    } else {
                        encodeSequenceField(bitArray, reader, translatorContext, registry, (Field) addField);
                    }

                    logger.trace("Open type for field {} : octet length={}", addField, perTranscoder.toByteCount(bitArray.getLength().intValueExact()));
                    perTranscoder.encodeLengthDeterminant(s, BigInteger.valueOf(perTranscoder.toByteCount((bitArray.getLength()).intValueExact())));
                    // align needed before any concat
                    bitArray.skipAlignedBits();
                    s.concatBitArray(bitArray);
                }
            }
        }

    }

    private void encodeSequenceField(BitArray s, FormatReader reader, TranslatorContext translatorContext, Map<String, String> registry, Field field) throws Exception {
        AbstractTranslator typeTranslator = field.getType();
        List<String> parameters = typeTranslator.getParameters();
        if (parameters.isEmpty()) {
            field.getType().encode(field.getName(), s, reader, translatorContext);
        } else {
            //Building parameter list to pass to target translator
            List<String> inputParameters = new ArrayList<>();
            for (String parameter : field.getParameters()) {
                inputParameters.add(registry.get(parameter));
            }
            typeTranslator.encode(field.getName(), s, reader, translatorContext, inputParameters);
        }
    }

    private void encodeSequenceFields(BitArray s, FormatReader reader, TranslatorContext translatorContext, List<String> inputFieldList, Map<String, String> registry, List<Field> fieldList) throws Exception {
        //Build preamble (bit-map)
        BigInteger preambleLength = BigInteger.ZERO;
        for (Field field : fieldList) {
            if (field.getOptionnal()) {
                preambleLength = preambleLength.add(BigInteger.ONE);
                if (inputFieldList.contains(field.getName())) {
                    s.writeBit(1);
                } else {
                    s.writeBit(0);
                }
            }
        }

        if (preambleLength.compareTo(BigInteger.valueOf(65536)) > 0) {
            throw new NotHandledCaseException("Preamble fragmentation");
        }

        for (Field field : fieldList) {
            if (inputFieldList.contains(field.getName())) {
                logger.trace("Encode field " + field.getName());
                encodeSequenceField(s, reader, translatorContext, registry, field);
            } else {
                if (!field.getOptionnal()) {
                    throw new InvalidParameterException("Sequence " + name + " need field " + field.getName());
                }
            }
        }
    }

    @Override
    public void doDecode(BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, Map<String, String> registry) throws Exception {
        logger.trace("{} : {}", this.name, this);
        boolean isExtendedSequence = false;

        if (hasEllipsis || optionalExtensionMarker || (extensionAndException != -1)) {
            isExtendedSequence = (1 == s.readBit());
        }

        logger.trace("{} : isExtendedSequence : {}", this.name, isExtendedSequence);

        decodeSequenceFields(s, writer, translatorContext, registry, fieldList, optionalBitmap);

        if (isExtendedSequence) {
            logger.trace("{} : is and extended sequence", this.name);

            int extensionsCount = perTranscoder.decodeNormallySmallNumber(s).intValueExact() + 1;

            if (0 == extensionsCount) {
                throw new IOException("\"n\" cannot be zero, as this procedure is only invoked if there is at least one extension addition being encoded");
            }

            logger.trace("{} : extensions count is {}", this.name, extensionsCount);

            boolean[] additionalBitmap = new boolean[extensionsCount];

            for (int i = 0; i < additionalBitmap.length; i++) {
                additionalBitmap[i] = (1 == s.readBit());
            }

            logger.trace("{} : additional bitmap is {}", this.name, additionalBitmap);

            // ALIGNED ONLY
            if (perTranscoder.isAligned()) {
                perTranscoder.skipAlignedBits(s);
            }

            Iterator<AdditionalField> additionalFieldsIterator = this.additionnalFieldList.iterator();
            for (boolean additionalBit : additionalBitmap) {
                AdditionalField additionalField;
                if (additionalFieldsIterator.hasNext()) {
                    additionalField = additionalFieldsIterator.next();
                } else {
                    additionalField = null;
                }

                if (additionalBit) {
                    byte[] data;
                    if (perTranscoder.isAligned()) {
                        data = s.readAlignedByteArray(perTranscoder.decodeLengthDeterminant(s));
                    } else {
                        data = s.readUnalignedByteArray(perTranscoder.decodeLengthDeterminant(s));
                    }
                    BitInputStream is = new BitInputStream(new ByteArrayInputStream(data));

                    if (null != additionalField) {
                        if (additionalField.isExtensionAdditionGroup()) {
                            // Extension Addition Group are encoded like Sequence
                            FieldGroup group = ((FieldGroup) additionalField);
                            decodeSequenceFields(is, writer, translatorContext, registry, group.getChildren(), new boolean[group.getOptionalCount()]);
                        } else {
                            decodeSequenceField(s, writer, translatorContext, registry, (Field)additionalField);
                        }
                    } else {
                        logger.error("skipped additional field of " + data.length + " bytes");
                    }
                }
            }
        }
    }

    private void decodeSequenceFields(BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, Map<String, String> registry, List<Field> fieldList, boolean[] optionalBitmap) throws Exception {
        boolean rootSequenceHasOptional = false;

        for (Field field : fieldList) {
            if (field.getOptionnal()) {
                rootSequenceHasOptional = true;
                break;
            }
        }

        if (rootSequenceHasOptional) {
            for (int i = 0; i < optionalBitmap.length; i++) {
                optionalBitmap[i] = (1 == s.readBit());
            }
        }

        logger.trace("{} : optional bitmap is {}", this.name, optionalBitmap);

        int optionalBitmapIndex = 0;

        for (Field field : fieldList) {
            if (!field.getOptionnal() || optionalBitmap[optionalBitmapIndex++]) {
                decodeSequenceField(s, writer, translatorContext, registry, field);
            }
        }
    }

    private void decodeSequenceField(BitInputStream s, FormatWriter writer, TranslatorContext translatorContext, Map<String, String> registry, Field field) throws Exception {
        logger.trace("{} : decode field {} ", this.name, field.getName());
        AbstractTranslator typeTranslator = field.getType();
        List<String> parameters = typeTranslator.getParameters();
        if (parameters.isEmpty()) {
            typeTranslator.decode(field.getName(), s, writer, translatorContext);
        } else {
            //Building parameter list to pass to target translator
            List<String> inputParameters = new ArrayList<>();
            for (String parameter : field.getParameters()) {
                inputParameters.add(registry.get(parameter));
            }
            typeTranslator.decode(field.getName(), s, writer, translatorContext, inputParameters);
        }
    }
}
