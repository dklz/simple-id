package dev.inflx.simpleid

import java.lang.Thread.sleep
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.random.Random
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.*

class SimpleIdTests {

    @RepeatedTest(1000)
    fun testNewId() {
        val customEpoch = Random.nextLong()
        val factory = SimpleIdFactoryV1(customEpoch = customEpoch)
        val id = factory.newId()

        assertEquals(0, id.value ushr 60, "The first 4 bits should be zeros")
        assertEquals(16, id.representation.length, "The representation should be 16 characters")
    }

    @RepeatedTest(1000)
    fun testNewIdConstruction() {
        val customEpoch = Random.nextLong()
        val factory = SimpleIdFactoryV1(customEpoch = customEpoch)
        val elapsedTime = Random.nextLong(2.0.pow(31).toLong())
        val randomness = Random.nextLong(2.0.pow(29).toLong())
        val id = factory.newId(customEpoch + elapsedTime, randomness)

        assertEquals(0, id.value ushr 60, "The first 4 bits should be zeros")
        assertEquals(
            elapsedTime,
            id.value ushr 29 and 0x7FFFFFFF,
            "Elapsed time was encoded incorrectly",
        )
        assertEquals(
            randomness,
            id.value and 0x1FFFFFFF,
            "The randomness bits was encoded incorrectly",
        )
    }

    @Test
    fun testIdOrdering() {
        val idList1 = (0..<100).map { SimpleId.newId() }
        sleep(2000)
        val idList2 = (0..<100).map { SimpleId.newId() }

        assertTrue(
            idList1.zip(idList2).all { (id1, id2) -> id1 < id2 },
            "Ids should be ordered by timestamp",
        )
    }

    @ParameterizedTest
    @CsvSource(
        value = ["100, 10000, 0, 0.0001", "1000, 10000, 0.0002, 0.0015", "10000, 1000, 0.05, 0.1"]
    )
    fun testCollisionRates(numberOfIds: Int, attempts: Int, minRate: Double, maxRate: Double) {
        var collisions = 0

        repeat(attempts) {
            val seen = mutableSetOf<SimpleId>()
            var duplicates = 0

            repeat(numberOfIds) {
                val id = SimpleId.newId()

                if (seen.add(id).not()) {
                    duplicates++
                }
            }

            if (duplicates > 0) {
                collisions++
            }
        }

        val collisionRate = collisions.toDouble() / attempts
        val format: (Double) -> String = DecimalFormat("0.00%")::format

        assertTrue(
            collisionRate in minRate..maxRate,
            "Expected ${format(minRate)} to ${format(maxRate)} of collisions, but found ${format(collisionRate)}",
        )
    }

    @ParameterizedTest
    @CsvSource(
        value =
            [
                "627368014968138507, HD6V-WAXS-WARB-B",
                "833974850860044122, Q4PZ-PN8H-YKTT-1",
                "894416410230312985, RTCT-ZHYG-XM0S-8",
                "258796108239875842, 75VD-P1TE-8BR2-5",
            ]
    )
    fun testParseInput(value: Long, representation: String) {
        assertEquals(value, SimpleId.valueOf(representation).value)
        assertEquals(representation, SimpleId.valueOf(value).representation)
    }

    @ParameterizedTest
    @ValueSource(
        strings =
            [
                "HD6V-WAXS-WARB-B0", // Too long
                "HD6V-WAUS-WARB-B", // Invalid character U
                "HD6V-WAXS-WARB-1", // Incorrect checksum
                "HD6V_WAXS_WARB_B", // Incorrect separator
                "HD6V~WAXS~WARB~B", // Incorrect separator
                "",
                "     ",
                "asdf",
            ]
    )
    fun testDecodeInvalidInput(input: String) {
        assertThrows<IllegalArgumentException> { SimpleId.valueOf(input) }
    }

    @Test
    fun testEncodeInvalidInput() {
        assertThrows<IllegalArgumentException> { SimpleId.valueOf(Long.MAX_VALUE) }
    }

    @ParameterizedTest
    @CsvSource(
        value =
            [
                "627368014968138507, HD6V-WaXS-WARB-B", // Replace A with a
                "833974850860044122, Q4PZ-PN8H-YKTT-I", // Replace 1 with I
                "894416410230312985, RTCT-ZHYG-XMOS-8", // Replace 0 with O
                "258796108239875842, 75VD-PlTE-8BR2-5", // Repace 1 with l
                "258796108239875842, 75VD-PlTE-8BR25", // Omit -
                "258796108239875842, 75VDPlTE8BR25", // Remove all -
                "258796108239875842, 7-5VD-P-lTE8-BR25", // Random - placements
            ]
    )
    fun testParseVariantInput(expected: Long, input: String) {
        assertEquals(expected, SimpleId.valueOf(input).value)
    }

    @RepeatedTest(1000)
    fun testEncodeDecode() {
        val value = Random.nextLong() ushr 4
        val representation = SimpleId.valueOf(value).representation
        assertEquals(value, SimpleId.valueOf(representation).value)
    }
}
