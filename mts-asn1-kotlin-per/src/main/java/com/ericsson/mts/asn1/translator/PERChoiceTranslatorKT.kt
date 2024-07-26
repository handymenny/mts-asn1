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
import java.io.IOException
import java.math.BigInteger

class PERChoiceTranslatorKT(private val perTranscoder: PERTranscoderKT) : AbstractChoiceTranslator() {
    @Throws(NotHandledCaseException::class)
    override fun doEncode(s: BitArray, reader: FormatReader, choiceValue: String) {
        logger.trace("Enter {} encoder, name {}", this.javaClass.simpleName, this.name)
        if (optionalExtensionMarker) {
            // 23.5
            s.writeBit(0)
        }

        // 23.4
        if (fieldList.size == 1) {
            if (fieldList[0].value0 == choiceValue) {
                fieldList[0].value1.encode(choiceValue, s, reader, null)
                return
            }
        }

        val found = fieldList.find { it.value0 == choiceValue }


        if (found == null) {
            // 23.8
            val foundAdditional = extensionFieldList.find { it.value0 == choiceValue }
            if (foundAdditional != null) {
                val index = extensionFieldList.indexOf(foundAdditional)
                val abstractTranslator = foundAdditional.value1
                val bitArray = BitArray()

                // index as non-negative normally small whole number
                perTranscoder.encodeNormallySmallWholeNumber(s, index.toBigInteger())

                // encode as open type field
                abstractTranslator.encode(choiceValue, bitArray, reader, null)

                // get len
                val len = toByteCount(bitArray.length.intValueExact())
                logger.trace(
                    "Open type for choice {} : octet length={}",
                    choiceValue,
                    len
                )

                if (len > UNIT_16K) {
                    throw NotHandledCaseException("Open type fragmentation")
                }

                // encode len
                perTranscoder.encodeUnconstrainedLengthDeterminant(s, len)


                // align needed before any concat
                perTranscoder.skipAlignedBits(bitArray, true)
                s.concatBitArray(bitArray)

                return
            }
        } else {
            // 23.6, 23.7
            val index = fieldList.indexOf(found)
            perTranscoder.encodeConstrainedInteger(
                s,
                index.toBigInteger(),
                BigInteger.ZERO,
                (fieldList.size - 1).toBigInteger(),
            )

            val abstractTranslator = found.value1
            abstractTranslator.encode(choiceValue, s, reader, null)
            return
        }

        // no match
        throw UnknownIdentifierException(choiceValue + " isn't part of translator " + this.name)
    }

    @Throws(IOException::class)
    override fun doDecode(s: BitInputStream, writer: FormatWriter) {
        logger.trace("Enter {} translator, name {}", this.javaClass.simpleName, this.name)

        var choiceWithinAdditionalValues = false
        if (optionalExtensionMarker) {
            // 23.5
            choiceWithinAdditionalValues = s.readBit() == 1
        }

        if (!choiceWithinAdditionalValues) {
            // 23.4, 23.6, 23.7
            val index = perTranscoder.decodeConstrainedInteger(BigInteger.ZERO, (fieldList.size - 1).toBigInteger(), s)
                .intValueExact()
            fieldList[index].value1.decode(fieldList[index].value0, s, writer, null)
        } else {
            // 23.8
            val index = perTranscoder.decodeNormallySmallWholeNumber(s).intValueExact()
            val choiceData = ByteArray(perTranscoder.decodeUnconstrainedLengthDeterminant(s))

            if (s.read(choiceData) == -1) {
                throw IOException("reached end of stream")
            }

            extensionFieldList[index - fieldList.size - 1].value1.decode(
                extensionFieldList[index - fieldList.size - 1].value0,
                BitInputStream(ByteArrayInputStream(choiceData)),
                writer,
                null
            )
        }
    }
}
