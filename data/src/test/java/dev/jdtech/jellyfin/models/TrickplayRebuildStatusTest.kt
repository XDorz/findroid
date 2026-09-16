package dev.jdtech.jellyfin.models

import java.util.UUID
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrickplayRebuildStatusTest {
    @Test
    fun jellyfinCompactGuidsAndOmittedNullFieldsDecodeCorrectly() {
        // Matches a response observed from an actual Jellyfin 12.1 server.
        val status =
            Json.decodeFromString<TrickplayRebuildStatus>(
                """{"instanceId":"d0ca6f1cfd4e4911baa85acd79bcbcf9","itemId":"992b6ee108ba6c4f48cf44baf6f0ed68","state":"none","apiVersion":1}"""
            )
        assertEquals(1, status.apiVersion)
        assertFalse(status.active)
        assertTrue(status.matchesVideo(UUID.fromString("992b6ee1-08ba-6c4f-48cf-44baf6f0ed68")))
        assertFalse(status.matchesVideo(UUID.randomUUID()))
        assertTrue(status.copy(state = "queued").active)
        assertFalse(status.copy(state = "failed").active)
    }
}
