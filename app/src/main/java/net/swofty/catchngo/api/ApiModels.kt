package net.swofty.catchngo.api

/**
 * Data models for API requests and responses
 */
object ApiModels {
    /**
     * Login response model
     */
    data class LoginResponse(
        val status: String,
        val authenticationCookie: String?
    ) {
        fun isSuccess(): Boolean = "success" == status
    }

    /**
     * Register response model
     */
    data class RegisterResponse(
        val status: String,
        val authenticationCookie: String?,
        val reason: String?
    ) {
        fun isSuccess(): Boolean = "success" == status
    }

    /**
     * Question model for registration personality questions
     */
    data class Question(
        val id: Int,
        val questionText: String
    )

    /**
     * Question and answer pair for registration
     */
    data class QuestionAnswer(
        val id: Int,
        val answer: String
    )
}