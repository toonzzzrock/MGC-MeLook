package com.example.mgc_keyboard.dashboard.bridge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Talks to Server/backend (see MGC/Server/README.md). Every call here is
 * only ever invoked from behind the Settings > Clinical Bridge toggle — the
 * rest of the app has no reference to this class.
 */
class ClinicalBridgeClient(private val baseUrl: String) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json".toMediaType()

    data class RegisterResult(val deviceId: Long, val pairingCode: String, val deviceToken: String)

    suspend fun register(displayName: String): Result<RegisterResult> = post(
        path = "/api/devices/register",
        token = null,
        body = JSONObject().put("displayName", displayName)
    ).mapCatching { json ->
        RegisterResult(
            deviceId = json.getLong("deviceId"),
            pairingCode = json.getString("pairingCode"),
            deviceToken = json.getString("deviceToken")
        )
    }

    /** Backs the Settings "Test connection" row. */
    suspend fun ping(deviceToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$baseUrl/api/devices/ping")
                .header("Authorization", "Bearer $deviceToken")
                .get()
                .build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("ping failed: HTTP ${response.code}")
            }
        }
    }

    suspend fun sync(deviceToken: String, snapshot: BridgeStatsSnapshot): Result<Unit> = post(
        path = "/api/devices/sync",
        token = deviceToken,
        body = snapshot.toJson()
    ).map { }

    private suspend fun post(path: String, token: String?, body: JSONObject): Result<JSONObject> =
        withContext(Dispatchers.IO) {
            runCatching {
                val requestBuilder = Request.Builder()
                    .url("$baseUrl$path")
                    .post(body.toString().toRequestBody(jsonMedia))
                if (token != null) requestBuilder.header("Authorization", "Bearer $token")
                http.newCall(requestBuilder.build()).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}: $responseBody")
                    if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)
                }
            }
        }
}

data class BridgeStatsSnapshot(
    val recordedAt: String,
    val backspaceRate: Float?,
    val sentimentScore: Float?,
    val screenTimeMinutes: Float?,
    val appVarietyCount: Int?,
    val keyPresses: Int?,
    val wordsScored: Int?
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("recordedAt", recordedAt)
        backspaceRate?.let { put("backspaceRate", it) }
        sentimentScore?.let { put("sentimentScore", it) }
        screenTimeMinutes?.let { put("screenTimeMinutes", it) }
        appVarietyCount?.let { put("appVarietyCount", it) }
        keyPresses?.let { put("keyPresses", it) }
        wordsScored?.let { put("wordsScored", it) }
    }
}
