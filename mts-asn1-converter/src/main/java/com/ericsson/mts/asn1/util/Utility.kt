package com.ericsson.mts.asn1.util

import java.util.*

fun <E> Stack<E>.peekOrNull(): E? {
    return if (this.isEmpty()) {
        null
    } else {
        this.peek()
    }
}

