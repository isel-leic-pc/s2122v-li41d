package pt.isel.pc.sync

import org.junit.jupiter.api.Test
import pt.isel.pc.TestHelper
import java.time.Instant
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertTrue
import kotlin.time.Duration

class SemaphoreTests {

    @Test
    fun `semaphore does not provides more units than the initial units`() {

        val testHelper = TestHelper(Instant.now().plusSeconds(15))
        val nOfThreads = 10
        val maxUnits = nOfThreads / 2
        val semaphore = NArySemaphoreWithFiFoUsingKernelStyle(maxUnits)
        val counter = AtomicInteger(maxUnits)
        testHelper.createAndStartMultiple(nOfThreads) { _, isDone ->
            while (!isDone()) {
                semaphore.tryAcquire(1, Duration.INFINITE)
                try {
                    val newValue = counter.addAndGet(-1)
                    assertTrue(newValue >= 0, "newValue ($newValue) must not be negative")
                    counter.addAndGet(1)
                } finally {
                    semaphore.release(1)
                }
            }
        }
        testHelper.join()
    }
    @Test
    fun `semaphore provides all available units`() {

        val testHelper = TestHelper(Instant.now().plusSeconds(15))
        val nOfThreads = 10
        val maxUnits = nOfThreads / 2
        val semaphore = NArySemaphoreWithFiFoUsingKernelStyle(maxUnits)
        val counter = AtomicInteger(maxUnits)
        val cyclicBarrier = CyclicBarrier(maxUnits)
        testHelper.createAndStartMultiple(nOfThreads) { _, isDone ->
            while (!isDone()) {
                semaphore.tryAcquire(1, Duration.INFINITE)
                try {
                    val newValue = counter.addAndGet(-1)
                    assertTrue(newValue >= 0, "newValue ($newValue) must not be negative")
                    cyclicBarrier.await()
                    counter.addAndGet(1)
                } finally {
                    semaphore.release(1)
                }
            }
        }
        testHelper.join()
    }
}