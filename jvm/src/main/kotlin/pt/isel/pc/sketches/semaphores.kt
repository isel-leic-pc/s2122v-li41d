package pt.isel.pc.sketches

import pt.isel.pc.utils.NodeLinkedList
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class UnarySemaphoreWithFifo(
    initialUnits: Int
) {
    private val lock: Lock = ReentrantLock()
    private val condition: Condition = lock.newCondition()

    private var units: Int = initialUnits
    private val requests: NodeLinkedList<Request> = NodeLinkedList()

    private class Request

    fun acquire() = lock.withLock {

        if (units > 0 && requests.empty) {
            units -= 1
            return
        }
        val myRequestNode = requests.enqueue(Request())
        while (!(units > 0 && requests.isHeadNode(myRequestNode))) {
            condition.await() // TODO handle InterruptedException
        }
        units -= 1
        requests.remove(myRequestNode)
        signalAllIfNeeded()
    }

    fun release() = lock.withLock {
        units += 1
        signalAllIfNeeded()
    }

    private fun signalAllIfNeeded() {
        if (units > 0 && requests.notEmpty) {
            condition.signalAll()
        }
    }
}

class UnarySemaphoreWithFifoAndSpecificNotification(
    initialUnits: Int
) {
    private val lock: Lock = ReentrantLock()

    private var units: Int = initialUnits
    private val requests: NodeLinkedList<Request> = NodeLinkedList()

    private class Request(
        val condition: Condition
    )

    fun acquire() = lock.withLock {

        if (units > 0 && requests.empty) {
            units -= 1
            return
        }
        val myCondition = lock.newCondition()
        val myRequestNode = requests.enqueue(Request(myCondition))
        while (!(units > 0 && requests.isHeadNode(myRequestNode))) {
            myCondition.await() // TODO handle InterruptedException
        }
        units -= 1
        requests.remove(myRequestNode)
        signalAllIfNeeded()
    }

    fun release() = lock.withLock {
        units += 1
        signalAllIfNeeded()
    }

    private fun signalAllIfNeeded() {
        if (units > 0 && requests.notEmpty) {
            requests.headValue?.condition?.signal()
        }
    }
}

class NArySemaphoreUsingKernalStyle(initialUnits: Int) {
    private val lock: Lock = ReentrantLock()

    private var units: Int = initialUnits
    private val requests: NodeLinkedList<Request> = NodeLinkedList()

    private class Request(
        val condition: Condition,
        val requestedUnits: Int,
    ) {
        var isDone: Boolean = false
    }

    fun acquire(requestedUnits: Int) = lock.withLock {
        // fast-path
        if (requests.empty && units >= requestedUnits) {
            units -= requestedUnits
            return
        }
        // wait-path
        val myCondition = lock.newCondition()
        val myRequestNode = requests.enqueue(
            Request(
                condition = myCondition,
                requestedUnits = requestedUnits
            )
        )
        while (true) {
            try {
                myCondition.await()
            } catch (e: InterruptedException) {
                if (myRequestNode.value.isDone) {
                    // put the interrupt status to true again
                    Thread.currentThread().interrupt()
                    return
                }
                requests.remove(myRequestNode)
                completeAll()
                throw e
            }
            if (myRequestNode.value.isDone) {
                return
            }
        }
    }

    fun release(releasedUnits: Int) = lock.withLock {
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