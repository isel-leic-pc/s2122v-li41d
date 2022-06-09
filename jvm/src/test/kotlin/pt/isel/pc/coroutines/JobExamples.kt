package pt.isel.pc.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import pt.isel.pc.apps.echoserver.readAsync2
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class JobExamples {

    @Test
    fun first() = runBlocking {

        val j0 = launch {
            val j1 = launch {
                delay(1500)
            }
            delay(1000)
            logger.info("j0 coroutine ending")
        }
        while (!j0.isCompleted) {
            logger.info("j0 isActive={}", j0.isActive)
            delay(100)
        }
    }

    @Test
    fun `cancelling example 1`() {
        runBlocking(Dispatchers.Default) {
            val j = launch {
                try {
                    logger.info("coroutine started, before delay")
                    repeat(5) {
                        Thread.sleep(100)
                        logger.info("isActive={}", isActive)
                    }
                    delay(1000)
                    logger.info("after delay, about to end")
                } catch (ex: CancellationException) {
                    logger.info("caught '{}'", ex.message)
                }
            }
            delay(100)
            j.cancel()
        }
        logger.info("after runBlocking")
    }

    @Test
    fun `suspendCoroutine is not cancellation aware`() {
        val executor = Executors.newSingleThreadScheduledExecutor()
        suspend fun mydelay(ms: Long) {
            suspendCoroutine<Unit> { continuation ->
                executor.schedule({ continuation.resume(Unit) }, ms, TimeUnit.MILLISECONDS)
            }
        }
        runBlocking {
            val j = launch {
                try {
                    logger.info("coroutine started, before mydelay")
                    mydelay(1000)
                    logger.info("after mydelay, about to end")
                } catch (ex: CancellationException) {
                    logger.info("caught '{}'", ex.message)
                }
            }
            delay(100)
            j.cancel()
        }
        logger.info("after runBlocking")
    }

    @Test
    fun `using suspendCancellableCoroutine to implement mydelay`() {
        val executor = Executors.newSingleThreadScheduledExecutor()
        suspend fun mydelay(ms: Long) {
            suspendCancellableCoroutine<Unit> { continuation ->
                val future = executor.schedule({
                    logger.info("scheduled callback called")
                    continuation.resume(Unit)
                }, ms, TimeUnit.MILLISECONDS)
                logger.info("Before invokeOnCancellation")
                continuation.invokeOnCancellation {
                    logger.info("On invokeOnCancellation callback")
                    future.cancel(true)
                }
                logger.info("After invokeOnCancellation")
            }
        }
        runBlocking {
            val j = launch {
                try {
                    logger.info("coroutine started, before mydelay")
                    mydelay(1000)
                    logger.info("after mydelay, about to end")
                } catch (ex: CancellationException) {
                    logger.info("caught '{}'", ex.message)
                }
            }
            delay(100)
            j.cancel()
        }
        logger.info("after runBlocking")
        // Wait a bit more to see if something interesting happens
        Thread.sleep(2000)
    }

    @Test
    fun `cancel read`() = runBlocking {

        val socket = AsynchronousSocketChannel.open()
        val f0 = socket.connect(InetSocketAddress("httpbin.org", 80))
        f0.get()
        val request = "GET /delay/5 HTTP/1.1\nHost:httpbin.org\n\n"
        val bytes = request.toByteArray(Charsets.UTF_8)
        val writeBuffer = ByteBuffer.wrap(bytes)
        val f1 = socket.write(writeBuffer)
        f1.get()
        val job = launch {
            try {
                val readBuffer = ByteBuffer.allocate(4 * 1024)
                val len = socket.readAsync2(readBuffer)
                val response = String(readBuffer.array(), 0, len)
                logger.info(response)
            } catch (ex: Exception) {
                logger.info("Exception: {}", ex.javaClass.name)
            }
        }
        delay(1000)
        job.cancel()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JobExamples::class.java)
    }
}