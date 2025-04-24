package net.swofty.catchngo.api

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Base API manager that handles making HTTP requests to the backend
 */
class ApiManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val BASE_URL = "http://localhost:8000"
        private const val PREFS_NAME = "CatchNGoPrefs"
        private const val AUTH_COOKIE_KEY = "authCookie"

        // Header name for the authentication token
        private const val AUTH_HEADER = "X-Auth-Cookie"

        @Volatile
        private var instance: ApiManager? = null

        /**
         * Get singleton instance of ApiManager
         */
        fun getInstance(context: Context): ApiManager {
            return instance ?: synchronized(this) {
                instance ?: ApiManager(context).also { instance = it }
            }
        }
    }

    /**
     * Get stored authentication cookie
     */
    fun getAuthCookie(): String? {
        return prefs.getString(AUTH_COOKIE_KEY, null)
    }

    /**
     * Save authentication cookie
     */
    fun saveAuthCookie(cookie: String) {
        prefs.edit().putString(AUTH_COOKIE_KEY, cookie).apply()
    }

    /**
     * Clear authentication cookie (logout)
     */
    fun clearAuthCookie() {
        prefs.edit().remove(AUTH_COOKIE_KEY).apply()
    }

    /**
     * Perform a GET request
     */
    suspend fun get(endpoint: String, headers: Map<String, String>? = null): ApiResponse {
        return withContext(Dispatchers.IO) {
            executeRequest(endpoint, "GET", headers, null)
        }
    }

    /**
     * Perform a POST request
     */
    suspend fun post(endpoint: String, headers: Map<String, String>? = null, body: JSONObject? = null): ApiResponse {
        return withContext(Dispatchers.IO) {
            executeRequest(endpoint, "POST", headers, body?.toString())
        }
    }

    /**
     * Perform a DELETE request
     */
    suspend fun delete(endpoint: String, headers: Map<String, String>? = null): ApiResponse {
        return withContext(Dispatchers.IO) {
            executeRequest(endpoint, "DELETE", headers, null)
        }
    }

    /**
     * Execute HTTP request
     */
    private fun executeRequest(
        endpoint: String,
        method: String,
        headers: Map<String, String>?,
        body: String?
    ): ApiResponse {
        try {
            val url = URL(BASE_URL + endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = method

            // Set auth cookie in header if available
            val authCookie = getAuthCookie()
            if (!authCookie.isNullOrEmpty()) {
                connection.setRequestProperty(AUTH_HEADER, authCookie)
            }

            // Add custom headers
            headers?.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            connection.setRequestProperty("Content-Type", "application/json")
            connection.doInput = true

            // Add body if provided
            if (body != null) {
                connection.doOutput = true
                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(body)
                    writer.flush()
                }
            }

            val responseCode = connection.responseCode
            val isSuccess = responseCode in 200..299

            // Check for authentication cookie in response headers
            val responseAuthCookie = connection.getHeaderField(AUTH_HEADER)
            if (!responseAuthCookie.isNullOrEmpty()) {
                saveAuthCookie(responseAuthCookie)
            }

            val responseBody = if (isSuccess) {
                readResponse(connection)
            } else {
                try {
                    readErrorResponse(connection)
                } catch (e: Exception) {
                    "Error: $responseCode"
                }
            }

            return if (isSuccess) {
                ApiResponse.Success(responseBody)
            } else {
                ApiResponse.Error(responseCode, responseBody)
            }
        } catch (e: Exception) {
            return ApiResponse.Exception(e)
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        return reader.use { it.readText() }
    }

    private fun readErrorResponse(connection: HttpURLConnection): String {
        connection.errorStream ?: return "Unknown error"
        val reader = BufferedReader(InputStreamReader(connection.errorStream))
        return reader.use { it.readText() }
    }
}