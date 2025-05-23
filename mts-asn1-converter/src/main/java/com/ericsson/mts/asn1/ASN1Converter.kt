/*
 * Copyright 2022 Andrea Mennillo, git at amennillo dot eu
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1

import com.ericsson.mts.asn1.converter.AbstractConverter
import com.ericsson.mts.asn1.factory.FormatWriter
import com.ericsson.mts.asn1.registry.ConverterRegistry
import com.ericsson.mts.asn1.visitor.ConverterVisitor
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.tree.ParseTree
import java.io.IOException
import java.io.InputStream

class ASN1Converter {
    private val registry: ConverterRegistry = ConverterRegistry()
    private val converter: AbstractConverter

    constructor(converter: AbstractConverter, tree: ParseTree) {
        this.converter = converter
        beginVisit(tree)
    }

    constructor(converter: AbstractConverter, asnDefinitions: List<InputStream>) {
        this.converter = converter
        for (stream in asnDefinitions) {
            val tree = parseTreeFromStream(stream)
            beginVisit(tree)
        }
    }


    /**
     *
     * This method converts a textual representation of ASN message to the format specified by formatWriter.
     *
     * Note that ASN structures must be properly indented
     *
     * @param messageType the type of the ASN message.
     * @param messageBody the content of the ASN message.
     * @param formatWriter the [FormatWriter] that will store the result of the conversion
     */
    fun convert(messageType: String, messageBody: InputStream, formatWriter: FormatWriter) {
        converter.convert(messageType, messageBody, formatWriter, registry)
    }

    @Throws(IOException::class)
    private fun parseTreeFromStream(stream: InputStream): ParseTree {
        val inputStream = CharStreams.fromStream(stream)
        val asn1Lexer = ASN1Lexer(inputStream)
        val commonTokenStream = CommonTokenStream(asn1Lexer)
        val asn1Parser = ASN1Parser(commonTokenStream)
        asn1Parser.interpreter.predictionMode = PredictionMode.SLL
        return asn1Parser.moduleDefinition()
    }

    private fun beginVisit(tree: ParseTree) {
        ConverterVisitor(registry).visit(tree)
    }
}