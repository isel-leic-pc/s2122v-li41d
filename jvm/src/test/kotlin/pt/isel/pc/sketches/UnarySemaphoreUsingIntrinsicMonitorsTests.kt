package pt.isel.pc.sketches

import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

class UnarySemaphoreUsingIntrinsicMonitorsTests {

    @Test
    fun simple() {

        val nOfUnits = 10
        val nOfThreads = 2 * nOfUnits
        val timeout = Duration.ofSeconds(2)
        val semaphore = UnarySemaphoreUsingIntrinsicMonitors(nOfUnits)
        val successes = AtomicInteger(0)
        val timeouts = AtomicInteger(0)
        val threads = List(nOfThreads) {
            Thread {
                val result = semaphore.acquire(timeout)
                if (result) {
                    successes.incrementAndGet()
                } else {
                    timeouts.incrementAndGet()
                }
            }.also {
                it.start()
            }
        }
        threads.forEach {
            it.join()
        }
        assertEquals(nOfThreads / 2, successes.get())
        assertEquals(nOfThreads / 2, timeouts.get())
    }
}