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
}