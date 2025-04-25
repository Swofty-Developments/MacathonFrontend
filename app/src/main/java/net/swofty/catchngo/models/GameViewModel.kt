// ────────────────────────────────────────────────────────────────────────────────
// GameViewModel.kt   –  observes LocationStore.latest
// ────────────────────────────────────────────────────────────────────────────────
package net.swofty.catchngo.models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.categories.AuthApiCategory
import net.swofty.catchngo.services.LocationStore
import org.json.JSONObject

/**
 * Fetches user profile (/auth/me) and exposes live GPS fixes via LocationStore.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val authApi = AuthApiCategory(application.applicationContext)

    private val _profile = kotlinx.coroutines.flow.MutableStateFlow<JSONObject?>(null)
    val profile: kotlinx.coroutines.flow.StateFlow<JSONObject?> = _profile

    /** Live location supplied by LocationTrackingService */
    val location: StateFlow<android.location.Location?> = LocationStore.latest

    init {
        refreshProfile()
    }

    /** Re-query /auth/me */
    fun refreshProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _profile.value = authApi.me()
                Log.i("Test", authApi.me().toString())
            } catch (e: Exception) {
                _profile.value = JSONObject().put("error", e.message ?: "Unknown error")
            }
        }
    }

    /* Handy getters --------------------------------------------------------- */
    fun getUsername(): String = _profile.value?.optString("name") ?: ""
    fun getPoints()  : Int    = _profile.value?.optInt("points") ?: 0
    fun getUserId()  : String = _profile.value?.optString("_id") ?: ""
}
