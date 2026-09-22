package com.example.lanshare

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Test

class ReceiveDirectoryTest {
    @Test
    fun viewSpecTargetsTheLegacyReceiveDirectory() {
        val spec = ReceiveDirectory.viewSpec()

        assertEquals(Intent.ACTION_VIEW, spec.action)
        assertEquals("vnd.android.document/directory", spec.mimeType)
        assertEquals(ReceiveDirectory.VIEW_URI, spec.uri)
    }

    @Test
    fun treeSpecStartsAtTheLegacyReceiveDirectory() {
        val spec = ReceiveDirectory.treeSpec()

        assertEquals(Intent.ACTION_OPEN_DOCUMENT_TREE, spec.action)
        assertEquals(ReceiveDirectory.TREE_URI, spec.uri)
    }
}
