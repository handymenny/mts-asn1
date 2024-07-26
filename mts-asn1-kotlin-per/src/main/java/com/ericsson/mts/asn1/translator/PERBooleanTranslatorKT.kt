// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.translator

import com.ericsson.mts.asn1.BitArray
import com.ericsson.mts.asn1.BitInputStream
import java.io.IOException

class PERBooleanTranslatorKT : AbstractBooleanTranslator() {
    @Throws(IOException::class)
    override fun doEncode(s: BitArray, value: Boolean) {
        // 12.1, 12.2, 12.3
        logger.trace("Enter {} encoder, name {}", this.javaClass.simpleName, this.name)
        if (value) {
            s.writeBit(1)
        } else {
            s.writeBit(0)
        }
    }

    @Throws(IOException::class)
    override fun doDecode(s: BitInputStream): Boolean {
        // 12.1, 12.2, 12.3
        logger.trace("Enter {} translator, name {}", this.javaClass.simpleName, this.name)
        return s.readBit() == 1
    }
}
