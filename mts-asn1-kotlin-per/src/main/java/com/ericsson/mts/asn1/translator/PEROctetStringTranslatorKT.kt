// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.translator

import com.ericsson.mts.asn1.*
import com.ericsson.mts.asn1.factory.FormatReader
import com.ericsson.mts.asn1.factory.FormatWriter
import com.ericsson.mts.asn1.util.UNIT_64K
import com.ericsson.mts.asn1.util.getIntBounds
import com.ericsson.mts.asn1.util.toHexStr
import java.io.IOException

class PEROctetStringTranslatorKT(private val perTranscoder: PERTranscoderKT) : AbstractOctetStringTranslator() {
    @Throws(IOException::class)
    override fun doEncode(s: BitArray, reader: FormatReader, value: String) {
        logger.trace("Enter {} encoder, name {}", this.javaClass.simpleName, this.name)

        var (lb, ub) = constraints.getIntBounds()
        var ubUnset = ub == Int.MAX_VALUE

        if (constraints.isExtensible) {
            if (value.length < lb || value.length > ub) {
                // 17.3
                s.writeBit(1)
                // Invoke 17.8
                lb = 0
                ub = Int.MAX_VALUE
                ubUnset = true
            } else {
                s.writeBit(0)
            }
        }

        if (ub == 0) {
            // 17.5
            return
        } else if (lb == ub && lb <= 2) {
            // 17.6 no align
        } else if (lb == ub && lb < UNIT_64K) {
            //17.7
            perTranscoder.skipAlignedBits(s)
        } else {
            //17.8
            val length = value.length / 2 + value.length % 2
            if (ubUnset) {
                perTranscoder.encodeUnconstrainedLengthDeterminant(s, length - lb)
            } else {
                perTranscoder.encodeConstrainedInteger(
                    s,
                    length.toBigInteger(),
                    lb.toBigInteger(),
                    ub.toBigInteger()
                )
                perTranscoder.skipAlignedBits(s)
            }
        }
        perTranscoder.encodeOctetString(s, value, value.length * 4)
    }

    @Throws(IOException::class)
    override fun doDecode(s: BitInputStream, writer: FormatWriter): ByteArray? {
        logger.trace("Enter {} : {} translator", this.name, this)
        val octetstring: ByteArray
        var (lb, ub) = constraints.getIntBounds()
        var ubUnset = ub == Int.MAX_VALUE


        if (constraints.isExtensible) {
            val isExtendedOctetString = 1 == s.readBit()
            if (isExtendedOctetString) {
                // 17.3 -> Invoke 17.8
                lb = 0
                ub = Int.MAX_VALUE
                ubUnset = true
            }
        }

        if (ub == 0) {
            //17.5
            return null
        } else if (lb == ub && lb <= 2) {
            //17.6
            octetstring = perTranscoder.decodeOctetString(s, lb)
        } else if (lb == ub && lb < UNIT_64K) {
            //17.7
            perTranscoder.skipAlignedBits(s)
            octetstring = perTranscoder.decodeOctetString(s, lb)
        } else {
            //17.8
            val length = if (ubUnset) {
                perTranscoder.decodeUnconstrainedLengthDeterminant(s) + lb
            } else {
                perTranscoder.decodeConstrainedInteger(lb.toBigInteger(), ub.toBigInteger(), s).intValueExact()
            }
            octetstring = perTranscoder.decodeOctetString(s, length, false)
        }
        logger.trace("Result {}", { octetstring.toHexStr() })
        return octetstring
    }
}
