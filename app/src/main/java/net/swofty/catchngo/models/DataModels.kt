package net.swofty.catchngo.models

import androidx.annotation.DrawableRes

/**
 * Models for the application UI
 */

/**
 * Nearby player that can be caught
 */
data class NearbyPlayer(
    val id: String,
    val username: String,
    val distanceMeters: Int
)

/**
 * User profile basic information
 */
data class UserProfile(
    val username: String,
    val score: Int,
    val level: Int
)