package com.ericsson.mts.asn1.converter

import com.ericsson.mts.asn1.ASN1Parser.*
import java.math.BigInteger
import java.util.Stack

/**
 * Implementation of [AbstractConverter] for Wireshark
 */
class ConverterWireshark : AbstractConverter() {
    private var previousWasContaining = false
    private var identifierArrayStack = Stack<String>()
    private val ignoredIdentifiers = listOf("Item", "Items")
    private val stringRegex by lazy { """:\s([\w-]*)""".toRegex() }
    private val intRegex by lazy { """:\s(\d*)""".toRegex() }
    private val bitsRegex by lazy { """,\s+([01\s.]*)\s+decimal\s+value""".toRegex() }
    private val booleanString = ": True"

    override fun resetStatus() {
        super.resetStatus()
        previousWasContaining = false
        identifierArrayStack.clear()
    }

    override fun popStacks(indentation: Int) {
        while (indentationArrayStack.isNotEmpty() && indentationArrayStack.peek() >= indentation) {
            identifierArrayStack.pop()
            indentationArrayStack.pop()
            writer.leaveArray(null)
        }
        super.popStacks(indentation)
    }

    private fun getArrayIdentifier(): String? {
        if (identifierArrayStack.isNotEmpty()) {
            if(indentationObjectStack.isEmpty() || indentationArrayStack.peek() > indentationObjectStack.peek()) {
                return identifierArrayStack.peek()
            }
        }
        return null
    }

    override fun cleanup(lines: List<String>): List<String> {
        // tshark doesn't correctly indent elements that are too nested
        // TODO: Find a universal solution
        val spaceWorkarounds = mapOf(
            "eLCID-Support-r15" to 1,
            "FeatureSetDL-PerCC-Id-r15" to 1,
            "FeatureSetUL-PerCC-Id-r15" to 1,
            "eutra-CGI-Reporting-ENDC-r15" to 1,
            "utra-GERAN-CGI-Reporting-ENDC-r15" to 1,
            "ul-256QAM-perCC-r14" to 1,
            "SupportedBandNR-r15" to 1,
            "bandNR-r15" to 2,
            "bandParameterList-v1530" to 1,
            "BandParameters-v1530" to 2,
            "dl-1024QAM-r15" to 3
        )
        val mutableLines = lines.toMutableList()
        for (i in mutableLines.indices) {
            val line = mutableLines[i]
            spaceWorkarounds.keys.find { it in line }?.let {
                val spaces = spaceWorkarounds[it] ?: 0
                mutableLines[i] = " ".repeat(spaces).plus(line)
            }
        }
        return mutableLines
    }

    override fun skipLine(line: String, identifier: String, indentationLevel: Int): Boolean {
        return indentationLevel == line.length || // Skip blank lines
                identifier in ignoredIdentifiers || // Skip Item X, Items X
                line[indentationLevel] == '[' // Skip wireshark comments
    }

    override fun parseSequence(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SequenceTypeContext
    ): Int {
        if (!previousWasContaining) {
            val identifierCustom = getArrayIdentifier() ?: identifier
            writer.enterObject(identifierCustom)
        } else {
            previousWasContaining = false
        }
        indentationObjectStack.push(indentation)
        typesStack.push(
            combineComponentListAndAdditions(
                context.componentTypeLists().rootComponentTypeList(0).componentTypeList()
                    .componentType(),
                context.componentTypeLists().extensionAdditions()?.extensionAdditionList()
                    ?.extensionAddition() ?: emptyList()
            )
        )
        return 1
    }

    override fun parseEnumerated(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: EnumeratedTypeContext
    ): Int {
        val identifierCustom = getArrayIdentifier() ?: identifier
        writer.stringValue(identifierCustom, getStringValue(lineArray[index]))
        return 1
    }

    override fun parseBitString(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: BitStringTypeContext
    ): Int {
        var read = 1
        val newLevel = lineArray.getOrNull(index + read)?.let {
            getIndentationLevel(it)
        } ?: return read
        if (newLevel > indentation) {
            // Skip description lines
            do {
                read++
            } while (
                newLevel ==
                lineArray.getOrNull(index + read)?.let {
                    getIndentationLevel(it)
                }
            )
        }
        val identifierCustom = getArrayIdentifier() ?: identifier
        // Get min number of bits
        var bits = getBitsValue(lineArray[index]) ?: "0"
        val minLength = getLowerBound(identifier)?.intValueExact() ?: 0
        // Pad if needed
        bits = bits.padEnd(minLength, '0')
        writer.bitsValue(identifierCustom, bits)
        return read
    }

    override fun parseInteger(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: IntegerTypeContext
    ): Int {
        val identifierCustom = getArrayIdentifier() ?: identifier
        writer.intValue(identifierCustom, getIntValue(lineArray[index]), null)
        return 1
    }

    override fun parseObjectClassField(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: ObjectClassFieldTypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseObjectidentifier(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: ObjectidentifiertypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseBuiltin(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: BuiltinTypeContext
    ): Int {
        if (context.BOOLEAN_LITERAL() != null) {
            val identifierCustom = getArrayIdentifier() ?: identifier
            writer.booleanValue(identifierCustom, getBooleanValue(lineArray[index]))
        }
        return 1
    }

    override fun parseCharacterString(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: CharacterStringTypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseSequenceOf(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SequenceOfTypeContext
    ): Int {
        val identifierCustom = getArrayIdentifier() ?: identifier
        writer.enterArray(identifierCustom)
        indentationArrayStack.push(indentation)
        identifierArrayStack.push(identifierCustom)
        return 1
    }

    override fun parseSet(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SetTypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseSetOf(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SetOfTypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseOctetString(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: OctetStringTypeContext
    ): Int {
        val identifierCustom = getArrayIdentifier() ?: identifier
        writer.enterObject(identifierCustom)
        previousWasContaining = true
        return 1
    }

    override fun parseReal(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: RealTypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseChoiceType(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: ChoiceTypeContext
    ): Int {
        if (context.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList() != null) {
            val identifierCustom = getArrayIdentifier() ?: identifier
            writer.enterObject(identifierCustom)
            typesStack.push(
                context.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList()
                    .namedType()
            )
            indentationObjectStack.push(indentation)
        }
        return 1
    }

    private fun getStringValue(line: String): String? {
        return stringRegex.find(line)?.groupValues?.get(1)
    }
    private fun getIntValue(line: String): BigInteger? {
        val value = intRegex.find(line)?.groupValues?.get(1)
        return value?.let {
            BigInteger(value)
        }
    }

    private fun getBooleanValue(line: String): Boolean {
        return booleanString in line
    }

    private fun getBitsValue(line: String): String? {
        return bitsRegex.find(line)?.groupValues?.get(1)
            ?.replace(" ", "")?.replace(".", "")
    }
}
