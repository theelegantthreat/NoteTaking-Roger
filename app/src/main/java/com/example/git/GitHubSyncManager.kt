package com.example.git

import android.util.Base64
import com.example.data.Note
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GitHubSyncManager {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val notesListAdapter = moshi.adapter<List<Note>>(
        Types.newParameterizedType(List::class.java, Note::class.java)
    )

    private val apiService: GitHubApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }

    /**
     * Push current notes to GitHub repository.
     */
    suspend fun pushNotes(
        token: String,
        repo: String,
        filePath: String,
        notes: List<Note>
    ): Result<String> {
        return try {
            val parts = repo.split("/")
            if (parts.size != 2) {
                return Result.failure(Exception("Repository must be in username/repo format"))
            }
            val owner = parts[0].trim()
            val repoName = parts[1].trim()
            val path = filePath.trim()

            val authHeader = "token $token"

            // 1. Get current SHA if file exists
            var currentSha: String? = null
            val getResponse = apiService.getContent(authHeader, owner = owner, repo = repoName, path = path)
            if (getResponse.isSuccessful) {
                currentSha = getResponse.body()?.sha
            }

            // 2. Prepare JSON content
            val jsonNotes = notesListAdapter.toJson(notes)
            val base64Content = Base64.encodeToString(jsonNotes.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            // 3. Put request
            val message = "NoteTaking Roger Backup: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}"
            val requestBody = GitHubPutRequest(
                message = message,
                content = base64Content,
                sha = currentSha
            )

            val putResponse = apiService.putContent(
                authorization = authHeader,
                owner = owner,
                repo = repoName,
                path = path,
                body = requestBody
            )

            if (putResponse.isSuccessful) {
                Result.success("Pushed successfully!")
            } else {
                val errorMsg = putResponse.errorBody()?.string() ?: "Sync failed with status code ${putResponse.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pull notes from GitHub repository.
     */
    suspend fun pullNotes(
        token: String,
        repo: String,
        filePath: String
    ): Result<List<Note>> {
        return try {
            val parts = repo.split("/")
            if (parts.size != 2) {
                return Result.failure(Exception("Repository must be in username/repo) format"))
            }
            val owner = parts[0].trim()
            val repoName = parts[1].trim()
            val path = filePath.trim()

            val authHeader = "token $token"

            val response = apiService.getContent(authHeader, owner = owner, repo = repoName, path = path)
            if (response.isSuccessful) {
                val ghResponse = response.body() ?: return Result.failure(Exception("Empty response body from GitHub"))
                val encodedContent = ghResponse.content ?: return Result.failure(Exception("File is empty or contains no content"))
                
                // Clean and decode Base64 content
                val cleanedBase64 = encodedContent.replace("\n", "").replace("\r", "")
                val decodedBytes = Base64.decode(cleanedBase64, Base64.DEFAULT)
                val decodedJson = String(decodedBytes, Charsets.UTF_8)

                val parsedNotes = notesListAdapter.fromJson(decodedJson)
                if (parsedNotes != null) {
                    Result.success(parsedNotes)
                } else {
                    Result.failure(Exception("Failed to parse notes layout"))
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Fetch failed with status code ${response.code()}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Test connection to GitHub API with provided credentials.
     */
    suspend fun testConnection(
        token: String,
        repoName: String,
        owner: String,
        path: String
    ): Result<Boolean> {
        return try {
            val authHeader = "token $token"
            val response = apiService.getContent(authHeader, owner = owner, repo = repoName, path = path)
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errCode = response.code()
                val errBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("HTTP $errCode: $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
