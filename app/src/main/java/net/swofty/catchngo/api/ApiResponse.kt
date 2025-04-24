package net.swofty.catchngo.api

/**
 * Sealed class for API responses to handle different result types
 */
sealed class ApiResponse {
    /**
     * Successful API response
     */
    data class Success(val data: String) : ApiResponse()

    /**
     * Error response from server
     */
    data class Error(val code: Int, val message: String) : ApiResponse()

    /**
     * Exception occurred during API call
     */
    data class Exception(val exception: Throwable) : ApiResponse() {
        val message: String
            get() = exception.message ?: "Unknown error"
    }

    /**
     * Convert ApiResponse to a Result object for easier use with Kotlin's Result pattern
     */
    fun <T> toResult(transform: (String) -> T): Result<T> {
        return when (this) {
            is Success -> {
                Result.success(transform(data))
            }
            is Error -> Result.failure(java.lang.Exception("HTTP Error: $code - $message"))
            is Exception -> Result.failure(exception)
        }
    }
}