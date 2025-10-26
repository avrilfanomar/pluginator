package com.github.avrilfanomar.pluginator.service

import com.github.avrilfanomar.pluginator.api.Plugin
import com.github.avrilfanomar.pluginator.api.PluginatorApiException
import com.github.avrilfanomar.pluginator.api.PluginatorClient
import com.github.avrilfanomar.pluginator.api.Recommendation
import com.github.avrilfanomar.pluginator.api.RecommendationRequest
import com.github.avrilfanomar.pluginator.api.RecommendationUpdateRequest
import com.github.avrilfanomar.pluginator.config.PluginatorConfig
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Application-level service for managing Pluginator API communications.
 *
 * This service is a singleton that provides a centralized way to interact with
 * the Pluginator server API. It handles client lifecycle and provides
 * convenient methods for making API calls.
 */
@Service(Service.Level.APP)
class PluginatorService {
    private val logger = Logger.getInstance(PluginatorService::class.java)

    @Volatile
    private var client: PluginatorClient? = null

    /**
     * Gets or creates the Pluginator client instance.
     * Thread-safe lazy initialization.
     */
    private fun getClient(): PluginatorClient {
        return client ?: synchronized(this) {
            client ?: PluginatorClient(PluginatorConfig.serverUrl).also {
                client = it
                logger.info("PluginatorClient initialized with URL: ${PluginatorConfig.serverUrl}")
            }
        }
    }

    /**
     * Resets the client instance (useful when server URL changes).
     */
    fun resetClient() {
        synchronized(this) {
            client?.close()
            client = null
            logger.info("PluginatorClient reset")
        }
    }

    /**
     * Retrieves plugin recommendations for the user.
     *
     * @param userUuid Unique user identifier
     * @param ideName Name of the IDE (e.g., "IntelliJ IDEA")
     * @param ideVersion Version of the IDE
     * @param os Operating system name (optional)
     * @param installedPlugins List of installed plugins
     * @return Recommendation object with suggested plugins, or null if request fails
     */
    suspend fun getRecommendations(
        userUuid: String,
        ideName: String,
        ideVersion: String,
        os: String = "",
        installedPlugins: List<Plugin>
    ): Recommendation? {
        return withContext(Dispatchers.IO) {
            try {
                val request = RecommendationRequest(
                    userUuid = userUuid,
                    ideName = ideName,
                    ideVersion = ideVersion,
                    os = os,
                    installedPlugins = installedPlugins
                )

                logger.info("Requesting recommendations for user: $userUuid")
                val recommendation = getClient().getRecommendations(request)
                logger.info("Received ${recommendation.plugins.size} recommendations (ID: ${recommendation.id})")
                recommendation
            } catch (e: PluginatorApiException) {
                logger.warn("Failed to get recommendations: ${e.message}", e)
                null
            } catch (e: Exception) {
                logger.error("Unexpected error getting recommendations", e)
                null
            }
        }
    }

    /**
     * Sends feedback about a recommendation to the server.
     *
     * @param plugin Name of the plugin
     * @param userUuid Unique user identifier
     * @param accepted True if user installed the plugin, false if rejected
     * @return True if feedback was successfully sent
     */
    suspend fun updateRecommendation(
        plugin: String,
        userUuid: String,
        accepted: Boolean
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = RecommendationUpdateRequest(
                    plugin = plugin,
                    userUuid = userUuid,
                    accepted = accepted
                )

                logger.info("Sending feedback for plugin: $plugin (accepted: $accepted)")
                getClient().updateRecommendation(request)
                logger.info("Feedback sent successfully")
                true
            } catch (e: PluginatorApiException) {
                logger.warn("Failed to send feedback: ${e.message}", e)
                false
            } catch (e: Exception) {
                logger.error("Unexpected error sending feedback", e)
                false
            }
        }
    }

    /**
     * Checks if the Pluginator server is reachable.
     *
     * @return True if server is reachable
     */
    suspend fun isServerReachable(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Try a simple request with minimal data to check connectivity
                val testRequest = RecommendationRequest(
                    userUuid = "00000000-0000-0000-0000-000000000000",
                    ideName = "test",
                    ideVersion = "1.0",
                    os = "",
                    installedPlugins = listOf(Plugin("pluginator", "1.0.0"))
                )

                getClient().getRecommendations(testRequest)
                true
            } catch (e: Exception) {
                logger.info("Server not reachable: ${e.message}")
                false
            }
        }
    }

    companion object {
        /**
         * Gets the application-level instance of PluginatorService.
         */
        fun getInstance(): PluginatorService {
            return com.intellij.openapi.components.service()
        }
    }
}
