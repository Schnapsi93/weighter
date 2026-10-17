package com.aljaz.weighttracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/** Tests for Nextcloud URL handling and backup file naming. */
class NextcloudUrlTest {

    private fun config(server: String, user: String = "aljaz") =
        NextcloudConfig(server, user, "secret")

    @Test
    fun normalize_addsHttpsAndRemovesTrailingSlash() {
        assertEquals(
            "https://cloud.example.com",
            NextcloudClient.normalizeServerUrl(" cloud.example.com/ "),
        )
    }

    @Test
    fun normalize_rejectsPlainHttp() {
        assertNull(NextcloudClient.normalizeServerUrl("http://cloud.example.com"))
    }

    @Test
    fun normalize_rejectsEmptyInput() {
        assertNull(NextcloudClient.normalizeServerUrl("   "))
    }

    @Test
    fun folderUrl_isBuiltCorrectly() {
        assertEquals(
            "https://cloud.example.com/remote.php/dav/files/aljaz/WeightTracker",
            NextcloudClient.folderUrl(config("https://cloud.example.com"), "WeightTracker").toString(),
        )
    }

    @Test
    fun folderUrl_supportsNextcloudInSubfolder() {
        assertEquals(
            "https://example.com/nextcloud/remote.php/dav/files/aljaz/WeightTracker",
            NextcloudClient.folderUrl(config("https://example.com/nextcloud"), "WeightTracker").toString(),
        )
    }

    @Test
    fun folderUrl_encodesSpacesInUsername() {
        assertEquals(
            "https://cloud.example.com/remote.php/dav/files/john%20doe/WeightTracker",
            NextcloudClient.folderUrl(config("https://cloud.example.com", "john doe"), "WeightTracker").toString(),
        )
    }

    @Test
    fun backupFileName_isPerMonth() {
        assertEquals(
            "weighter-2026-10.csv",
            BackupManager.fileNameFor(LocalDate.of(2026, 10, 16)),
        )
    }

    @Test
    fun folderUrl_supportsNestedFolders() {
        assertEquals(
            "https://cloud.example.com/remote.php/dav/files/aljaz/myapps/weighter/backups",
            NextcloudClient.folderUrl(config("https://cloud.example.com"), "myapps/weighter/backups").toString(),
        )
    }
}