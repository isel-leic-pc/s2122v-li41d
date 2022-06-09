package pt.isel.pc

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket

class SocketsExample {

    @Test
    fun `sockets example`() {
        val socket = Socket()
        socket.connect(InetSocketAddress("httpbin.org", 80))
        val writer = OutputStreamWriter(socket.getOutputStream())
        writer.write("GET /delay/1 HTTP/1.1\nHost: httpbin.org\n\n")
        writer.flush()
        val reader = InputStreamReader(socket.getInputStream())
        reader.forEachLine {
            logger.info("line: {}", it)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SocketsExample::class.java)
    }
}