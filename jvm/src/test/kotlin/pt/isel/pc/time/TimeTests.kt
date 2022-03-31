package pt.isel.pc.time

import org.junit.jupiter.api.Test
import pt.isel.pc.utils.MutableTimeout
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class TimeTests {

    @Test
    fun `INFINITE timeout never elapses`() {
        val mutableTimeout = MutableTimeout(Duration.INFINITE)
        assertFalse(mutableTimeout.elapsed)
    }

    @Test
    fun `ZERO timeout is immediately elapsed`() {
        val mutableTimeout = MutableTimeout(Duration.ZERO)
        assertTrue(mutableTimeout.elapsed)
    }

    @Test
    fun `test decrement`() {
        val mutableTimeout = MutableTimeout(1L.toDuration(DurationUnit.MILLISECONDS))
        assertFalse(mutableTimeout.elapsed)

        mutableTimeout.nanos = mutableTimeout.nanos - 600_000
        assertFalse(mutableTimeout.elapsed)

        mutableTimeout.nanos = mutableTimeout.nanos - 600_000
        assertTrue(mutableTimeout.elapsed)

        assertEquals(-200_000, mutableTimeout.nanos)
    }
}