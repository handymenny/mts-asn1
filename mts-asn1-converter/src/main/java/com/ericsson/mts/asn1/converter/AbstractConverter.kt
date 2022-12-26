/*
 * Copyright 2022 Andrea Mennillo, git at amennillo dot eu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.ericsson.mts.asn1.converter

import com.ericsson.mts.asn1.ASN1Parser.*
import com.ericsson.mts.asn1.factory.FormatWriter
import com.ericsson.mts.asn1.registry.ConverterRegistry
import org.antlr.v4.runtime.ParserRuleContext
import java.math.BigInteger
import java.util.*

/**
 *
 * This class can convert a textual representation of ASN.1 message to the format specified by formatWriter.
 * This is a stateful converter, the internal state of the converter is automatically reset before starting the conversion.
 * For these reasons a single instance of this class can't convert multiple messages concurrently.
 *
 */
abstract class AbstractConverter {
    protected val typesStack: Stack<List<NamedTypeContext>> = Stack()
    protected val indentationObjectStack: Stack<Int> = Stack()
    protected val indentationArrayStack: Stack<Int> = Stack()
    protected lateinit var writer: FormatWriter
    private lateinit var registry: ConverterRegistry

    /**
     *
     * This method converts a textual representation of ASN.1 message to the format specified by formatWriter.
     * This method assumes that ASN.1 structures are properly indented.
     * This method will also reset the "state" of this converter before starting the conversion. For this reason,
     * a single instance of this class can't convert multiple messages concurrently.
     *
     * @param messageType the type of the ASN.1 message.
     * @param messageBody the content of the ASN.1 message.
     * @param formatWriter an [FormatWriter] that will store the result of the conversion
     * @param converterRegistry the registry that stores the ASN.1 definitions
     */
    fun convert(messageType: String, messageBody: String, formatWriter: FormatWriter, converterRegistry: ConverterRegistry) {
        writer = formatWriter
        registry = converterRegistry
        resetStatus()
        val lineArray = cleanup(messageBody).lines()
        var index = -1

        // Process messageType, using special -1 indentation
        index += processLines(0, listOf(messageType).plus(lineArray), overrideIndentation = -1)

        // process messageBody
        while (index < lineArray.size) {
            index += processLines(index, lineArray)
        }
        // pops -1 indentation
        popStacks(-1)
    }

    /**
     *
     * This method processes some lines representing an ASN.1 item.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param overrideIndentation the indentation that will override the one derived automatically.
     * @param overrideType the ASN.1 Type that will override the one derived automatically.
     * @param overrideIdentifier the identifier that will override the one derived automatically.
     * @param disablePop setting this to true will disable "autoPop" functionality, i.e. the function [popStacks]
     * won't be called (automatically)
     * @return the number of lines processed.
     */
    protected fun processLines(
        index: Int,
        lineArray: List<String>,
        overrideIndentation: Int? = null,
        overrideType: ParserRuleContext? = null,
        overrideIdentifier: String? = null,
        disablePop: Boolean = false
    ): Int {
        val line = lineArray[index]

        // Skip blank lines
        if (line.isBlank()) {
            return 1
        }

        val identifier = overrideIdentifier ?: getIdentifier(line)
        val indentation = overrideIndentation ?: getIndentationLevel(line)

        if(!disablePop)
            popStacks(indentation)

        return when (val type = overrideType ?: getType(identifier)) {
            is BitStringTypeContext -> parseBitString(index, lineArray, identifier, indentation, type)
            is CharacterStringTypeContext -> parseCharacterString(index, lineArray, identifier, indentation, type)
            is ChoiceTypeContext -> parseChoiceType(index, lineArray, identifier, indentation, type)
            is EnumeratedTypeContext -> parseEnumerated(index, lineArray, identifier, indentation, type)
            is IntegerTypeContext -> parseInteger(index, lineArray, identifier, indentation, type)
            is ObjectClassFieldTypeContext -> parseObjectClassField(index, lineArray, identifier, indentation, type)
            is ObjectidentifiertypeContext -> parseObjectidentifier(index, lineArray, identifier, indentation, type)
            is OctetStringTypeContext -> parseOctetString(index, lineArray, identifier, indentation, type)
            is RealTypeContext -> parseReal(index, lineArray, identifier, indentation, type)
            is SequenceOfTypeContext -> parseSequenceOf(index, lineArray, identifier, indentation, type)
            is SequenceTypeContext -> parseSequence(index, lineArray, identifier, indentation, type)
            is SetOfTypeContext -> parseSetOf(index, lineArray, identifier, indentation, type)
            is SetTypeContext -> parseSet(index, lineArray, identifier, indentation, type)
            is BuiltinTypeContext -> parseBuiltin(index, lineArray, identifier, indentation, type)
            else -> return 1
        }
    }

