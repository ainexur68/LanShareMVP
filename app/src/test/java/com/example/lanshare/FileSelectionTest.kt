package com.example.lanshare

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSelectionTest {
    @Test
    fun appendsOnlyUrisThatAreNotAlreadySelected() {
        assertEquals(
            listOf("content://files/one", "content://files/two"),
            appendUniqueByKey(
                existing = listOf("content://files/one"),
                additions = listOf("content://files/one", "content://files/two")
            ) { it }
        )
    }
}
