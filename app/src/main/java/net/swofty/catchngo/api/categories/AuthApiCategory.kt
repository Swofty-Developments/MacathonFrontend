package net.swofty.catchngo.api.categories

import android.content.Context
import net.swofty.catchngo.api.ApiManager
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.ApiResponse
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles authentication-related API endpoints
 */
class AuthApiCategory(context: Context) {
    private val apiManager = ApiManager.getInstance(context)

    /**
     * Login with username and password
     *
     * @param username User's username
     * @param password User's password
     * @return LoginResponse with result
     * @throws Exception if login fails
     */
    suspend fun login(username: String, password: String): ApiModels.LoginResponse {
        val headers = mapOf(
            "X-Auth-Username" to username,
            "X-Auth-Password" to password
        )

        val response = apiManager.post("/auth/login", headers, null)

        return when (response) {
            is ApiResponse.Success -> {
                val jsonResponse = JSONObject(response.data)
                val status = jsonResponse.getString("status")
                val authCookie = jsonResponse.optString("authenticationCookie", null.toString())

                if ("success" == status && !authCookie.isNullOrEmpty()) {
                    apiManager.saveAuthCookie(authCookie)
                }

                ApiModels.LoginResponse(status, authCookie)
            }
            is ApiResponse.Error -> {
                throw Exception("HTTP Error: ${response.code} - ${response.message}")
            }
            is ApiResponse.Exception -> {
                throw response.exception
            }
        }
    }

    /**
     * Register a new user
     *
     * @param username New username
     * @param password New password
     * @param questions List of personality questions and answers
     * @return RegisterResponse with result
     * @throws Exception if registration fails
     */
    suspend fun register(
        username: String,
        password: String,
        questions: List<ApiModels.QuestionAnswer>
    ): ApiModels.RegisterResponse {
        val headers = mutableMapOf(
            "X-Auth-Username" to username,
            "X-Auth-Password" to password
        )

        // Create questions JSON
        val questionsJson = JSONObject()
        val questionsArray = JSONArray()

        for (question in questions) {
            val questionJson = JSONObject()
            questionJson.put("id", question.id)
            questionJson.put("answer", question.answer)
            questionsArray.put(questionJson)
        }

        questionsJson.put("questions", questionsArray)
        headers["X-Auth-Questions"] = questionsJson.toString()

        val response = apiManager.post("/auth/register", headers, null)

        return when (response) {
            is ApiResponse.Success -> {
                val jsonResponse = JSONObject(response.data)
                val status = jsonResponse.getString("status")
                val authCookie = jsonResponse.optString("authenticationCookie", null)
                val reason = jsonResponse.optString("reason", null)

                if ("success" == status && !authCookie.isNullOrEmpty()) {
                    apiManager.saveAuthCookie(authCookie)
                }

                ApiModels.RegisterResponse(status, authCookie, reason)
            }
            is ApiResponse.Error -> {
                throw Exception("HTTP Error: ${response.code} - ${response.message}")
            }
            is ApiResponse.Exception -> {
                throw response.exception
            }
        }
    }

    /**
     * Fetch personality questions for registration
     *
     * @return List of questions
     * @throws Exception if fetching questions fails
     */
    suspend fun fetchQuestions(): List<ApiModels.Question> {
        val response = apiManager.get("/questions/fetch", null)

        return when (response) {
            is ApiResponse.Success -> {
                val jsonResponse = JSONObject(response.data)

                if ("success" == jsonResponse.getString("status")) {
                    val questionsArray = jsonResponse.getJSONArray("questions")
                    val questions = mutableListOf<ApiModels.Question>()

                    for (i in 0 until questionsArray.length()) {
                        val questionObj = questionsArray.getJSONObject(i)
                        questions.add(
                            ApiModels.Question(
                                id = questionObj.getInt("id"),
                                questionText = questionObj.getString("questionText")
                            )
                        )
                    }

                    questions
                } else {
                    throw Exception("Failed to fetch questions")
                }
            }
            is ApiResponse.Error -> {
                throw Exception("HTTP Error: ${response.code} - ${response.message}")
            }
            is ApiResponse.Exception -> {
                throw response.exception
            }
        }
    }

    /**
     * Logout the current user
     *
     * @return Boolean indicating if logout was successful
     */
    suspend fun logout(): Boolean {
        val response = apiManager.delete("/auth", null)

        return when (response) {
            is ApiResponse.Success -> {
                apiManager.clearAuthCookie()
                true
            }
            else -> false
        }
    }

    /**
     * Check if user is currently logged in
     *
     * @return true if logged in, false otherwise
     */
    fun isLoggedIn(): Boolean {
        val authCookie = apiManager.getAuthCookie()
        return !authCookie.isNullOrEmpty()
    }

    /**
     * Logout locally without calling the API
     */
    fun localLogout() {
        apiManager.clearAuthCookie()
    }
}