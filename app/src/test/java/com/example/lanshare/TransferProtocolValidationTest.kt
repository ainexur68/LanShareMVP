package com.example.lanshare

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assert.assertThrows
import java.io.File
import java.nio.file.Files

class TransferProtocolValidationTest {
    @Test
    fun acceptsNormalClientPrepareMetadata() {
        val request = TransferProtocolValidation.parsePrepare(
            prepareBody(
                file(
                    id = "f0-550e8400-e29b-41d4-a716-446655440000",
                    name = "photo.jpg",
                    size = 1_024L,
                    mime = "image/jpeg"
                )
            ),
            defaultSenderAlias = "Pixel"
        )

        assertEquals("session-123", request.sessionId)
        assertEquals("Pixel", request.senderAlias)
        assertEquals(1, request.files.size)
        assertEquals(1_024L, request.files.single().size)
    }

    @Test
    fun rejectsPathTraversalAndUnsupportedIdentifiers() {
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.requireSessionId("../outside")
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.requireFileId("nested/file")
        }

        val root = Files.createTempDirectory("lanshare-validation").toFile()
        val sessionDirectory = TransferProtocolValidation.sessionDirectory(File(root, "incoming"), "session-1")
        val part = TransferProtocolValidation.partFile(sessionDirectory, "file-1")
        assertEquals(File(root, "incoming/session-1/file-1.part").canonicalFile, part)
    }

    @Test
    fun rejectsDuplicateIdsAndMetadataBounds() {
        val duplicate = file("same-id", "one.txt", 1L, "text/plain")
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.parsePrepare(prepareBody(duplicate, duplicate), "Phone")
        }

        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.parsePrepare(
                prepareBody(file("file-1", "one.txt", -1L, "text/plain")),
                "Phone"
            )
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.parsePrepare(
                prepareBody(file("file-1", "one.txt", TransferProtocolValidation.MAX_FILE_SIZE_BYTES + 1L, "text/plain")),
                "Phone"
            )
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.parsePrepare(
                prepareBody(file("file-1", "x".repeat(TransferProtocolValidation.MAX_FILE_NAME_LENGTH + 1), 1L, "text/plain")),
                "Phone"
            )
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.parsePrepare(
                prepareBody(file("file-1", "one.txt", 1L, "x".repeat(TransferProtocolValidation.MAX_MIME_LENGTH + 1))),
                "Phone"
            )
        }
    }

    @Test
    fun validatesOffsetContentLengthAndRemainingBytes() {
        assertTrue(TransferProtocolValidation.validateUpload(0, 10, 0, 10).acceptedOffset)
        assertTrue(TransferProtocolValidation.validateUpload(10, 0, 10, 10).acceptedOffset)
        assertFalse(TransferProtocolValidation.validateUpload(0, 10, 5, 10).acceptedOffset)

        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.validateUpload(-1, 1, 0, 1)
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.validateUpload(11, 0, 11, 10)
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.validateUpload(8, 3, 8, 10)
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.validateUpload(0, -1, 0, 1)
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.validateUpload(0, 0, 0, 1)
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.requireContentLength(false, 0)
        }
    }

    @Test
    fun acceptsOptionalPrepareDigestAndValidCompleteDigest() {
        val body = prepareBody(file("file-1", "one.txt", 1L, "text/plain"))
        body.put("files", JSONArray().put(file("file-1", "one.txt", 1L, "text/plain").put("sha256", "a".repeat(64))))
        val parsed = TransferProtocolValidation.parsePrepare(body, "Phone")
        assertEquals("a".repeat(64), parsed.files.single().sha256)

        val complete = TransferProtocolValidation.parseComplete(
            JSONObject()
                .put("sessionId", "session-123")
                .put("fileId", "file-1")
                .put("sha256", "b".repeat(64))
        )
        assertEquals("b".repeat(64), complete.sha256)
    }

    private fun prepareBody(vararg files: JSONObject): JSONObject {
        val array = JSONArray()
        files.forEach { array.put(it) }
        return JSONObject()
            .put("sessionId", "session-123")
            .put("senderAlias", "Phone")
            .put("files", array)
    }

    private fun file(id: String, name: String, size: Long, mime: String): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("size", size)
        .put("mime", mime)
}
