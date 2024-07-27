// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.translator

import com.ericsson.mts.asn1.*
import com.ericsson.mts.asn1.factory.FormatReader
import com.ericsson.mts.asn1.factory.FormatWriter
import com.ericsson.mts.asn1.util.UNIT_16K
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
        } else if (!ubUnset) {
            //17.8
            val length = value.length / 2 + value.length % 2
            perTranscoder.encodeConstrainedInteger(
                s,
                length.toBigInteger(),
                lb.toBigInteger(),
                ub.toBigInteger()
            )
            perTranscoder.skipAlignedBits(s)
        } else {
            //17.8 + 11.9.3.8 fragmentation
            val length = value.length / 2 + value.length % 2
            if (length < UNIT_16K) {
                // no fragmentation
                perTranscoder.encodeUnconstrainedLengthDeterminant(s, length)
            } else {
                val fragments = value.chunked(UNIT_16K * 2)
                logger.trace("Fragmentation, {} fragments", fragments.size)
                for (fragment in fragments) {
                    // fragment length in bytes
                    val fragmentLength = fragment.length / 2 + fragment.length % 2
                    perTranscoder.encodeUnconstrainedLengthDeterminant(s, fragmentLength, true)
                    perTranscoder.encodeOctetString(s, fragment, fragment.length * 4)
                }
                // don't execute last line
                return
            }
        }
        // executed for all cases except fragmentation
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
        } else if (!ubUnset) {
            //17.8
            val length = perTranscoder.decodeConstrainedInteger(lb.toBigInteger(), ub.toBigInteger(), s).intValueExact()
            octetstring = perTranscoder.decodeOctetString(s, length, false)
        } else {
            //17.8 + 11.9.3.8 fragmentation
            var counter = 1

            // 1st fragment
            var length = perTranscoder.decodeUnconstrainedLengthDeterminant(s, true)
            var fragments = perTranscoder.decodeOctetString(s, length, false)

            while (length % UNIT_16K == 0) {
                // log previous fragment
                logger.trace("Fragmentation, fragment {}: {} bytes", counter++, length)
                // read length of next fragment
                length = perTranscoder.decodeUnconstrainedLengthDeterminant(s, true)
                fragments += perTranscoder.decodeOctetString(s, length, false)
            }

            octetstring = fragments

            if (counter > 1) {
                // Fragmentation happened
                logger.trace("Fragmentation, last fragment: {} bytes", length)
                logger.trace("Fragmentation, read {} fragments: {} bytes", counter, octetstring.size)
            }
        }
        logger.trace("Result {}", { octetstring.toHexStr() })
        return octetstring
    }
}
