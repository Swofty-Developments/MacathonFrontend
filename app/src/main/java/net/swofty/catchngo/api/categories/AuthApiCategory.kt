package net.swofty.catchngo.api.categories

import android.content.Context
import android.util.Log
import net.swofty.catchngo.api.ApiManager
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.ApiResponse
import org.json.JSONArray
import org.json.JSONObject
import okhttp3.MultipartBody
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Auth-related endpoints (/auth)
**/
class AuthApiCategory(context: Context) {

    private val api = ApiManager.getInstance(context)

    /* -------------------------------------------------------------------- */
    /*  Login – returns OAuth2 bearer token                                 */
    /* -------------------------------------------------------------------- */

    suspend fun login(username: String, password: String): ApiModels.TokenResponse {
        return when (val res = api.postMultipart("/auth/login", mapOf(
                "grant_type" to "password",
                "username"   to username,
                "password"   to password
        ))) {
            is ApiResponse.Success -> {
                val json  = JSONObject(res.data)
                val token = json.getString("access_token")
                val type  = json.getString("token_type")
                api.saveAccessToken(token)
                ApiModels.TokenResponse(token, type)
            }
            is ApiResponse.Error     -> error("HTTP ${res.code}: ${res.message}")
            is ApiResponse.Exception -> throw res.exception
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Register – creates user & (optionally) returns token                */
    /* -------------------------------------------------------------------- */

    suspend fun register(
        username: String,
        password: String,
        questions: List<ApiModels.QuestionAnswer>
    ): ApiModels.RegisterResponse {

        /* Encode the questions list as a *bare JSON array*                 */
        val questionsArray = JSONArray().apply {
            questions.forEach { qa ->
                put(JSONObject().put("id", qa.id).put("answer", qa.answer))
            }
        }

        /* Request body is now JUST the array (not wrapped in an object)    */
        val payload = questionsArray.toString()

        /* Build URL with query parameters for username + password          */
        val encodedUser = URLEncoder.encode(username, StandardCharsets.UTF_8)
        val encodedPass = URLEncoder.encode(password, StandardCharsets.UTF_8)
        val url         = "/auth/register?username=$encodedUser&password=$encodedPass"

        Log.i("AuthApi", "Register payload → $payload")

        return when (val res = api.post(url, payload)) {
            is ApiResponse.Success -> {
                val json  = JSONObject(res.data.ifBlank { "{}" })
                val token = json.optString("access_token", null)
                val type  = json.optString("token_type",  null)
                token?.let { api.saveAccessToken(it) }
                ApiModels.RegisterResponse(ok = true, accessToken = token, tokenType = type)
            }
            is ApiResponse.Error     -> ApiModels.RegisterResponse(false, reason = res.message)
            is ApiResponse.Exception -> throw res.exception
        }
    }

    /* -------------------------------------------------------------------- */
    /*  /auth/me – get profile                                              */
    /* -------------------------------------------------------------------- */

    suspend fun me(): JSONObject {
        return when (val res = api.post("/auth/me")) {
            is ApiResponse.Success -> JSONObject(res.data)
            is ApiResponse.Error     -> error("HTTP ${res.code}: ${res.message}")
            is ApiResponse.Exception -> throw res.exception
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Logout (server + local)                                             */
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