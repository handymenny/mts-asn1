// SPDX-License-Identifier: MIT
/*
 * Copyright 2019 Ericsson, https://www.ericsson.com/en
 * Copyright 2024 Andrea Mennillo, https://amennillo.eu
 */
package com.ericsson.mts.asn1

import com.ericsson.mts.asn1.exception.NotHandledCaseException
import com.ericsson.mts.asn1.util.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.math.BigInteger
import java.util.*

class PERTranscoderKT @JvmOverloads constructor(val isAligned: Boolean, val isPermissive: Boolean = false) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass.simpleName)

    /**
     * Read n bytes
     *
     * @param s binary stream
     * @param n number of bytes read
     * @return byte array containing the bytes read (aligned in aligned variant)
     * @throws IOException
     */
    @Throws(IOException::class)
    fun readBytes(s: BitInputStream, n: Int): ByteArray {
        return if (isAligned) {
            s.readAlignedByteArray(n)
        } else {
            s.readUnalignedByteArray(n)
        }
    }

    /**
     * Skip Aligned bits
     *
     * @param stream bit input stream
     * @param alwaysAlign if false align only for PER ALIGNED, it true for both
     */
    fun skipAlignedBits(stream: BitInputStream, alwaysAlign: Boolean = false) {
        if (alwaysAlign || isAligned) {
            stream.skipUnreadedBits()
        }
    }

    /**
     * Skip Aligned bits
     *
     * @param array the bit array stream
     * @param alwaysAlign if false align only for PER ALIGNED, it true for both
     * @throws IOException
     */
    @Throws(IOException::class)
    fun skipAlignedBits(array: BitArray, alwaysAlign: Boolean = false) {
        if (alwaysAlign || isAligned) {
            array.skipAlignedBits()
        }
    }

    /**
     * Decode a constrained integer. ITU-T X.691 13.2 and 11.5.
     *
     * @param lb lower bound
     * @param ub upper bound
     * @param stream input stream
     * @return number decoded
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeConstrainedInteger(lb: BigInteger, ub: BigInteger, stream: BitInputStream): BigInteger {
        logger.trace("decodeConstrainedInteger min={}, max={}", lb, ub)
        // 13.2.1 -> 11.5.4, 13.2.2 -> 11.5

        // 11.5.2, 11.5.3
        val max = ub - lb
        val range = ub - lb + 1
        require(range > 0) { "illegal ranges lb=$lb, ub=$ub" }

        val bitLength: Int
        if (range == BigInteger.ONE) {
            // 11.5.4
            bitLength = 0
        } else if (!isAligned || range < 256) {
            // 11.5.6, 11.5.7.1
            bitLength = max.bitLength()
        } else if (range == 256.toBigInteger()) {
            // 11.5.7.2
            skipAlignedBits(stream)
            bitLength = 8
        } else if (range <= UNIT_64K) {
            logger.trace("257 <= range <= 64K, decode on two bytes")
            // 11.5.7.3
            skipAlignedBits(stream)
            bitLength = 16
        } else {
            // 11.5.7.4 + length determinant 13.2.6
            val maxBits = max.bitLengthAligned()
            val maxByteLen = toByteCount(maxBits)

            logger.trace(
                "decodeConstrainedInteger (range {}) encoded over {} bits ({} bytes)",
                max,
                maxBits,
                maxByteLen
            )
            val intLen = decodeConstrainedLengthDeterminant(1, maxByteLen, stream)
            skipAlignedBits(stream)
            bitLength = intLen * 8
        }

        val n = lb + decodeNonNegativeNumber(bitLength, stream)

        logger.trace("decodeConstrainedInteger result={}", n)

        return n
    }

    /**
     * Encode a constrained integer. ITU-T X.691 13.2 and 11.5.
     *
     * @param s output bitarray
     * @param number value to encode
     * @param lb lower bound
     * @param ub upper bound
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encodeConstrainedInteger(s: BitArray, number: BigInteger, lb: BigInteger, ub: BigInteger) {
        logger.trace("encodeConstrainedInteger : number={}, lb={}, ub={}", number, lb, ub)
        // 13.2.1 -> 11.5.4, 13.2.2 -> 11.5

        // 11.5.2, 11.5.3
        val value = number - lb
        val max = ub - lb
        val range = ub - lb + 1

        require(range > 0) { "illegal ranges lb=$lb, ub=$ub" }

        if (range == BigInteger.ONE) {
            // 11.5.4
            return
        }

        if (!isAligned || range < 256) {
            // 11.5.6, 11.5.7.1
            encodeNonNegativeNumber(s, value, max.bitLength())
        } else if (range == 256.toBigInteger()) {
            // 11.5.7.2
            skipAlignedBits(s)
            encodeNonNegativeNumber(s, value, 8)
        } else if (range <= UNIT_64K) {
            // 11.5.7.3
            skipAlignedBits(s)
            encodeNonNegativeNumber(s, value, 16)
        } else {
            // 11.5.7.4 + length determinant 13.2.6
            encodeConstrainedLengthDeterminant(
                s,
                toByteCount(value.bitLength()),
                1,
                toByteCount(max.bitLength())
            )
            skipAlignedBits(s)
            val valueBits = value.bitLengthAligned()
            encodeNonNegativeNumber(s, value, valueBits)
        }
    }


    /**
     * Decode a constrained length determinant. ITU-T X.691 11.9.
     *
     * @param lb lower bound
     * @param ub upper bound
     * @param stream input stream
     * @return length decoded
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeConstrainedLengthDeterminant(
        lb: Int,
        ub: Int,
        stream: BitInputStream
    ): Int {
        logger.trace("decodeConstrainedLengthDeterminant : {} - {}", lb, ub)

        val len = if (ub < UNIT_64K) {
            // 11.9.3.3, 11.9.4.1
            decodeConstrainedInteger(lb.toBigInteger(), ub.toBigInteger(), stream).intValueExact()
        } else {
            // 11.9.3.5, 11.9.4.2
            decodeUnconstrainedLengthDeterminant(stream)
        }
        logger.trace("decodeConstrainedLengthDeterminant : {}", len)
        return len
    }

    /**
     * Encode a constrained length determinant. ITU-T X.691 11.9
     *
     * @param s output bitarray
     * @param length length to encode
     * @param lb lower bound
     * @param ub upper bound
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encodeConstrainedLengthDeterminant(s: BitArray, length: Int, lb: Int, ub: Int) {
        logger.trace("encodeConstrainedLengthDeterminant : length={} , lb={} , ub={}", length, lb, ub)
        if (ub < UNIT_64K) {
            // 11.9.3.3, 11.9.4.1
            encodeConstrainedInteger(s, length.toBigInteger(), lb.toBigInteger(), ub.toBigInteger())
        } else {
            // 11.9.3.5, 11.9.4.2
            encodeUnconstrainedLengthDeterminant(s, length)
        }
    }

    /**
     * Decode a Normally Small length determinant. ITU-T X.691 11.9.
     *
     * @param stream input stream
     * @return length decoded
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeNormallySmallLengthDeterminant(stream: BitInputStream): Int {
        logger.trace("decodeNormallySmallLengthDeterminant")
        // 11.9.4.2, 11.9.3.4

        val unconstrained = stream.readBit() == 1
        val len = if (unconstrained) {
            decodeUnconstrainedLengthDeterminant(stream)
        } else {
            decodeNonNegativeNumber(6, stream).intValueExact() + 1
        }

        logger.trace("decodeNormallySmallLengthDeterminant : {}", len)
        return len
    }

    /**
     * Encode a normally small length determinant. ITU-T X.691 11.9.
     *
     * @param s output bitarray
     * @param length length to encode
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encodeNormallySmallLengthDeterminant(s: BitArray, length: Int) {
        logger.trace("encodeNormallySmallLengthDeterminant : length={}", length)
        // 11.9.4.2, 11.9.3.4

        if (length <= 64) {
            s.writeBit(0)
            encodeNonNegativeNumber(s, (length - 1).toBigInteger(), 6)
        } else {
            s.writeBit(1)
            encodeUnconstrainedLengthDeterminant(s, length)
        }
    }

    /**
     * Decode an unconstrained length determinant. ITU-T X.691 11.9.
     *
     * @param stream input stream
     * @return length determinant
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeUnconstrainedLengthDeterminant(stream: BitInputStream): Int {
        skipAlignedBits(stream)

        var result = stream.read()

        val type = result shr 6

        if (type < 0b10) {
            // 11.9.3.6: len <= 127
            // A single octet containing "n" with bit 8 set to zero;
        } else if (type == 0b10) {
            // 11.9.3.7: 127 < len < 16K
            result = (result and 0x3f) shl 8
            result = result or stream.read()
        } else if (type == 0b11) {
            // 11.9.3.8: >= 16K
            throw NotHandledCaseException("number of 16k chunks not supported")
        }

        logger.trace("decoded Unconstrained len determinant {}", result)
        return result
    }

    /**
     * Encode an unconstrained length determinant. ITU-T X.691 11.9.
     *
     * @param s output bitarray
     * @param length length to encode
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encodeUnconstrainedLengthDeterminant(s: BitArray, length: Int) {
        logger.trace("encodeLengthDeterminant : length={}", length)
        skipAlignedBits(s)

        if (length <= 0x7F) {
            s.write(length and 0x7F)
        } else if (length < UNIT_16K) {
            val firstByte = (length shr 8 or 0x80) and 0xBF
            val secondByte = length and 0xFF
            s.write(firstByte)
            s.write(secondByte)
        } else {
            throw NotHandledCaseException("number of 16k chunks not supported")
        }
    }

    /**
     * Decode a normally whole number. ITU-T X.691 11.6
     *
     * @param stream input stream
     * @return normally small number
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeNormallySmallWholeNumber(stream: BitInputStream): BigInteger {
        val small = stream.readBit() == 0

        val result = if (small) {
            // 11.6.1
            decodeNonNegativeNumber(6, stream)
        } else {
            // 11.6.2
            decodeSemiConstrainedWholeNumber(BigInteger.ZERO, stream)
        }
        return result
    }

    /**
     * Encode a normally whole number. ITU-T X.691 11.6
     *
     * @param s output bitarray
     * @param number value to encode
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encodeNormallySmallWholeNumber(s: BitArray, number: BigInteger) {
        logger.trace("encodeNormallySmallWholeNumber : number={}", number)

        if (number <= 63) {
            s.writeBit(0)
            encodeNonNegativeNumber(s, number, 6)
        } else {
            s.writeBit(1)
            encodeSemiConstrainedWholeNumber(s, BigInteger.ZERO, number)
        }
    }

    /**
     * Decode a semi constrained whole number. ITU-T X.691 11.7.
     *
     * @param lb lower bound
     * @param stream binary input
     * @return semi constrained integer
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeSemiConstrainedWholeNumber(lb: BigInteger, stream: BitInputStream): BigInteger {
        val octetLen = decodeUnconstrainedLengthDeterminant(stream)
        skipAlignedBits(stream)

        return decodeNonNegativeNumber(octetLen * 8, stream) + lb
    }

    /**
     * Encode a semi constrained whole number. ITU-T X.691 11.7.
     *
     * @param s output bitarray
     * @param lb lower bound
     * @param number number to encode
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encodeSemiConstrainedWholeNumber(s: BitArray, lb: BigInteger, number: BigInteger) {
        val value = number - lb
        val octetLength = value.bitLengthAligned() / 8

        logger.trace("encodeSemiConstrainedWholeNumber : value= {}, length= {}", number, octetLength)

        encodeUnconstrainedLengthDeterminant(s, octetLength)
        s.skipAlignedBits()
        encodeNonNegativeNumber(s, number, octetLength)
    }

    /**
     * Decode a non-negative integer from a bit field.
     *
     * @param bitLength the maximum number of bits to read
     * @param stream input stream
     * @return non-negative integer decoded
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun decodeNonNegativeNumber(bitLength: Int, stream: BitInputStream): BigInteger {
        return if (bitLength < 32) {
            // read as int if < 32 bits
            stream.readBits(bitLength).toBigInteger()
        } else {
            stream.bigReadBits(bitLength)
        }
    }

    /**
     * Encode a non negative integer in a bit field.
     *
     * @param s bit array
     * @param number to encode
     * @param length length of the bit field
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun encodeNonNegativeNumber(s: BitArray, number: BigInteger, length: Int) {
        val bits = number.toString(2).padStart(length, '0')
        encodeBitString(s, bits)
    }

    /**
     * Decode an octet string as a ByteArray. ITU-T X.691 17.
     *
     * @param stream input stream
     * @param len octet string length in bytes
     * @param fixed if the octet string has fixed length
     * @return octet string as a byte array
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeOctetString(stream: BitInputStream, len: Int, fixed: Boolean = true): ByteArray {
        if (len < UNIT_64K) {
            // Octet strings of fixed length less than or equal to two octets are not octet-aligned.
            // All other octet strings are octet-aligned in the ALIGNED variant.
            if (!isAligned || fixed && len <= 2) {
                return stream.readUnalignedByteArray(len)
            }
            return stream.readAlignedByteArray(len)
        } else {
            throw NotHandledCaseException("number of 16k chunks not supported")
        }
    }

    /**
     * Encode an hexString as Octet String. ITU-T X.691 17.
     *
     * @param s output bitarray
     * @param hexString hex string to encode
     * @param length length of the string in bits
     * @throws IOException
     */
    @Throws(IOException::class)
    fun encodeOctetString(s: BitArray, hexString: String, length: Int) {
        logger.trace("encodeOctetString length : {}, length={}", hexString.length, length)
        logger.trace("encodeOctetString value : {}", hexString)

        val bits = hexString.toBigInteger(16).toString(2).padStart(length, '0')
        encodeBitString(s, bits)
    }

    /**
     * Read n bits and return it as a string.
     *
     * @param s input stream
     * @return string containing n bits read (aligned left)
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decodeBitString(s: BitInputStream, n: Int): String {
        return decodeNonNegativeNumber(n, s).toString(2).padStart(n, '0')
    }

    /**
     * Encode the given bit-string.
     *
     * @param s output bitarray
     * @param bitString string containing the bits to write
     */
    fun encodeBitString(s: BitArray, bitString: String) {
        for (digit in bitString) {
            if (digit == '1') {
                s.writeBit(1)
            } else {
                s.writeBit(0)
            }
        }
    }
}

