package com.example.lanshare

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files

class TransferProtocolIoTest {
    @Test
    fun smallUploadBodyRemainsOnStreamingInput() {
        val payload = "small-file-payload".toByteArray(StandardCharsets.UTF_8)
        val requestBytes = buildString {
            append("PUT /v1/upload?sessionId=session-1&fileId=file-1&offset=0 HTTP/1.1\r\n")
            append("Content-Length: ${payload.size}\r\n")
            append("\r\n")
        }.toByteArray(StandardCharsets.ISO_8859_1) + payload
        val input = ByteArrayInputStream(requestBytes)

        val request = HttpRequest.read(input)!!
        val remaining = ByteArray(payload.size)
        assertNull(request.body)
        assertEquals(payload.size.toLong(), request.contentLength)
        assertEquals(payload.size, input.read(remaining))
        assertArrayEquals(payload, remaining)
    }

    @Test
    fun zeroBytePartIsCreatedForReceiveAndPublish() {
        val root = Files.createTempDirectory("lanshare-empty-part").toFile()
        val sessionDirectory = TransferProtocolValidation.sessionDirectory(File(root, "incoming"), "session-1")
        sessionDirectory.mkdirs()
        val part = TransferProtocolValidation.partFile(sessionDirectory, "empty-file")

        assertFalse(part.exists())
        TransferProtocolValidation.ensurePartFile(part, 0L)

        assertTrue(part.isFile)
        assertEquals(0L, part.length())
    }
}
