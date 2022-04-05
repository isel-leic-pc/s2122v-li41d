package pt.isel.pc.sync

import kotlin.jvm.Throws
import kotlin.time.Duration

interface NArySemaphore {
    @Throws(InterruptedException::class)
    fun tryAcquire(requestedUnits: Int, timeout: Duration): Boolean
    fun release(releasedUnits: Int)
}