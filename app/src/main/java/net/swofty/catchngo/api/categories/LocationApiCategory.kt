package net.swofty.catchngo.api.categories

import android.content.Context
import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.swofty.catchngo.api.ApiManager
import net.swofty.catchngo.api.ApiModels
import net.swofty.catchngo.api.ApiResponse
import org.json.JSONArray
import org.json.JSONException
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
    private fun parseNearbyUsers(payload: String): List<ApiModels.NearbyUser> {
        // First try the wrapped-object format
        try {
            val root   = JSONObject(payload)
            val status = root.optString("status", "success")
            if (status != "success") error("Server responded with $status")

            val dataArray = root.getJSONArray("data")
            return decodeArray(dataArray)
        } catch (_: JSONException) {
            // Not an object wrapper – fall through to raw-array handling
        }

        // Second chance: bare JSON array
        try {
            val dataArray = JSONArray(payload)
            return decodeArray(dataArray)
        } catch (ex: JSONException) {
            throw IllegalStateException(
                "Unsupported /location/nearby JSON format: ${ex.localizedMessage}",
                ex
            )
        }
    }

    private fun decodeArray(array: JSONArray): List<ApiModels.NearbyUser> =
        List(array.length()) { idx ->
            val obj = array.getJSONObject(idx)

            val questions = List(obj.getJSONArray("questions").length()) { qIdx ->
                val q = obj.getJSONArray("questions").getJSONObject(qIdx)
                ApiModels.QuestionAnswer(
                    id     = q.getInt("id"),
                    answer = q.getString("answer")
                )
            }

            val friends = List(obj.getJSONArray("friends").length()) { fIdx ->
                obj.getJSONArray("friends").getString(fIdx)
            }

            ApiModels.NearbyUser(
                id            = obj.optString("id", null),
                name          = obj.getString("name"),
                points        = obj.getInt("points"),
                disabled      = obj.getBoolean("disabled"),
                questions     = questions,
                friends       = friends,
                selectedFriend= obj.optString("selected_friend", null),
                latitude      = obj.getDouble("latitude"),
                longitude     = obj.getDouble("longitude")
            )
        }
}