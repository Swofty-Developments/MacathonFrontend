package net.swofty.catchngo.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.categories.LeaderboardApiCategory

/**
 * ViewModel that handles leaderboard data.
 */
class LeaderboardViewModel(application: Application) : AndroidViewModel(application) {

    private val leaderboardApi = LeaderboardApiCategory(application.applicationContext)

    /* -------------------------------------------------------------------- */
    /*  StateFlow – Top Users                                               */
    /* -------------------------------------------------------------------- */

    private val _topUsers = MutableStateFlow<TopUsersState>(TopUsersState.Initial)
    val topUsers: StateFlow<TopUsersState> = _topUsers

    /* -------------------------------------------------------------------- */
    /*  StateFlow – User Rank                                               */
    /* -------------------------------------------------------------------- */

    private val _userRank = MutableStateFlow<UserRankState>(UserRankState.Initial)
    val userRank: StateFlow<UserRankState> = _userRank

    /* -------------------------------------------------------------------- */
    /*  Public helpers                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * Fetch the top users on the leaderboard.
     *
     * @param size The number of top users to fetch
     */
    fun fetchTopUsers(size: Int = 10) {
        _topUsers.value = TopUsersState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val users = leaderboardApi.getTopUsers(size)
                _topUsers.value = TopUsersState.Success(users)
            } catch (e: Exception) {
                _topUsers.value = TopUsersState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Fetch the rank of a specific user.
     *
     * @param userId The ID of the user to get the rank for
     */
    fun fetchUserRank(userId: String) {
        _userRank.value = UserRankState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rank = leaderboardApi.getUserRank(userId)
                if (rank != null) {
                    _userRank.value = UserRankState.Success(rank)
                } else {
                    _userRank.value = UserRankState.Error("Could not retrieve user rank")
                }
            } catch (e: Exception) {
                _userRank.value = UserRankState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /* -------------------------------------------------------------------- */
    /*  UI-friendly sealed classes                                          */
    /* -------------------------------------------------------------------- */

    sealed class TopUsersState {
        object Initial : TopUsersState()
        object Loading : TopUsersState()
        data class Success(val users: List<ApiModels.LeaderboardUser>) : TopUsersState()
        data class Error(val message: String) : TopUsersState()
    }

    sealed class UserRankState {
        object Initial : UserRankState()
        object Loading : UserRankState()
        data class Success(val rank: Int) : UserRankState()
        data class Error(val message: String) : UserRankState()
    }
}