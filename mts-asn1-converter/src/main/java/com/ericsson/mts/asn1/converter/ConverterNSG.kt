package com.ericsson.mts.asn1.converter

import com.ericsson.mts.asn1.ASN1Parser.*
import java.math.BigInteger

/**
 * Implementation of [AbstractConverter] for NSG 4.x and AirScreen 2.x
 */
class ConverterNSG : AbstractConverter() {
    override fun cleanup(text: String): String {
       return text
    }

    override fun parseSequence(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SequenceTypeContext
    ): Int {
        writer.enterObject(identifier)
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
        val line = lineArray[index]
        var read = 1
        var bits = getBitsValue(identifier, line)
        if (bits.isNullOrBlank()) {
            bits = ""
            var nextLine = lineArray[index + read]
            val newLevel = getIndentationLevel(nextLine)
            if (newLevel > indentation) {
                do {
                    bits += if (getBooleanValueCustom(nextLine, "supported")) {
                        "1"
                    } else {
                        "0"
                    }
                    read++
                    nextLine = lineArray[index + read]
                } while (newLevel == getIndentationLevel(nextLine))
            }
        }
        // Min number of bits
        bits = bits.dropLastWhile { it == '0' }
        val minLength = getLowerBound(identifier)?.intValueExact() ?: 0
        // Pad if needed
        bits = bits.padEnd(minLength, '0')
        writer.bitsValue(identifier, bits)
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
        var read = 1
        writer.enterArray(identifier)
        indentationArrayStack.push(indentation)
        val test = context.asnType().referencedType().definedType().IDENTIFIER()[0]
        when (val subType = getType(test.toString())) {
            is SequenceTypeContext -> {
                val indentationObject =
                    combineComponentListAndAdditions(
                        subType.componentTypeLists().rootComponentTypeList(0).componentTypeList()
                            .componentType(),
                        subType.componentTypeLists().extensionAdditions()?.extensionAdditionList()
                            ?.extensionAddition() ?: emptyList()
                    )
                var nextLine = lineArray[index + read]
                var newLevel = getIndentationLevel(nextLine)
                while (indentation < newLevel) {
                    popStacks(newLevel)
                    if(nextLine.matches("^\\s*\\[\\d+]\\s*".toRegex())) {
                        indentationObjectStack.push(newLevel)
                        typesStack.push(indentationObject)
                        writer.enterObject(identifier)
                        read++
                    } else {
                        read += processLines(index + read, lineArray)
                    }
                    nextLine = lineArray[index + read]
                    newLevel = getIndentationLevel(nextLine)
                }
            }
            is EnumeratedTypeContext -> {
                var nextLine = lineArray[index + read]
                while (nextLine.matches("^\\s*\\[\\d+]\\s*:.*$".toRegex())) {
                    writer.stringValue(identifier, getStringValue("", nextLine))
                    read++
                    nextLine = lineArray[index + read]
                }
            }
            is SequenceOfTypeContext -> {
                var nextLine = lineArray[index + read]
                var newLevel = getIndentationLevel(nextLine)
                while (indentation < newLevel) {
                    popStacks(newLevel)
                    read += processLines(index + read, lineArray, overrideType = subType, overrideIdentifier = identifier)
                    nextLine = lineArray[index + read]
                    newLevel = getIndentationLevel(nextLine)
                }

            }
            is IntegerTypeContext -> {
                var nextLine = lineArray[index + read]
                while (nextLine.matches("^\\s*\\[\\d+]\\s*:.*$".toRegex())) {
                    writer.intValue(identifier, getIntValue("", nextLine), null)
                    read++
                    nextLine = lineArray[index + read]
                }
            }
            is ChoiceTypeContext -> {
                val indentationObject = subType.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList().namedType()
                var nextLine = lineArray[index + read]
                var newLevel = getIndentationLevel(nextLine)
                while (indentation < newLevel) {
                    popStacks(newLevel)
                    if (nextLine.matches("^\\s*\\[\\d+]\\s*.*".toRegex())) {
                        indentationObjectStack.push(newLevel)
                        typesStack.push(indentationObject)
                        writer.enterObject(identifier)
                        if (nextLine.contains("->")) {
                            processLines(0, listOf( nextLine.split("->").last()), overrideIndentation = newLevel, disablePop= true)
                        }
                        read++
                    } else {
                        read += processLines(index + read, lineArray)
                    }
                    nextLine = lineArray[index + read]
                    newLevel = getIndentationLevel(nextLine)
                }
            }
            else -> {
                println(subType?.javaClass?.simpleName)
            }
        }
        return read
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
        var read = 1
        val subType = getContaining(identifier)
        if (subType != null) {
            read = processLines(index, lineArray, overrideType = subType, overrideIdentifier = identifier)
        }
        return read
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
        val line = lineArray[index]
        if (line.contains("->")) {
            processLines(0, listOf(line.split("->").last()), overrideIndentation = indentation, disablePop = true)
        }
        return 1
    }


    private fun getStringValue(identifier: String, line: String): String? {
        return "$identifier\\s:\\s(\\w*)".toRegex().find(line)?.groups?.get(1)?.value
    }

    private fun getIntValue(identifier: String, line: String): BigInteger? {
        val value = "$identifier\\s:\\s(\\d*)".toRegex().find(line)?.groups?.get(1)?.value
        return value?.let {
            BigInteger(value)
        }
    }

    private fun getBooleanValue(identifier: String, line: String): Boolean {
        return "true" == "$identifier\\s:\\s(true|false)".toRegex().find(line)?.groups?.get(1)?.value
    }

    private fun getBitsValue(identifier: String, line: String): String? {
        return "$identifier\\s:\\s'([0-1\\s]+)'B".toRegex().find(line)?.groups?.get(1)?.value?.replace("[\\s.]".toRegex(), "")

    }

    private fun getBooleanValueCustom(line: String, trueString: String): Boolean {
        return trueString == "\\s:\\s($trueString)".toRegex().find(line)?.groups?.get(1)?.value
    }
}
