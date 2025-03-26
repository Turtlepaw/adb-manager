package com.turtlepaw.adb

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json

class UpdateChecker(private val currentVersion: String) {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkForUpdate(githubRepoUrl: String): UpdateResult {
        return try {
            val response: HttpResponse = client.get("$githubRepoUrl/releases/latest")
            val latestVersion = extractVersionFromResponse(response)

            val updateAvailable = compareVersions(currentVersion, latestVersion)
            UpdateResult(
                isUpdateAvailable = updateAvailable,
                latestVersion = latestVersion
            )
        } catch (e: Exception) {
            UpdateResult(
                isUpdateAvailable = false,
                error = e.message
            )
        }
    }

    private fun compareVersions(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }

        // Use indices to iterate safely
        repeat(minOf(currentParts.size, latestParts.size)) { i ->
            when {
                latestParts[i] > currentParts[i] -> return true
                latestParts[i] < currentParts[i] -> return false
            }
        }

        return latestParts.size > currentParts.size
    }

    private suspend fun extractVersionFromResponse(response: HttpResponse): String {
        // Implement version extraction logic based on your GitHub release format
        // This is a simplified example
        val responseBody = response.bodyAsText()
        // Extract version from JSON or response headers
        return responseBody.substringAfter("\"tag_name\":\"v", "").substringBefore("\"")
    }
}

data class UpdateResult(
    val isUpdateAvailable: Boolean,
    val latestVersion: String? = null,
    val error: String? = null
)
