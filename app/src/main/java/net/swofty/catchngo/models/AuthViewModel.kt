package net.swofty.catchngo.models

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.categories.AuthApiCategory

/**
 * ViewModel that coordinates authentication calls and exposes simple UI state.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authApi = AuthApiCategory(application.applicationContext)

    /* -------------------------------------------------------------------- */
    /*  LiveData – Login                                                    */
    /* -------------------------------------------------------------------- */

    private val _loginState = MutableLiveData<LoginState>(LoginState.Initial)
    val loginState: LiveData<LoginState> = _loginState

    /* -------------------------------------------------------------------- */
    /*  LiveData – Register                                                 */
    /* -------------------------------------------------------------------- */

    private val _registerState = MutableLiveData<RegisterState>(RegisterState.Initial)
    val registerState: LiveData<RegisterState> = _registerState

    init {
        // If we already have a bearer token cached, treat the user as logged-in.
        if (authApi.isLoggedIn()) {
            _loginState.value = LoginState.Success
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Public API                                                          */
    /* -------------------------------------------------------------------- */

    fun login(username: String, password: String) {
        _loginState.value = LoginState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Will throw if credentials are wrong or connection fails.
                authApi.login(username, password)
                _loginState.postValue(LoginState.Success)
            } catch (e: Exception) {
                _loginState.postValue(LoginState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun register(
        username: String,
        password: String,
        questions: List<ApiModels.QuestionAnswer>
    ) {
        _registerState.value = RegisterState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = authApi.register(username, password, questions)
                if (res.ok) {
                    _registerState.postValue(RegisterState.Success)
                } else {
                    _registerState.postValue(
                        RegisterState.Error(res.reason ?: "Registration failed")
                    )
                }
            } catch (e: Exception) {
                _registerState.postValue(RegisterState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            authApi.logout()       // also clears token locally
            _loginState.postValue(LoginState.Initial)
        }
    }

    /* -------------------------------------------------------------------- */
    /*  UI-friendly sealed classes                                          */
    /* -------------------------------------------------------------------- */

    sealed class LoginState {
        object Initial : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    sealed class RegisterState {
        object Initial : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    sealed class QuestionsState {
        object Loading : QuestionsState()
        data class Success(val questions: List<ApiModels.Question>) : QuestionsState()
        data class Error(val message: String) : QuestionsState()
    }
}
