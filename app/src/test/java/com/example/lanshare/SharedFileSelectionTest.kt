package com.example.lanshare

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedFileSelectionTest {
    @Test
    fun appPickerAppendsAndDeduplicatesByKey() {
        val merged = SharedFileSelection.merge(
            existing = listOf("old-a", "old-b"),
            incoming = listOf("old-b", "new-c", "new-c"),
            replaceExisting = false,
            key = { it }
        )

        assertEquals(listOf("old-a", "old-b", "new-c"), merged)
    }

    @Test
    fun externalShareReplacesPreviousSelectionAndDeduplicates() {
        val merged = SharedFileSelection.merge(
            existing = listOf("stale-a", "stale-b"),
            incoming = listOf("new-a", "new-a", "new-b"),
            replaceExisting = true,
            key = { it }
        )

        assertEquals(listOf("new-a", "new-b"), merged)
    }

    @Test
    fun emptyExternalShareCanRepresentAnEmptyNewTask() {
        val merged = SharedFileSelection.merge(
            existing = listOf("stale-a"),
            incoming = emptyList(),
            replaceExisting = true,
            key = { it }
        )

        assertEquals(emptyList<String>(), merged)
    }
}
