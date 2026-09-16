package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.jdtech.jellyfin.core.R
import dev.jdtech.jellyfin.models.FindroidSource
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.presentation.utils.LocalOfflineMode
import dev.jdtech.jellyfin.presentation.utils.rememberSafePadding
import java.util.UUID

@Composable
fun TrickplayRebuildHost(
    itemId: UUID,
    sources: List<FindroidSource>,
    content: @Composable (button: @Composable () -> Unit, status: @Composable () -> Unit) -> Unit,
) {
    val viewModel: TrickplayRebuildViewModel = hiltViewModel(key = "trickplay-$itemId")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val offline = LocalOfflineMode.current
    val versions = sources.filter { it.type == FindroidSourceType.REMOTE }.distinctBy { it.id }
    var selected by remember(itemId) { mutableStateOf<String?>(null) }
    var chooseVersion by remember { mutableStateOf(false) }
    val source = versions.firstOrNull { it.id == selected } ?: versions.firstOrNull()
    val safePadding = rememberSafePadding()

    LaunchedEffect(itemId, source?.id, offline, state.refresh) {
        if (!offline && source != null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.observe(itemId, source.id)
            }
        }
    }
    LaunchedEffect(viewModel, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { snackbar.showSnackbar(context.getString(it)) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        content(
            {
                if (!offline && state.administrator && source != null) {
                    FilledTonalIconButton(
                        onClick = {
                            if (
                                versions.size > 1 &&
                                    state.status?.active != true &&
                                    !state.submitting
                            ) {
                                chooseVersion = true
                            } else viewModel.submit(itemId, source.id)
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_image_refresh),
                            contentDescription = stringResource(R.string.trickplay_rebuild_button),
                        )
                    }
                }
            },
            {
                if (
                    !offline &&
                        state.administrator &&
                        source != null &&
                        state.status?.active == true
                ) {
                    AssistChip(
                        onClick = { viewModel.submit(itemId, source.id) },
                        label = {
                            Text(
                                stringResource(
                                    if (state.status?.state == "queued")
                                        R.string.trickplay_rebuild_queued
                                    else R.string.trickplay_rebuild_running
                                )
                            )
                        },
                    )
                }
            },
        )
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).padding(bottom = safePadding.bottom + 12.dp),
        )
    }
    if (chooseVersion) {
        AlertDialog(
            onDismissRequest = { chooseVersion = false },
            title = { Text(stringResource(R.string.trickplay_rebuild_choose_version)) },
            text = {
                Column {
                    versions.forEach { version ->
                        TextButton(
                            onClick = {
                                selected = version.id
                                chooseVersion = false
                                viewModel.submit(itemId, version.id)
                            }
                        ) {
                            Text(version.name.ifBlank { version.id })
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { chooseVersion = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
        )
    }
}
