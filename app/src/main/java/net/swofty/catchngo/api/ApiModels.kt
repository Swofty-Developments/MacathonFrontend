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
}
