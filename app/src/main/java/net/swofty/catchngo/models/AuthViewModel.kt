package net.swofty.catchngo.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.categories.AuthApiCategory

/**
 * ViewModel that handles authentication operations
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authApi = AuthApiCategory(application.applicationContext)

    // Login state
    private val _loginState = MutableLiveData<LoginState>(LoginState.Initial)
    val loginState: LiveData<LoginState> = _loginState

    // Register state
    private val _registerState = MutableLiveData<RegisterState>(RegisterState.Initial)
    val registerState: LiveData<RegisterState> = _registerState

    // Questions state
    private val _questionsState = MutableLiveData<QuestionsState>(QuestionsState.Loading)
    val questionsState: LiveData<QuestionsState> = _questionsState

    init {
        // Check if already logged in
        if (authApi.isLoggedIn()) {
            _loginState.value = LoginState.Success
        }
    }

    /**
     * Attempt to login with provided credentials
     */
    fun login(username: String, password: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = authApi.login(username, password)

                if (response.isSuccess()) {
                    _loginState.postValue(LoginState.Success)
                } else {
                    _loginState.postValue(LoginState.Error("Login failed: Invalid credentials"))
                }
            } catch (e: Exception) {
                _loginState.postValue(LoginState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Register a new user
     */
    fun register(username: String, password: String, questions: List<ApiModels.QuestionAnswer>) {
        _registerState.value = RegisterState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = authApi.register(username, password, questions)

                if (response.isSuccess()) {
                    _registerState.postValue(RegisterState.Success)
                } else {
                    val reason = response.reason ?: "Registration failed"
                    _registerState.postValue(RegisterState.Error(reason))
                }
            } catch (e: Exception) {
                _registerState.postValue(RegisterState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Fetch registration questions
     */
    fun fetchQuestions() {
        _questionsState.value = QuestionsState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val questions = authApi.fetchQuestions()
                _questionsState.postValue(QuestionsState.Success(questions))
            } catch (e: Exception) {
                _questionsState.postValue(QuestionsState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Logout the current user
     */
    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            authApi.logout()
            _loginState.postValue(LoginState.Initial)
        }
    }

    /**
     * Kotlin-friendly login state sealed class
     */
    sealed class LoginState {
        object Initial : LoginState()
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    /**
     * Kotlin-friendly register state sealed class
     */
    sealed class RegisterState {
        object Initial : RegisterState()
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    /**
     * Kotlin-friendly questions state sealed class
     */
    sealed class QuestionsState {
        object Loading : QuestionsState()
        data class Success(val questions: List<ApiModels.Question>) : QuestionsState()
        data class Error(val message: String) : QuestionsState()
    }
}