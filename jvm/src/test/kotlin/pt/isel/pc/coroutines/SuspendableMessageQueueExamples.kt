package pt.isel.pc.coroutines

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

class SuspendableMessageQueueExamples {

    companion object {
        private val dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()
        private val logger = LoggerFactory.getLogger(SuspendableMessageQueueExamples::class.java)
        private val poisonPill = String()
    }

    private suspend fun readLoop(id: Int, queue: SuspendableMessageQueue<String>) {
        while (true) {
            val message = queue.get()
            if (message === poisonPill) {
                return
            }
            logger.info("Coroutine {} received '{}'", id, message)
        }
    }

    @Test
    fun first() = runBlocking(dispatcher) {

        val queues: List<SuspendableMessageQueue<String>> = List(10) {
            val queue = SuspendableMessageQueue<String>()
            val ix = it
            launch {
                readLoop(ix, queue)
            }
            queue
        }

        repeat(5) {
            queues.forEachIndexed { ix, queue ->
                if (ix % 2 == 0) {
                    queue.put("Message $it")
                }
            }
            delay(1000)
        }

        queues.forEach {
            it.put(poisonPill)
        }
    }
}