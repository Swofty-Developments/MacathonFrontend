package net.swofty.catchngo.models

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.categories.AchievementsApiCategory

/**
 * ViewModel that coordinates achievements operations and exposes simple UI state.
 */
class AchievementsViewModel(application: Application) : AndroidViewModel(application) {

    private val achievementsApi = AchievementsApiCategory(application.applicationContext)

    /* -------------------------------------------------------------------- */
    /*  LiveData – User Achievements                                        */
    /* -------------------------------------------------------------------- */

    private val _userAchievementsState = MutableLiveData<UserAchievementsState>(UserAchievementsState.Initial)
    val userAchievementsState: LiveData<UserAchievementsState> = _userAchievementsState

    /* -------------------------------------------------------------------- */
    /*  LiveData – Missing Achievements                                     */
    /* -------------------------------------------------------------------- */

    private val _missingAchievementsState = MutableLiveData<MissingAchievementsState>(MissingAchievementsState.Initial)
    val missingAchievementsState: LiveData<MissingAchievementsState> = _missingAchievementsState

    /* -------------------------------------------------------------------- */
    /*  Public API                                                          */
    /* -------------------------------------------------------------------- */

    /**
     * Fetch achievements the user has earned.
     */
    fun getUserAchievements() {
        _userAchievementsState.value = UserAchievementsState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val achievements = achievementsApi.getUserAchievements()
                _userAchievementsState.postValue(UserAchievementsState.Success(achievements))
            } catch (e: Exception) {
                _userAchievementsState.postValue(UserAchievementsState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Fetch achievements the user hasn't earned yet.
     */
    fun getMissingAchievements() {
        _missingAchievementsState.value = MissingAchievementsState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val achievements = achievementsApi.getMissingAchievements()
                _missingAchievementsState.postValue(MissingAchievementsState.Success(achievements))
            } catch (e: Exception) {
                _missingAchievementsState.postValue(MissingAchievementsState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Reset states to initial.
     */
    fun resetStates() {
        _userAchievementsState.value = UserAchievementsState.Initial
        _missingAchievementsState.value = MissingAchievementsState.Initial
    }

    /* -------------------------------------------------------------------- */
    /*  UI-friendly sealed classes                                          */
    /* -------------------------------------------------------------------- */

    sealed class UserAchievementsState {
        object Initial : UserAchievementsState()
        object Loading : UserAchievementsState()
        data class Success(val achievements: List<ApiModels.Achievement>) : UserAchievementsState()
        data class Error(val message: String) : UserAchievementsState()
    }

    sealed class MissingAchievementsState {
        object Initial : MissingAchievementsState()
        object Loading : MissingAchievementsState()
        data class Success(val achievements: List<ApiModels.MissingAchievement>) : MissingAchievementsState()
        data class Error(val message: String) : MissingAchievementsState()
    }
}