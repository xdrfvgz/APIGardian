import com.example.kiinteraktion.MainActivity
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
    private lateinit var notificationManager: NotificationManagerCompat
    private val CHANNEL_ID = "APIMonitorChannel"
    private val NOTIFICATION_ID = 1

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
        notificationManager = NotificationManagerCompat.from(this)
        createNotificationChannel()
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(title: String, content: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    private fun startMonitoring() {
        if (!isMonitoring) {
            isMonitoring = true
            job = CoroutineScope(Dispatchers.Default).launch {
                while (isActive && isMonitoring) {
                    checkAPI()
                    delay(60000) // Check every minute
                }
            }
            sendNotification("API Monitoring Started", "Checking API every minute")
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
        sendNotification("API Monitoring Stopped", "Monitoring service has been stopped")
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
            withContext(Dispatchers.IO) {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("API request failed with code: ${response.code}")
                    }

                    val responseBody = response.body?.string() ?: throw IOException("Empty response body")
                    Log.d("APIMonitorService", "API Response: $responseBody")

                    val value = if (isJson) {
                        try {
                            extractValueFromJson(responseBody, jsonPath ?: "")
                        } catch (e: Exception) {
                            throw IOException("Error parsing JSON: ${e.message}")
                        }
                    } else {
                        responseBody
                    }

                    Log.d("APIMonitorService", "Extracted value: $value, Target value: $targetValue")

                    if (value.contains(targetValue, ignoreCase = true)) {
                        Log.d("APIMonitorService", "Target value matched. Playing alarm.")
                        withContext(Dispatchers.Main) {
                            playAlarm()
                            sendNotification("Target Value Detected", "The API response matched the target value: $targetValue")
                        }
                    }
                }
            }
        } catch (e: UnknownHostException) {
            Log.e("APIMonitorService", "Unknown host: $apiUrl", e)
            notifyMainActivity("Unknown host: $apiUrl. Please check your internet connection and API URL.")
            sendNotification("API Check Error", "Unknown host: $apiUrl")
        } catch (e: IOException) {
            Log.e("APIMonitorService", "Network error for URL: $apiUrl", e)
            notifyMainActivity("Network error: ${e.message}. Please check your internet connection.")
            sendNotification("API Check Error", "Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e("APIMonitorService", "Error checking API", e)
            notifyMainActivity("Error checking API: ${e.message}")
            sendNotification("API Check Error", "Error checking API: ${e.message}")
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
