package dev.jdtech.jellyfin.models

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class TrickplayRebuildStatus(
    val apiVersion: Int,
    val instanceId: String,
    val itemId: String,
    val state: String,
    val jobId: String? = null,
    val updatedAt: String? = null,
) {
    val active: Boolean
        get() = state == "queued" || state == "running"

    fun matchesVideo(id: UUID): Boolean =
        itemId.replace("-", "").equals(id.toString().replace("-", ""), ignoreCase = true)
}
