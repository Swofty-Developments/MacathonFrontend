package net.swofty.catchngo.models

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.lifecycle.*
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

    // Default radius (metres) sent to the backend
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
            location.collectLatest { loc ->
                loc?.let { uploadLocation(it) }
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Public API                                                          */
    /* -------------------------------------------------------------------- */

    /**  Set the current user ID and begin polling for nearby players.  */
    fun setUserId(id: String) {
        if (id == userId) return
        userId = id
    }

    /**  Start periodic fetching of nearby users.  */
    fun startNearbyUsersWatch(radiusMeters: Double = defaultRadius) {
        // Cancel any existing job
        nearbyUsersJob?.cancel()

        nearbyUsersJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                fetchNearbyUsers(radiusMeters)
                kotlinx.coroutines.delay(500) // every 0.5s
            }
        }
    }

    /**  Stop polling for nearby users.  */
    fun stopNearbyUsersWatch() {
        nearbyUsersJob?.cancel()
        nearbyUsersJob = null
    }

    /**  Manually trigger a fetch right now.  */
    fun refreshNearbyUsers(radiusMeters: Double = defaultRadius) {
        viewModelScope.launch(Dispatchers.IO) { fetchNearbyUsers(radiusMeters) }
    }

    /* -------------------------------------------------------------------- */
    /*  Private helpers                                                     */
    /* -------------------------------------------------------------------- */

    /**  Upload the device’s current location to the server.  */
    private suspend fun uploadLocation(location: Location): Boolean = try {
        locationApi.uploadLocation(location)
    } catch (e: Exception) {
        false
    }

    /**  Fetch nearby users within the specified radius.  */
    private suspend fun fetchNearbyUsers(radiusMeters: Double) {
        val id = userId ?: return
        _nearbyUsers.postValue(NearbyUsersState.Loading)

        try {
            val users = locationApi.fetchNearbyUsers(id, radiusMeters)
            Log.i("LocationViewModel", "Fetched ${users.size} nearby users ${users}")
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
