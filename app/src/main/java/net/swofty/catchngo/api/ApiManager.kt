package net.swofty.catchngo.api

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Centralised HTTP helper used by all API categories.
 * – Sends JSON bodies.
 * – Automatically adds an Authorization: Bearer <token> header if one is saved.
 */
class ApiManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val BASE_URL = "https://macathon.fadednetworks.com"
        private const val PREFS_NAME = "CatchNGoPrefs"
        private const val TOKEN_KEY = "accessToken"
        private const val AUTH_HDR   = "Authorization"

        @Volatile
        private var instance: ApiManager? = null

        fun getInstance(context: Context): ApiManager =
            instance ?: synchronized(this) {
                instance ?: ApiManager(context).also { instance = it }
            }
    }

    /* -------------------------------------------------------------------- */
    /*  Access-token helpers                                                */
    /* -------------------------------------------------------------------- */

    fun getAccessToken(): String? = prefs.getString(TOKEN_KEY, null)

    suspend fun saveAccessToken(token: String): Boolean =
        withContext(Dispatchers.IO) {          // keep this off the main thread
            Log.i("pog", "Saving access token $token")
            prefs.edit()
                .putString(TOKEN_KEY, token)
                .commit()                      // synchronous; Boolean result
        }

    fun clearAccessToken() {
        prefs.edit().remove(TOKEN_KEY).apply()
    }

    /* -------------------------------------------------------------------- */
    /*  Public request helpers                                              */
    /* -------------------------------------------------------------------- */

    suspend fun get(endpoint: String): ApiResponse =
        withContext(Dispatchers.IO) { executeRequest(endpoint, "GET", null) }

    suspend fun post(endpoint: String, bodyJson: String? = null): ApiResponse =
        withContext(Dispatchers.IO) {
            val bytes = bodyJson?.toByteArray(StandardCharsets.UTF_8)
            executeRequest(
                endpoint,
                "POST",
                bodyBytes   = bytes,
                contentType = "application/json"
            )
        }

    suspend fun postMultipart(
        endpoint: String,
        fields: Map<String, String>
    ): ApiResponse = withContext(Dispatchers.IO) {
        val boundary   = "----CatchNGo${System.currentTimeMillis()}"
        val bodyBytes  = buildMultipartBody(fields, boundary)
        val typeHeader = "multipart/form-data; boundary=$boundary"
        executeRequest(
            endpoint,
            "POST",
            bodyBytes   = bodyBytes,
            contentType = typeHeader
        )
    }

    suspend fun delete(endpoint: String): ApiResponse =
        withContext(Dispatchers.IO) { executeRequest(endpoint, "DELETE", null) }

    /* -------------------------------------------------------------------- */
    /*  Core implementation                                                 */
    /* -------------------------------------------------------------------- */

    private fun executeRequest(
        endpoint: String,
        method: String,
        bodyBytes: ByteArray? = null,
        contentType: String?  = null
    ): ApiResponse {
        return try {
            val url        = URL(BASE_URL + endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.doInput = true

            // choose content type
            val type = contentType ?: "application/json"
            connection.setRequestProperty("Content-Type", type)

            // auth header if token present
            getAccessToken()?.let { token ->
                connection.setRequestProperty(AUTH_HDR, "Bearer $token")
            }

            // write body if supplied
            if (bodyBytes != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Length", bodyBytes.size.toString())
                connection.outputStream.use { os: OutputStream ->
                    os.write(bodyBytes)
                    os.flush()
                }
            }

            val code = connection.responseCode
            val body = if (code in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else {
                connection.errorStream?.let {
                    BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() }
                } ?: "HTTP $code"
            }

            if (code in 200..299) ApiResponse.Success(body)
            else                   ApiResponse.Error(code, body)
        } catch (e: Exception) {
            ApiResponse.Exception(e)
        }
    }


    /* -------------------------------------------------------------------- */
    /*  Helper: build multipart bytes                                       */
    /* -------------------------------------------------------------------- */

    private fun buildMultipartBody(fields: Map<String, String>, boundary: String): ByteArray {
        val lb = "\r\n"
        val sb = StringBuilder()

        fields.forEach { (name, value) ->
            sb.append("--").append(boundary).append(lb)
            sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"").append(lb)
            sb.append(lb)
            sb.append(value).append(lb)
        }

        sb.append("--").append(boundary).append("--").append(lb)
        return sb.toString().toByteArray(StandardCharsets.UTF_8)
    }
}
