// ────────────────────────────────────────────────────────────────────────────────
// AuthApiCategory.kt
// ────────────────────────────────────────────────────────────────────────────────
package net.swofty.catchngo.api.categories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.delay
import net.swofty.catchngo.api.ApiManager
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.ApiResponse
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Handles all /auth-related endpoints.
 */
class AuthApiCategory(context: Context) {

    private val api = ApiManager.getInstance(context)

    /* -------------------------------------------------------------------- */
    /*  Helpers: safely unwrap “status / data” or fall back to flat JSON    */
    /* -------------------------------------------------------------------- */

    /** Returns the *payload* object, after verifying `status == success`. */
    private fun unwrap(json: JSONObject): JSONObject {
        val status = json.optString("status", "success")
        if (status != "success") error("Server responded with status=$status")

        // If there’s a {data:{…}} block, use it; otherwise keep the root
        return json.optJSONObject("data") ?: json
    }

    /* -------------------------------------------------------------------- */
    /*  POST /auth/login  → bearer token                                    */
    /* -------------------------------------------------------------------- */

    suspend fun login(
        username: String,
        password: String
    ): ApiModels.TokenResponse {

        val res = api.postMultipart(
            "/auth/login",
            mapOf(
                "grant_type" to "password",
                "username"   to username,
                "password"   to password
            )
        )

        Log.i("AuthApi", "Login response → $res")

        val result = when (res) {
            is ApiResponse.Success -> {
                val data   = unwrap(JSONObject(res.data))
                val token  = data.getString("access_token")
                val type   = data.optString("token_type", "bearer")
                api.saveAccessToken(token)
                ApiModels.TokenResponse(token, type)
            }
            is ApiResponse.Error     -> error("HTTP ${res.code}: ${res.message}")
            is ApiResponse.Exception -> throw res.exception
        }

        delay(500)                       // ← 500 ms delay before returning
        return result
    }

    /* -------------------------------------------------------------------- */
    /*  POST /auth/register  → (optional) bearer token                      */
    /* -------------------------------------------------------------------- */

    suspend fun register(
        username: String,
        password: String,
        questions: List<ApiModels.QuestionAnswer>
    ): ApiModels.RegisterResponse {

        // Build bare-array JSON for the question answers
        val questionsArray = JSONArray().apply {
            questions.forEach { qa ->
                put(JSONObject().put("id", qa.id).put("answer", qa.answer))
            }
        }
        val payload = questionsArray.toString()

        // Encode username & password as query params
        val encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8)
        val encodedPass = URLEncoder.encode(password, StandardCharsets.UTF_8)
        val url         = "/auth/register?username=$encodedUser&password=$encodedPass"

        Log.i("AuthApi", "Register payload → $payload")

        val res = api.post(url, payload)
        Log.i("AuthApi", "Register response → $res")

        val result = when (res) {
            is ApiResponse.Success -> {
                ApiModels.RegisterResponse(ok = true)
            }
            is ApiResponse.Error     -> ApiModels.RegisterResponse(false, reason = res.message)
            is ApiResponse.Exception -> throw res.exception
        }
        return result
    }

    /* -------------------------------------------------------------------- */
    /*  POST /auth/me  → profile JSON                                       */
    /* -------------------------------------------------------------------- */

    suspend fun me(): JSONObject {
        val res = api.post("/auth/me")
        return when (res) {
            is ApiResponse.Success   -> unwrap(JSONObject(res.data))
            is ApiResponse.Error     -> error("HTTP ${res.code}: ${res.message}")
            is ApiResponse.Exception -> throw res.exception
        }
    }

    /* -------------------------------------------------------------------- */
    /*  DELETE /auth/  → logout                                             */
    /* -------------------------------------------------------------------- */

    suspend fun logout(): Boolean {
        val ok = when (api.delete("/auth/")) {
            is ApiResponse.Success -> true
            else                   -> false
        }
        api.clearAccessToken()
        return ok
    }

    fun isLoggedIn(): Boolean = api.getAccessToken()?.isNotEmpty() == true
}
