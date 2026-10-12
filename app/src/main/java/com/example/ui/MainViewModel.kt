package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BuildHistoryRecord
import com.example.data.model.BuildStageItem
import com.example.data.model.GitHubConfig
import com.example.data.model.OverallBuildStatus
import com.example.data.model.ProjectZipMetadata
import com.example.data.model.StageStatus
import com.example.data.repository.BuildProgressEvent
import com.example.data.repository.CompilerRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class MainUiState(
    val githubConfig: GitHubConfig = GitHubConfig(),
    val selectedZip: ProjectZipMetadata? = null,
    val stages: List<BuildStageItem> = emptyList(),
    val currentStageIndex: Int = 0,
    val overallStatus: OverallBuildStatus = OverallBuildStatus.IDLE,
    val allLogs: String = "",
    val apkFile: File? = null,
    val errorMessage: String? = null,
    val isBuilding: Boolean = false,
    val timerSeconds: Int = 0,
    val showSettingsSheet: Boolean = false,
    val showWorkflowGuide: Boolean = false,
    val showLogsCopiedFeedback: Boolean = false,
    val isVerifyingConfig: Boolean = false,
    val isAutoSettingUp: Boolean = false,
    val autoSetupProgressMessage: String? = null,
    val configVerificationMessage: String? = null,
    val buildHistory: List<BuildHistoryRecord> = emptyList(),
    val userSnackbarMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CompilerRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var compilationJob: Job? = null
    private var timerJob: Job? = null

    init {
        loadInitialConfig()
    }

    private fun loadInitialConfig() {
        val config = repository.getGitHubConfig()
        _uiState.update { it.copy(githubConfig = config) }

        if (config.token.isNotBlank() && !config.isConfigured) {
            autoSetupToken(config.token)
        }
    }

    fun onZipSelected(uri: Uri, autoStart: Boolean = true) {
        viewModelScope.launch {
            val result = repository.inspectAndCacheZip(uri)
            result.onSuccess { meta ->
                _uiState.update {
                    it.copy(
                        selectedZip = meta,
                        overallStatus = OverallBuildStatus.IDLE,
                        stages = emptyList(),
                        apkFile = null,
                        errorMessage = null,
                        allLogs = ""
                    )
                }
                if (autoStart) {
                    startCompilation()
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        userSnackbarMessage = "Erro ao ler arquivo ZIP: ${err.message}"
                    )
                }
            }
        }
    }

    fun loadSampleProject() {
        viewModelScope.launch {
            val result = repository.createSampleProjectZip()
            result.onSuccess { meta ->
                _uiState.update {
                    it.copy(
                        selectedZip = meta,
                        overallStatus = OverallBuildStatus.IDLE,
                        stages = emptyList(),
                        apkFile = null,
                        errorMessage = null,
                        allLogs = ""
                    )
                }
                startCompilation()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(userSnackbarMessage = "Erro ao gerar projeto de teste: ${err.message}")
                }
            }
        }
    }

    fun startCompilation(simulateFailure: Boolean = false) {
        val zipMeta = _uiState.value.selectedZip ?: return
        if (_uiState.value.isBuilding) return

        compilationJob?.cancel()
        timerJob?.cancel()

        _uiState.update {
            it.copy(
                isBuilding = true,
                overallStatus = OverallBuildStatus.RUNNING,
                apkFile = null,
                errorMessage = null,
                allLogs = "",
                timerSeconds = 0
            )
        }

        // Start timer
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(timerSeconds = it.timerSeconds + 1) }
            }
        }

        compilationJob = viewModelScope.launch {
            val config = _uiState.value.githubConfig
            val flow = if (simulateFailure) {
                repository.executeSimulatedFailure(zipMeta)
            } else {
                repository.executeCompilationPipeline(zipMeta, config)
            }

            flow.collect { event ->
                when (event) {
                    is BuildProgressEvent.StagesUpdated -> {
                        _uiState.update {
                            it.copy(
                                stages = event.stages,
                                currentStageIndex = event.currentStageIndex,
                                overallStatus = event.overallStatus
                            )
                        }
                    }
                    is BuildProgressEvent.BuildSuccess -> {
                        timerJob?.cancel()
                        val durationText = formatSeconds(_uiState.value.timerSeconds)
                        val historyItem = BuildHistoryRecord(
                            id = UUID.randomUUID().toString(),
                            projectName = zipMeta.fileName,
                            dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                            status = OverallBuildStatus.SUCCESS,
                            apkPath = event.apkFile.absolutePath,
                            logs = event.totalLogs,
                            durationFormatted = durationText
                        )
                        _uiState.update {
                            it.copy(
                                isBuilding = false,
                                overallStatus = OverallBuildStatus.SUCCESS,
                                apkFile = event.apkFile,
                                stages = event.stages,
                                allLogs = event.totalLogs,
                                buildHistory = listOf(historyItem) + it.buildHistory,
                                userSnackbarMessage = "Compilação concluída com sucesso!"
                            )
                        }
                    }
                    is BuildProgressEvent.BuildFailure -> {
                        timerJob?.cancel()
                        val durationText = formatSeconds(_uiState.value.timerSeconds)
                        val historyItem = BuildHistoryRecord(
                            id = UUID.randomUUID().toString(),
                            projectName = zipMeta.fileName,
                            dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()),
                            status = OverallBuildStatus.FAILED,
                            apkPath = null,
                            logs = event.totalLogs,
                            durationFormatted = durationText
                        )
                        _uiState.update {
                            it.copy(
                                isBuilding = false,
                                overallStatus = OverallBuildStatus.FAILED,
                                errorMessage = event.errorMessage,
                                stages = event.stages,
                                allLogs = event.totalLogs,
                                buildHistory = listOf(historyItem) + it.buildHistory,
                                userSnackbarMessage = "Compilação falhou."
                            )
                        }
                    }
                }
            }
        }
    }

    fun cancelCompilation() {
        compilationJob?.cancel()
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                isBuilding = false,
                overallStatus = OverallBuildStatus.IDLE,
                stages = emptyList(),
                userSnackbarMessage = "Compilação cancelada."
            )
        }
    }

    fun resetForNewBuild() {
        compilationJob?.cancel()
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                selectedZip = null,
                overallStatus = OverallBuildStatus.IDLE,
                stages = emptyList(),
                apkFile = null,
                errorMessage = null,
                allLogs = "",
                timerSeconds = 0
            )
        }
    }

    fun copyLogsToClipboard(context: Context) {
        val logs = _uiState.value.allLogs
        if (logs.isBlank()) {
            _uiState.update { it.copy(userSnackbarMessage = "Nenhum log disponível.") }
            return
        }

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Logs de Compilação", logs)
        clipboard?.setPrimaryClip(clip)

        _uiState.update {
            it.copy(
                showLogsCopiedFeedback = true,
                userSnackbarMessage = "Logs copiados para a área de transferência!"
            )
        }

        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(showLogsCopiedFeedback = false) }
        }
    }

    fun installApk(context: Context) {
        val apk = _uiState.value.apkFile
        if (apk == null || !apk.exists()) {
            _uiState.update { it.copy(userSnackbarMessage = "Arquivo APK não encontrado.") }
            return
        }

        // Check canRequestPackageInstalls on Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                _uiState.update {
                    it.copy(userSnackbarMessage = "Permita a instalação de fontes desconhecidas para este app.")
                }
                return
            }
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apk)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            _uiState.update {
                it.copy(userSnackbarMessage = "Erro ao iniciar instalação: ${e.message}")
            }
        }
    }

    fun shareApk(context: Context) {
        val apk = _uiState.value.apkFile ?: return
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apk)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartilhar Aplicativo Compilado"))
        } catch (e: Exception) {
            _uiState.update { it.copy(userSnackbarMessage = "Erro ao compartilhar: ${e.message}") }
        }
    }

    fun autoSetupToken(token: String) {
        _uiState.update {
            it.copy(
                isAutoSettingUp = true,
                autoSetupProgressMessage = "Iniciando configuração automática...",
                configVerificationMessage = null
            )
        }

        viewModelScope.launch {
            val result = repository.autoSetupWithToken(token) { progressMsg ->
                _uiState.update { it.copy(autoSetupProgressMessage = progressMsg) }
            }

            result.onSuccess { config ->
                _uiState.update {
                    it.copy(
                        isAutoSettingUp = false,
                        autoSetupProgressMessage = null,
                        githubConfig = config,
                        showSettingsSheet = false,
                        userSnackbarMessage = "GitHub configurado com sucesso! Repositório e workflow criados automaticamente."
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isAutoSettingUp = false,
                        autoSetupProgressMessage = null,
                        configVerificationMessage = "Erro: ${err.message}"
                    )
                }
            }
        }
    }

    fun disconnectGitHub() {
        val emptyConfig = GitHubConfig()
        repository.saveGitHubConfig(emptyConfig)
        _uiState.update {
            it.copy(
                githubConfig = emptyConfig,
                userSnackbarMessage = "Conta do GitHub desconectada."
            )
        }
    }

    fun saveGitHubConfig(config: GitHubConfig) {
        repository.saveGitHubConfig(config)
        _uiState.update {
            it.copy(
                githubConfig = config,
                showSettingsSheet = false,
                userSnackbarMessage = "Configurações do GitHub salvas com sucesso!"
            )
        }
    }

    fun verifyConfig(config: GitHubConfig) {
        _uiState.update { it.copy(isVerifyingConfig = true, configVerificationMessage = null) }
        viewModelScope.launch {
            val result = repository.verifyGitHubConnection(config)
            result.onSuccess { msg ->
                _uiState.update {
                    it.copy(
                        isVerifyingConfig = false,
                        configVerificationMessage = "Conexão bem-sucedida! $msg"
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isVerifyingConfig = false,
                        configVerificationMessage = "Falha na conexão: ${err.message}"
                    )
                }
            }
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettingsSheet = true, configVerificationMessage = null) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(showSettingsSheet = false) }
    }

    fun openWorkflowGuide() {
        _uiState.update { it.copy(showWorkflowGuide = true) }
    }

    fun closeWorkflowGuide() {
        _uiState.update { it.copy(showWorkflowGuide = false) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(userSnackbarMessage = null) }
    }

    private fun formatSeconds(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }
}
