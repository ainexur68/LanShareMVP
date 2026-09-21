package com.example.lanshare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.nio.file.Files

class TransferProtocolValidationTest {
    @Test
    fun acceptsNormalClientPrepareMetadata() {
        val files = TransferProtocolValidation.validateFiles(
            listOf(
                file(
                    id = "f0-550e8400-e29b-41d4-a716-446655440000",
                    name = "photo.jpg",
                    size = 1_024L,
                    mime = "image/jpeg"
                )
            )
        )

        assertEquals(1, files.size)
        assertEquals(1_024L, files.single().size)
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
            TransferProtocolValidation.validateFiles(listOf(duplicate, duplicate))
        }

        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.validateFiles(listOf(file("file-1", "one.txt", -1L, "text/plain")))
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.validateFiles(
                listOf(file("file-1", "one.txt", TransferProtocolValidation.MAX_FILE_SIZE_BYTES + 1L, "text/plain"))
            )
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.validateFiles(
                listOf(file("file-1", "x".repeat(TransferProtocolValidation.MAX_FILE_NAME_LENGTH + 1), 1L, "text/plain"))
            )
        }
        assertThrows(TransferProtocolValidation.ProtocolValidationException::class.java) {
            TransferProtocolValidation.validateFiles(
                listOf(file("file-1", "one.txt", 1L, "x".repeat(TransferProtocolValidation.MAX_MIME_LENGTH + 1)))
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
    fun acceptsValidPrepareDigestMetadata() {
        val files = TransferProtocolValidation.validateFiles(
            listOf(file("file-1", "one.txt", 1L, "text/plain", sha256 = "a".repeat(64)))
        )
        assertEquals("a".repeat(64), files.single().sha256)
        assertTrue(files.single().sha256!!.matches(Regex("[0-9a-f]{64}")))
    }

    private fun file(id: String, name: String, size: Long, mime: String, sha256: String? = null) =
        IncomingFileMeta(id = id, name = name, size = size, mime = mime, sha256 = sha256)
}
