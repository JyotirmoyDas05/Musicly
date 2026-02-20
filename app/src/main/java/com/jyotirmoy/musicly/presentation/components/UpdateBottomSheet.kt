@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.jyotirmoy.musicly.presentation.components

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.jyotirmoy.musicly.BuildConfig
import com.jyotirmoy.musicly.R
import com.jyotirmoy.musicly.domain.model.DownloadState
import com.jyotirmoy.musicly.presentation.viewmodel.UpdateViewModel
import com.jyotirmoy.musicly.utils.installApk
import com.jyotirmoy.musicly.utils.openUrl
import java.io.File

@Composable
fun UpdateBottomSheet(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    latestVersion: String = "",
    releaseNotes: String = "",
    apkUrl: String = "",
    viewModel: UpdateViewModel? = null
) {
    val actualViewModel = viewModel ?: hiltViewModel<UpdateViewModel>()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val activity = context as? Activity
    val interactionSources = remember { List(2) { MutableInteractionSource() } }
    val downloadState by actualViewModel.downloadState.collectAsState()
    val apkName = "musicly-update.apk"
    val apkFile = remember { File(context.getExternalFilesDir(null), apkName) }
    var pendingInstall by rememberSaveable { mutableStateOf(false) }
    var permissionPromptShown by rememberSaveable { mutableStateOf(false) }
    var showDownloadButton by rememberSaveable { mutableStateOf(true) }
    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val canInstall =
            context.packageManager.canRequestPackageInstalls()

        if (canInstall && pendingInstall) {
            activity?.installApk(apkFile)
            pendingInstall = false
        }
    }

    LaunchedEffect(downloadState) {
        when (val state = downloadState) {
            is DownloadState.Success -> {
                showDownloadButton = true

                val file = state.file

                val canInstall = context.packageManager.canRequestPackageInstalls()

                if (canInstall) {
                    activity?.installApk(file)
                } else if (!permissionPromptShown) {
                    pendingInstall = true
                    permissionPromptShown = true
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        "package:${context.packageName}".toUri()
                    )
                    settingsLauncher.launch(intent)
                } else {
                    Toast.makeText(
                        context,
                        "Unknown sources permission not granted",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }

            is DownloadState.Started -> {
                showDownloadButton = false
            }

            is DownloadState.Progress -> {
                showDownloadButton = false
            }

            is DownloadState.Cancelled -> {
                showDownloadButton = true
            }

            is DownloadState.Error -> {
                showDownloadButton = true
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT)
                    .show()
            }

            else -> Unit
        }
    }

    val currentProgress = when (downloadState) {
        is DownloadState.Progress -> (downloadState as DownloadState.Progress).percent
        else -> 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
    )

    val downloadButtonClickAction: () -> Unit = {
        Log.d("UpdateBottomSheet", "Starting APK download: $apkUrl")
        actualViewModel.downloadApk(apkUrl, apkName)
    }

    ModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        dragHandle = null,
        onDismissRequest = {
            onDismiss()
            permissionPromptShown = false
        }
    ) {
        Text(
            modifier = Modifier.padding(20.dp),
            text = "Update Available",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            text = "Latest Version: $latestVersion",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            modifier = Modifier.padding(horizontal = 20.dp),
            text = "Current Version: ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelLarge,
        )

        if (releaseNotes.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "What's New",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // parse markdown lines into clean bullet items
            val parsedLines = remember(releaseNotes) {
                releaseNotes.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .map { line ->
                        line.removePrefix("- ")
                            .removePrefix("* ")
                            .removePrefix("• ")
                            .replace(Regex("^#+\\s*"), "")
                            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
                            .trim()
                    }
                    .filter { it.isNotBlank() }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                parsedLines.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            text = item,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {

            when (downloadState) {
                is DownloadState.Started -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is DownloadState.Progress -> {
                    LinearWavyProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .align(Alignment.Center),
                        progress = { animatedProgress }
                    )
                }

                else -> {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .align(Alignment.CenterStart),
                        maxLines = 3,
                        text = "Tap 'Download' to get the latest version",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

        }

        @Suppress("DEPRECATION")
        ButtonGroup(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
        ) {
            OutlinedButton(
                onClick = {
                    permissionPromptShown = false

                    if (showDownloadButton) {
                        onDismiss()
                    } else {
                        actualViewModel.cancelDownload()
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = "Cancel")
            }

            if (showDownloadButton)
                Button(
                    onClick = {
                        downloadButtonClickAction()
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = "Download")
                }
        }
    }
}
