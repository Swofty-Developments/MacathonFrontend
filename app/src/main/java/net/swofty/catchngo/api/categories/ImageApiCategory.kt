// ────────────────────────────────────────────────────────────────────────────────
// ImageApiCategory.kt
// ────────────────────────────────────────────────────────────────────────────────
package net.swofty.catchngo.api.categories

import android.content.Context
import android.util.Log
import net.swofty.catchngo.api.ApiManager
import net.swofty.catchngo.api.ApiResponse
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Handles all /picture-related endpoints.
 *
 *  • GET  /picture/get_picture/{user_id}   → base-64 string
 *  • POST /picture/set_picture            → upload player picture
 */
class ImageApiCategory(context: Context) {

    private val api = ApiManager.getInstance(context)

    /* -------------------------------------------------------------------- */
    /*  GET /picture/get_picture/{user_id}                                  */
    /* -------------------------------------------------------------------- */

    /**
     * Fetches a user’s profile picture (base-64 encoded PNG/JPEG).
     *
     * @return base-64 string on success, or `null` if not found / error.
     */
    suspend fun getPicture(userId: String): String? {
        val res = api.get("/picture/get_picture/$userId")
        Log.i("ImageApi", "GET picture → $res")

        return when (res) {
            is ApiResponse.Success -> try {
                /* The server sometimes returns:
                 *   {"status":"success","data":{"image":"<base64>"}}       – OR –
                 *   "<base64>"                                             – raw string
                 */
                val root   = JSONObject(res.data)
                val status = root.optString("status", "success")
                if (status == "success") {
                    root.getJSONObject("data").getString("image")
                } else null
            } catch (_: Exception) {              // not JSON → treat as raw base-64
                res.data.takeIf { it.isNotBlank() }
            }

            is ApiResponse.Error, is ApiResponse.Exception -> null
        }
    }

    /* -------------------------------------------------------------------- */
    /*  POST /picture/set_picture                                           */
    /* -------------------------------------------------------------------- */

    /**
     * Uploads / replaces **your own** profile picture.
     *
     * @param base64Image  Raw base-64 data URI or plain base-64 bytes.
     * @return `true` if the server responded 2xx, otherwise `false`.
     */
    suspend fun setPicture(base64Image: String): Boolean {
        val encoded = URLEncoder.encode(base64Image, StandardCharsets.UTF_8)
        val res     = api.post("/picture/set_picture?picture=$encoded")
        Log.i("ImageApi", "POST picture → $res")
        return res is ApiResponse.Success
    }
}
