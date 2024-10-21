package com.example.kiinteraktion

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.UnknownHostException

class APIMonitorService : Service() {
    private var job: Job? = null
    private lateinit var client: OkHttpClient
    private var mediaPlayer: MediaPlayer? = null
    private var isMonitoring = false

    companion object {
        const val ACTION_START_MONITORING = "com.example.kiinteraktion.START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.kiinteraktion.STOP_MONITORING"
    }

    override fun onCreate() {
        super.onCreate()
        client = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        Log.d("APIMonitorService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("APIMonitorService", "Service started with action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true
            job = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && isMonitoring) {
                    Log.d("APIMonitorService", "Starting API check")
                    checkAPI()
                    delay(60000) // Check every minute
                }
            }
            // Broadcast that monitoring has started
            sendBroadcast(Intent("com.example.kiinteraktion.MONITORING_STATUS_CHANGED"))
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        job?.cancel()
        job = null
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        // Broadcast that monitoring has stopped
        sendBroadcast(Intent("com.example.kiinteraktion.MONITORING_STATUS_CHANGED"))
        stopSelf()
    }

    private suspend fun checkAPI() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val apiUrl = sharedPrefs.getString("apiUrl", "") ?: return
        val isJson = sharedPrefs.getBoolean("isJson", false)
        val jsonPath = sharedPrefs.getString("jsonPath", "")
        val targetValue = sharedPrefs.getString("targetValue", "") ?: return

        Log.d("APIMonitorService", "Checking API: $apiUrl")

        val request = Request.Builder().url(apiUrl).build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("APIMonitorService", "API request failed: ${response.code}")
                    notifyMainActivity("API request failed with code: ${response.code}")
                    return
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                Log.d("APIMonitorService", "API Response: $responseBody")

                val value = if (isJson) {
                    try {
                        extractValueFromJson(responseBody, jsonPath ?: "")
                    } catch (e: Exception) {
                        Log.e("APIMonitorService", "Error parsing JSON", e)
                        notifyMainActivity("Error parsing JSON response")
                        return
                    }
                } else {
                    responseBody
                }

                Log.d("APIMonitorService", "Extracted value: $value, Target value: $targetValue")

                if (value.contains(targetValue, ignoreCase = true)) {
                    Log.d("APIMonitorService", "Target value matched. Playing alarm.")
                    withContext(Dispatchers.Main) {
                        playAlarm()
                    }
                }
            }
        } catch (e: UnknownHostException) {
            Log.e("APIMonitorService", "Unknown host: $apiUrl", e)
            notifyMainActivity("Unknown host: $apiUrl")
        } catch (e: IOException) {
            Log.e("APIMonitorService", "Network error for URL: $apiUrl", e)
            notifyMainActivity("Network error for URL: $apiUrl")
        } catch (e: Exception) {
            Log.e("APIMonitorService", "Error checking API", e)
            notifyMainActivity("Error checking API")
        }
    }

    private fun extractValueFromJson(json: String, path: String): String {
        val jsonObject = JSONObject(json)
        val keys = path.split(".")
        var result: Any = jsonObject
        for (key in keys) {
            result = (result as JSONObject).get(key)
        }
        return result.toString()
    }

    private fun playAlarm() {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val alarmSoundUri = sharedPrefs.getString("alarmSoundUri", null)
        Log.d("APIMonitorService", "Attempting to play alarm sound: $alarmSoundUri")

        mediaPlayer?.release()
        mediaPlayer = if (alarmSoundUri != null) {
            try {
                MediaPlayer.create(this, Uri.parse(alarmSoundUri))
            } catch (e: Exception) {
                Log.e("APIMonitorService", "Error creating MediaPlayer with custom URI", e)
                MediaPlayer.create(this, R.raw.default_alarm_sound)
            }
        } else {
            MediaPlayer.create(this, R.raw.default_alarm_sound)
        }
        mediaPlayer?.start()
    }

    private fun notifyMainActivity(message: String) {
        val intent = Intent("com.example.kiinteraktion.API_ERROR")
        intent.putExtra("errorMessage", message)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        Log.d("APIMonitorService", "Service destroyed")
    }

    override fun onBind(intent: Intent): IBinder? = null
}
