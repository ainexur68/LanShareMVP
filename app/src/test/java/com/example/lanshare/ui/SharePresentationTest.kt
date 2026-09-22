package com.example.lanshare.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SharePresentationTest {
    @Test
    fun switchesBetweenEmptyAndSelectedLayoutsFromFileCount() {
        assertEquals(ShareLayoutState.EMPTY, shareLayoutState(0))
        assertEquals(ShareLayoutState.SELECTED, shareLayoutState(2))
    }

    @Test
    fun usesDifferentActionCopyForEmptyAndSelectedStates() {
        assertEquals("发送到 PHY110", shareActionLabel(fileCount = 0, peerAlias = "PHY110"))
        assertEquals("发送 2 个文件", shareActionLabel(fileCount = 2, peerAlias = "PHY110"))
        assertEquals("选择设备", shareActionLabel(fileCount = 0, peerAlias = null))
    }

    @Test
    fun reportsUnknownTransferSizeWithoutPretendingItIsZero() {
        assertEquals("共 大小未知 · 局域网传输", shareTransferSummary(totalSize = 0, hasUnknownSize = true))
    }

    @Test
    fun keepsTheFooterGeometryStableAcrossShareStates() {
        val empty = shareFooterSpec(fileCount = 0)
        val selected = shareFooterSpec(fileCount = 2)

        assertEquals(empty.topSpacingDp, selected.topSpacingDp)
        assertEquals(empty.summarySlotHeightDp, selected.summarySlotHeightDp)
        assertEquals(empty.bottomSpacingDp, selected.bottomSpacingDp)
        assertEquals(false, empty.showSummary)
        assertEquals(true, selected.showSummary)
    }

    @Test
    fun enablesContentScrollOnlyForShortExpandedWindows() {
        assertEquals(true, shareContentNeedsScroll(expanded = true, availableHeightDp = 400))
        assertEquals(false, shareContentNeedsScroll(expanded = true, availableHeightDp = 640))
        assertEquals(false, shareContentNeedsScroll(expanded = false, availableHeightDp = 400))
    }
}
