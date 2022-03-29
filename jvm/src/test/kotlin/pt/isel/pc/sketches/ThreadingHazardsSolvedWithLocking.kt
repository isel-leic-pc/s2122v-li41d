package pt.isel.pc.sketches

import org.junit.jupiter.api.Test
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.test.assertEquals

// Number of threads used on each test
private const val N_OF_THREADS = 10

// Number of repetitions performed by each thread
private const val N_OF_REPS = 1000000

class ThreadingHazardsSolvedWithLocking {

    class LockedSimpleLinkedStack<T> {

        private class Node<T>(val item: T, val next: Node<T>?)

        private val lock = ReentrantLock()

        // mutable
        private var head: Node<T>? = null

        fun push(value: T) = lock.withLock {
            head = Node(item = value, next = head)
        }

        fun pop(): T? = lock.withLock {
            val observedHead = head ?: return null
            head = observedHead.next
            return observedHead.item
        }

        val isEmpty: Boolean
            get() = lock.withLock { head == null }
    }

    // N.B. `nonThreadSafeList` is immutable, however the referenced data structure is mutable
    private val threadSafeList = LockedSimpleLinkedStack<Int>()

    @Test
    fun `not loosing items on a linked list`() {

        val threads = List(N_OF_THREADS) {
            Thread {
                // note that this code runs in a different thread
                repeat(N_OF_REPS) {
                    threadSafeList.push(1)
                }
            }.apply(Thread::start)
        }

        threads.forEach(Thread::join)

        var acc = 0
        while (!threadSafeList.isEmpty) {
            val elem = threadSafeList.pop()
            checkNotNull(elem)
            acc += elem
        }

        assertEquals(N_OF_THREADS * N_OF_REPS, acc)
    }
}