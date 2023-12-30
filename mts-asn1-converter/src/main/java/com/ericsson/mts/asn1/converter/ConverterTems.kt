package com.ericsson.mts.asn1.converter

import com.ericsson.mts.asn1.ASN1Parser.*
import java.math.BigInteger

/**
 * Implementation of [AbstractConverter] for Tems Logs
 */
class ConverterTems : AbstractConverter() {
    private val stringRegex = """:\s([\w-]+)""".toRegex()
    private val intRegex = """:\s(\d+)""".toRegex()
    private val bitsRegex = """:\s([01]+)""".toRegex()
    private val booleanString = ": true" // case insensitive
    private val ignoredIdentifiers = setOf("extensionBit0")


    override fun cleanup(lines: List<String>): List<String> {
        // TODO: Find a universal solution
        /*  x -> Triple(a, b, c):
        *   x = string to find in the current line
        *   a = string to find in the previous line
        *   b = string to insert as previous line
        *   c = mode:
        *       -1 = insert has -1 indentation, current line has +0 indentation
        *       1 = insert has +0 indentation, current line has +1 indentation
        *       0 = replace x with b in the current line
        */
        val deOptimization = mapOf(
            "profile0x0001" to Triple("pdcp-Parameters", "supportedROHC-Profiles", -1),
            "utraFDD" to Triple("31", "interRAT-Parameters", 1),
            "geran" to Triple("31", "interRAT-Parameters", 1),
            "profile0x0006-r15" to Triple("profile0x0104-r15", "rohc-ProfilesUL-Only-r15", 1),
            "UE-EUTRA-Capability-v9a0-IEs" to Triple("", "lateNonCriticalExtension", -1),
            "Feature Group Indicators (R10)" to Triple("", "featureGroupIndRel10", 0),
            "Feature Group Indicators (R9)" to Triple("", "featureGroupInd-r9", 0),
            "Feature Group Indicators" to Triple("", "featureGroupInd", 0)
        )
        // list of string inserted
        val fixed = mutableSetOf<String>()
        val mutableLines = lines.toMutableList()
        var i = 0
        while (i < mutableLines.size) {
            val line = mutableLines[i]
            deOptimization.keys.find { it in line }?.let {
                val deOptimize = deOptimization[it] ?: return@let
                val (needle, insert, mode) = deOptimize
                val prevLine = mutableLines.getOrNull(i - 1) ?: ""
                if (prevLine.contains(needle)) {
                    val spaces = getIndentationLevel(line) + if (mode == -1) -1 else 0
                    val insertIndented = " ".repeat(spaces).plus(insert)
                    if (mode == 0) {
                        mutableLines[i] = insertIndented
                    } else {
                        mutableLines.add(i, insertIndented)
                        i++ // bump i as mutableLines size increased
                    }
                    fixed.add(needle)
                }
                if (needle in fixed && mode == 1) {
                    mutableLines[i] = " $line"
                }
            }
            i++
        }
        return mutableLines
    }

    override fun preprocessLine(line: String, identifier: String, indentationLevel: Int): Boolean {
        // Skip blank and optional lines
        return indentationLevel == line.length || identifier in ignoredIdentifiers
    }

    override fun parseSequence(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: SequenceTypeContext
    ): Int {
        writer.enterObject(identifier)
        indentationObjectStack.push(indentation)
        typesStack.push(
            combineComponentListAndAdditions(
                context.componentTypeLists().rootComponentTypeList(0).componentTypeList().componentType(),
                context.componentTypeLists().extensionAdditions()?.extensionAdditionList()?.extensionAddition()
                    ?: emptyList()
            )
        )
        return 1
    }

    override fun parseEnumerated(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: EnumeratedTypeContext
    ): Int {
        writer.stringValue(identifier, getStringValue(lineArray[index]))
        return 1
    }

    override fun parseBitString(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: BitStringTypeContext
    ): Int {
        var read = 1
        val minLength = getLowerBound(identifier)?.intValueExact() ?: 0
        var nextLine = lineArray.getOrNull(index) ?: return read
        var bits = getBitsValue(nextLine)

        if (bits.isNullOrEmpty()) {
            nextLine = lineArray.getOrNull(index + read) ?: return read
            if (nextLine.contains("Contents")) {
                val str = getStringValue(nextLine) ?: ""
                bits = BigInteger(str, 16).toString(2).padStart(minLength, '0')
            } else {
                bits = getBitsValue(nextLine) ?: ""
            }
            read++
        }

        // consume bitstring expansion
        nextLine = lineArray.getOrNull(index + read) ?: return read
        val newLevel = getIndentationLevel(nextLine)
        if (newLevel > indentation) {
            do {
                read++
                nextLine = lineArray.getOrNull(index + read) ?: break
            } while (newLevel == getIndentationLevel(nextLine))
        }

        if (bits.length < minLength) {
            // Pad if needed
            bits = bits.padEnd(minLength, '0')
        }
        writer.bitsValue(identifier, bits)
        return read
    }

