package net.swofty.catchngo.api.categories

import android.content.Context
import net.swofty.catchngo.api.ApiManager
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.ApiResponse
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Handles all leaderboard-related endpoints.
 */
class LeaderboardApiCategory(context: Context) {

    private val api = ApiManager.getInstance(context)

    /**
     * GET /leaderboard?size={size}
     * Fetches the top users on the leaderboard.
     *
     * @param size The number of top users to fetch
     * @return List of LeaderboardUser objects or empty list if request failed
     */
    suspend fun getTopUsers(size: Int): List<ApiModels.LeaderboardUser> {
        return when (val res = api.get("/leaderboard?size=$size")) {
            is ApiResponse.Success -> parseLeaderboardUsers(res.data)
            is ApiResponse.Error -> emptyList()
            is ApiResponse.Exception -> throw res.exception
        }
    }

    /**
     * GET /leaderboard/rank/{user_id}
     * Gets the rank of a specific user.
     *
     * @param userId The ID of the user to get the rank for
     * @return The user's rank or null if request failed
     */
    suspend fun getUserRank(userId: String): Int? {
        return when (val res = api.get("/leaderboard/rank/$userId")) {
            is ApiResponse.Success -> {
                try {
                    val jsonObj = JSONObject(res.data)
                    val status = jsonObj.optString("status", "success")

                    if (status == "success") {
                        return jsonObj.optInt("data")
                    } else {
                        null
                    }
                } catch (e: JSONException) {
                    try {
                        // Try parsing as a simple integer response
                        return res.data.toIntOrNull()
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            is ApiResponse.Error, is ApiResponse.Exception -> null
        }
    }

    /* -------------------------------------------------------------------- */
    /*  Private helpers                                                     */
    /* -------------------------------------------------------------------- */

    /**
     * Parses the JSON response into a list of LeaderboardUser objects.
     */
    private fun parseLeaderboardUsers(json: String): List<ApiModels.LeaderboardUser> {
        val result = mutableListOf<ApiModels.LeaderboardUser>()

        try {
            // First try parsing as a wrapped object
            val jsonObj = JSONObject(json)
            val status = jsonObj.optString("status", "success")

            if (status == "success") {
                val data = jsonObj.optJSONArray("data")
                if (data != null) {
                    return parseLeaderboardArray(data)
                }
            }

            // If that didn't work, try parsing as a direct array
            val jsonArray = JSONArray(json)
            return parseLeaderboardArray(jsonArray)

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    /**
     * Parses a JSONArray of leaderboard users into a List<LeaderboardUser>.
     */
    private fun parseLeaderboardArray(array: JSONArray): List<ApiModels.LeaderboardUser> {
        val result = mutableListOf<ApiModels.LeaderboardUser>()

        for (i in 0 until array.length()) {
            try {
                val userJson = array.getJSONObject(i)
                result.add(
                    ApiModels.LeaderboardUser(
                        id = userJson.getString("id"),
                        name = userJson.getString("name"),
                        points = userJson.getInt("points")
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return result
    }
}