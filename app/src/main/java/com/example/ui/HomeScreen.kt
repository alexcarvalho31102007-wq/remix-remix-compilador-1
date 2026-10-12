package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BuildStageItem
import com.example.data.model.OverallBuildStatus
import com.example.data.model.StageStatus
import com.example.ui.components.GitHubSettingsBottomSheet
import com.example.ui.theme.CoralError
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.SkyBlue40
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onZipSelected(uri, autoStart = true)
        }
    }

    LaunchedEffect(uiState.userSnackbarMessage) {
        uiState.userSnackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate900,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate900)
        ) {
            // Top row: only the settings gear button in the top corner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.openSettings() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("open_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configurações",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Main center content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = uiState.overallStatus,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "main_state"
                ) { status ->
                    when (status) {
                        OverallBuildStatus.IDLE -> {
                            // Only the clean ZIP select button
                            Button(
                                onClick = { zipPickerLauncher.launch("*/*") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 340.dp)
                                    .height(64.dp)
                                    .testTag("select_zip_button"),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SkyBlue40,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Selecionar arquivo ZIP",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        OverallBuildStatus.RUNNING -> {
                            // Minimal clean step-by-step progress
                            MinimalStagesProgressView(
                                stages = uiState.stages,
                                timerSeconds = uiState.timerSeconds,
                                onCancel = { viewModel.cancelCompilation() }
                            )
                        }

                        OverallBuildStatus.SUCCESS -> {
                            // Clean Success state: Install App Button
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 340.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.installApk(context) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .testTag("install_app_button"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = EmeraldSuccess,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.UploadFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Instalar Aplicativo",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.resetForNewBuild() },
                                    modifier = Modifier.testTag("new_build_button")
                                ) {
                                    Text(
                                        text = "Selecionar outro ZIP",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }

                        OverallBuildStatus.FAILED -> {
                            // Clean Failure state: Copy Logs Button
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 340.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.copyLogsToClipboard(context) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .testTag("copy_logs_button"),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.showLogsCopiedFeedback) EmeraldSuccess else CoralError,
                                        contentColor = Color.White
                                    ),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.showLogsCopiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (uiState.showLogsCopiedFeedback) "Logs Copiados!" else "Copiar todos os logs",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    TextButton(
                                        onClick = { viewModel.startCompilation() },
                                        modifier = Modifier.testTag("retry_build_button")
                                    ) {
                                        Text(
                                            text = "Tentar novamente",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp
                                        )
                                    }

                                    TextButton(
                                        onClick = { viewModel.resetForNewBuild() },
                                        modifier = Modifier.testTag("new_build_button")
                                    ) {
                                        Text(
                                            text = "Selecionar outro ZIP",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings Bottom Sheet
    if (uiState.showSettingsSheet) {
        GitHubSettingsBottomSheet(
            currentConfig = uiState.githubConfig,
            isAutoSettingUp = uiState.isAutoSettingUp,
            autoSetupProgressMessage = uiState.autoSetupProgressMessage,
            errorMessage = uiState.configVerificationMessage,
            onAutoSetup = { token -> viewModel.autoSetupToken(token) },
            onDisconnect = { viewModel.disconnectGitHub() },
            onDismiss = { viewModel.closeSettings() }
        )
    }
}

@Composable
fun MinimalStagesProgressView(
    stages: List<BuildStageItem>,
    timerSeconds: Int,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mins = timerSeconds / 60
    val secs = timerSeconds % 60
    val timerText = String.format("%02d:%02d", mins, secs)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Slate800)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = timerText,
            color = Color(0xFF94A3B8),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            stages.forEachIndexed { index, stage ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (stage.status) {
                            StageStatus.COMPLETED -> {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            StageStatus.IN_PROGRESS -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = SkyBlue40
                                )
                            }
                            StageStatus.FAILED -> {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(CoralError),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            StageStatus.PENDING -> {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = stage.title,
                        fontSize = 15.sp,
                        fontWeight = if (stage.status == StageStatus.IN_PROGRESS) FontWeight.Bold else FontWeight.Medium,
                        color = when (stage.status) {
                            StageStatus.COMPLETED -> Color.White
                            StageStatus.IN_PROGRESS -> SkyBlue40
                            StageStatus.FAILED -> CoralError
                            StageStatus.PENDING -> Color(0xFF64748B)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
            onClick = onCancel,
            modifier = Modifier.testTag("cancel_build_button")
        ) {
            Text(
                text = "Cancelar",
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
        }
    }
}
