package pt.isel.pc.sketches

import java.time.Duration
import java.time.Instant

class UnarySemaphoreUsingIntrinsicMonitors2(initialUnits: Int) {

    private var units = initialUnits
    private val monitor = Object()

    fun release() {
        synchronized(monitor) {
            units += 1
            monitor.notify()
        }
    }

    @Throws(InterruptedException::class)
    fun acquire(timeout: Duration): Boolean {
        synchronized(monitor) {
            if (units > 0) {
                units -= 1
                return true
            }

            if (timeout.isZero) {
                return false
            }

            val deadline = Instant.now().plus(timeout)
            var remainingMillis = Duration.between(Instant.now(), deadline).toMillis()
            while (true) {
                try {
                    monitor.wait(remainingMillis)
                } catch (e: InterruptedException) {
                    if (units > 0) {
                        monitor.notify()
                    }
                    throw e
                }
                if (units > 0) {
                    units -= 1
                    return true
                }
                remainingMillis = Duration.between(Instant.now(), deadline).toMillis()
                if (remainingMillis <= 0) {
                    return false
                }
            }
        }
    }
}