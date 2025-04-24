package net.swofty.catchngo.api

sealed class ApiResponse {

    data class Success(val data: String) : ApiResponse()

    data class Error(val code: Int, val message: String) : ApiResponse()

    data class Exception(val exception: Throwable) : ApiResponse() {
        val message: String get() = exception.message ?: "Unknown error"
    }

    fun <T> toResult(mapper: (String) -> T): Result<T> = when (this) {
        is Success -> Result.success(mapper(data))
        is Error -> Result.failure(java.lang.Exception("HTTP $code – $message"))
        is Exception -> Result.failure(exception)
    }
}
