package net.swofty.catchngo.models

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.categories.FriendexApiCategory

/**
 * ViewModel that coordinates Friendex operations and exposes UI state.
 */
class FriendexViewModel(application: Application) : AndroidViewModel(application) {

    private val friendexApi = FriendexApiCategory(application.applicationContext)

    /* -------------------------------------------------------------------- */
    /*  StateFlow – Selection Status (for real-time tracking)                */
    /* -------------------------------------------------------------------- */

    private val _selectionStatusFlow = MutableStateFlow<ApiModels.SelectionStatus?>(null)
    val selectionStatusFlow = _selectionStatusFlow.asStateFlow()

    private var selectionPollingActive = false

    /* -------------------------------------------------------------------- */
    /*  LiveData – User Entry                                               */
    /* -------------------------------------------------------------------- */

    private val _userEntryState = MutableLiveData<UserEntryState>(UserEntryState.Initial)
    val userEntryState: LiveData<UserEntryState> = _userEntryState

    /* -------------------------------------------------------------------- */
    /*  LiveData – User Friends                                             */
    /* -------------------------------------------------------------------- */

    private val _friendsState = MutableLiveData<FriendsState>(FriendsState.Initial)
    val friendsState: LiveData<FriendsState> = _friendsState

    /* -------------------------------------------------------------------- */
    /*  LiveData – Unmet Players                                            */
    /* -------------------------------------------------------------------- */

    private val _unmetPlayersState = MutableLiveData<UnmetPlayersState>(UnmetPlayersState.Initial)
    val unmetPlayersState: LiveData<UnmetPlayersState> = _unmetPlayersState

    /* -------------------------------------------------------------------- */
    /*  LiveData – Select User                                              */
    /* -------------------------------------------------------------------- */

    private val _selectUserState = MutableLiveData<SelectUserState>(SelectUserState.Initial)
    val selectUserState: LiveData<SelectUserState> = _selectUserState

    /* -------------------------------------------------------------------- */
    /*  LiveData – Deselect User                                            */
    /* -------------------------------------------------------------------- */

    private val _deselectUserState = MutableLiveData<DeselectUserState>(DeselectUserState.Initial)
    val deselectUserState: LiveData<DeselectUserState> = _deselectUserState

    /* -------------------------------------------------------------------- */
    /*  LiveData – Add Friend                                               */
    /* -------------------------------------------------------------------- */

    private val _addFriendState = MutableLiveData<AddFriendState>(AddFriendState.Initial)
    val addFriendState: LiveData<AddFriendState> = _addFriendState

    /* -------------------------------------------------------------------- */
    /*  Public API                                                          */
    /* -------------------------------------------------------------------- */

