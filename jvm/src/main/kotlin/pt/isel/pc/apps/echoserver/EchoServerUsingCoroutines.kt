package pt.isel.pc.apps.echoserver

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private fun main() {

    val server = EchoServerUsingCoroutines()
    runBlocking {
        server.acceptLoop()
    }
}

class EchoServerUsingCoroutines {

    suspend fun acceptLoop() {

        val serverSocket = AsynchronousServerSocketChannel.open()
        serverSocket.bind(InetSocketAddress("0.0.0.0", 8080))
        coroutineScope {
            var clientId = 0
            while (true) {
                logger.info("Accepting client")
                val clientSocket = serverSocket.acceptAsync()
                logger.info("Client accepted, launching client loop")
                // create coroutine with echoLoop
                launch {
                    echoLoop(clientSocket, clientId++)
                }
            }
        }
    }

    private suspend fun echoLoop(socket: AsynchronousSocketChannel, id: Int) {
        logger.info("Client loop started")
        val buffer = ByteBuffer.allocate(1024)
        while (true) {
            logger.info("{}: reading bytes", id)
            socket.readAsync(buffer)
            buffer.flip()
            logger.info("{}: Writing bytes", id)
            socket.writeAsync(buffer)
            buffer.clear()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EchoServerUsingCoroutines::class.java)
    }
}

suspend fun AsynchronousSocketChannel.readAsync(buffer: ByteBuffer): Int {

    val res = suspendCoroutine<Int> { continuation ->
        this.read(
            buffer,
            Unit,
            object : CompletionHandler<Int, Unit> {

                override fun completed(result: Int, attachment: Unit) {
                    continuation.resume(result)
                }

                override fun failed(exc: Throwable, attachment: Unit) {
                    continuation.resumeWithException(exc)
                }
            }
        )
    }
    return res
}

suspend fun AsynchronousSocketChannel.writeAsync(buffer: ByteBuffer): Int {

    var counter = 0
    while (buffer.hasRemaining()) {
        counter += suspendCoroutine<Int> { continuation ->
            this.write(
                buffer,
                Unit,
                object : CompletionHandler<Int, Unit> {

                    override fun completed(result: Int, attachment: Unit) {
                        continuation.resume(result)
                    }

                    override fun failed(exc: Throwable, attachment: Unit) {
                        continuation.resumeWithException(exc)
                    }
                }
            )
        }
    }
    return counter
}

suspend fun AsynchronousServerSocketChannel.acceptAsync(): AsynchronousSocketChannel {
    val socket: AsynchronousSocketChannel = suspendCoroutine<AsynchronousSocketChannel> { continuation ->
        this.accept(
            Unit,
            object : CompletionHandler<AsynchronousSocketChannel, Unit> {
                override fun completed(result: AsynchronousSocketChannel, attachment: Unit?) {
                    continuation.resume(result)
                }

                override fun failed(exc: Throwable, attachment: Unit?) {
                    continuation.resumeWithException(exc)
                }
            }
        )
    }
    return socket
}