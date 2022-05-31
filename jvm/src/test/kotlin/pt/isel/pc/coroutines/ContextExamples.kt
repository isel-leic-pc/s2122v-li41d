package pt.isel.pc.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class ContextExamples {

    companion object {
        private val logger = LoggerFactory.getLogger(ContextExamples::class.java)
    }

    @Test
    fun first() = runBlocking {
        repeat(3) {
            launch {
                logger.info("step 0")
                withContext(Dispatchers.IO) {
                    logger.info("step 1")
                    Thread.sleep(1000)
                    logger.info("step 2")
                }
                logger.info("step 3")
            }
        }
    }
}