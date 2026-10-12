package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

@JsonClass(generateAdapter = true)
data class GitHubUserResponse(
    @Json(name = "login") val login: String,
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubCreateRepoRequest(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = "Repositório de compilação em nuvem criado automaticamente pelo App Compiler",
    @Json(name = "private") val isPrivate: Boolean = true,
    @Json(name = "auto_init") val autoInit: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GitHubRepoResponse(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "default_branch") val defaultBranch: String? = null,
    @Json(name = "private") val isPrivate: Boolean? = null,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubContentUploadRequest(
    @Json(name = "message") val message: String,
    @Json(name = "content") val content: String, // Base64
    @Json(name = "branch") val branch: String,
    @Json(name = "sha") val sha: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubFileContentResponse(
    @Json(name = "name") val name: String? = null,
    @Json(name = "path") val path: String? = null,
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubContentUploadResponse(
    @Json(name = "content") val content: GitHubContentItem? = null
)

@JsonClass(generateAdapter = true)
data class GitHubContentItem(
    @Json(name = "name") val name: String? = null,
    @Json(name = "path") val path: String? = null,
    @Json(name = "sha") val sha: String? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "download_url") val downloadUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowDispatchRequest(
    @Json(name = "ref") val ref: String,
    @Json(name = "inputs") val inputs: Map<String, String>? = emptyMap()
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRunsListResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "workflow_runs") val workflowRuns: List<GitHubWorkflowRunItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRunItem(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String? = null,
    @Json(name = "status") val status: String, // queued, in_progress, completed
    @Json(name = "conclusion") val conclusion: String? = null, // success, failure, cancelled
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubJobsListResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "jobs") val jobs: List<GitHubJobItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubJobItem(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "status") val status: String,
    @Json(name = "conclusion") val conclusion: String? = null,
    @Json(name = "steps") val steps: List<GitHubStepItem>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubStepItem(
    @Json(name = "name") val name: String,
    @Json(name = "status") val status: String,
    @Json(name = "conclusion") val conclusion: String? = null,
    @Json(name = "number") val number: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubArtifactsListResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "artifacts") val artifacts: List<GitHubArtifactItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubArtifactItem(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "size_in_bytes") val sizeInBytes: Long = 0,
    @Json(name = "archive_download_url") val archiveDownloadUrl: String? = null,
    @Json(name = "expired") val expired: Boolean = false
)

interface GitHubApiService {

    @GET("user")
    suspend fun getAuthenticatedUser(
        @Header("Authorization") authHeader: String
    ): Response<GitHubUserResponse>

    @POST("user/repos")
    suspend fun createRepository(
        @Header("Authorization") authHeader: String,
        @Body body: GitHubCreateRepoRequest
    ): Response<GitHubRepoResponse>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRepoResponse>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Query("ref") ref: String? = null
    ): Response<GitHubFileContentResponse>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun uploadFile(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: GitHubContentUploadRequest
    ): Response<GitHubContentUploadResponse>

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun triggerWorkflowDispatch(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: String,
        @Body body: GitHubWorkflowDispatchRequest
    ): Response<Unit>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun listWorkflowRuns(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("event") event: String? = null,
        @Query("per_page") perPage: Int = 10
    ): Response<GitHubWorkflowRunsListResponse>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}")
    suspend fun getWorkflowRun(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<GitHubWorkflowRunItem>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/jobs")
    suspend fun getWorkflowRunJobs(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<GitHubJobsListResponse>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/artifacts")
    suspend fun getWorkflowRunArtifacts(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<GitHubArtifactsListResponse>

    @Streaming
    @GET("repos/{owner}/{repo}/actions/artifacts/{artifact_id}/zip")
    suspend fun downloadArtifactZip(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("artifact_id") artifactId: Long
    ): Response<ResponseBody>

    @GET("repos/{owner}/{repo}/actions/jobs/{job_id}/logs")
    suspend fun getJobLogs(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("job_id") jobId: Long
    ): Response<ResponseBody>
}
