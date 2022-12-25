package com.ericsson.mts.asn1.converter

import com.ericsson.mts.asn1.ASN1Parser.*
import java.math.BigInteger

/**
 * Implementation of [AbstractConverter] for Wireshark
 */
class ConverterWireshark : AbstractConverter() {
    private var previousWasContaining = false

    override fun resetStatus() {
        super.resetStatus()
        previousWasContaining = false
    }

    override fun cleanup(text: String): String {
        val cleanupRegex = listOf("^\\s*\\[.*".toRegex(RegexOption.MULTILINE), "^\\s*Item\\s\\d+".toRegex(RegexOption.MULTILINE))
        var result: String = text
        cleanupRegex.forEach {
            result = result.replace(it, "")
        }
        return result
    }

    override fun addMessageType(messageBody: String, messageType: String): String {
        val indentation = getIndentation(messageBody) ?: ""
        return "$indentation$messageType\n" + messageBody
    }

    override fun parseSequence(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SequenceTypeContext
    ): Int {
        if (!previousWasContaining) {
            writer.enterObject(identifier)
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
        writer.stringValue(identifier, getStringValue(identifier, lineArray[index]))
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
        val newLevel = getIndentationLevel(lineArray[index + read])
        if (newLevel > indentation) {
            do {
                read++
            } while (newLevel == getIndentationLevel(lineArray[read + 1]))
        }
        writer.bitsValue(identifier, getBitsValue(lineArray[index]))
        return read
    }

    override fun parseInteger(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: IntegerTypeContext
    ): Int {
        writer.intValue(identifier, getIntValue(identifier, lineArray[index]), null)
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
            writer.booleanValue(identifier, getBooleanValue(identifier, lineArray[index]))
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
        writer.enterArray(identifier)
        indentationArrayStack.push(indentation)
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
        writer.enterObject(identifier)
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
            writer.enterObject(identifier)
            typesStack.push(
                context.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList()
                    .namedType()
            )
            indentationObjectStack.push(indentation)
        }
        return 1
    }

    private fun getStringValue(identifier: String, line: String): String? {
        return "$identifier:\\s(\\w*)".toRegex().find(line)?.groups?.get(1)?.value
    }

    private fun getIntValue(identifier: String, line: String): BigInteger? {
        val value = "$identifier:\\s(\\d*)".toRegex().find(line)?.groups?.get(1)?.value
        return value?.let {
            BigInteger(value)
        }
    }

    private fun getBooleanValue(identifier: String, line: String): Boolean {
        return "True" == "$identifier:\\s(True|False)".toRegex().find(line)?.groups?.get(1)?.value
    }

    private fun getBitsValue(line: String): String? {
        return ",\\s+([01\\s.]*)\\s+decimal\\s+value".toRegex()
            .find(line)?.groups?.get(1)?.value?.replace("[\\s.]".toRegex(), "")
    }
}
