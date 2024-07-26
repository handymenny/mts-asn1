// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.translator

import com.ericsson.mts.asn1.BitInputStream
import com.ericsson.mts.asn1.PERTranscoderKT
import com.ericsson.mts.asn1.exception.NotHandledCaseException
import java.math.BigDecimal

class PERRealTranslatorKT(perTranscoder: PERTranscoderKT?) : AbstractRealTranslator() {
    override fun doDecode(s: BitInputStream): BigDecimal {
        throw NotHandledCaseException("Real decoding is not implemented yet")
    }
}
