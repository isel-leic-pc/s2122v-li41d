package pt.isel.pc

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

typealias TestFunction = (Int, () -> Boolean) -> Unit

class TestHelper(
    private val deadline: Instant
) {
    private val failures = ConcurrentLinkedQueue<AssertionError>()
    private val errors = ConcurrentLinkedQueue<Exception>()
    private val threads = ConcurrentLinkedQueue<Thread>()

    private fun isDone() = Instant.now().isAfter(deadline)

    private fun createAndStart(index: Int, block: TestFunction) {
        val th = Thread {
            try {
                block(index, this::isDone)
            } catch (e: InterruptedException) {
                // ignore
            } catch (e: AssertionError) {
                failures.add(e)
            } catch (e: Exception) {
                errors.add(e)
            }
        }
        th.start()
        threads.add(th)
    }

    fun createAndStartMultiple(nOfThreads: Int, block: TestFunction) =
        repeat(nOfThreads) { createAndStart(it, block) }

    @Throws(InterruptedException::class)
    fun join() {
        val deadlineForJoin = deadline.plusMillis(2000)
        for (th in threads) {
            val timeout = Duration.between(Instant.now(), deadlineForJoin)
            th.join(timeout.toMillis())
            if (th.isAlive) {
                throw AssertionError("Thread '$th' did not end in the expected time")
            }
        }
        if (!failures.isEmpty()) {
            throw failures.peek()
        }
        if (!errors.isEmpty()) {
            throw errors.peek()
        }
    }
}