package pt.isel.pc.sketches

import org.junit.jupiter.api.Test

class HowManyThreadsTest {

    @Test
    fun createThreadsTest() {
        repeat(1_000) {
            Thread { Thread.sleep(1000) }.start()
        }
    }
}