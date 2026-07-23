package com.example.minimallauncher

import com.example.minimallauncher.data.ReasonLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherLogicTest {
    @Test
    fun moved_reordersValidIndicesAndKeepsInvalidInput() {
        val original = listOf("a", "b", "c")
        assertEquals(listOf("b", "c", "a"), original.moved(0, 2))
        assertEquals(original, original.moved(-1, 2))
        assertEquals(original, original.moved(0, 8))
    }

    @Test
    fun migrateFolderKey_replacesOrRemovesAndDeduplicates() {
        val keys = listOf("app:a", "folder:買い物", "folder:生活")
        assertEquals(
            listOf("app:a", "folder:生活"),
            migrateFolderKey(keys, "folder:買い物", "folder:生活"),
        )
        assertEquals(
            listOf("app:a", "folder:生活"),
            migrateFolderKey(keys, "folder:買い物", null),
        )
    }

    @Test
    fun launchGate_combinesReasonAndDelaySettings() {
        val friction = setOf("sns")
        val delay = setOf("sns", "game")
        assertEquals(LaunchGate(true, 5), launchGateFor("sns", friction, delay, 5))
        assertEquals(LaunchGate(false, 5), launchGateFor("game", friction, delay, 5))
        assertFalse(launchGateFor("phone", friction, delay, 5).isRequired)
    }

    @Test
    fun pruneReasonLog_removesOldEntriesAndSortsNewestFirst() {
        val day = 24L * 60L * 60L * 1000L
        val now = 100L * day
        val entries = listOf(
            logAt(now - 31 * day),
            logAt(now - 2 * day),
            logAt(now),
        )

        val result = pruneReasonLog(entries, now, 30 * day, 500)

        assertEquals(listOf(now, now - 2 * day), result.map { it.timestamp })
        assertTrue(result.none { it.timestamp == now - 31 * day })
    }

    private fun logAt(timestamp: Long) = ReasonLogEntry(
        packageName = "pkg.$timestamp",
        label = "App",
        reason = "理由",
        timestamp = timestamp,
    )
}