    protected abstract fun cleanup(text: String): String

    /**
     *
     * This method processes lines representing a BIT STRING.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [BIT STRING context][BitStringTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseBitString(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: BitStringTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing a Builtin Type.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [Builtin type context][BuiltinTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseBuiltin(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: BuiltinTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing a CHARACTER STRING.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [CHARACTER STRING context][CharacterStringTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseCharacterString(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: CharacterStringTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing a CHOICE.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [CHOICE context][ChoiceTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseChoiceType(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: ChoiceTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing an ENUMERATED.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [ENUMERATED context][EnumeratedTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseEnumerated(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: EnumeratedTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing an INTEGER.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [INTEGER context][IntegerTypeContext].
     * @return the number of lines processed.
     */    
    protected abstract fun parseInteger(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: IntegerTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing an Object class field type.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [Object class field type context][ObjectClassFieldTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseObjectClassField(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: ObjectClassFieldTypeContext
    ): Int


    /**
     *
     * This method processes some lines representing an OBJECT IDENTIFIER.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [OBJECT IDENTIFIER context][ObjectidentifiertypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseObjectidentifier(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: ObjectidentifiertypeContext
    ): Int

    /**
     *
     * This method processes some lines representing an OCTECT STRING.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [OCTECT STRING context][OctetStringTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseOctetString(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: OctetStringTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing a REAL.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [REAL context][RealTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseReal(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: RealTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing a SEQUENCE.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [SEQUENCE context][SequenceTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseSequence(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SequenceTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing a SEQUENCE OF
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [SEQUENCE OF context][SequenceOfTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseSequenceOf(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SequenceOfTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing a SET.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [SET context][SetTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseSet(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SetTypeContext
    ): Int

    /**
     *
     * This method processes some lines representing a SET OF.
     * Starts at [index] and stops as soon as it processes a line that can be parsed independently, usually 1.
     * Processing involves parsing the content of the lines and passing it to
     * [writer][com.ericsson.mts.asn1.converter.AbstractConverter.writer].
     *
     * @param index the index of the first line to process.
     * @param lineArray the array containing the lines to process.
     * @param identifier the ASN.1 object identifier.
     * @param context the [SET OF context][SetOfTypeContext].
     * @return the number of lines processed.
     */
    protected abstract fun parseSetOf(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SetOfTypeContext
    ): Int

    /**
     *
     * This method resets the "internal state" of this convertor.
     * It is run each time [convert] is executed.
     * For this reason, a single instance of this class cannot
     * be used to parse multiple messages concurrently.
     *
     * Implementors of this class can override this method to add more things to reset.
     *
     */
    protected open fun resetStatus() {
        typesStack.clear()
        indentationObjectStack.clear()
        indentationArrayStack.clear()
    }

    /**
     *
     * This method combines a List of [ComponentTypeContext] and a List of [ExtensionAdditionContext].
     *
     * @param componentTypeList the list of [ComponentTypeContext]s.
     * @param extensionAdditionList the list of [ExtensionAdditionsContext]s.
     * @return a list of all [NamedTypeContext] contained in the input lists.
     *
     */
    protected fun combineComponentListAndAdditions(
        componentTypeList: List<ComponentTypeContext>,
        extensionAdditionList: List<ExtensionAdditionContext>
    ): List<NamedTypeContext> {
        val list = extensionAdditionList.flatMap {
            it.extensionAdditionGroup()?.componentTypeList()?.componentType() ?: listOf(it.componentType())
        }
        return componentTypeList.plus(list).map { it.namedType() }
    }


    /**
     *
     * This method returns the [ParserRuleContext] associated with the type of the given [identifier].
     *
     * @param identifier the identifier of the object whose type must be returned.
     * @return the [ParserRuleContext] associated with the type of the given [identifier].
     *
     */
    protected fun getType(identifier: String): ParserRuleContext? {
        val found = find(identifier)

        return if (found?.builtinType() != null) {
            registry.getType(found)
        } else {
            registry.getType(found?.text ?: identifier)
        }
    }

    /**
     *
     * This method returns the [ParserRuleContext] referenced by CONTAINING/ENCODED for the given [identifier].
     *
     * @param identifier the identifier of the object whose [ParserRuleContext] referenced by CONTAINING/ENCODED
     * must be returned.
     * @return the [ParserRuleContext] referenced by CONTAINING/ENCODED for the given [identifier]
     *
     */
    protected fun getContaining(identifier: String): ParserRuleContext? {
        val found = find(identifier)

        return if (found?.builtinType() != null) {
            registry.getContaining(found)
        } else {
            registry.getContaining(found?.text ?: identifier)
        }
    }

    /**
     *
     * This method extracts an identifier from the given [line].
     *
     * @param line the string from which to extract the identifier
     * @return the identifier as [String]
     *
     */
    protected fun getIdentifier(line: String): String {
        return "([A-Za-z][A-Za-z\\-_0-9]+)".toRegex().find(line)?.groups?.get(0)?.value ?: ""
    }

    /**
     *
     * This method returns the length of the indentation for the given [line].
     *
     * @param line the string of which to return the length of the indentation
     * @return the length of the indentation
     */
    protected fun getIndentationLevel(line: String): Int {
        return getIndentation(line)?.length ?: 0
    }

    /**
     *
     * This method extracts the spaces preceding the first non-space character in the given [line].
     *
     * @param line the string from which to extract the indentation
     * @return the indentation of the given [line]
     *
     */
    protected fun getIndentation(line: String): String? {
        return "^\\s*".toRegex().find(line)?.groups?.get(0)?.value
    }

    /**
     *
     * This method returns the Lower Bound of the SizeConstraint for the given [identifier].
     *
     * @param identifier the identifier of the object whose LowerBound must be returned.
     * @return the Lower Bound of the SizeConstraint for the given [identifier]
     *
     */
    protected fun getLowerBound(
        identifier: String,
    ): BigInteger? {
        val found = find(identifier)

        return if (found?.builtinType() != null) {
            registry.getLowerBoundSizeConstraint(found)
        } else {
            registry.getLowerBoundSizeConstraint(found?.text ?: identifier)
        }
    }

    /**
     *
     * This method pops from stacks the arrays/objects with an indentation level greater than or equal to the given one
     *
     * @param indentation the indentation level
     *
     */
    protected open fun popStacks(indentation: Int) {
        while (indentationObjectStack.isNotEmpty() && indentationObjectStack.peek() >= indentation) {
            indentationObjectStack.pop()
            typesStack.pop()
            writer.leaveObject(null)
        }
        while (indentationArrayStack.isNotEmpty() && indentationArrayStack.peek() >= indentation) {
            indentationArrayStack.pop()
            writer.leaveArray(null)
        }
    }

    private fun find(identifier: String): AsnTypeContext? {
        if (typesStack.isEmpty()) {
            return null
        }

        val componentTypeList = typesStack.peek()

        val componentType = componentTypeList.find {
            it.IDENTIFIER().text == identifier
        } ?: componentTypeList.find {
            /* Try to find even types that have changed name in recent specs
            example profile-0x0001 -> profile-0x0001-r15 */
            it.IDENTIFIER().text.split("-").first() == identifier
        }
        return componentType?.asnType()
    }
}