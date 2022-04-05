package pt.isel.pc.utils

import java.util.concurrent.locks.Condition
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun Duration.isZero() = this.inWholeMilliseconds == 0L

fun Duration.noWait() = this.inWholeMilliseconds <= 0

class MutableTimeout(var duration: Duration) {
    val elapsed: Boolean
        get() = nanos <= 0

    var nanos: Long
        get() = duration.inWholeNanoseconds
        set(value) {
            duration = value.toDuration(DurationUnit.NANOSECONDS)
        }
}

fun Condition.await(mutableTimeout: MutableTimeout) {
    if (mutableTimeout.duration.isInfinite()) {
        await()
    } else {
        awaitNanos(mutableTimeout.nanos).also {
            mutableTimeout.nanos = it
        }
    }
}