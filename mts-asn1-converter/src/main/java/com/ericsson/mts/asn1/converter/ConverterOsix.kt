package com.ericsson.mts.asn1.converter

import com.ericsson.mts.asn1.ASN1Parser.*
import java.math.BigInteger

/**
 * Implementation of [AbstractConverter] for Osix Logs
 */
class ConverterOsix : AbstractConverter() {
    private val stringRegex = """\s\(([\w-]*)\)""".toRegex()
    private val intRegex = """=\s(\d*)""".toRegex()
    private val bitsRegex = """\s'([01\s]+)'B""".toRegex()
    private val booleanString = "= TRUE"

    override fun cleanup(lines: List<String>): List<String> {
        return lines
    }

    override fun preprocessLine(line: String, identifier: String, indentationLevel: Int): Boolean {
        // Skip blank lines
        return indentationLevel == line.length
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
        writer.stringValue(identifier, getStringValue(lineArray[index]))
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
        val minLength = getLowerBound(identifier)?.intValueExact() ?: 0
        var bits = getBitsValue(line) ?: ""
        var nextLine = lineArray.getOrNull(index + read)
        if (bits.isEmpty() && nextLine != null) {
            val newLevel = getIndentationLevel(nextLine)
            if (newLevel > indentation) {
                do {
                    bits += getIntValue(nextLine!!)?.toInt()
                    read++
                    nextLine = lineArray.getOrNull(index + read) ?: break
                } while (newLevel == getIndentationLevel(nextLine!!))
            }
        }
        if (bits.length < minLength) {
            // Pad if needed
            bits = bits.padEnd(minLength, '0')
        }
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
        writer.intValue(identifier, getIntValue(lineArray[index]), null)
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
            writer.booleanValue(identifier, getBooleanValue(lineArray[index]))
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
        when (val subType = getSubType(context.asnType())) {
            is SequenceTypeContext -> {
                val subTypeComponents =
                    combineComponentListAndAdditions(
                        subType.componentTypeLists().rootComponentTypeList(0).componentTypeList()
                            .componentType(),
                        subType.componentTypeLists().extensionAdditions()?.extensionAdditionList()
                            ?.extensionAddition() ?: emptyList()
                    )
                var nextLine: String? = lineArray.getOrNull(index + read) ?: return read
                var newLevel = getIndentationLevel(nextLine!!)
                while (indentation < newLevel) {
                    // SupportedBandList nr 0
                    val newIdentifier = getIdentifier(nextLine!!.substring(newLevel))
                    if (identifier == newIdentifier) {
                        popStacks(newLevel)
                        indentationObjectStack.push(newLevel)
                        typesStack.push(subTypeComponents)
                        writer.enterObject(identifier)
                        read++
                    } else {
                        read += processLines(index + read, lineArray)
                    }
                    nextLine = lineArray.getOrNull(index + read) ?: break
                    newLevel = getIndentationLevel(nextLine)
                }
            }

            is EnumeratedTypeContext -> {
                var nextLine = lineArray.getOrNull(index + read) ?: return read
                var newLevel = getIndentationLevel(nextLine)
                while (indentation < newLevel) {
                    writer.stringValue(identifier, getStringValue(nextLine))
                    read++
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                    newLevel = getIndentationLevel(nextLine)
                }
            }

            is SequenceOfTypeContext -> {
                var nextLine = lineArray.getOrNull(index + read) ?: return read
                var newLevel = getIndentationLevel(nextLine)
                while (indentation < newLevel) {
                    popStacks(newLevel)
                    read += processLines(
                        index + read, lineArray, overrideType = subType,
                        overrideIdentifier = identifier
                    )
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                    newLevel = getIndentationLevel(nextLine)
                }

            }

            is IntegerTypeContext -> {
                var nextLine = lineArray.getOrNull(index + read) ?: return read
                var newLevel = getIndentationLevel(nextLine)
                while (indentation < newLevel) {
                    writer.intValue(identifier, getIntValue(nextLine), null)
                    read++
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                    newLevel = getIndentationLevel(nextLine)
                }
            }

            is ChoiceTypeContext -> {
                val indentationObject = subType.alternativeTypeLists()
                    .rootAlternativeTypeList()
                    .alternativeTypeList()
                    .namedType()
                var nextLine = lineArray.getOrNull(index + read) ?: return read
                var newLevel = getIndentationLevel(nextLine)
                while (indentation < newLevel) {
                    popStacks(newLevel)
                    val newIdentifier = getIdentifier(nextLine.substring(newLevel))
                    if (identifier == newIdentifier) {
                        indentationObjectStack.push(newLevel)
                        typesStack.push(indentationObject)
                        writer.enterObject(identifier)
                        read++
                    } else {
                        read += processLines(index + read, lineArray)
                    }
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                    newLevel = getIndentationLevel(nextLine)
                }
            }

            else -> {
                // Do nothing
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
        return bitsRegex.find(line)?.groupValues?.get(1)?.replace(" ", "")

    }
}
