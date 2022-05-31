package pt.isel.pc

import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import kotlin.test.assertEquals

class ByteBufferExamples {

    @Test
    fun first() {
        val bb = ByteBuffer.allocate(10)
        // \x6f\x6c\xc3\xa1
        bb.put(0x6f)
        bb.put(0x6c)
        bb.put(0xc3.toByte())
        bb.put(0xa1.toByte())
        bb.flip()
        assertEquals(0, bb.position())
        assertEquals(4, bb.limit())
        val bytes: ByteArray = bb.array()
        assertEquals(10, bytes.size)
        val s = String(bytes, 0, 4)
        assertEquals("olá", s)
    }
}