// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.translator

import com.ericsson.mts.asn1.*
import com.ericsson.mts.asn1.exception.NotHandledCaseException
import java.io.IOException
import java.math.BigInteger

class PERIntegerTranslatorKT(private val perTranscoder: PERTranscoderKT) : AbstractIntegerTranslator() {
    @Throws(IOException::class)
    override fun doEncode(s: BitArray, value: BigInteger) {
        logger.trace("Enter {}", this)

        var lb: BigInteger? = null
        var ub: BigInteger? = null

        if (constraints.hasValueRangeConstraint()) {
            lb = constraints.lowerRange
            ub = constraints.upperRange
        }

        val singleValue = constraints.hasSingleValueConstraints()
        if (singleValue || lb != null && lb == ub) {
            // 13.2.1
            return
        }

        if (constraints.isExtensible) {
            // 13.1
            if ((lb != null && value < lb) || (ub != null && value > ub)) {
                //WARNING : Look at 13.2.6b before removing this exception !
                throw NotHandledCaseException("Encoding of unconstrained integers not supported")
            } else {
                s.writeBit(0)
            }
        }

        if (lb != null && ub != null) {
            //13.2.2, 13.2.5, 13.2.6
            // encodeConstrainedInteger also decodes any length determinant
            perTranscoder.encodeConstrainedInteger(s, value, lb, ub)
        } else if (lb != null) {
            //13.2.3 - semi-constrained whole number + 13.2.6b
            // encodeSemiConstrainedWholeNumber also decodes the length determinant
            perTranscoder.encodeSemiConstrainedWholeNumber(s, lb, value.subtract(lb))
        } else {
            //13.2.4 - unconstrained whole number + 13.2.6b
            throw NotHandledCaseException("Decoding of unconstrained integers not supported")
        }
    }

    @Throws(NotHandledCaseException::class, IOException::class)
    override fun doDecode(s: BitInputStream): BigInteger {
        logger.trace("Enter {}", this)
        var isExtendedInteger = false
        val number: BigInteger

        if (constraints.hasSingleValueConstraints()) {
            // 13.2.1
            return constraints.singleValueConstraint
        }

        var lb: BigInteger? = null
        var ub: BigInteger? = null

        if (constraints.hasValueRangeConstraint()) {
            lb = constraints.lowerRange
            ub = constraints.upperRange
        }

        if (lb != null && lb == ub) {
            // 13.2.1
            return lb
        }

        if (constraints.isExtensible) {
            // 13.1
            isExtendedInteger = s.readBit() == 1
        }

        if (isExtendedInteger) {
            // 13.2.6b
            throw NotHandledCaseException("Decoding of unconstrained integers not supported")
        } else {
            if (lb != null && ub != null) {
                //13.2.2
                logger.trace("Decode ConstrainedNumber")
                logger.trace("lb={}, ub={}", lb, ub)
                // 13.2.5, 13.2.6
                // encodeConstrainedInteger also decodes any length determinant
                number = perTranscoder.decodeConstrainedInteger(lb, ub, s)
            } else if (lb != null) {
                // 13.2.3, 13.2.6b
                logger.trace("Decode SemiConstrainedNumber")
                // decodeSemiConstrainedNumber also decodes the length determinant
                number = perTranscoder.decodeSemiConstrainedWholeNumber(lb, s)
            } else {
                //13.2.4, 13.2.6b
                throw NotHandledCaseException("Decoding of unconstrained integers not supported")
            }
            return number
        }
    }
}
