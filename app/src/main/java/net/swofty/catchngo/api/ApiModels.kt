package net.swofty.catchngo.api

/**
 * DTOs shared by API categories.
 */
object ApiModels {

    /* ---------- OAuth2 token ---------- */
    data class TokenResponse(
        val accessToken: String,
        val tokenType: String
    )

    /* ---------- Registration ---------- */
    data class RegisterResponse(
        val ok: Boolean,
        val accessToken: String? = null,
        val tokenType: String? = null,
        val reason: String? = null
    )

    /* ---------- Questions ------------- */
    data class Question(val id: Int, val questionText: String)
    data class QuestionAnswer(val id: Int, val answer: String)

    /**
     * A user nearby the current user.
     */
    data class NearbyUser(
        val id: String,
        val name: String,
        val points: Int,
        val disabled: Boolean,
        val questions: List<QuestionAnswer>,
        val friends: List<String>,
        val selectedFriend: String?,
        val latitude: Double,
        val longitude: Double
    ) {
        /**
         * Calculate distance to another location in meters.
         */
        fun distanceTo(otherLat: Double, otherLng: Double): Float {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                latitude, longitude,
                otherLat, otherLng,
                results
            )
            return results[0]
        }
    }
}
