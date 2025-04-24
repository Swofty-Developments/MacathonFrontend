package net.swofty.catchngo.models

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel for game functionality after login
 */
class GameViewModel : ViewModel() {

    // User score
    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score

    // Nearby players
    private val _nearbyPlayers = MutableStateFlow<List<NearbyPlayer>>(emptyList())
    val nearbyPlayers: StateFlow<List<NearbyPlayer>> = _nearbyPlayers

    // Location tracking toggle
    val locationTrackingEnabled = mutableStateOf(true)

    // Demo data - in a real app this would come from a location-based service
    private val samplePlayers = listOf(
        NearbyPlayer(
            id = "user1",
            username = "CoolRunner99",
            distanceMeters = 45
        ),
        NearbyPlayer(
            id = "user2",
            username = "Explorer42",
            distanceMeters = 120
        ),
        NearbyPlayer(
            id = "user3",
            username = "MapMaster",
            distanceMeters = 85
        )
    )

    init {
        // Initialize with a sample score
        _score.value = 350
    }

    /**
     * Toggle location tracking on/off
     */
    fun toggleLocationTracking() {
        locationTrackingEnabled.value = !locationTrackingEnabled.value

        // If tracking is turned off, clear nearby players
        if (!locationTrackingEnabled.value) {
            _nearbyPlayers.value = emptyList()
        }
    }

    /**
     * Scan for nearby players
     */
    fun scanForPlayers() {
        if (locationTrackingEnabled.value) {
            // Simulate finding nearby players - in a real app this would use location services
            _nearbyPlayers.value = samplePlayers.shuffled().take((1..3).random())
        }
    }

    /**
     * Catch a player
     */
    fun catchPlayer(playerId: String) {
        // Find the player
        val player = _nearbyPlayers.value.find { it.id == playerId } ?: return

        // Award points based on distance (closer = more points)
        val pointsAwarded = when {
            player.distanceMeters < 50 -> 50
            player.distanceMeters < 100 -> 30
            else -> 20
        }

        // Update score
        _score.value += pointsAwarded

        // Remove caught player from nearby list
        _nearbyPlayers.value = _nearbyPlayers.value.filter { it.id != playerId }
    }
}