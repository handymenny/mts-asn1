package com.ericsson.mts.asn1.converter

import com.ericsson.mts.asn1.ASN1Parser.*
import java.util.Stack

/**
 * Implementation of [AbstractConverter] for qcat Logs
 */
class ConverterQcat : AbstractConverter() {
    private val bitsRegex = """\s'([01\s]+)'B""".toRegex()

    // false -> array, true -> Object
    private val isObjectStack: Stack<Boolean> = Stack()

    override fun cleanup(lines: List<String>): List<String> {
        return lines
    }

    override fun preprocessLine(line: String, identifier: String, indentationLevel: Int): Boolean {
        // Skip blank lines
        if (indentationLevel == line.length) {
            return true
        }

        // pop stacks when line contains "}"
        if (line.substring(indentationLevel).startsWith("}")) {
            if (isObjectStack.size > 0) {
                val isObject = isObjectStack.pop()
                if (isObject) {
                    typesStack.pop()
                    writer.leaveObject(null)
                } else {
                    writer.leaveArray(null)
                }
            }
            return true
        }

        return false
    }

    override fun popStacks(indentation: Int) {
        if (indentation == -1) {
            while (isObjectStack.size > 0) {
                val isObject = isObjectStack.pop()
                if (isObject) {
                    typesStack.pop()
                    writer.leaveObject(null)
                } else {
                    writer.leaveArray(null)
                }
            }
        }
    }

    override fun parseSequence(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: SequenceTypeContext
    ): Int {
        isObjectStack.push(true)
        writer.enterObject(identifier)
        typesStack.push(
            combineComponentListAndAdditions(
                context.componentTypeLists().rootComponentTypeList(0).componentTypeList()
                    .componentType(),
                context.componentTypeLists().extensionAdditions()?.extensionAdditionList()
                    ?.extensionAddition() ?: emptyList()
            )
        )
        return 2
    }

    override fun parseEnumerated(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: EnumeratedTypeContext
    ): Int {
        writer.stringValue(identifier, getValue(lineArray[index], indentation))
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
        val minLength = getLowerBound(identifier)?.intValueExact() ?: 0
        var bits = getBitsValue(line) ?: ""
        if (bits.length < minLength) {
            // Pad if needed
            bits = bits.padEnd(minLength, '0')
        }
        writer.bitsValue(identifier, bits)
        return 1
    }

    override fun parseInteger(
        index: Int,
        lineArray: List<String>,
        identifier: String,
        indentation: Int,
        context: IntegerTypeContext
    ): Int {
        writer.intValue(identifier, getValue(lineArray[index], indentation).toBigInteger(), null)
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
            writer.booleanValue(identifier, getValue(lineArray[index], indentation).toBoolean())
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
        isObjectStack.push(false)
        when (val subType = getSubType(context.asnType())) {
            is SequenceTypeContext -> {
                val subTypeComponents =
                    combineComponentListAndAdditions(
                        subType.componentTypeLists().rootComponentTypeList(0).componentTypeList()
                            .componentType(),
                        subType.componentTypeLists().extensionAdditions()?.extensionAdditionList()
                            ?.extensionAddition() ?: emptyList()
                    )
                var nextLine: String = lineArray.getOrNull(index + read) ?: return read

                val stackSize = isObjectStack.size
                while (isObjectStack.size >= stackSize) {
                    if (isObjectStack.size == stackSize && nextLine.contains("{")) {
                        typesStack.push(subTypeComponents)
                        isObjectStack.push(true)
                        writer.enterObject(identifier)
                        read++
                    } else {
                        read += processLines(index + read, lineArray)
                    }
                    nextLine = lineArray.getOrNull(index + read) ?: break
                }
            }

            is EnumeratedTypeContext -> {
                read++
                var nextLine = lineArray.getOrNull(index + read) ?: return read
                var newLevel = getIndentationLevel(nextLine)
                while (!nextLine.contains("}")) {
                    writer.stringValue(identifier, getValue(nextLine, newLevel))
                    read++
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                    newLevel = getIndentationLevel(nextLine)
                }
            }

            is SequenceOfTypeContext -> {
                val stackSize = isObjectStack.size
                var nextLine: String
                while (isObjectStack.size >= stackSize) {
                    read += processLines(
                        index + read, lineArray, overrideType = subType,
                        overrideIdentifier = identifier
                    )
                    nextLine = lineArray.getOrNull(index + read) ?: return read
                    if (isObjectStack.size == stackSize && nextLine.contains("}")) {
                        writer.leaveArray(identifier)
                        isObjectStack.pop()
                        read++
                    }
                }
            }

            is IntegerTypeContext -> {
                read++
                var nextLine = lineArray.getOrNull(index + read) ?: return read
                var newLevel = getIndentationLevel(nextLine)
                while (!nextLine.contains("}")) {
                    writer.intValue(identifier, getValue(nextLine, newLevel).toBigInteger(), null)
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
                val stackSize = isObjectStack.size
                while (isObjectStack.size >= stackSize) {
                    if (isObjectStack.size == stackSize && nextLine.contains(":")) {
                        typesStack.push(indentationObject)
                        isObjectStack.push(true)
                        writer.enterObject(null)
                    }
                    read += processLines(index + read, lineArray)
                    if (nextLine.contains("}") && isObjectStack.size == stackSize + 1) {
                        typesStack.pop()
                        isObjectStack.pop()
                        writer.leaveObject(null)
                    }
                    nextLine = lineArray.getOrNull(index + read) ?: return read
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
        var read = 0
        if (context.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList() != null) {
            isObjectStack.push(true)
            writer.enterObject(identifier)
            typesStack.push(
                context.alternativeTypeLists().rootAlternativeTypeList().alternativeTypeList()
                    .namedType()
            )
        }
        val preStackSize = isObjectStack.size
        read += processLines(
            index,
            lineArray,
            overrideIdentifier = lineArray[index].substring(indentation).split(" ").getOrNull(1)
        )
        val postStackSize = isObjectStack.size

        if (postStackSize > preStackSize) {
            while (isObjectStack.size >= postStackSize) {
                read += processLines(index + read, lineArray)
            }
        }

        if (postStackSize > preStackSize) {
            typesStack.pop()
            isObjectStack.pop()
            writer.leaveObject(null)
        } else {
            isObjectStack.pop()
            writer.leaveObject(null)
            typesStack.pop()
        }
        return read
    }


    private fun getBitsValue(line: String): String? {
        return bitsRegex.find(line)?.groupValues?.get(1)?.replace(" ", "")

    }

    private fun getValue(line: String, indentation: Int): String {
        return line.substring(indentation).split(" ").last().trimEnd(',')
    }
}
