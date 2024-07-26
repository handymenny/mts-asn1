// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1.translator

import com.ericsson.mts.asn1.*
import com.ericsson.mts.asn1.exception.InvalidParameterException
import com.ericsson.mts.asn1.exception.NotHandledCaseException
import com.ericsson.mts.asn1.factory.FormatReader
import com.ericsson.mts.asn1.factory.FormatWriter
import com.ericsson.mts.asn1.util.UNIT_64K
import com.ericsson.mts.asn1.util.toByteCount
import java.io.ByteArrayInputStream

class PERSequenceTranslatorKT(private val perTranscoder: PERTranscoderKT) : AbstractSequenceTranslator() {
    @Throws(Exception::class)
    override fun doEncode(
        s: BitArray,
        reader: FormatReader,
        translatorContext: TranslatorContext,
        inputFieldList: List<String>,
        registry: Map<String, String>
    ) {
        logger.trace(
            "Enter {} encoder, name {}, with input fields {}",
            this.javaClass.simpleName,
            this.name,
            inputFieldList
        )

        // 19.1, 19.6, 19.7
        val extensionBitmap = buildExtensionBitmap(inputFieldList)
        val extensionPresent = extensionBitmap?.any { it } == true

        if (extensionBitmap != null) {
            // 19.1
            s.writeBit(if (extensionPresent) 1 else 0)
        }

        // 19.2, 19.3, 19.4, 19.5
        encodeSequenceFields(s, reader, translatorContext, inputFieldList, registry, fieldList)

        if (extensionPresent) {
            // 19.7, 19.8, 19.9
            encodeExtension(s, extensionBitmap!!, reader, translatorContext, inputFieldList, registry)
        }
    }

    private fun buildExtensionBitmap(
        inputFieldList: List<String>,
    ): BooleanArray? {
        // 19.6
        if (!hasEllipsis && !optionalExtensionMarker && extensionAndException == -1) {
            return null
        }

        // 19.7
        val extensionBitmap = additionnalFieldList.map { field ->
            if (field is FieldGroup) {
                field.children.any { it.getName() in inputFieldList }
            } else {
                (field as Field).getName() in inputFieldList
            }
        }

        return extensionBitmap.toBooleanArray()
    }

    private fun encodeExtension(
        s: BitArray,
        extensionBitmap: BooleanArray,
        reader: FormatReader,
        translatorContext: TranslatorContext,
        inputFieldList: List<String>,
        registry: Map<String, String>
    ) {
        // 19.8
        val extensionLength = extensionBitmap.size
        // write len, min len is 1
        perTranscoder.encodeNormallySmallLengthDeterminant(s, extensionLength)
        // write extension bitmap
        extensionBitmap.forEach {
            val bit = if (it) 1 else 0
            s.writeBit(bit)
        }
        // align if needed
        perTranscoder.skipAlignedBits(s)

        // 19.9
        val fieldsToAdd = additionnalFieldList.filterIndexed { index, _ -> extensionBitmap[index] }

        for (field in fieldsToAdd) {
            val bitArray = BitArray()

            if (field is FieldGroup) {
                // Extension Addition Group are encoded like Sequence
                encodeSequenceFields(bitArray, reader, translatorContext, inputFieldList, registry, field.children)
            } else {
                encodeSequenceField(bitArray, reader, translatorContext, registry, field as Field)
            }

            val len = toByteCount(bitArray.length.intValueExact())

            logger.trace("Open type for field {} : octet length={}", field, len)
            perTranscoder.encodeUnconstrainedLengthDeterminant(s, len)
            // align needed before any concat
            perTranscoder.skipAlignedBits(bitArray, true)
            s.concatBitArray(bitArray)
        }
    }

    @Throws(Exception::class)
    private fun encodeSequenceFields(
        s: BitArray,
        reader: FormatReader,
        translatorContext: TranslatorContext,
        inputFieldList: List<String>,
        registry: Map<String, String>,
        fieldList: List<Field>
    ) {
        // 19.2 - Build and write preamble (bit-map)
        val optionalFields = fieldList.filter { it.optionnal }
        val preambleBitmap = optionalFields.map { it.getName() in inputFieldList }
        if (preambleBitmap.size >= UNIT_64K) {
            // 19.3
            throw NotHandledCaseException("Preamble fragmentation")
        }
        // write preamble bitmap
        preambleBitmap.forEach {
            val bit = if (it) 1 else 0
            s.writeBit(bit)
        }

        for (field in fieldList) {
            val fieldName = field.getName()
            if (fieldName in inputFieldList) {
                logger.trace("Encode field {}", fieldName)
                // 19.4
                encodeSequenceField(s, reader, translatorContext, registry, field)
            } else if (!field.optionnal) {
                throw InvalidParameterException("Sequence $name need field $fieldName")
            }
        }
    }

