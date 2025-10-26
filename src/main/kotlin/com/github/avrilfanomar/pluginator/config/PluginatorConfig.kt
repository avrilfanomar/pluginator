package com.github.avrilfanomar.pluginator.config

/**
 * Configuration settings for the Pluginator plugin.
 *
 * This class manages connection settings and other configuration parameters
 * for communicating with the Pluginator server.
 */
object PluginatorConfig {
    /**
     * Default server URL for local development.
     */
    const val DEFAULT_SERVER_URL = "http://37.157.254.65:6705/pluginator"

    /**
     * Current server URL to use for API requests.
     * Can be modified by users in plugin settings.
     */
    var serverUrl: String = DEFAULT_SERVER_URL

    /**
     * Timeout for HTTP requests in milliseconds.
     */
    const val REQUEST_TIMEOUT_MS = 30_000L

    /**
     * Maximum number of plugins to send in a recommendation request.
     */
    const val MAX_PLUGINS_PER_REQUEST = 200

    /**
     * Resets configuration to default values.
     */
    fun resetToDefaults() {
        serverUrl = DEFAULT_SERVER_URL
    }
}
