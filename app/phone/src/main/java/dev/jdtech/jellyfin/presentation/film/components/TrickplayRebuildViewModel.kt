package dev.jdtech.jellyfin.presentation.film.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.TrickplayRebuildStatus
import dev.jdtech.jellyfin.repository.JellyfinRepositoryImpl
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.sdk.api.client.exception.InvalidStatusException

data class TrickplayRebuildState(
    val administrator: Boolean = false,
    val submitting: Boolean = false,
    val status: TrickplayRebuildStatus? = null,
    val refresh: Int = 0,
)

@HiltViewModel
class TrickplayRebuildViewModel
@Inject
constructor(private val repository: JellyfinRepositoryImpl) : ViewModel() {
    private val _state = MutableStateFlow(TrickplayRebuildState())
    val state = _state.asStateFlow()
    private val messages = Channel<Int>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()
    private val requests = Mutex()
    private var selectedSource: String? = null

    // Called only while the detail page is resumed. Lifecycle cancellation stops polling.
    suspend fun observe(itemId: UUID, sourceId: String) {
        if (selectedSource != sourceId) {
            selectedSource = sourceId
            _state.update { it.copy(status = null) }
        }
        try {
            val admin = repository.isAdministrator()
            _state.update { it.copy(administrator = admin) }
            if (!admin) return
            do {
                val status =
                    try {
                        requests.withLock {
                            repository.rebuildTrickplay(
                                itemId,
                                sourceUuid(sourceId),
                                submit = false,
                            )
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val statusCode = (e as? InvalidStatusException)?.status
                        if (
                            _state.value.status?.active == true &&
                                (statusCode == null || statusCode >= 500 || statusCode == 429)
                        ) {
                            // Retry only a read, never the rebuild POST. A transient outage must
                            // not
                            // leave a permanently stale running tag blocking further user actions.
                            delay(10_000)
                            continue
                        }
                        throw e
                    }
                val previous = _state.value.status
                if (
                    previous?.active == true &&
                        (status.state == "none" || previous.instanceId != status.instanceId)
                ) {
                    messages.send(R.string.trickplay_rebuild_lost)
                } else if (previous?.active == true && !status.active) {
                    messages.send(
                        when (status.state) {
                            "completed" -> R.string.trickplay_rebuild_completed
                            "interrupted" -> R.string.trickplay_rebuild_lost
                            else -> R.string.trickplay_rebuild_failed
                        }
                    )
                }
                _state.update { it.copy(status = status) }
                if (status.state == "completed") invalidate(itemId, sourceId)
                if (!status.active) return
                delay(10_000)
            } while (true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is InvalidStatusException && e.status in listOf(401, 403)) {
                _state.update { it.copy(administrator = false, status = null) }
            } else if (e is InvalidStatusException && e.status in listOf(400, 404, 405, 422)) {
                _state.update { it.copy(status = null) }
            }
            // Initial capability/status checks are quiet. An explicit click always gets feedback.
        }
    }

    fun submit(itemId: UUID, sourceId: String) {
        if (selectedSource != sourceId && !_state.value.submitting) {
            selectedSource = sourceId
            _state.update { it.copy(status = null) }
        }
        if (_state.value.submitting || _state.value.status?.active == true) {
            messages.trySend(R.string.trickplay_rebuild_duplicate)
            return
        }
        _state.update { it.copy(submitting = true) }
        viewModelScope.launch {
            try {
                val status = requests.withLock {
                    repository.rebuildTrickplay(itemId, sourceUuid(sourceId), submit = true)
                }
                _state.update { it.copy(status = status) }
                messages.send(R.string.trickplay_rebuild_submitted)
                invalidate(itemId, sourceId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message =
                    when ((e as? InvalidStatusException)?.status) {
                        401,
                        403 -> R.string.trickplay_rebuild_forbidden
                        409 -> R.string.trickplay_rebuild_duplicate
                        429 -> R.string.trickplay_rebuild_busy
                        404,
                        405,
                        503 -> R.string.trickplay_rebuild_unavailable
                        400,
                        422 -> R.string.trickplay_rebuild_unsupported
                        500,
                        502 -> R.string.trickplay_rebuild_failed
                        else -> R.string.trickplay_rebuild_uncertain
                    }
                messages.send(message)
                if (e is InvalidStatusException && e.status == 409) invalidate(itemId, sourceId)
            } finally {
                _state.update { it.copy(submitting = false, refresh = it.refresh + 1) }
            }
        }
    }

    private suspend fun invalidate(itemId: UUID, sourceId: String) {
        try {
            repository.invalidateTrickplayCache(itemId, sourceId)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A cache cleanup error must not turn an accepted server job into a submission failure.
            messages.send(R.string.trickplay_rebuild_cache_error)
        }
    }

    private fun sourceUuid(value: String): UUID {
        val compact = value.replace("-", "")
        require(compact.length == 32)
        return UUID.fromString(
            "${compact.take(8)}-${compact.substring(8, 12)}-${compact.substring(12, 16)}-${compact.substring(16, 20)}-${compact.substring(20)}"
        )
    }
}
