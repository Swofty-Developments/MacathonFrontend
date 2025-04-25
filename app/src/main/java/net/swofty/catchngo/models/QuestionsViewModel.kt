package net.swofty.catchngo.models

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.categories.QuestionApiCategory

class QuestionsViewModel(application: Application) : AndroidViewModel(application) {

    private val questionsApi = QuestionApiCategory(application.applicationContext)

    // ─── states for loading the questions ───────────────────────────────
    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    // ─── states for validating the answers ──────────────────────────────
    private val _validation = MutableLiveData<ValidationState>(ValidationState.Idle)
    val validation: LiveData<ValidationState> = _validation

    // ─── states for MCQ questions ───────────────────────────────────────
    private val _mcqState = MutableLiveData<McqState>(McqState.Loading)
    val mcqState: LiveData<McqState> = _mcqState

    // ─── states for validating MCQ answers ────────────────────────────────
    private val _mcqValidation = MutableLiveData<McqValidationState>(McqValidationState.Idle)
    val mcqValidation: LiveData<McqValidationState> = _mcqValidation

    /** Refresh the question list **/
    fun refresh() {
        _state.value = State.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = questionsApi.fetchQuestions()
                _state.postValue(State.Success(list))
            } catch (e: Exception) {
                _state.postValue(State.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Fetch MCQ questions for a specific user.
     * @param userId The ID of the user to fetch questions about
     */
    fun fetchMcqQuestions(userId: String) {
        _mcqState.value = McqState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val questions = questionsApi.fetchMcqQuestions(userId)
                _mcqState.postValue(McqState.Success(questions))
            } catch (e: Exception) {
                _mcqState.postValue(McqState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Validate multiple-choice question answers for a friend.
     * @param userId The ID of the friend being quizzed about
     * @param answers List of selected option IDs corresponding to each question
     */
    fun validateMcqAnswers(userId: String, answers: List<Int>) {
        _mcqValidation.value = McqValidationState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allCorrect = questionsApi.validateAnswers(userId, answers)
                _mcqValidation.postValue(McqValidationState.Success(allCorrect))
            } catch (e: Exception) {
                _mcqValidation.postValue(McqValidationState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Reset MCQ validation state (e.g., when starting a new quiz)
     */
    fun resetMcqValidation() {
        _mcqValidation.value = McqValidationState.Idle
    }

    // ────────────────────────────────────────────────────────────────────
    // States for regular questions
    sealed class State {
        object Loading : State()
        data class Success(val questions: List<ApiModels.Question>) : State()
        data class Error(val message: String) : State()
    }

    sealed class ValidationState {
        object Idle    : ValidationState()
        object Loading : ValidationState()
        data class Success(val allCorrect: Boolean) : ValidationState()
        data class Error(val message: String)      : ValidationState()
    }

    // States for MCQ questions
    sealed class McqState {
        object Loading : McqState()
        data class Success(val questions: List<ApiModels.McqQuestion>) : McqState()
        data class Error(val message: String) : McqState()
    }

    sealed class McqValidationState {
        object Idle    : McqValidationState()
        object Loading : McqValidationState()
        data class Success(val allCorrect: Boolean) : McqValidationState()
        data class Error(val message: String)      : McqValidationState()
    }
}