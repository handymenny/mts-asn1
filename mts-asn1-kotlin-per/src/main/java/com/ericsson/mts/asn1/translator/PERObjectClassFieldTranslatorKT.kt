// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.translator

import com.ericsson.mts.asn1.*
import com.ericsson.mts.asn1.exception.NotHandledCaseException
import com.ericsson.mts.asn1.exception.UnknownIdentifierException
import com.ericsson.mts.asn1.factory.FormatReader
import com.ericsson.mts.asn1.factory.FormatWriter
import com.ericsson.mts.asn1.util.UNIT_16K
import com.ericsson.mts.asn1.util.toByteCount
import java.io.ByteArrayInputStream

class PERObjectClassFieldTranslatorKT(private val perTranscoder: PERTranscoderKT) :
    AbstractObjectClassFieldTranslator() {
    @Throws(Exception::class)
    override fun doEncode(
        name: String,
        s: BitArray,
        reader: FormatReader,
        translatorContext: TranslatorContext,
        registry: Map<String, String>
    ) {
        if (constraints.targetComponent == null) {
            val typeTranslator = classHandler.getTypeTranslator(fieldName)
            typeTranslator.encode(name, s, reader, translatorContext)
        } else {
            var typeTranslator = classHandler.getTypeTranslator(fieldName)
            if (typeTranslator != null) {
                typeTranslator.encode(name, s, reader, translatorContext)
            } else {
                val uniqueKey = translatorContext[constraints.targetComponent]

                if (uniqueKey == null) {
                    throw UnknownIdentifierException("Unique key not found in context for field " + fieldName + " target component " + constraints.targetComponent)
                }

                //OpenType
                val bitArray = BitArray()
                typeTranslator =
                    classHandler.getTypeTranslator(fieldName, registry[constraints.objectSetName], uniqueKey)
                if (typeTranslator == null) {
                    throw UnknownIdentifierException("Unknown field " + fieldName + " in object with " + toString())
                }
                val tag = ++openTypeTag
                logger.trace("Enter open type : tag={} , name={}", tag, this.getName())

                reader.enterObject(name)
                typeTranslator.encode(typeTranslator.getName(), bitArray, reader, translatorContext)
                val len = toByteCount(bitArray.length.intValueExact())
                logger.trace("Leave open type : tag={} , name={}", tag, name)
                logger.trace(
                    "Open type for field {} : octet length={}",
                    name,
                    len
                )
                perTranscoder.encodeUnconstrainedLengthDeterminant(
                    s, len
                )
                // align needed before any concat
                perTranscoder.skipAlignedBits(bitArray, true)
                s.concatBitArray(bitArray)
                reader.leaveObject(name)
            }
        }
    }

    @Throws(Exception::class)
    public override fun doDecode(
        name: String,
        s: BitInputStream,
        writer: FormatWriter,
        translatorContext: TranslatorContext,
        registry: Map<String, String>
    ) {
        if (constraints.targetComponent == null) {
            val typeTranslator = classHandler.getTypeTranslator(fieldName)
            typeTranslator.decode(name, s, writer, translatorContext)
        } else {
            var typeTranslator = classHandler.getTypeTranslator(fieldName)
            if (typeTranslator != null) {
                typeTranslator.decode(name, s, writer, translatorContext)
            } else {
                val uniqueKey = translatorContext[constraints.targetComponent]

                if (uniqueKey == null) {
                    throw UnknownIdentifierException("Unique key not found in context for field " + fieldName + " target component " + constraints.targetComponent)
                }

                //OpenType
                typeTranslator =
                    classHandler.getTypeTranslator(fieldName, registry[constraints.objectSetName], uniqueKey)
                val len = perTranscoder.decodeUnconstrainedLengthDeterminant(s)
                if (len >= UNIT_16K) {
                    throw NotHandledCaseException("Open type fragmentation")
                }


                writer.enterObject(name)
                val data = perTranscoder.readBytes(s, len)
                try {
                    typeTranslator.decode(
                        typeTranslator.getName(),
                        BitInputStream(ByteArrayInputStream(data)),
                        writer,
                        translatorContext
                    )
                } catch (e: Exception) {
                    writer.enterObject("mts-asn1-error")
                    writer.stringValue("message", e.message)
                    writer.stringValue("type", typeTranslator.getName())
                    writer.bytesValue("data", data)
                    writer.leaveObject("mts-asn1-error")
                    if (!perTranscoder.isPermissive) {
                        throw e
                    } else {
                        logger.warn("Error decoding opentype content", e)
                    }
                } finally {
                    writer.leaveObject(name)
                }
            }
        }
    }

    companion object {
        //Use for debug
        private var openTypeTag = 0
    }
}
