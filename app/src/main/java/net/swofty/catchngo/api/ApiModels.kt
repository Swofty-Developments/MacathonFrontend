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

        /**
         * Check if registration failed because username already exists
         */
        fun isUsernameTaken(): Boolean =
            !isSuccess() && "USERNAME_EXISTS" == reason
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

    /**
     * User profile model
     */
    data class UserProfile(
        val username: String,
        val score: Int,
        val level: Int,
        val achievements: List<Achievement>
    )

    /**
     * Achievement model
     */
    data class Achievement(
        val id: Int,
        val name: String,
        val description: String,
        val points: Int,
        val unlocked: Boolean,
        val unlockedDate: String?
    )

    /**
     * Leaderboard entry model
     */
    data class LeaderboardEntry(
        val username: String,
        val score: Int,
        val position: Int
    )

    /**
     * Friend model for FriendDex feature
     */
    data class Friend(
        val username: String,
        val closenessLevel: Int,
        val lastCaptured: String?,
        val captureCount: Int,
        val answeredQuestions: List<QuestionAnswer>
    )
}