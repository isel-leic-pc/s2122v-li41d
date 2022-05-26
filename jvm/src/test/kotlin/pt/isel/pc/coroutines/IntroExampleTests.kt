package pt.isel.pc.coroutines

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class IntroExampleTests {

    fun f() {
        logger.info("hello")
        Thread.sleep(10000)
        logger.info("world")
    }

    suspend fun g() {
        logger.info("hello")
        // delay(1_000)
        mydelay(1_000)
        // Thread.sleep(1_000)
        logger.info("world")
    }

    @Test
    fun `intro example with threads`() {
        repeat(2) {
            Thread(::f).start()
        }
        Thread.sleep(2000)
    }

    @Test
    fun `intro example with coroutines`() {
        runBlocking(dispatcher) {
            repeat(2) {
                launch(block = { g() })
            }
        }
    }
}

private val logger = LoggerFactory.getLogger(IntroExampleTests::class.java)

val scheduledExecutor: ScheduledExecutorService =
    Executors.newSingleThreadScheduledExecutor()

val dispatcher = Executors.newFixedThreadPool(2).asCoroutineDispatcher()

suspend fun mydelay(ms: Long) {
    logger.info("mydelay: start")
    // scheme: call with current continuation
    suspendCoroutine<Unit> { continuation ->
        logger.info("suspendCoroutine block started")
        scheduledExecutor.schedule({
            logger.info("scheduled runnable called")
            continuation.resume(Unit)
        }, ms, TimeUnit.MILLISECONDS)
        logger.info("suspendCoroutine block ending")
    }
    logger.info("mydelay: end")
}