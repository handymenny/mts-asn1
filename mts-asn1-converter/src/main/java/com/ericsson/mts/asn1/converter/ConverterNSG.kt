package com.ericsson.mts.asn1.converter

import com.ericsson.mts.asn1.ASN1Parser.*
import java.math.BigInteger

/**
 * Implementation of [AbstractConverter] for NSG 4.x and AirScreen 2.x
 */
class ConverterNSG : AbstractConverter() {
    private var nsgVersion36 = false

    override fun resetStatus() {
        super.resetStatus()
        nsgVersion36 = false
    }

    override fun cleanup(lines: List<String>): List<String> {
       return lines
    }

    override fun skipLine(line: String, identifier: String, indentationLevel: Int): Boolean {
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
        var bits = getBitsValue(line, minLength) ?: ""
        var nextLine = lineArray.getOrNull(index + read)
        if (bits.isEmpty() && nextLine != null) {
            val newLevel = getIndentationLevel(nextLine)
            if (newLevel > indentation) {
                do {
                    bits += if (getBooleanValueCustom(nextLine!!, "supported")) {
                        "1"
                    } else {
                        "0"
                    }
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
        val subtypeIdentifier = context.asnType().referencedType().definedType().IDENTIFIER()[0]
        when (val subType = getType(subtypeIdentifier.toString())) {
            is SequenceTypeContext -> {
                val subTypeComponents =
                    combineComponentListAndAdditions(
                        subType.componentTypeLists().rootComponentTypeList(0).componentTypeList()
                            .componentType(),
                        subType.componentTypeLists().extensionAdditions()?.extensionAdditionList()
                            ?.extensionAddition() ?: emptyList()
                    )
                val useNsg36path = nsgVersion36 || lineArray[index].matches("^\\s*[a-z][A-Za-z\\-_0-9]+\\[0]$".toRegex())
                if (useNsg36path) {
                    read = 0
                }
                var nextLine: String? = lineArray.getOrNull(index + read) ?: return read
                var newLevel = getIndentationLevel(nextLine!!)
                while (indentation < newLevel || (indentation <= newLevel && useNsg36path)) {

                    if (!useNsg36path) {
                        popStacks(newLevel)
                    }

                    // True if newIdentifier is in subTypeComponents
                    var objectFound = false

                    // NSG 2.x = SupportedBandList[0], NSG 3.6 = supportedBandlist[0], NSG 4.x = [0]
                    val newIdentifier = getIdentifier(nextLine!!)

                    if (!useNsg36path && newIdentifier.isNotBlank()) {
                        // NSG 2.x path
                        if (findIdInComponentTypes(newIdentifier, subTypeComponents) != null) {
                            objectFound = true
                        }
                    }

                    if(!objectFound && (!useNsg36path || newIdentifier == "" || identifier == newIdentifier) &&
                        nextLine.matches("^.*\\[\\d+]\\s*".toRegex())) {
                        if(useNsg36path) {
                            popStacks(newLevel + 1)
                            if (read > 0 && indentationObjectStack.isNotEmpty()) {
                                indentationObjectStack.pop()
                                typesStack.pop()
                                writer.leaveObject(identifier)
                            }
                        }
                        indentationObjectStack.push(newLevel)
                        typesStack.push(subTypeComponents)
                        writer.enterObject(identifier)
                        read++
                    } else {
                        if(useNsg36path) {
                            popStacks(newLevel)
                        }
                        read += processLines(index + read, lineArray, disablePop = useNsg36path)
                    }
                    nextLine = lineArray.getOrNull(index + read) ?: break
                    newLevel = getIndentationLevel(nextLine)
                }
            }
            is EnumeratedTypeContext -> {
                var nextLine = lineArray.getOrNull(index + read) ?: return read
                while (nextLine.matches("^.*\\[\\d+]\\s*:.*$".toRegex())) {
                    writer.stringValue(identifier, getStringValue(nextLine))
                    read++
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                }
            }
            is SequenceOfTypeContext -> {
                val useNsg36path = nsgVersion36 || lineArray[index].matches("^\\s*[a-z][A-Za-z\\-_0-9]+\\[0]\$".toRegex())
                if (useNsg36path) {
                    nsgVersion36 = true
                    read = 0
                }
                var nextLine = lineArray.getOrNull(index + read) ?: return read
                var newLevel = getIndentationLevel(nextLine)
                while (indentation < newLevel || (useNsg36path && indentation <= newLevel)) {
                    if (indentation == newLevel) {
                        popStacks(newLevel+1)
                        read++
                    } else {
                        popStacks(newLevel)
                    }
                    read += processLines(index + read, lineArray, overrideType = subType, overrideIdentifier = identifier)
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                    newLevel = getIndentationLevel(nextLine)
                }

            }
            is IntegerTypeContext -> {
                if (lineArray[index].matches("^.*\\[\\d+]\\s*:.*$".toRegex())) {
                    read = 0
                }
                var nextLine = lineArray.getOrNull(index + read) ?: return read
                while (nextLine.matches("^.*\\[\\d+]\\s*:.*$".toRegex())) {
                    writer.intValue(identifier, getIntValue(nextLine), null)
                    read++
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                }
            }
            is ChoiceTypeContext -> {
                val useNsg36path = nsgVersion36 || lineArray[index].matches("^\\s*[a-z][A-Za-z\\-_0-9]+\\[0]\$".toRegex())
                if (useNsg36path) {
                    read = 0
                }
                val indentationObject = subType.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList().namedType()
                var nextLine = lineArray.getOrNull(index + read) ?: return read
                var newLevel = getIndentationLevel(nextLine)
                while (indentation < newLevel || (useNsg36path && indentation <= newLevel)) {
                    if (!useNsg36path) {
                        popStacks(newLevel)
                    }
                    val newIdentifier = getIdentifier(nextLine)
                    if (nextLine.matches("^.*\\[\\d+]\\s*.*".toRegex()) && (!useNsg36path || newIdentifier == "" || identifier == newIdentifier)) {
                        if(useNsg36path) {
                            popStacks(newLevel + 1)
                            if (read > 0 && indentationObjectStack.isNotEmpty()) {
                                indentationObjectStack.pop()
                                typesStack.pop()
                                writer.leaveObject(identifier)
                            }
                        }
                        indentationObjectStack.push(newLevel)
                        typesStack.push(indentationObject)
                        writer.enterObject(identifier)
                        if (nextLine.contains("->")) {
                            processLines(0, listOf( nextLine.split("->").last()), overrideIndentation = newLevel, disablePop= true)
                        }
                        read++
                    } else {
                        if(useNsg36path) {
                            popStacks(newLevel)
                        }
                        read += processLines(index + read, lineArray)
                    }
                    nextLine = lineArray.getOrNull(index + read) ?: return read
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


    private fun getStringValue(line: String): String? {
        return "\\s:\\s([\\w-]*)".toRegex().find(line)?.groups?.get(1)?.value
    }

    private fun getIntValue(line: String): BigInteger? {
        val value = "\\s:\\s(\\d*)".toRegex().find(line)?.groups?.get(1)?.value
        return value?.let {
            BigInteger(value)
        }
    }

    private fun getBooleanValue(line: String): Boolean {
        return "true" == "\\s:\\s(true|false)".toRegex().find(line)?.groups?.get(1)?.value
    }

    private fun getBitsValue(line: String, lowerBound: Int = 0): String? {
        // NSG 2.x = F(4 bits) or F0(4 bits) or 'F'H '1'B (31)
        if (line.contains("bits")) {
            val match = "\\s:\\s([0-9A-F]+)\\((\\d+)\\sbit".toRegex().find(line)
            return match?.groups?.get(1)?.value?.let {
                BigInteger(it, 16).toString(2).padStart(
                    match.groups[2]?.value?.toInt() ?: 0, '0'
                ).dropLastWhile { it == '0' }.padEnd(
                    match.groups[2]?.value?.toInt() ?: 0, '0'
                )
            }
        } else if(line.contains("'H")) {
            return "\\s:\\s'[0-9A-F]*'?H?\\s?'?[0-1]*'?B?\\s?\\((\\d+)\\)".toRegex().find(line)
                ?.groups?.get(1)?.value?.let {
                BigInteger(it).toString(2).padStart(
                    lowerBound, '0'
                )
            }
        }
        // NSG 3.x and 4.x '11000000 00000000 00000000 00000000'B(3221225472) or '11'B(3)
        return "\\s:\\s'([0-1\\s]+)'B".toRegex().find(line)?.groups?.get(1)?.value?.replace(" ", "")

    }

    private fun getBooleanValueCustom(line: String, trueString: String): Boolean {
        return trueString == "\\s:\\s($trueString)".toRegex().find(line)?.groups?.get(1)?.value
    }
}
