package pt.isel.pc.sync

import kotlin.jvm.Throws
import kotlin.time.Duration

interface UnarySemaphore {
    @Throws(InterruptedException::class)
    fun tryAcquire(timeout: Duration): Boolean
    fun release(): Unit
}