package com.github.avrilfanomar.pluginator.examples

import com.github.avrilfanomar.pluginator.api.Plugin
import com.github.avrilfanomar.pluginator.service.PluginatorService
import kotlinx.coroutines.runBlocking

/**
 * Example demonstrating how to use the Pluginator HTTP client.
 *
 * This is a reference implementation showing how to:
 * 1. Get the PluginatorService instance
 * 2. Request plugin recommendations
 * 3. Send feedback about recommendations
 * 4. Check server connectivity
 */
object ClientUsageExample {

    /**
     * Example: Get plugin recommendations
     */
    fun getRecommendationsExample() = runBlocking {
        // Get the service instance
        val service = PluginatorService.getInstance()

        // Prepare installed plugins list (must include "pluginator")
        val installedPlugins = listOf(
            Plugin("pluginator", "1.0.0"),
            Plugin("Kotlin", "1.9.0"),
            Plugin("Database Tools", "2023.3.1"),
            Plugin("GitToolBox", "500.0.0")
        )

        // Request recommendations
        val recommendation = service.getRecommendations(
            userUuid = "550e8400-e29b-41d4-a716-446655440000",
            ideName = "IntelliJ IDEA",
            ideVersion = "2023.3",
            os = "Linux",
            installedPlugins = installedPlugins
        )

        // Process the recommendations
        recommendation?.let {
            println("Received recommendation ID: ${it.id}")
            println("Recommended plugins:")
            it.plugins.forEach { plugin ->
                println("  - $plugin")
            }
        } ?: println("Failed to get recommendations")
    }

    /**
     * Example: Send feedback about a recommendation
     */
    fun sendFeedbackExample() = runBlocking {
        val service = PluginatorService.getInstance()

        // User accepted (installed) a recommended plugin
        val success = service.updateRecommendation(
            plugin = "Docker",
            userUuid = "550e8400-e29b-41d4-a716-446655440000",
            accepted = true
        )

        if (success) {
            println("Feedback sent successfully")
        } else {
            println("Failed to send feedback")
        }
    }

    /**
     * Example: Check if server is reachable
     */
    fun checkConnectivityExample() = runBlocking {
        val service = PluginatorService.getInstance()

        if (service.isServerReachable()) {
            println("Server is reachable")
        } else {
            println("Server is not reachable")
        }
    }

    /**
     * Example: Change server URL (e.g., for production)
     */
    fun changeServerUrlExample() {
        val service = PluginatorService.getInstance()

        // Change the server URL
        com.github.avrilfanomar.pluginator.config.PluginatorConfig.serverUrl = "https://api.pluginator.com"

        // Reset the client to use the new URL
        service.resetClient()

        println("Server URL updated")
    }
}
