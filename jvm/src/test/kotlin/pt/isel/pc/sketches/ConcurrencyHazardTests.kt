package pt.isel.pc.sketches

import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

private const val N_OF_THREADS = 10
private const val N_OF_REPS = 100_000

class ConcurrencyHazardTests {

    // var counter = 0
    val counter = AtomicInteger(0)

    @Test
    fun `shared mutation`() {

        val threads = List(N_OF_THREADS) {
            val th = Thread {
                repeat(N_OF_REPS) {
                    counter.incrementAndGet()
                }
            }
            th.start()
            th
        }

        threads.forEach {
            it.join()
        }

        assertEquals(N_OF_THREADS * N_OF_REPS, counter.get())
    }
}