package dev.inflx.simpleid

import kotlin.random.Random
import kotlin.time.*

private val DEFAULT_CUSTOM_EPOCH = Instant.parse("2026-01-01T00:00:00Z").epochSeconds

interface SimpleId : Comparable<SimpleId> {
    val value: Long
    val representation: String

    companion object Default : SimpleIdFactory by SimpleIdFactoryV1()
}

interface SimpleIdFactory {

    fun newId(): SimpleId

    fun valueOf(input: String): SimpleId

    fun valueOf(input: Long): SimpleId

    fun parse(input: String): SimpleId? = runCatching { valueOf(input) }.getOrNull()

    fun parse(input: Long): SimpleId? = runCatching { valueOf(input) }.getOrNull()
}

class SimpleIdFactoryV1(
    private val customEpoch: Long = DEFAULT_CUSTOM_EPOCH,
    private val random: Random = Random,
) : SimpleIdFactory {

    override fun newId() = newId(Clock.System.now().epochSeconds, random.nextLong())

    internal fun newId(currentEpochSecond: Long, randomness: Long): SimpleId {
        return SimpleIdV1(
            ((currentEpochSecond - customEpoch) and 0x7FFFFFFF shl 29) or
                (randomness and 0x1FFFFFFF)
        )
    }

    override fun valueOf(input: String): SimpleId = SimpleIdV1(input)

    override fun valueOf(input: Long): SimpleId = SimpleIdV1(input)

    private class SimpleIdV1
    private constructor(override val value: Long, override val representation: String) : SimpleId {

        constructor(value: Long) : this(value, encode(value))

        constructor(representation: String) : this(decode(representation), representation)

        override fun compareTo(other: SimpleId) = value.compareTo(other.value)

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }

            if (other !is SimpleId) {
                return false
            }

            return value == other.value
        }

        override fun hashCode() = value.hashCode()

        override fun toString() = "SimpleIdV1(value=$value, repr=$representation)"
    }

    companion object {
        private fun encode(input: Long): String = buildString {
            require((input ushr 60) == 0L) { "Invalid input: $input" }

            append(ENC[(input ushr 55 and 0x1FL).toInt()])
            append(ENC[(input ushr 50 and 0x1FL).toInt()])
            append(ENC[(input ushr 45 and 0x1FL).toInt()])
            append(ENC[(input ushr 40 and 0x1FL).toInt()])
            append("-")
            append(ENC[(input ushr 35 and 0x1FL).toInt()])
            append(ENC[(input ushr 30 and 0x1FL).toInt()])
            append(ENC[(input ushr 25 and 0x1FL).toInt()])
            append(ENC[(input ushr 20 and 0x1FL).toInt()])
            append("-")
            append(ENC[(input ushr 15 and 0x1FL).toInt()])
            append(ENC[(input ushr 10 and 0x1FL).toInt()])
            append(ENC[(input ushr 5 and 0x1FL).toInt()])
            append(ENC[(input and 0x1FL).toInt()])
            append("-")
            append(ENC[checksum(input)])
        }

        private fun decode(input: String): Long {
            val chars = input.filter { it != '-' }.uppercase()
            require(chars.length == 13 && chars.all(::isValid)) { "Invalid input: $input" }

            val decoded = chars.map { DEC[it.code] }
            var result = decoded[0]
            result = (result shl 5) or decoded[1]
            result = (result shl 5) or decoded[2]
            result = (result shl 5) or decoded[3]
            result = (result shl 5) or decoded[4]
            result = (result shl 5) or decoded[5]
            result = (result shl 5) or decoded[6]
            result = (result shl 5) or decoded[7]
            result = (result shl 5) or decoded[8]
            result = (result shl 5) or decoded[9]
            result = (result shl 5) or decoded[10]
            result = (result shl 5) or decoded[11]

            require(checksum(result) == decoded[12].toInt())

            return result
        }

        private fun checksum(input: Long): Int = (input.toULong() % 31UL).toInt()

        private fun isValid(char: Char) =
            char.code in 48..57 || // 0-9
                (char.code in 65..90 && char.code != 85) // A-Z excluding U

        private const val ENC = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

        private val DEC =
            longArrayOf(
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                0,
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                10,
                11,
                12,
                13,
                14,
                15,
                16,
                17,
                1,
                18,
                19,
                1,
                20,
                21,
                0,
                22,
                23,
                24,
                25,
                26,
                -1,
                27,
                28,
                29,
                30,
                31,
                -1,
                -1,
                -1,
                -1,
                -1,
                -1,
                10,
                11,
                12,
                13,
                14,
                15,
                16,
                17,
                -1,
                18,
                19,
                -1,
                20,
                21,
                -1,
                22,
                23,
                24,
                25,
                26,
                -1,
                27,
                28,
                29,
                30,
                31,
            )
    }
}
