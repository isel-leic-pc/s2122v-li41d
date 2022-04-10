package pt.isel.pc.sketches

import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals

class SimpleThreadPoolTests {

    @Test
    fun first() {

        val nOfItems = 5
        val pool = SimpleThreadPool(2)
        val countDownLatch = CountDownLatch(5)
        val threadNameSet = Collections.synchronizedSet(mutableSetOf<Thread>())
        val runnable = Runnable {
            threadNameSet.add(Thread.currentThread())
            Thread.sleep(200)
            countDownLatch.countDown()
        }
        repeat(nOfItems) {
            pool.execute(runnable)
        }

        countDownLatch.await()
        assertEquals(2, threadNameSet.size)
    }
}