    @Throws(Exception::class)
    private fun encodeSequenceField(
        s: BitArray,
        reader: FormatReader,
        translatorContext: TranslatorContext,
        registry: Map<String, String>,
        field: Field
    ) {
        // 19.4
        val typeTranslator = field.getType()
        val parameters = typeTranslator.getParameters()
        if (parameters.isEmpty()) {
            field.getType().encode(field.getName(), s, reader, translatorContext)
        } else {
            //Building parameter list to pass to target translator
            val inputParameters = field.getParameters().map { registry[it] }

            typeTranslator.encode(field.getName(), s, reader, translatorContext, inputParameters)
        }
    }

    @Throws(Exception::class)
    public override fun doDecode(
        s: BitInputStream,
        writer: FormatWriter,
        translatorContext: TranslatorContext,
        registry: Map<String, String>
    ) {
        logger.trace("{} : {}", this.name, this)
        var isExtendedSequence = false

        if (hasEllipsis || optionalExtensionMarker || (extensionAndException != -1)) {
            // 19.1
            isExtendedSequence = s.readBit() == 1
        }

        logger.trace("{} : isExtendedSequence : {}", this.name, isExtendedSequence)

        // 19.2, 19.3, 19.4, 19.5
        decodeSequenceFields(s, writer, translatorContext, registry, fieldList, optionalBitmap)

        if (isExtendedSequence) {
            logger.trace("{} : is and extended sequence", this.name)

            decodeExtension(s, writer, translatorContext, registry)
        }
    }

    private fun decodeExtension(
        s: BitInputStream,
        writer: FormatWriter,
        translatorContext: TranslatorContext,
        registry: Map<String, String>
    ) {
        // 19.8
        val extensionsCount = perTranscoder.decodeNormallySmallLengthDeterminant(s)

        logger.trace("{} : extensions count is {}", this.name, extensionsCount)

        if (extensionsCount == 0) {
            throw NotHandledCaseException("Extension count cannot be 0. This procedure is only invoked if there is at least one extension addition being encoded.")
        }

        // 19.7
        val additionalBitmap = (0 until extensionsCount).map {
            s.readBit() == 1
        }

        logger.trace("{} : additional bitmap is {}", this.name, additionalBitmap)

        // ALIGNED ONLY
        perTranscoder.skipAlignedBits(s)

        // 19.9
        for (index in additionalBitmap.indices) {
            // not present
            if (!additionalBitmap[index]) continue

            // read field payload
            val data = perTranscoder.readBytes(s, perTranscoder.decodeUnconstrainedLengthDeterminant(s))

            val field = additionnalFieldList.getOrNull(index)

            if (field == null) {
                logger.error("skipped additional field of {} bytes", data.size)
                continue
            }

            val input = BitInputStream(ByteArrayInputStream(data))

            if (field is FieldGroup) {
                // Extension Addition Group are encoded like Sequence
                decodeSequenceFields(
                    input,
                    writer,
                    translatorContext,
                    registry,
                    field.children,
                    BooleanArray(field.optionalCount)
                )
            } else {
                decodeSequenceField(s, writer, translatorContext, registry, field as Field)
            }
        }
    }

    @Throws(Exception::class)
    private fun decodeSequenceFields(
        s: BitInputStream,
        writer: FormatWriter,
        translatorContext: TranslatorContext,
        registry: Map<String, String>,
        fieldList: List<Field>,
        optionalBitmap: BooleanArray
    ) {
        // 19.2
        val rootSequenceHasOptional = fieldList.any { it.optionnal }

        // 19.3
        if (rootSequenceHasOptional) {
            if (optionalBitmap.size >= UNIT_64K) {
                throw NotHandledCaseException("Preamble fragmentation")
            }

            for (i in optionalBitmap.indices) {
                optionalBitmap[i] = s.readBit() == 1
            }
        }

        logger.trace("{} : optional bitmap is {}", this.name, optionalBitmap)

        var optionalBitmapIndex = 0
        for (field in fieldList) {
            // 19.4, 19.5
            if (!field.optionnal || optionalBitmap[optionalBitmapIndex++]) {
                decodeSequenceField(s, writer, translatorContext, registry, field)
            }
        }
    }

    @Throws(Exception::class)
    private fun decodeSequenceField(
        s: BitInputStream,
        writer: FormatWriter,
        translatorContext: TranslatorContext,
        registry: Map<String, String>,
        field: Field
    ) {
        // 19.4
        logger.trace("{} : decode field {} ", this.name, field.getName())
        val typeTranslator = field.getType()
        val parameters = typeTranslator.getParameters()
        if (parameters.isEmpty()) {
            typeTranslator.decode(field.getName(), s, writer, translatorContext)
        } else {
            //Building parameter list to pass to target translator
            val inputParameters = field.getParameters().map { registry[it] }
            typeTranslator.decode(field.getName(), s, writer, translatorContext, inputParameters)
        }
    }
}
