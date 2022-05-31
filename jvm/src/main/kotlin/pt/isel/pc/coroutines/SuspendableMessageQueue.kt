package pt.isel.pc.coroutines

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SuspendableMessageQueue<M> {

    private val lock = ReentrantLock()
    private val messages = NodeLinkedList<M>()
    private val requests = NodeLinkedList<Request<M>>()

    class Request<M>(
        val continuation: Continuation<M>
    )

    fun put(m: M) {
        var continuation: Continuation<M>? = null
        lock.withLock {
            if (requests.notEmpty) {
                val request = requests.pull().value
                continuation = request.continuation
            } else {
                messages.enqueue(m)
            }
        }
        continuation?.resume(m)
    }

    suspend fun get(): M {

        // fast-path
        lock.lock()
        if (messages.notEmpty) {
            val message = messages.pull().value
            lock.unlock()
            return message
        }

        // suspend-path
        return suspendCoroutine<M> { continuation ->
            requests.enqueue(Request(continuation))
            lock.unlock()
        }
    }
}