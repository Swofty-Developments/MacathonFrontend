package net.swofty.catchngo.api.categories

import android.content.Context
import net.swofty.catchngo.api.ApiManager
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.ApiResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Handles the /questions endpoint.
 */
class QuestionApiCategory(context: Context) {

    private val api = ApiManager.getInstance(context)

    /**
     * GET /questions
     *
     * Accepts either of these payloads:
     *   • [{…}, {…}]                                   // raw array
     *   • {"status":"success","questions":[{…}, …]}    // wrapped object
     *
     * @return list of Question objects (throws on error)
     */
    suspend fun fetchQuestions(): List<ApiModels.Question> {
        return when (val res = api.get("/questions")) {
            is ApiResponse.Success -> parseQuestionsJson(res.data)
            is ApiResponse.Error   -> error("HTTP ${res.code}: ${res.message}")
            is ApiResponse.Exception -> throw res.exception
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Private helpers                                                     */
    /* -------------------------------------------------------------------- */

    /**
     * Tries to read the payload first as a wrapped object, then falls back to
     * a raw array.  Throws if neither format matches.
     */
    private fun parseQuestionsJson(payload: String): List<ApiModels.Question> {
        // Attempt 1: {"status":"success","questions":[…]}
        try {
            val root   = JSONObject(payload)
            val status = root.optString("status", "success")
            if (status != "success") error("Server responded with $status")

            val questionsArray = root.getJSONArray("data")
            return parseArray(questionsArray)
        } catch (_: JSONException) {
            // Not an object wrapper – fall through to raw array handling
        }

        // Attempt 2: [{…}, {…}]
        try {
            val questionsArray = JSONArray(payload)
            return parseArray(questionsArray)
        } catch (ex: JSONException) {
            // Still nothing we understand → propagate a clear error
            throw IllegalStateException(
                "Unsupported /questions JSON format: ${ex.localizedMessage}", ex
            )
        }
    }

    /** Converts a JSONArray → Kotlin List<ApiModels.Question> */
    private fun parseArray(array: JSONArray): List<ApiModels.Question> =
        List(array.length()) { idx ->
            val obj = array.getJSONObject(idx)
            ApiModels.Question(
                id           = obj.getInt("id"),
                questionText = obj.getString("questionText")
            )
        }
}
