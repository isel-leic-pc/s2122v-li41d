package pt.isel.pc.sketches

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import pt.isel.pc.TestHelper
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

class TreiberStackTests {

    @Test
    fun simple() {
        val testHelper = TestHelper(Instant.now().plusSeconds(2))
        val nOfThreads = 10
        val stack = TreiberStack<Int>()
        val insertCounter = AtomicInteger(0)
        val removeCounter = AtomicInteger(0)
        testHelper.createAndStartMultiple(nOfThreads) { ix, isDone ->
            while (!isDone()) {
                insertCounter.addAndGet(ix)
                stack.enqueue(ix)
                stack.dequeue()?.let {
                    removeCounter.addAndGet(it)
                }
            }
        }
        testHelper.join()
        assertEquals(insertCounter.get(), removeCounter.get())
    }
}