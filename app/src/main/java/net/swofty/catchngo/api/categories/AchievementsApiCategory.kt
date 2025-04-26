package net.swofty.catchngo.api.categories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.swofty.catchngo.api.ApiManager
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.ApiResponse
import org.json.JSONObject

/**
 * Handles all /user/achievements-related endpoints.
 */
class AchievementsApiCategory(context: Context) {

    private val api = ApiManager.getInstance(context)

    /* -------------------------------------------------------------------- */
    /*  Helpers: safely unwrap "status / data" or fall back to flat JSON    */
    /* -------------------------------------------------------------------- */

    /** Returns the *payload* object, after verifying `status == success`. */
    private fun unwrap(json: JSONObject): JSONObject {
        val status = json.optString("status", "success")
        if (status != "success") error("Server responded with status=$status")

        // If there's a {data:{…}} block, use it; otherwise keep the root
        return json.optJSONObject("data") ?: json
    }

    /**
     * GET /user/has
     * Get list of achievements the user has.
     *
     * @return List of achievements or empty list if request failed
     */
    suspend fun getUserAchievements(): List<ApiModels.Achievement> = withContext(Dispatchers.IO) {
        val response = api.get("/user/has")
        Log.i("AchievementsApi", "Get achievements → $response")

        return@withContext when (response) {
            is ApiResponse.Success -> {
                try {
                    val jsonObject = JSONObject(response.data)
                    val data = unwrap(jsonObject)
                    val achievementsArray = data.getJSONArray("achievements")

                    val achievements = mutableListOf<ApiModels.Achievement>()
                    for (i in 0 until achievementsArray.length()) {
                        val achievement = achievementsArray.getJSONObject(i)
                        achievements.add(
                            ApiModels.Achievement(
                                title = achievement.getString("title"),
                                description = achievement.getString("description"),
                                points = achievement.optInt("reward", 0)
                            )
                        )
                    }
                    achievements
                } catch (e: Exception) {
                    Log.e("AchievementsApi", "Failed to parse user achievements", e)
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    /**
     * GET /user/missing
     * Get list of achievements the user hasn't earned yet.
     *
     * @return List of missing achievements or empty list if request failed
     */
    suspend fun getMissingAchievements(): List<ApiModels.MissingAchievement> = withContext(Dispatchers.IO) {
        val response = api.get("/user/missing")
        Log.i("AchievementsApi", "Get missing achievements → $response")

        return@withContext when (response) {
            is ApiResponse.Success -> {
                try {
                    val jsonObject = JSONObject(response.data)
                    val data = unwrap(jsonObject)
                    val achievementsArray = data.getJSONArray("missing_achievements")

                    val achievements = mutableListOf<ApiModels.MissingAchievement>()
                    for (i in 0 until achievementsArray.length()) {
                        val achievement = achievementsArray.getJSONObject(i)
                        achievements.add(
                            ApiModels.MissingAchievement(
                                title = achievement.getString("title"),
                                description = achievement.getString("description"),
                                points = achievement.getInt("points"),
                                minFriends = achievement.optInt("min_friends", 0)
                            )
                        )
                    }
                    achievements
                } catch (e: Exception) {
                    Log.e("AchievementsApi", "Failed to parse missing achievements", e)
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }
}