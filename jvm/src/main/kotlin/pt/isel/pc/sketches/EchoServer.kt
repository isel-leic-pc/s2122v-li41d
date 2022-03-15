package pt.isel.pc.sketches

import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.ServerSocket

const val address = "127.0.0.1"
const val port = 8080

private fun main() = EchoServer().listenLoop()

class EchoServer {
    companion object {
        private val logger = LoggerFactory.getLogger(EchoServer::class.java)
    }

    fun listenLoop() {
        val serverSocket = ServerSocket()

        serverSocket.bind(InetSocketAddress(address, port))
        logger.info(
            "server socket is bound to address {} on port {}",
            address, port
        )
        while (true) {
            val clientSocket = serverSocket.accept()
            logger.info("Client socket accepted")
            Thread {
                clientSocket.use {
                    var lineNumber = 0
                    val reader = clientSocket.getInputStream().bufferedReader()
                    val writer = clientSocket.getOutputStream().bufferedWriter()
                    while (true) {
                        val line: String = reader.readLine()
                        logger.info("Client line read")
                        writer.writeLine("${lineNumber++}:${line.uppercase()}")
                    }
                }
            }.also {
                it.start()
            }
        }
    }
}