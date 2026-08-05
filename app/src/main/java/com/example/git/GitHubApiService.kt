package com.example.git

import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getContent(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): Response<GitHubContentResponse>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putContent(
        @Header("Authorization") authorization: String,
        @Header("Accept") accept: String = "application/vnd.github+json",
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: GitHubPutRequest
    ): Response<GitHubPutResponse>
}

data class GitHubContentResponse(
    val sha: String,
    val content: String?
)

data class GitHubPutRequest(
    val message: String,
    val content: String, // Base64 encoded
    val sha: String?
)

data class GitHubPutResponse(
    val content: GitHubContentInfo?
)

data class GitHubContentInfo(
    val name: String,
    val sha: String
)
