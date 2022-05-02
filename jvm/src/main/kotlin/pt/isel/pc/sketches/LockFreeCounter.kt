package pt.isel.pc.sketches

import java.util.concurrent.atomic.AtomicInteger

class LockFreeCounter {

    private val counter = AtomicInteger(0)

    fun inc() {
        while (true) {
            val observed = counter.get()
            val nextValue = observed + 1
            if (counter.compareAndSet(observed, nextValue)) {
                return
            }
            // retry
        }
    }

    fun dec() {
        while (true) {
            val observed = counter.get()
            val nextValue = observed - 1
            if (counter.compareAndSet(observed, nextValue)) {
                return
            }
            // retry
        }
    }

    fun wrongIncrement() {
        while (true) {
            if (counter.compareAndSet(counter.get(), counter.get() + 1)) {
                return
            }
            // retry
        }
    }
}