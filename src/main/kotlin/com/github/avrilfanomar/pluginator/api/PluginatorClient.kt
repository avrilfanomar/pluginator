package com.github.avrilfanomar.pluginator.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * HTTP client for communicating with the Pluginator server API.
 *
 * This client provides methods to:
 * - Get plugin recommendations based on user profile and installed plugins
 * - Update recommendation feedback (accept/reject)
 *
 * @property baseUrl Base URL of the Pluginator server (e.g., "http://localhost:8080")
 */
class PluginatorClient(private val baseUrl: String) {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            })
        }

        engine {
            requestTimeout = 30_000 // 30 seconds
        }
    }

    /**
     * Retrieves plugin recommendations from the server.
     *
     * @param request Recommendation request containing user info and installed plugins
     * @return Recommendation object with list of suggested plugin names
     * @throws PluginatorApiException if the request fails or returns an error
     */
    suspend fun getRecommendations(request: RecommendationRequest): Recommendation {
        return try {
            val response = client.post("$baseUrl/recommendations") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            when (response.status) {
                HttpStatusCode.OK -> response.body<Recommendation>()
                HttpStatusCode.BadRequest -> {
                    val errorMessage = response.body<String>()
                    throw PluginatorApiException("Bad request: $errorMessage", response.status.value)
                }
                else -> {
                    val errorMessage = response.body<String>()
                    throw PluginatorApiException("Request failed: $errorMessage", response.status.value)
                }
            }
        } catch (e: PluginatorApiException) {
            throw e
        } catch (e: Exception) {
            throw PluginatorApiException("Failed to connect to server: ${e.message}", 0, e)
        }
    }

    /**
     * Updates recommendation feedback on the server.
     *
     * Sends user feedback about whether they accepted (installed) or rejected a recommended plugin.
     * This helps improve future recommendations.
     *
     * @param request Update request containing plugin name, user UUID, and acceptance status
     * @return Response message from the server (typically "accepted")
     * @throws PluginatorApiException if the request fails or returns an error
     */
    suspend fun updateRecommendation(request: RecommendationUpdateRequest): String {
        return try {
            val response = client.put("$baseUrl/recommendations/update") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            when (response.status) {
                HttpStatusCode.OK -> response.body<String>()
                HttpStatusCode.BadRequest -> {
                    val errorMessage = response.body<String>()
                    throw PluginatorApiException("Bad request: $errorMessage", response.status.value)
                }
                else -> {
                    val errorMessage = response.body<String>()
                    throw PluginatorApiException("Request failed: $errorMessage", response.status.value)
                }
            }
        } catch (e: PluginatorApiException) {
            throw e
        } catch (e: Exception) {
            throw PluginatorApiException("Failed to connect to server: ${e.message}", 0, e)
        }
    }

    /**
     * Closes the HTTP client and releases resources.
     * Should be called when the client is no longer needed.
     */
    fun close() {
        client.close()
    }
}

/**
 * Exception thrown when the Pluginator API returns an error or the request fails.
 *
 * @property message Error message describing what went wrong
 * @property statusCode HTTP status code (0 if connection failed)
 * @property cause Original exception that caused this error (if any)
 */
class PluginatorApiException(
    message: String,
    val statusCode: Int,
    cause: Throwable? = null
) : Exception(message, cause)
