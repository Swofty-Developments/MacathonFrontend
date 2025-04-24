package net.swofty.catchngo.models

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.categories.QuestionApiCategory

/**
 * ViewModel that exposes registration-question data to the UI.
 */
class QuestionsViewModel(application: Application) : AndroidViewModel(application) {

    private val questionsApi = QuestionApiCategory(application.applicationContext)

    /* -------------------------------------------------------------------- */
    /*  LiveData for UI                                                     */
    /* -------------------------------------------------------------------- */

    private val _state = MutableLiveData<State>(State.Loading)
    val state: LiveData<State> = _state

    /* -------------------------------------------------------------------- */
    /*  Public helpers                                                      */
    /* -------------------------------------------------------------------- */

    /** Call this from the UI to (re)load questions. */
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

    /* -------------------------------------------------------------------- */
    /*  UI-friendly sealed state                                            */
    /* -------------------------------------------------------------------- */

    sealed class State {
        object Loading : State()
        data class Success(val questions: List<ApiModels.Question>) : State()
        data class Error(val message: String) : State()
    }
}
