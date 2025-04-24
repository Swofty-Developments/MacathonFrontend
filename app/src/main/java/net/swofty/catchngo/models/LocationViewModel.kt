package net.swofty.catchngo.models

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.categories.LocationApiCategory
import net.swofty.catchngo.services.LocationStore

/**
 * ViewModel that handles location data and nearby users.
 */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val locationApi = LocationApiCategory(application.applicationContext)

    // Store current user ID for nearby user searches
    private var userId: String? = null

    // Default radius in meters for nearby search
    private val defaultRadius = 100.0

    /* -------------------------------------------------------------------- */
    /*  LiveData – Location                                                 */
    /* -------------------------------------------------------------------- */

    // Use the shared LocationStore's Flow to get updates
    val location = LocationStore.latest

    /* -------------------------------------------------------------------- */
    /*  LiveData – Nearby Users                                             */
    /* -------------------------------------------------------------------- */

    private val _nearbyUsers = MutableLiveData<NearbyUsersState>(NearbyUsersState.Initial)
    val nearbyUsers: LiveData<NearbyUsersState> = _nearbyUsers

    // Job for controlling the periodic nearby users fetch
    private var nearbyUsersJob: Job? = null

    /* -------------------------------------------------------------------- */
    /*  Initialization                                                      */
    /* -------------------------------------------------------------------- */

    init {
        // Set up location collection and automatic uploads
        viewModelScope.launch {
            location.collectLatest { location ->
                location?.let { uploadLocation(it) }
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Public API                                                          */
    /* -------------------------------------------------------------------- */

    /**
     * Set the current user ID and start watching for nearby users.
     */
    fun setUserId(id: String) {
        userId = id
        startNearbyUsersWatch()
    }

    /**
     * Start periodic fetching of nearby users.
     */
    fun startNearbyUsersWatch(radiusMeters: Double = defaultRadius) {
        // Cancel any existing job
        nearbyUsersJob?.cancel()

        // Start a new job to fetch nearby users periodically
        nearbyUsersJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                fetchNearbyUsers(radiusMeters)
                kotlinx.coroutines.delay(30000) // 30 seconds between updates
            }
        }
    }

    /**
     * Stop watching for nearby users.
     */
    fun stopNearbyUsersWatch() {
        nearbyUsersJob?.cancel()
        nearbyUsersJob = null
    }

    /**
     * Manually trigger a fetch of nearby users.
     */
    fun refreshNearbyUsers(radiusMeters: Double = defaultRadius) {
        viewModelScope.launch(Dispatchers.IO) {
            fetchNearbyUsers(radiusMeters)
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Private helpers                                                     */
    /* -------------------------------------------------------------------- */

    /**
     * Upload the location to the server.
     */
    private suspend fun uploadLocation(location: Location): Boolean {
        return try {
            locationApi.uploadLocation(location)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Fetch nearby users within the specified radius.
     */
    private suspend fun fetchNearbyUsers(radiusMeters: Double) {
        val id = userId ?: return

        _nearbyUsers.postValue(NearbyUsersState.Loading)

        try {
            val users = locationApi.fetchNearbyUsers(id, radiusMeters)
            _nearbyUsers.postValue(NearbyUsersState.Success(users))
        } catch (e: Exception) {
            _nearbyUsers.postValue(NearbyUsersState.Error(e.message ?: "Unknown error"))
        }
    }

    /* -------------------------------------------------------------------- */
    /*  UI-friendly sealed classes                                          */
    /* -------------------------------------------------------------------- */

    sealed class NearbyUsersState {
        object Initial : NearbyUsersState()
        object Loading : NearbyUsersState()
        data class Success(val users: List<ApiModels.NearbyUser>) : NearbyUsersState()
        data class Error(val message: String) : NearbyUsersState()
    }
}