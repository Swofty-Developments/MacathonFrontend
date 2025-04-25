// ────────────────────────────────────────────────────────────────────────────────
// ImageViewModel.kt
// ────────────────────────────────────────────────────────────────────────────────
package net.swofty.catchngo.models

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.swofty.catchngo.api.categories.ImageApiCategory

/**
 * ViewModel that coordinates profile-picture operations and exposes simple UI
 * state flows for Compose / XML views.
 */
class ImageViewModel(application: Application) : AndroidViewModel(application) {

    private val imageApi = ImageApiCategory(application.applicationContext)

    /* -------------------------------------------------------------------- */
    /*  LiveData – Download                                                 */
    /* -------------------------------------------------------------------- */

    private val _downloadState = MutableLiveData<DownloadState>(DownloadState.Initial)
    val downloadState: LiveData<DownloadState> = _downloadState

    /* -------------------------------------------------------------------- */
    /*  LiveData – Upload                                                   */
    /* -------------------------------------------------------------------- */

    private val _uploadState = MutableLiveData<UploadState>(UploadState.Initial)
    val uploadState: LiveData<UploadState> = _uploadState

    /* -------------------------------------------------------------------- */
    /*  Public helpers                                                      */
    /* -------------------------------------------------------------------- */

    /** Retrieve another player’s picture (or your own, if you pass your ID). */
    fun fetchPicture(userId: String) {
        _downloadState.value = DownloadState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val img = imageApi.getPicture(userId)
                if (img != null) {
                    _downloadState.postValue(DownloadState.Success(img))
                } else {
                    _downloadState.postValue(DownloadState.Error("No picture found"))
                }
            } catch (e: Exception) {
                _downloadState.postValue(DownloadState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /** Upload / replace **your own** profile picture. */
    fun uploadPicture(base64Image: String) {
        _uploadState.value = UploadState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val ok = imageApi.setPicture(base64Image)
                _uploadState.postValue(
                    if (ok) UploadState.Success else UploadState.Error("Upload failed")
                )
            } catch (e: Exception) {
                _uploadState.postValue(UploadState.Error(e.message ?: "Unknown error"))
            }
        }
    }

    /** Reset transient states after the UI has reacted. */
    fun resetUploadState()  { _uploadState.value  = UploadState.Initial }
    fun resetDownloadState() { _downloadState.value = DownloadState.Initial }

    /* -------------------------------------------------------------------- */
    /*  UI-friendly sealed classes                                          */
    /* -------------------------------------------------------------------- */

    sealed class DownloadState {
        object Initial : DownloadState()
        object Loading : DownloadState()
        data class Success(val base64Image: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    sealed class UploadState {
        object Initial : UploadState()
        object Loading : UploadState()
        object Success : UploadState()
        data class Error(val message: String) : UploadState()
    }
}
