package pt.isel.pc.apps.http

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets
import javax.servlet.Servlet
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

private val log = LoggerFactory.getLogger("http-example")
private const val PORT = 8080

fun setup(): Server {
    val server = Server(PORT)
    val handler = ServletHandler()

    var outsideRequestUri: String? = null

    // Note how we are creating a *single* servlet, which will be shared between all threads
    val servlet: Servlet = object : HttpServlet() {

        override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
            log.info("doGet request: URI='{}', method='{}", request.method, request.requestURI)

            // PRIVATE to the thread
            val localRequestURI = request.requestURI

            // SHARED between all threads
            outsideRequestUri = request.requestURI // T1: 123 T2: abc

            Thread.sleep(1000)

            val bodyString = String.format(
                "Request processed on thread '%s', method='%s', URI='%s', URI = '%s'\n",
                Thread.currentThread().name,
                request.method,
                localRequestURI,
                outsideRequestUri
            )
            val bodyBytes = bodyString.toByteArray(StandardCharsets.UTF_8)
            response.addHeader("Content-Type", "text/plain, charset=utf-8")
            response.addHeader("Content-Length", bodyBytes.size.toString())
            response.outputStream.write(bodyBytes)
        }
    }
    handler.addServletWithMapping(ServletHolder(servlet), "/*")
    log.info("registered {} on all paths", servlet)
    server.handler = handler

    return server
}

fun main() {

    val server = setup()
    server.start()
    log.info("server started listening on port {}", PORT)
    log.info("Waiting for server to end")
    server.join()
    log.info("main is ending")
}