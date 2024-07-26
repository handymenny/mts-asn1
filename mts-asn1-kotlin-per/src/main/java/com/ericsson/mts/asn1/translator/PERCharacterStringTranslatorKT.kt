// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.translator

import com.ericsson.mts.asn1.*
import com.ericsson.mts.asn1.exception.NotHandledCaseException
import com.ericsson.mts.asn1.factory.FormatReader
import com.ericsson.mts.asn1.factory.FormatWriter
import com.ericsson.mts.asn1.util.UNIT_64K
import com.ericsson.mts.asn1.util.getIntBounds
import com.ericsson.mts.asn1.util.toByteCount
import com.ericsson.mts.asn1.util.toHexStr
import java.io.IOException
import java.nio.charset.StandardCharsets

class PERCharacterStringTranslatorKT(private val perTranscoder: PERTranscoderKT) :
    AbstractRestrictedCharacterStringTranslator() {
    @Throws(IOException::class)
    override fun doEncode(s: BitArray, reader: FormatReader, value: String) {
        logger.trace("Enter {} encoder, name {}", this.javaClass.simpleName, this.name)

        if (!isknownMultiplierCharacterStringType && KnownMultiplierCharacterString.PrintableString != knownMultiplierCharacterString) {
            throw NotHandledCaseException("Only Printable string supported")
        }

        // 30.5.2. Note: Effective permitted-alphabet constraints not supported
        val bitLength =
            if (perTranscoder.isAligned) {
                value.length * knownMultiplierCharacterString.b2
            } else {
                value.length * knownMultiplierCharacterString.b
            }

        var (alb: Int, aub: Int) = constraints.getIntBounds()
        var aubUnset = aub >= UNIT_64K

        if (constraints.isExtensible) {
            // 30.4
            if (value.length < alb || value.length > aub) {
                s.writeBit(1)
                alb = 0
                aub = Int.MAX_VALUE
                aubUnset = true
            } else {
                s.writeBit(0)
            }
        }

        val len = toByteCount(bitLength)

        if (aub == alb && !aubUnset) {
            // 30.5.6
            if (bitLength > 16) {
                perTranscoder.skipAlignedBits(s)
            }
        } else {
            // 30.5.7
            if (aubUnset) {
                perTranscoder.encodeSemiConstrainedWholeNumber(
                    s,
                    alb.toBigInteger(),
                    len.toBigInteger()
                )
            } else {
                perTranscoder.encodeConstrainedInteger(
                    s,
                    len.toBigInteger(),
                    alb.toBigInteger(),
                    aub.toBigInteger()
                )
            }
            if (bitLength >= 16) {
                perTranscoder.skipAlignedBits(s)
            }
        }

        perTranscoder.encodeOctetString(
            s,
            value.toByteArray(StandardCharsets.US_ASCII).toHexStr(),
            bitLength
        )
    }

    @Throws(IOException::class)
    override fun doDecode(s: BitInputStream, writer: FormatWriter): String {
        logger.trace("Enter {} translator, name {}", this.javaClass.simpleName, this.name)

        if (!isknownMultiplierCharacterStringType && KnownMultiplierCharacterString.PrintableString != knownMultiplierCharacterString) {
            throw NotHandledCaseException("Only Printable string supported")
        }

        var (alb, aub) = constraints.getIntBounds()
        var aubUnset = aub >= UNIT_64K

        if (constraints.isExtensible) {
            // 30.4
            val isExtendedRestrictedString = s.readBit() == 1
            if (isExtendedRestrictedString) {
                alb = 0
                aub = Int.MAX_VALUE
                aubUnset = true
            }
        }

        // 30.5.2. Note: Effective permitted-alphabet constraints not supported
        val bitsPerChar = if (perTranscoder.isAligned) {
            knownMultiplierCharacterString.b2
        } else {
            knownMultiplierCharacterString.b
        }

        val length: Int
        if (aub == alb && !aubUnset) {
            // 30.5.6
            length = aub
            if (length * bitsPerChar > 16) {
                perTranscoder.skipAlignedBits(s)
            }
        } else {
            // 30.5.7
            length = if (aubUnset) {
                perTranscoder.decodeSemiConstrainedWholeNumber(alb.toBigInteger(), s).intValueExact()
            } else {
                perTranscoder.decodeConstrainedInteger(alb.toBigInteger(), aub.toBigInteger(), s).intValueExact()
            }
            if (length * bitsPerChar >= 16) {
                perTranscoder.skipAlignedBits(s)
            }
        }

        val bytes = s.readAlignedBitArray(length * bitsPerChar)
        val result = String(bytes, StandardCharsets.US_ASCII)
        logger.trace("Result {}", result)
        return result
    }
}