    override fun parseInteger(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: IntegerTypeContext
    ): Int {
        writer.intValue(identifier, getIntValue(lineArray[index]), null)
        return 1
    }

    override fun parseObjectClassField(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: ObjectClassFieldTypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseObjectidentifier(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: ObjectidentifiertypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseBuiltin(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: BuiltinTypeContext
    ): Int {
        if (context.BOOLEAN_LITERAL() != null) {
            writer.booleanValue(identifier, getBooleanValue(lineArray[index]))
        }
        return 1
    }

    override fun parseCharacterString(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: CharacterStringTypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseSequenceOf(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: SequenceOfTypeContext
    ): Int {
        var read = 1
        var nextLine: String = lineArray.getOrNull(index + read) ?: return read
        var newLevel = getIndentationLevel(nextLine)

        if (nextLine[newLevel] != '[') {
            // we should read more
            read++
            nextLine = lineArray.getOrNull(index + read) ?: return read
            newLevel = getIndentationLevel(nextLine)
        }

        writer.enterArray(identifier)
        indentationArrayStack.push(indentation)
        when (val subType = getSubType(context.asnType())) {
            is SequenceTypeContext -> {
                val subTypeComponents = combineComponentListAndAdditions(
                    subType.componentTypeLists().rootComponentTypeList(0).componentTypeList().componentType(),
                    subType.componentTypeLists().extensionAdditions()?.extensionAdditionList()?.extensionAddition()
                        ?: emptyList()
                )

                while (indentation < newLevel) {
                    // [nr ]:
                    if (nextLine[newLevel] == '[') {
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
                while (nextLine[newLevel] == '[') {
                    // skip invalid lines
                    do {
                        read++
                        nextLine = lineArray.getOrNull(index + read) ?: return read
                    } while (preprocessLine(nextLine, getIdentifier(nextLine), getIndentationLevel(nextLine)))

                    writer.stringValue(identifier, getStringValue(nextLine))
                    read++
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                    newLevel = getIndentationLevel(nextLine)
                }
            }

            is SequenceOfTypeContext -> {
                while (indentation < newLevel) {
                    popStacks(newLevel)
                    read += processLines(
                        index + read, lineArray, overrideType = subType, overrideIdentifier = identifier
                    )
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                    newLevel = getIndentationLevel(nextLine)
                }

            }

            is IntegerTypeContext -> {
                while (indentation < newLevel) {
                    writer.intValue(identifier, getIntValue(nextLine), null)
                    read++
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                    newLevel = getIndentationLevel(nextLine)
                }
            }

            is ChoiceTypeContext -> {
                val indentationObject =
                    subType.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList().namedType()
                while (indentation < newLevel) {
                    if (nextLine[newLevel] == '[') {
                        popStacks(newLevel + 1)
                        if (read > 2 && indentationObjectStack.isNotEmpty()) {
                            indentationObjectStack.pop()
                            typesStack.pop()
                            writer.leaveObject(identifier)
                        }
                        indentationObjectStack.push(newLevel)
                        typesStack.push(indentationObject)
                        writer.enterObject(identifier)
                        read++
                        nextLine = lineArray.getOrNull(index + read) ?: return read
                        processLines(0, listOf(nextLine.split(":").last()), overrideIndentation = newLevel + 1)
                        read++
                    } else {
                        popStacks(newLevel)
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
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: SetTypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseSetOf(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: SetOfTypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseOctetString(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: OctetStringTypeContext
    ): Int {
        var read = 1
        val subType = getContaining(identifier)
        if (subType != null) {
            read += processLines(index + read, lineArray, overrideType = subType, overrideIdentifier = identifier)
        }
        return read
    }

    override fun parseReal(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: RealTypeContext
    ): Int {
        TODO("Not yet implemented")
    }

    override fun parseChoiceType(
        index: Int, lineArray: List<String>, identifier: String, indentation: Int, context: ChoiceTypeContext
    ): Int {
        if (context.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList() != null) {
            writer.enterObject(identifier)
            typesStack.push(
                context.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList().namedType()
            )
            indentationObjectStack.push(indentation)
        }

        val line = lineArray[index + 1]
        processLines(0, listOf(line.split(":").last()), overrideIndentation = indentation, disablePop = true)
        return 2
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
        return line.contains(booleanString, ignoreCase = true)
    }

    private fun getBitsValue(line: String): String? {
        return bitsRegex.find(line)?.groupValues?.get(1)?.replace(" ", "")
    }
}
