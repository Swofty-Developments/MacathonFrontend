package net.swofty.catchngo.api.categories

import android.content.Context
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.swofty.catchngo.api.ApiManager
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.ApiResponse
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles all location-related endpoints.
 */
class LocationApiCategory(context: Context) {

    private val api = ApiManager.getInstance(context)

    /**
     * POST /location/upload/
     * Uploads the user's current location to the server.
     *
     * @param location The location to upload
     * @return True if upload was successful
     */
    suspend fun uploadLocation(location: Location): Boolean = withContext(Dispatchers.IO) {
        val queryParams = mapOf(
            "latitude" to location.latitude.toString(),
            "longitude" to location.longitude.toString()
        )

        val response = api.post(
            "/location/upload/?latitude=${location.latitude}&longitude=${location.longitude}"
        )

        return@withContext when (response) {
            is ApiResponse.Success -> true
            else -> false
        }
    }

    /**
     * GET /location/radius-fetch/{user_id}
     * Fetches users within a certain radius of a specified user.
     *
     * @param userId The ID of the user to check around
     * @param radius The radius in meters to search for nearby users
     * @return List of nearby users or empty list if request failed
     */
    suspend fun fetchNearbyUsers(userId: String, radius: Double): List<ApiModels.NearbyUser> = withContext(Dispatchers.IO) {
        val response = api.get("/location/radius-fetch/$userId?radius=$radius")

        return@withContext when (response) {
            is ApiResponse.Success -> parseNearbyUsers(response.data)
            else -> emptyList()
        }
    }

    /**
     * Parses the JSON response into a list of NearbyUser objects.
     */
    private fun parseNearbyUsers(json: String): List<ApiModels.NearbyUser> {
        val result = mutableListOf<ApiModels.NearbyUser>()

        try {
            val jsonArray = JSONArray(json)

            for (i in 0 until jsonArray.length()) {
                val userJson = jsonArray.getJSONObject(i)

                // Parse questions
                val questionsArray = userJson.getJSONArray("questions")
                val questions = mutableListOf<ApiModels.QuestionAnswer>()

                for (j in 0 until questionsArray.length()) {
                    val questionJson = questionsArray.getJSONObject(j)
                    questions.add(
                        ApiModels.QuestionAnswer(
                            id = questionJson.getInt("id"),
                            answer = questionJson.getString("answer")
                        )
                    )
                }

                // Parse friends
                val friendsArray = userJson.getJSONArray("friends")
                val friends = mutableListOf<String>()

                for (j in 0 until friendsArray.length()) {
                    friends.add(friendsArray.getString(j))
                }

                // Create the NearbyUser object
                result.add(
                    ApiModels.NearbyUser(
                        id = userJson.getString("id"),
                        name = userJson.getString("name"),
                        points = userJson.getInt("points"),
                        disabled = userJson.getBoolean("disabled"),
                        questions = questions,
                        friends = friends,
                        selectedFriend = userJson.optString("selected_friend", "").takeIf { it.isNotEmpty() },
                        latitude = userJson.getDouble("latitude"),
                        longitude = userJson.getDouble("longitude")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }
}