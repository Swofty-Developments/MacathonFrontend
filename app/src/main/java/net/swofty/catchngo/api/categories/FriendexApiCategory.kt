package net.swofty.catchngo.api.categories

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.swofty.catchngo.api.ApiManager
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.ApiResponse
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles all /friendex-related endpoints.
 */
class FriendexApiCategory(context: Context) {

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
     * GET /friendex/{user_id}
     * Get user entry information by ID.
     *
     * @param userId The ID of the user to fetch
     * @return User data or null if request failed
     */
    suspend fun getUserEntry(userId: String): ApiModels.UserEntry? = withContext(Dispatchers.IO) {
        val response = api.get("/friendex/$userId")

        return@withContext when (response) {
            is ApiResponse.Success -> {
                try {
                    val jsonObject = JSONObject(response.data)
                    val data = unwrap(jsonObject)

                    // Parse questions
                    val questionsArray = data.getJSONArray("questions")
                    val questions = mutableListOf<ApiModels.QuestionAnswer>()

                    for (i in 0 until questionsArray.length()) {
                        val questionObj = questionsArray.getJSONObject(i)
                        questions.add(
                            ApiModels.QuestionAnswer(
                                id = questionObj.getInt("id"),
                                answer = questionObj.getString("answer")
                            )
                        )
                    }

                    ApiModels.UserEntry(
                        id = data.getString("_id"),
                        name = data.getString("name"),
                        points = data.getInt("points"),
                        disabled = data.getBoolean("disabled"),
                        questions = questions
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            else -> null
        }
    }

    /**
     * GET /friendex/friends/{user_id}
     * Get list of friends for a user.
     *
     * @param userId The ID of the user to fetch friends for
     * @return List of friend IDs or empty list if request failed
     */
    suspend fun getUserFriends(userId: String): List<String> = withContext(Dispatchers.IO) {
        val response = api.get("/friendex/friends/$userId")

        return@withContext when (response) {
            is ApiResponse.Success -> {
                try {
                    val jsonObject = JSONObject(response.data)
                    val data = unwrap(jsonObject).getJSONArray("data")

                    if (data is JSONArray) {
                        val friendsList = mutableListOf<String>()
                        for (i in 0 until data.length()) {
                            friendsList.add(data.getString(i))
                        }
                        friendsList
                    } else {
                        emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    emptyList()
                }
            }
            else -> emptyList()
        }
    }

    /**
     * GET /friendex/unmet-players/{user_id}
     * Get list of players the user hasn't met yet.
     *
     * @param userId The ID of the user to fetch unmet players for
     * @return List of unmet player IDs or empty list if request failed
     */
    suspend fun getUnmetPlayers(userId: String): List<String> =
        withContext(Dispatchers.IO) {

            val res = api.get("/friendex/unmet-players/$userId")
            Log.i("FriendexApi", "Unmet players → $res")

            if (res !is ApiResponse.Success) return@withContext emptyList()

            return@withContext try {
                val root  = JSONObject(res.data)
                val array = root.optJSONArray("data") ?: JSONArray()

                List(array.length()) { idx ->
                    array.getJSONObject(idx).getString("name")
                }
            } catch (e: Exception) {
                Log.e("FriendexApi", "Failed to parse unmet players", e)
                emptyList()
            }
        }

    /**
     * POST /friendex/select/{user_id}
     * Select a user as a target.
     *
     * @param userId The ID of the user to select
     * @return True if selection was successful
     */
    suspend fun selectUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        val response = api.post("/friendex/select/$userId")

        return@withContext response is ApiResponse.Success
    }

    /**
     * GET /friendex/select/check
     * Check currently selected user and tracking status
     *
     * @return Selection status or null if request failed
     */
    suspend fun checkSelection(): ApiModels.SelectionStatus? = withContext(Dispatchers.IO) {
        val response = api.get("/friendex/select/check")

        return@withContext when (response) {
            is ApiResponse.Success -> {
                try {
                    val jsonObject = JSONObject(response.data)
                    val data = unwrap(jsonObject)

                    ApiModels.SelectionStatus(
                        selectedFriend = data.optString("selectedFriend", null),
                        isInitiator = data.optBoolean("isInitiator", false),
                        timeRemaining = data.optDouble("timeRemaining", 0.0),
                        elapsedTime = data.optDouble("elapsedTime", 0.0),
                        questionsReady = data.optBoolean("questionsReady", false),
                        pointsAccumulated = data.optInt("pointsAccumulated", 0)
                    )
                } catch (e: Exception) {
                    Log.e("FriendexApi", "Failed to parse selection status", e)
                    null
                }
            }
            else -> null
        }
    }

    /**
     * POST /friendex/deselect
     * Deselect the currently selected user.
     *
     * @return True if deselection was successful
     */
    suspend fun deselectUser(): Boolean = withContext(Dispatchers.IO) {
        val response = api.post("/friendex/deselect")

        return@withContext response is ApiResponse.Success
    }

    /**
     * POST /friendex/addfriend
     * Add a user as a friend.
     *
     * @param friendId The ID of the user to add as a friend
     * @return True if adding friend was successful
     */
    suspend fun addFriend(friendId: String): Boolean = withContext(Dispatchers.IO) {
        val response = api.post("/friendex/addfriend?friend_id=$friendId")

        return@withContext response is ApiResponse.Success
    }
}