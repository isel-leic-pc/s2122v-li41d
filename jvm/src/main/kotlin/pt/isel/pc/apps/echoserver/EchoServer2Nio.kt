package pt.isel.pc.apps.echoserver

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val logger = LoggerFactory.getLogger("main")

private fun main() {
    val countDownLatch = CountDownLatch(1)
    val echoServer = EchoServer2Nio(
        "127.0.0.1",
        8080,
        object : CompletionHandler<Unit, Unit> {
            override fun completed(result: Unit, attachment: Unit) {
                logger.info("Server completed successfully")
                countDownLatch.countDown()
            }

            override fun failed(exc: Throwable, attachment: Unit) {
                logger.error("Server completed with error", exc)
                countDownLatch.countDown()
            }
        }
    )
    echoServer.start()
    logger.info("Waiting for key")
    readln()
    logger.info("Ending everything")
    echoServer.stop()
    countDownLatch.await()
    EchoServer2Nio.close()
}

class EchoServer2Nio(
    private val address: String,
    private val port: Int,
    continuation: CompletionHandler<Unit, Unit>,
) {

    private val serverSocket = AsynchronousServerSocketChannel.open(channelGroup)
    private var currentClientId = 0
    private val completionSynchronizer = CompletionSynchronizer(continuation)

    private val acceptContinuation = completionHandler(
        ::acceptCompleted,
        ::error
    )

    fun stop() {
        completionSynchronizer.stop()
        serverSocket.close()
    }

    fun start() {
        serverSocket.bind(InetSocketAddress(address, port))
        completionSynchronizer.begin()
        serverSocket.accept(Unit, acceptContinuation)
        logger.info("accept started")
    }

    private fun acceptCompleted(socket: AsynchronousSocketChannel) {
        logger.info("accept completed")
        val clientId = currentClientId++
        val continuation = object : CompletionHandler<Int, Unit> {
            override fun completed(result: Int, attachment: Unit) {
                logger.info("Client '{}' ended normally", clientId)
                completionSynchronizer.end()
            }

            override fun failed(exc: Throwable, attachment: Unit) {
                logger.info("Client '{}' ended with exception", clientId, exc)
                completionSynchronizer.end(exc)
            }
        }
        completionSynchronizer.begin()
        ClientHandler(clientId, socket, continuation).start()
        logger.info("client handler started")
        serverSocket.accept(Unit, acceptContinuation)
        logger.info("accept started")
    }

    private fun error(exc: Throwable) {
        if (completionSynchronizer.isStopped() && exc is AsynchronousCloseException) {
            logger.info("Expected 'AsynchronousCloseException', ending")
            completionSynchronizer.end()
        } else {
            logger.error("Error occurred", exc)
            completionSynchronizer.end(exc)
        }
    }

    private class ClientHandler(
        private val clientId: Int,
        private val socket: AsynchronousSocketChannel,
        private val continuation: CompletionHandler<Int, Unit>
    ) {

        private val buffer = ByteBuffer.allocate(1024)

        private val readContinuation = completionHandler(
            ::readCompleted,
            ::error,
        )

        private val writeContinuation = completionHandler(
            ::writeCompleted,
            ::error,
        )

        fun tryIt(block: () -> Unit): Unit = try {
            block()
        } catch (exc: Throwable) {
            finally(exc)
        }

        fun finally() {
            try {
                socket.close()
            } catch (exc: Throwable) {
                continuation.failed(exc, Unit)
            }
            continuation.completed(clientId, Unit)
        }

        fun finally(exc: Throwable) {
            try {
                socket.close()
            } catch (exc2: Throwable) {
                continuation.failed(exc2, Unit)
            }
            continuation.failed(exc, Unit)
        }

        fun start() = tryIt {
            // Just to force writes to complete without sending all bytes - debug purposes
            socket.setOption(StandardSocketOptions.SO_SNDBUF, 2)
            socket.read(buffer, Unit, readContinuation)
            logger.info("client socket read started")
        }

        private fun readCompleted(len: Int) = tryIt {
            logger.info("[{}] client socket read completed with '{}' bytes", clientId, len)
            if (len == -1) {
                finally()
            } else {
                buffer.flip()
                logger.info(
                    "[{}] client socket write starting for '{}' bytes", clientId,
                    buffer.limit() - buffer.position()
                )
                socket.write(buffer, Unit, writeContinuation)
            }
        }

        private fun writeCompleted(len: Int): Unit = tryIt {
            logger.info("[{}] client socket write completed with '{}' bytes", clientId, len)
            if (buffer.position() != buffer.limit()) {
                logger.info(
                    "[{}] client socket write starting for '{}' bytes", clientId,
                    buffer.limit() - buffer.position()
                )
                socket.write(buffer, Unit, writeContinuation)
            } else {
                buffer.clear()
                logger.info("[{}] client socket read started", clientId)
                socket.read(buffer, Unit, readContinuation)
            }
        }

        private fun error(exc: Throwable) {
            finally(exc)
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(EchoServer2Nio::class.java)

        private val threadPool = Executors.newFixedThreadPool(1)
        private val channelGroup = AsynchronousChannelGroup.withThreadPool(threadPool)

        fun close() {
            threadPool.shutdown()
            threadPool.awaitTermination(10, TimeUnit.SECONDS)
        }

        private val reentrancyCounter = ThreadLocal.withInitial { 0 }

        private fun runAsynchronously(block: () -> Unit) = if (reentrancyCounter.get() > 0) {
            logger.info("Reentrancy detected, scheduling on thread pool")
            threadPool.execute(block)
        } else {
            reentrancyCounter.set(1)
            try {
                block()
            } finally {
                reentrancyCounter.set(0)
            }
        }

        private fun <T> completionHandler(
            success: (T) -> Unit,
            error: (Throwable) -> Unit,
        ) = object : CompletionHandler<T, Unit> {
            override fun completed(result: T, attachment: Unit) {
                runAsynchronously { success(result) }
            }

            override fun failed(exc: Throwable, attachment: Unit) {
                runAsynchronously { error(exc) }
            }
        }
    }
}

class AggregateException(
    val exceptions: List<Throwable>
) : Exception()

class CompletionSynchronizer(
    private val continuation: CompletionHandler<Unit, Unit>
) {
    private val lock = ReentrantLock()
    private var stopped = false
    private var pending = 0
    private var errors = mutableListOf<Throwable>()

    fun stop() {
        if (internalStop()) {
            callContinuation()
        }
    }

    fun begin() = lock.withLock {
        if (stopped) {
            throw java.lang.IllegalStateException("Cannot begin if object is stopped")
        }
        pending += 1
    }

    fun end(error: Throwable? = null) {
        if (internalEnd(error)) {
            callContinuation()
        }
    }

    private fun internalEnd(error: Throwable?) = lock.withLock {
        if (error != null) {
            errors.add(error)
        }
        if (pending == 0) {
            throw java.lang.IllegalStateException("Cannot end if there is nothing pendind")
        }
        pending -= 1
        pending == 0 && stopped
    }

    private fun internalStop() = lock.withLock {
        val previousStopped = stopped
        stopped = true
        pending == 0 && !previousStopped
    }

    private fun callContinuation() {
        if (errors.isEmpty()) {
            continuation.completed(Unit, Unit)
        } else {
            continuation.failed(AggregateException(errors), Unit)
        }
    }

    fun isStopped() = lock.withLock {
        stopped
    }
}