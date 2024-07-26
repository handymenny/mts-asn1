// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.translator

import com.ericsson.mts.asn1.BitArray
import com.ericsson.mts.asn1.BitInputStream
import com.ericsson.mts.asn1.PERTranscoderKT
import com.ericsson.mts.asn1.exception.NotHandledCaseException
import com.ericsson.mts.asn1.factory.FormatReader
import com.ericsson.mts.asn1.factory.FormatWriter
import com.ericsson.mts.asn1.util.UNIT_64K
import com.ericsson.mts.asn1.util.getIntBounds
import java.io.IOException

class PERBitStringTranslatorKT(private val perTranscoder: PERTranscoderKT) : AbstractBitStringTranslator() {
    @Throws(NotHandledCaseException::class, IOException::class)
    override fun doEncode(s: BitArray, reader: FormatReader, value: String) {
        logger.trace("Enter {} encoder, name {}", this.javaClass.simpleName, this.name)

        val (lb, ub) = constraints.getIntBounds()
        // 16.4, 16.6, 16.11
        var ubUnset = ub >= UNIT_64K

        var newValue = value

        if (constraints.hasContentsConstraint()) {
            // 16.1
            throw NotHandledCaseException("PER-visible constraints can only constrain the length of the bitstring")
        }

        if (namedBitList.isNotEmpty()) {
            //16.2
            newValue = newValue.trimStart('0')

            if (lb > value.length) {
                // 16.3
                newValue = newValue.padStart(lb, '0')
            }
        }

        if (constraints.isExtensible) {
            //16.6
            if (newValue.length < lb || newValue.length > ub) {
                s.writeBit(1)
                ubUnset = true
            } else {
                s.writeBit(0)
            }
        }

        if (ub == 0) {
            // 16.8
            return
        } else if (lb == ub && ub <= 16) {
            // 16.9
            perTranscoder.encodeBitString(s, newValue)
        } else if (lb == ub && ub < UNIT_64K) {
            // 16.10
            perTranscoder.skipAlignedBits(s)
            perTranscoder.encodeBitString(s, newValue)
        } else {
            // 16.11
            if (ubUnset) {
                perTranscoder.encodeSemiConstrainedWholeNumber(s, lb.toBigInteger(), newValue.length.toBigInteger())
            } else {
                perTranscoder.encodeConstrainedInteger(
                    s,
                    newValue.length.toBigInteger(),
                    lb.toBigInteger(),
                    ub.toBigInteger()
                )
            }

            perTranscoder.skipAlignedBits(s)
            perTranscoder.encodeBitString(s, newValue)
        }
        logger.trace("Encode value={} , length={}", newValue, newValue.length)
    }

    @Throws(NotHandledCaseException::class, IOException::class)
    override fun doDecode(s: BitInputStream, writer: FormatWriter): String {
        logger.trace("Enter {} translator, name {}", this.javaClass.simpleName, this.name)
        var isExtendedBitString = false

        val (lb, ub) = constraints.getIntBounds()
        // 16.4, 16.6, 16.11
        var ubUnset = ub >= UNIT_64K

        if (constraints.hasContentsConstraint()) {
            // 16.1
            throw NotHandledCaseException("PER-visible constraints can only constrain the length of the bitstring")
        }

        if (constraints.isExtensible) {
            //16.6
            isExtendedBitString = s.readBit() == 1
        }

        if (isExtendedBitString) {
            //16.6
            ubUnset = true
        }

        if (ub == 0) { // 16.8
            return ""
        } else if (lb == ub && ub <= 16) {
            //16.9
            return perTranscoder.decodeBitString(s, ub)
        } else if (lb == ub && ub < UNIT_64K) {
            //16.10
            perTranscoder.skipAlignedBits(s)
            return perTranscoder.decodeBitString(s, ub)
        } else {
            //16.11
            val length = if (ubUnset) {
                perTranscoder.decodeSemiConstrainedWholeNumber(lb.toBigInteger(), s)
            } else {
                perTranscoder.decodeConstrainedInteger(lb.toBigInteger(), ub.toBigInteger(), s)
            }

            perTranscoder.skipAlignedBits(s)
            val value = perTranscoder.decodeBitString(s, length.toInt())
            logger.trace("Decode value={} , length={}", value, length)
            return value
        }
    }
}
