package dev.jdtech.jellyfin.models

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
}
