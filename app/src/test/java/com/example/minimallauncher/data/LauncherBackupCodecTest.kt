package com.example.minimallauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LauncherBackupCodecTest {
    @Test
    fun encodeThenDecode_preservesAllSettings() {
        val backup = LauncherBackup(
            allowedPackages = setOf("phone", "youtube"),
            categories = mapOf("youtube" to "動画"),
            dockPackages = setOf("phone"),
            dockOrder = listOf("app:phone"),
            homeOrder = listOf("folder:動画"),
            frictionPackages = setOf("youtube"),
            delayPackages = setOf("youtube"),
            reasonLog = listOf(ReasonLogEntry("youtube", "YouTube", "音楽", 1234L)),
            groupItemOrder = mapOf("動画" to listOf("youtube")),
            homeLabelMode = HomeLabelMode.LIGHT,
        )

        assertEquals(backup, LauncherBackupCodec.decode(LauncherBackupCodec.encode(backup)))
    }

    @Test
    fun decode_rejectsUnknownSchema() {
        assertThrows(IllegalArgumentException::class.java) {
            LauncherBackupCodec.decode("""{"schemaVersion":99}""")
        }
    }
}
