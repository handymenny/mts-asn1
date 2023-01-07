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

import com.ericsson.mts.asn1.factory.FormatReader
import kotlinx.serialization.json.*
import java.io.InputStream
import java.math.BigInteger
import java.util.*

/**
 *  Drop-in replacement for [JSONFormatReader](com.ericsson.mts.asn1.JSONFormatReader)
 *  Implemented using kotlinx-serialization-json
 **/
class KotlinJsonFormatReader(inputStream: InputStream, private val ignoredNode: String) : FormatReader {
    private var currentNode: JsonElement?
    private val stack = Stack<JsonElement>()
    private val arrayStack = Stack<Int>()

    init {
        currentNode = Json.parseToJsonElement(inputStream.bufferedReader().readText())
    }

    private fun getFromStack(node: JsonElement?): JsonElement? {
        (currentNode as? JsonArray)?.let {
            val currentIndex = arrayStack.pop()
            arrayStack.push(currentIndex + 1)
            return it[currentIndex]
        }
        return node
    }

    override fun enterObject(name: String?) {
        if (ignoredNode != name) {
            stack.push(currentNode)
            currentNode = getFromStack(currentNode)
            if (name != null) {
                currentNode = currentNode?.jsonObject?.get(name)
            }
            if (currentNode == null) {
                throw NullPointerException("Name : $name")
            }
        }
    }

    override fun leaveObject(name: String?) {
        if (ignoredNode != name) {
            currentNode = stack.pop()
            if (currentNode == null) {
                throw NullPointerException("Name : $name")
            }
        }
    }

    override fun enterArray(name: String?): Int {
        stack.push(currentNode)
        currentNode = getFromStack(currentNode)
        if (name != null) {
            currentNode = currentNode?.jsonObject?.get(name)
        }
        arrayStack.push(0)
        if (currentNode == null) {
            throw NullPointerException("Name : $name")
        }
        return (currentNode as? JsonArray)?.size ?: 0
    }

    override fun leaveArray(name: String?) {
        currentNode = stack.pop()
        arrayStack.pop()
        if (currentNode == null) {
            throw NullPointerException("Name : $name")
        }
    }

    override fun booleanValue(name: String?): Boolean {
        return getFromStack(currentNode)?.let {
            if (name == null) {
                it.jsonPrimitive.booleanOrNull
            } else {
                getFromStack(currentNode)?.jsonObject?.get(name)?.jsonPrimitive?.booleanOrNull
            }
        } ?: false
    }

    override fun bitsValue(name: String?): String {
        return getFromStack(currentNode)?.let {
            if (name == null) {
                it.jsonPrimitive.contentOrNull
            } else {
                getFromStack(currentNode)?.jsonObject?.get(name)?.jsonPrimitive?.contentOrNull
            }
        } ?: ""
    }

    override fun bytesValue(name: String?): String {
        return getFromStack(currentNode)?.let {
            if (name == null || it is JsonPrimitive) {
                it.jsonPrimitive.contentOrNull
            } else getFromStack(currentNode)?.jsonObject?.get(name)?.jsonPrimitive?.contentOrNull
        } ?: ""
    }

    override fun intValue(name: String?): BigInteger {
        return getFromStack(currentNode)?.let {
            if (name == null) {
                it.jsonPrimitive.long.toBigInteger()
            } else {
                it.jsonObject[name]?.jsonPrimitive?.long?.toBigInteger()
            }
        } ?: BigInteger.valueOf(0)
    }

    override fun fieldsValue(): List<String> {
        return getFromStack(currentNode)?.jsonObject?.keys?.toList() ?: emptyList()
    }

    override fun stringValue(name: String?): String {
        return getFromStack(currentNode)?.let {
            if (name == null) {
                it.jsonPrimitive.contentOrNull
            } else {
                getFromStack(currentNode)?.jsonObject?.get(name)?.jsonPrimitive?.contentOrNull
            }
        } ?: ""
    }

    override fun printCurrentnode(): String {
        return currentNode.toString()
    }
}
