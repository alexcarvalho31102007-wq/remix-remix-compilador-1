package com.example.data.model

enum class StageStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class BuildStageItem(
    val id: Int,
    val title: String,
    val description: String,
    val status: StageStatus = StageStatus.PENDING,
    val elapsedSeconds: Int = 0
)

enum class OverallBuildStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILED
}

data class ProjectZipMetadata(
    val fileName: String,
    val fileSizeFormatted: String,
    val fileSizeBytes: Long,
    val totalEntries: Int,
    val hasGradle: Boolean,
    val hasManifest: Boolean,
    val localCachedPath: String? = null
)

data class GitHubConfig(
    val token: String = "",
    val owner: String = "",
    val repo: String = "",
    val workflowFile: String = "build.yml",
    val branch: String = "main"
) {
    val isConfigured: Boolean
        get() = token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank()
        
    val repoFullName: String
        get() = if (owner.isNotBlank() && repo.isNotBlank()) "$owner/$repo" else ""
}

data class BuildHistoryRecord(
    val id: String,
    val projectName: String,
    val dateFormatted: String,
    val status: OverallBuildStatus,
    val apkPath: String? = null,
    val logs: String = "",
    val durationFormatted: String = ""
)
