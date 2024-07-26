// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.translator

import com.ericsson.mts.asn1.BitArray
import com.ericsson.mts.asn1.BitInputStream
import com.ericsson.mts.asn1.PERTranscoderKT
import com.ericsson.mts.asn1.exception.UnknownIdentifierException
import com.ericsson.mts.asn1.factory.FormatReader
import com.ericsson.mts.asn1.factory.FormatWriter
import java.io.IOException
import java.math.BigInteger

class PEREnumeratedTranslatorKT(private val perTranscoder: PERTranscoderKT) : AbstractEnumeratedTranslator() {
    @Throws(IOException::class)
    override fun doEncode(s: BitArray, reader: FormatReader, value: String) {
        logger.trace("Encode {}", this)

        val index = fieldList.indexOf(value)
        if (index != -1) {
            if (hasExtensionMarker) {
                // 14.3
                s.writeBit(0)
            }
            // 14.2
            perTranscoder.encodeConstrainedInteger(
                s,
                index.toBigInteger(),
                BigInteger.ZERO,
                (fieldList.size - 1).toBigInteger()
            )
        } else {
            val additionalIndex = additionalFieldsList.indexOf(value)
            if (hasExtensionMarker && additionalIndex != -1) {
                // 14.3
                s.writeBit(1)
                perTranscoder.encodeNormallySmallWholeNumber(s, additionalIndex.toBigInteger())
            } else {
                throw UnknownIdentifierException(value + " isn't part of translator " + this.name)
            }
        }
    }

    @Throws(IOException::class)
    override fun doDecode(s: BitInputStream, writer: FormatWriter): String {
        logger.trace("{} : {}", this.name, this)
        // read the extension bit and returns the extension status, if grammar indicates it can be extended
        val isExtendedValue = hasExtensionMarker && (s.readBit() == 1)
        logger.trace("{} is extended : {}", this.name, isExtendedValue)
        if (!isExtendedValue) {
            // 14.2
            return fieldList[perTranscoder.decodeConstrainedInteger(
                BigInteger.ZERO,
                (fieldList.size - 1).toBigInteger(),
                s
            ).toInt()]
        } else {
            // 14.3
            val choice = perTranscoder.decodeNormallySmallWholeNumber(s).intValueExact()
            return if (additionalFieldsList.size > choice) {
                additionalFieldsList[choice]
            } else {
                "UNKNOWN_EXTENDED($choice)"
            }
        }
    }
}
