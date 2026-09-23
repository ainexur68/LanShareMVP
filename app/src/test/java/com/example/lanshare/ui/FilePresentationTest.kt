package com.example.lanshare.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FilePresentationTest {
    @Test
    fun classifiesCommonFileTypesFromMimeOrName() {
        assertEquals(FileKind.IMAGE, fileKindFor("photo.JPG", "image/jpeg"))
        assertEquals(FileKind.VIDEO, fileKindFor("video.mp4", null))
        assertEquals(FileKind.PDF, fileKindFor("document.pdf", "application/octet-stream"))
        assertEquals(FileKind.GENERIC, fileKindFor("archive.zip", null))
    }

    @Test
    fun summarizesTheVisibleFileQueue() {
        assertEquals("3 个文件 · 1.8 GB", fileQueueSummary(3, 1_932_735_283L))
    }

    @Test
    fun keepsTheFileDialogFooterInsideItsSurface() {
        assertEquals(288, fileDialogHeightDp(3))
        assertEquals(522, fileDialogHeightDp(10))
    }
}
