package com.github.avrilfanomar.pluginator.api

import kotlinx.serialization.Serializable

/**
 * Represents an installed plugin with its name and version.
 */
@Serializable
data class Plugin(
    val name: String,
    val version: String
)

/**
 * Request model for getting plugin recommendations.
 *
 * @property userUuid Unique user identifier (UUID format, max 36 chars)
 * @property ideName Name of the IDE (1-100 chars, alphanumeric + _, -, space, .)
 * @property ideVersion Version of the IDE (1-20 chars, alphanumeric + _, -, space, .)
 * @property os Operating system name (0-20 chars, alphanumeric + _, -, space, ., optional)
 * @property installedPlugins List of installed plugins (1-200 plugins, must include "pluginator")
 */
@Serializable
data class RecommendationRequest(
    val userUuid: String,
    val ideName: String,
    val ideVersion: String,
    val os: String = "",
    val installedPlugins: List<Plugin>
)

/**
 * Response model containing plugin recommendations.
 *
 * @property id Recommendation ID
 * @property plugins List of recommended plugin names
 */
@Serializable
data class Recommendation(
    val id: Int,
    val plugins: List<String>
)

/**
 * Request model for updating recommendation feedback.
 *
 * @property plugin Name of the plugin (1-100 chars, alphanumeric + _, -, space, .)
 * @property userUuid Unique user identifier (UUID format, max 36 chars)
 * @property accepted True if user installed the plugin, false if rejected
 */
@Serializable
data class RecommendationUpdateRequest(
    val plugin: String,
    val userUuid: String,
    val accepted: Boolean
)
