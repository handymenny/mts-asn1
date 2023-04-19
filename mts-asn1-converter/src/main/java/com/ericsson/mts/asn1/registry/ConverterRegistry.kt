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
package com.ericsson.mts.asn1.registry

import com.ericsson.mts.asn1.ASN1Parser.*
import org.antlr.v4.runtime.ParserRuleContext
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap

/**
 * Used to store types from .asn files which are parsed with
 * [ConverterVisitor][com.ericsson.mts.asn1.visitor.ConverterVisitor]
 */
class ConverterRegistry private constructor(private val typeIndexingRegistry: MutableMap<String, TypeAssignmentContext>) {
    constructor(): this(ConcurrentHashMap<String, TypeAssignmentContext>())

    fun addAssignment(assignmentContext: AssignmentContext) {
        if (null != assignmentContext.typeAssignment()) {
            typeIndexingRegistry[assignmentContext.IDENTIFIER().text] = assignmentContext.typeAssignment()
        }
    }

    /**
     *
     * This method returns the type referenced by CONTAINING/ENCODED, if any
     *
     * @param ctx the context of an ASN.1 type
     * @return The [ParserRuleContext]  referenced by CONTAINING/ENCODED or null
     */
    fun getContaining(ctx: AsnTypeContext): ParserRuleContext? {
        if (ctx.constraint().size > 0 && ctx.constraint()[0] != null) {
            val contentsConstraint = ctx.constraint(0).constraintSpec().generalConstraint().contentsConstraint()
            if (contentsConstraint.ENCODED_LITERAL() != null || contentsConstraint.CONTAINING_LITERAL() != null) {
                return getType(contentsConstraint.asnType())
            }
        }
        return null
    }

    /**
     *
     * This method returns the type referenced by CONTAINING/ENCODED, if any
     *
     * @param identifier the identifier of an ASN.1 type
     * @return The [ParserRuleContext] referenced by CONTAINING/ENCODED or null
     */
    fun getContaining(identifier: String): ParserRuleContext? {
        return getTranslatorContext(identifier)?.let { type ->
            getContaining(type.asnType())
        }
    }

    /**
     *
     * This method returns the lower bound limit of the size constraint or null if missing
     *
     * @param ctx the context of an ASN.1 type
     * @return Lower bound size as [BigInteger]
     */
    fun getLowerBoundSizeConstraint(ctx: AsnTypeContext): BigInteger? {
        var subType: SubtypeElementsContext? = null
        var constraint = ctx.constraint(0)

        while (constraint != null) {
            subType = constraint.constraintSpec()?.subtypeConstraint()?.elementSetSpecs()?.rootElementSetSpec()
                ?.elementSetSpec()?.unions()?.intersections(0)?.intersectionElements(0)
                ?.elements()?.subtypeElements()
            val sizeConstraint = subType?.sizeConstraint()
            constraint = sizeConstraint?.constraint()
        }

        return subType?.value(0)?.builtinValue()?.let { size ->
            BigInteger(size.text)
        }
    }

    /**
     *
     * This method returns the lower bound limit of the size constraint or null if missing
     *
     * @param identifier the identifier of an ASN.1 type
     * @return Lower bound limit as [BigInteger]
     */
    fun getLowerBoundSizeConstraint(identifier: String): BigInteger? {
        return getTranslatorContext(identifier)?.let { type ->
            getLowerBoundSizeConstraint(type.asnType())
        }
    }

    /**
     *
     * This method returns the builtin type context of the given ASN.1 type or null if missing
     *
     * @param identifier the identifier of an ASN.1 type
     * @return builtin type context as [BuiltinTypeContext]
     */
    fun getType(identifier: String): ParserRuleContext? {
        return getTranslatorContext(identifier)?.asnType()?.let { getType(it) }
    }

    /**
     *
     * This method returns the builtin type context of the given ASN.1 type or null if missing
     *
     * @param ctx the context of an ASN.1 type
     * @return builtin type context as [BuiltinTypeContext]
     */
    fun getType(ctx: AsnTypeContext): ParserRuleContext? {
        var builtin = ctx.builtinType()
        if (builtin == null) {
            val referencedType = referencedType(ctx)
            if (referencedType != null) {
                builtin = getTranslatorContext(referencedType)?.asnType()?.builtinType()
            }
        }

        return builtin.enumeratedType()
            ?: builtin.sequenceType()
            ?: builtin.sequenceOfType()
            ?: builtin.integerType()
            ?: builtin.bitStringType()
            ?: builtin.choiceType()
            ?: builtin.octetStringType()
            ?: builtin.characterStringType()
            ?: builtin.objectClassFieldType()
            ?: builtin.objectidentifiertype()
            ?: builtin.realType()
            ?: builtin.setOfType()
            ?: builtin.setType()
            ?: builtin
    }

    private fun getTranslatorContext(identifier: String): TypeAssignmentContext? {
        return typeIndexingRegistry[identifier]
    }

    private fun referencedType(ctx: AsnTypeContext): String? {
        return ctx.referencedType()?.definedType()?.IDENTIFIER(0)?.text
    }

    fun clone(): ConverterRegistry = ConverterRegistry(ConcurrentHashMap(typeIndexingRegistry))
}