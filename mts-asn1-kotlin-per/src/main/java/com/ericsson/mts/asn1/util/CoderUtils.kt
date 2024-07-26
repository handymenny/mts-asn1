// SPDX-License-Identifier: MIT
/*
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.util

import com.ericsson.mts.asn1.constraint.Constraints
import java.math.BigInteger

internal operator fun BigInteger.plus(i: Int): BigInteger = add(i.toBigInteger())
internal operator fun BigInteger.minus(i: Int): BigInteger = subtract(i.toBigInteger())
internal operator fun BigInteger.compareTo(i: Int): Int = compareTo(i.toBigInteger())
internal fun BigInteger.bitLengthAligned() = toByteCount(bitLength()) * 8
internal fun Constraints.getIntBounds(): Pair<Int, Int> {
    var lb: Int?
    var ub: Int?
    if (hasSingleValueConstraints()) {
        ub = singleValueConstraint?.intValueExact()
        lb = ub
    } else if (hasSizeConstraint()) {
        lb = lowerBound?.intValueExact()
        ub = upperBound?.intValueExact()
    } else {
        lb = null
        ub = null
    }

    if (lb == null) {
        lb = 0
    }

    if (ub == null) {
        // Int max value = unset
        ub = Int.MAX_VALUE
    }
    return Pair(lb, ub)
}

@OptIn(ExperimentalStdlibApi::class)
internal fun ByteArray.toHexStr(): String = toHexString()


/**
 * Get the minimum bytes needed to represent the given bits.
 * The minimum value returned by this function is 1.
 */
fun toByteCount(bitCount: Int): Int {
    val fullByte = (bitCount + 8 - 1) / 8 // ceil div
    val result = fullByte.coerceAtLeast(1)

    return result
}


