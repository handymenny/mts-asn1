/*
 * Copyright 2022 Andrea Mennillo, git at amennillo dot eu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1

import com.ericsson.mts.asn1.factory.FormatWriter
import kotlinx.serialization.json.*
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

/**
 *  Drop-in replacement for [JSONFormatWriter](com.ericsson.mts.asn1.JSONFormatWriter).
 *  Implemented using kotlinx-serialization-json
 **/
class KotlinJsonFormatWriter : FormatWriter {
    private val stack = Stack<MutableJsonElement>()
    private var root: MutableJsonElement? = null

    val jsonNode
        get() = root?.jsonElement

    private fun addToTopMostNode(name: String?, node: JsonElement) {
        if (stack.isEmpty()) {
            return
        }
        when (val top = stack.peek()) {
            is MutableJsonArray -> {
                top.add(node)
            }

            is MutableJsonObject -> {
                if (name != null) {
                    top[name] = node
                } else {
                    throw RuntimeException("should not happen")
                }
            }

            else -> {
                throw RuntimeException("should not happen")
            }
        }
    }


    override fun enterObject(name: String?) {
        val obj = MutableJsonObject()
        addToTopMostNode(name, obj.jsonElement)
        stack.push(obj)
    }

    override fun leaveObject(name: String?) {
        val popped = stack.pop()
        if (stack.isEmpty()) {
            root = popped
        }
    }

    override fun enterArray(name: String?) {
        val array = MutableJsonArray()
        addToTopMostNode(name, array.jsonElement)
        stack.push(array)
    }

    override fun leaveArray(name: String?) {
        val popped = stack.pop()
        if (stack.isEmpty()) {
            root = popped
        }
    }

    override fun stringValue(name: String?, value: String?) {
        addToTopMostNode(name, JsonPrimitive(value))
    }

    override fun booleanValue(name: String?, value: Boolean) {
        addToTopMostNode(name, JsonPrimitive(value))
    }

    override fun intValue(name: String?, value: BigInteger?, namedValue: String?) {
        addToTopMostNode(name, JsonPrimitive(value))
    }

    override fun realValue(name: String?, value: BigDecimal?) {
        addToTopMostNode(name, JsonPrimitive(value))
    }

    override fun bytesValue(name: String?, value: ByteArray?) {
        addToTopMostNode(name, JsonPrimitive(FormatWriter.bytesToHex(value)))
    }

    override fun bitsValue(name: String?, value: String?) {
        addToTopMostNode(name, JsonPrimitive(value))
    }

    override fun nullValue(name: String?) {
        addToTopMostNode(name, JsonNull)
    }

    private interface MutableJsonElement {
        val jsonElement: JsonElement
    }

    private class MutableJsonObject : MutableJsonElement {
        private val content = mutableMapOf<String, JsonElement>()
        override val jsonElement = JsonObject(content)
        operator fun set(key: String, value: JsonElement) = content.set(key, value)
    }

    private class MutableJsonArray : MutableJsonElement {
        private val content = mutableListOf<JsonElement>()
        override val jsonElement = JsonArray(content)
        fun add(element: JsonElement) = content.add(element)
    }
}
