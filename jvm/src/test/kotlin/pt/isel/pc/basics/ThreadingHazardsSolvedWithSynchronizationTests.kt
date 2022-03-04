package pt.isel.pc.basics

import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.test.assertEquals

// Number of threads used on each test
private const val N_OF_THREADS = 10

// Number of repetitions performed by each thread
private const val N_OF_REPS = 1000000

/**
 * These tests illustrate how to solve the concurrency hazards illustrated on
 * [ThreadingHazardsTests] by using Locks or thread-safe classes
 */
class ThreadingHazardsSolvedWithSynchronizationTests {

    // mutable counter and a lock to "protect" it
    private val lock = ReentrantLock()
    private var simpleCounter = 0

    private fun incrementSimpleCounter() {
        repeat(N_OF_REPS) {
            lock.withLock {
                simpleCounter += 1
            }
        }
    }

    @Test
    fun `NOT loosing increments`() {

        val threads = List(N_OF_THREADS) {
            Thread(this::incrementSimpleCounter).apply(Thread::start)
        }

        threads.forEach(Thread::join)
        assertEquals(N_OF_THREADS * N_OF_REPS, simpleCounter)
    }

    // Just a simple stack using a linked list but with all methods with mutual exclusion
    class SimpleLinkedStackWithLock<T> {

        private class Node<T>(val item: T, val next: Node<T>?)

        private val lock = ReentrantLock()

        // mutable
        private var head: Node<T>? = null

        fun push(value: T) = lock.withLock {
            head = Node(value, head)
        }

        fun pop(): T? = lock.withLock {
            val observedHead = head ?: return null
            head = observedHead.next
            return observedHead.item
        }

        val isEmpty: Boolean
            get() = lock.withLock { head == null }
    }

    private val threadSafeList = SimpleLinkedStackWithLock<Int>()

    @Test
    fun `NOT loosing items on a linked list`() {

        val threads = List(N_OF_THREADS) {
            Thread {
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

    // Here the map is "thread-safe" and the counter is also "thread-safe"
    private val mapLock = ReentrantLock()
    private val map: MutableMap<Int, AtomicInteger> = Collections.synchronizedMap(mutableMapOf<Int, AtomicInteger>())

    @Test
    fun `NOT loosing increments with a synchronized map and atomics`() {
        val threads = List(N_OF_THREADS) {
            Thread {
                (0 until N_OF_REPS).forEach { index ->
                    mapLock.withLock {
                        val data = map[index]
                        if (data == null) {
                            map[index] = AtomicInteger(1)
                        } else {
                            data.incrementAndGet()
                        }
                    }
                }
            }.apply(Thread::start)
        }

        threads.forEach(Thread::join)

        val totalCount = map.values
            .map { it.get() }
            .reduce { acc, elem ->
                acc + elem
            }

        assertEquals(N_OF_THREADS * N_OF_REPS, totalCount)
    }

    private val concurrentMap: MutableMap<Int, AtomicInteger> = ConcurrentHashMap()

    @Test
    fun `NOT loosing increments using a ConcurrentHashMap`() {
        val threads = List(N_OF_THREADS) {
            Thread {
                repeat(N_OF_REPS) { index ->
                    concurrentMap.computeIfAbsent(index) { AtomicInteger(0) }.incrementAndGet()
                }
            }.apply(Thread::start)
        }

        threads.forEach(Thread::join)

        val totalCount = concurrentMap.values
            .map { it.get() }
            .reduce { acc, elem ->
                acc + elem
            }

        assertEquals(N_OF_THREADS * N_OF_REPS, totalCount)
    }
}