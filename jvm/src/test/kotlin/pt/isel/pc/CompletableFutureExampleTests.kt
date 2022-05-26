package pt.isel.pc

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool

class CompletableFutureExampleTests {

    @Test
    fun first() {
        val cf = CompletableFuture<Int>()
        val cf1 = cf.thenApply {
            logger.info("continuation 1 with {}", it)
            it + 1
        }
        val cf2 = cf1.thenApply {
            logger.info("continuation 2 with {}", it)
            it + 1
        }
        logger.info("Before complete")
        cf.complete(0)
        logger.info("After complete")
    }

    @Test
    fun second() {
        val cf = CompletableFuture<Int>()
        val cf1 = cf.thenApplyAsync {
            logger.info("continuation 1 with {}", it)
            it + 1
        }
        val cf2 = cf1.thenApplyAsync {
            logger.info("continuation 2 with {}", it)
            it + 1
        }
        logger.info("Before complete")
        cf.complete(0)
        logger.info("After complete")
        ForkJoinPool.commonPool().execute {
            logger.info("Hi, I'm running on the common pool")
        }
        Thread.sleep(1000)
    }

    @Test
    fun third() {
        val cf = CompletableFuture<Int>()
        cf.complete(0)
        logger.info("Before thenApply")
        val cf1 = cf.thenApply {
            logger.info("continuation 1 with {}", it)
            it + 1
        }
        logger.info("After thenApply")
    }

    @Test
    fun fourth() {
        val cf = CompletableFuture<Int>()
        cf.complete(0)
        logger.info("Before thenApply")
        val cf1 = cf.thenApplyAsync {
            logger.info("continuation 1 with {}", it)
            it + 1
        }
        logger.info("After thenApply")
        Thread.sleep(1000)
    }

    @Test
    fun fifth() {
        val latch = CountDownLatch(1)

        val client: HttpClient = HttpClient.newHttpClient()

        val request = HttpRequest.newBuilder()
            .GET()
            .uri(URI("https://httpbin.org/delay/4"))
            .build()

        val f: CompletableFuture<HttpResponse<String>> = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        f.thenApply {
            logger.info(it.body())
            latch.countDown()
        }

        latch.await()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CompletableFutureExampleTests::class.java)
    }
}