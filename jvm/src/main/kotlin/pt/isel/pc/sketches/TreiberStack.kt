package pt.isel.pc.sketches

import java.util.concurrent.atomic.AtomicReference

class TreiberStack<T> {

    private class Node<T>(val value: T, val next: Node<T>?)

    private val head: AtomicReference<Node<T>?> = AtomicReference(null)

    fun enqueue(value: T) {
        while (true) {
            val observedHead = head.get()
            val node = Node(value, observedHead)
            if (head.compareAndSet(observedHead, node)) {
                return
            }
        }
    }

    fun dequeue(): T? {
        while (true) {
            val observedHead = head.get()
            if (observedHead == null) {
                return null
            }
            if (head.compareAndSet(observedHead, observedHead.next)) {
                return observedHead.value
            }
        }
    }
}