    /**
     * Start polling the selection status at regular intervals.
     * This enables real-time tracking updates.
     */
    fun startSelectionStatusPolling() {
        if (selectionPollingActive) return

        selectionPollingActive = true
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive && selectionPollingActive) {
                try {
                    val status = friendexApi.checkSelection()
                    Log.i("FriendexViewModel", "Polling selection status → $status")
                    _selectionStatusFlow.value = status
                } catch (e: Exception) {
                    // Just continue polling on error
                }
                delay(1000) // Poll every 2 seconds
            }
        }
    }

    /**
     * Stop polling the selection status.
     */
    fun stopSelectionStatusPolling() {
        selectionPollingActive = false
    }

    /**
     * Check selection status once (without starting the continuous polling).
     */
    fun checkSelectionStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = friendexApi.checkSelection()
                _selectionStatusFlow.value = status
            } catch (e: Exception) {
                // Silently fail, the UI can handle null status
            }
        }
    }

    /**
     * Fetch user entry information.
     *
     * @param userId The ID of the user to fetch
     */
    fun getUserEntry(userId: String) {
        _userEntryState.value = UserEntryState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userEntry = friendexApi.getUserEntry(userId)
                if (userEntry != null) {
                    _userEntryState.postValue(UserEntryState.Success(userEntry))
                } else {
                    _userEntryState.postValue(UserEntryState.Error("Failed to load user entry"))
                }
            } catch (e: Exception) {
                _userEntryState.postValue(UserEntryState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Fetch user's friends list.
     *
     * @param userId The ID of the user to fetch friends for
     */
    fun getUserFriends(userId: String) {
        _friendsState.value = FriendsState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val friends = friendexApi.getUserFriends(userId)
                _friendsState.postValue(FriendsState.Success(friends))
            } catch (e: Exception) {
                _friendsState.postValue(FriendsState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Fetch unmet players list.
     *
     * @param userId The ID of the user to fetch unmet players for
     */
    fun getUnmetPlayers(userId: String) {
        _unmetPlayersState.value = UnmetPlayersState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val players = friendexApi.getUnmetPlayers(userId)
                _unmetPlayersState.postValue(UnmetPlayersState.Success(players))
            } catch (e: Exception) {
                _unmetPlayersState.postValue(UnmetPlayersState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Select a user as a target.
     *
     * @param userId The ID of the user to select
     */
    fun selectUser(userId: String) {
        _selectUserState.value = SelectUserState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = friendexApi.selectUser(userId)
                if (success) {
                    _selectUserState.postValue(SelectUserState.Success)
                } else {
                    _selectUserState.postValue(SelectUserState.Error("Failed to select user"))
                }
            } catch (e: Exception) {
                _selectUserState.postValue(SelectUserState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Deselect the currently selected user.
     */
    fun deselectUser() {
        _deselectUserState.value = DeselectUserState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = friendexApi.deselectUser()
                if (success) {
                    _deselectUserState.postValue(DeselectUserState.Success)
                    _selectionStatusFlow.value = null
                } else {
                    _deselectUserState.postValue(DeselectUserState.Error("Failed to deselect user"))
                }
            } catch (e: Exception) {
                _deselectUserState.postValue(DeselectUserState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Add a user as a friend.
     *
     * @param friendId The ID of the user to add as a friend
     */
    fun addFriend(friendId: String) {
        _addFriendState.value = AddFriendState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val success = friendexApi.addFriend(friendId)
                if (success) {
                    _addFriendState.postValue(AddFriendState.Success)
                } else {
                    _addFriendState.postValue(AddFriendState.Error("Failed to add friend"))
                }
            } catch (e: Exception) {
                _addFriendState.postValue(AddFriendState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Reset states to initial.
     */
    fun resetStates() {
        _selectUserState.value = SelectUserState.Initial
        _deselectUserState.value = DeselectUserState.Initial
        _addFriendState.value = AddFriendState.Initial
    }

    /**
     * Clean up resources when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        stopSelectionStatusPolling()
    }

    /* -------------------------------------------------------------------- */
    /*  UI-friendly sealed classes                                          */
    /* -------------------------------------------------------------------- */

    sealed class UserEntryState {
        object Initial : UserEntryState()
        object Loading : UserEntryState()
        data class Success(val userEntry: ApiModels.UserEntry) : UserEntryState()
        data class Error(val message: String) : UserEntryState()
    }

    sealed class FriendsState {
        object Initial : FriendsState()
        object Loading : FriendsState()
        data class Success(val friends: List<String>) : FriendsState()
        data class Error(val message: String) : FriendsState()
    }

    sealed class UnmetPlayersState {
        object Initial : UnmetPlayersState()
        object Loading : UnmetPlayersState()
        data class Success(val players: List<String>) : UnmetPlayersState()
        data class Error(val message: String) : UnmetPlayersState()
    }

    sealed class SelectUserState {
        object Initial : SelectUserState()
        object Loading : SelectUserState()
        object Success : SelectUserState()
        data class Error(val message: String) : SelectUserState()
    }

    sealed class DeselectUserState {
        object Initial : DeselectUserState()
        object Loading : DeselectUserState()
        object Success : DeselectUserState()
        data class Error(val message: String) : DeselectUserState()
    }

    sealed class AddFriendState {
        object Initial : AddFriendState()
        object Loading : AddFriendState()
        object Success : AddFriendState()
        data class Error(val message: String) : AddFriendState()
    }
}