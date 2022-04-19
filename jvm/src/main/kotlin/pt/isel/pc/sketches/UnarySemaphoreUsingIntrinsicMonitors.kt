package pt.isel.pc.sketches

import java.time.Duration
import java.time.Instant

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class UnarySemaphoreUsingIntrinsicMonitors(initialUnits: Int) : Object() {

    private var units = initialUnits

    @Synchronized
    fun release() {
        units += 1
        this.notify()
    }

    @Throws(InterruptedException::class)
    @Synchronized
    fun acquire(timeout: Duration): Boolean {
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
                this.wait(remainingMillis)
            } catch (e: InterruptedException) {
                if (units > 0) {
                    this.notify()
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