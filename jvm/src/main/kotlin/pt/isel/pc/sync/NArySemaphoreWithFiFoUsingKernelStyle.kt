package pt.isel.pc.sync

import pt.isel.pc.utils.MutableTimeout
import pt.isel.pc.utils.NodeLinkedList
import pt.isel.pc.utils.await
import pt.isel.pc.utils.noWait
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.Duration

class NArySemaphoreWithFiFoUsingKernelStyle(initialUnits: Int) : NArySemaphore {

    private val lock = ReentrantLock()
    private var units = initialUnits
    private val requests = NodeLinkedList<Request>()

    private class Request(
        val condition: Condition,
        val requestedUnits: Int
    ) {
        var isDone: Boolean = false
    }

    @Throws(InterruptedException::class)
    override fun tryAcquire(requestedUnits: Int, timeout: Duration): Boolean {
        require(requestedUnits > 0) { "Requested units must be greater than zero but was $requestedUnits" }
        lock.withLock {
            // fast-path
            if (requests.empty && units >= requestedUnits) {
                units -= requestedUnits
                return true
            }
            if (timeout.noWait()) {
                return false
            }
            // wait-path
            val myCondition = lock.newCondition()
            val myRequestNode = requests.enqueue(
                Request(
                    condition = myCondition,
                    requestedUnits = requestedUnits
                )
            )
            val mutableTimeout = MutableTimeout(timeout)
            while (true) {
                try {
                    myCondition.await(mutableTimeout)
                } catch (e: InterruptedException) {
                    if (myRequestNode.value.isDone) {
                        Thread.currentThread().interrupt()
                        return true
                    }
                    requests.remove(myRequestNode)
                    completeAll()
                    throw e
                }
                if (myRequestNode.value.isDone) {
                    return true
                }
                if (mutableTimeout.elapsed) {
                    requests.remove(myRequestNode)
                    completeAll()
                    return false
                }
            }
        }
    }

    override fun release(releasedUnits: Int) = lock.withLock {
        units += releasedUnits
        completeAll()
    }

    private fun completeAll() {
        while (requests.headValue?.let { units >= it.requestedUnits } == true) {
            // remove request
            val request = requests.pull().value
            // acquires request units
            units -= request.requestedUnits
            // marks the request as being done/completed
            request.isDone = true
            // signals the request condition
            request.condition.signal()
        }
    }
}