#package com.example.kiinteraktion
import com.example.kiinteraktion.APIMonitorService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager

class MainActivity : AppCompatActivity() {
    private val SETTINGS_REQUEST_CODE = 1001
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var statusTextView: TextView
    private lateinit var apiUrlTextView: TextView
    private lateinit var targetValueTextView: TextView
    private lateinit var alarmSoundTextView: TextView
    private lateinit var errorReceiver: BroadcastReceiver
    private lateinit var monitoringStatusReceiver: BroadcastReceiver
    private lateinit var startMonitoringButton: Button
    private lateinit var stopMonitoringButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        statusTextView = findViewById(R.id.statusTextView)
        apiUrlTextView = findViewById(R.id.apiUrlTextView)
        targetValueTextView = findViewById(R.id.targetValueTextView)
        alarmSoundTextView = findViewById(R.id.alarmSoundTextView)

        startMonitoringButton = findViewById(R.id.startMonitoringButton)
        stopMonitoringButton = findViewById(R.id.stopMonitoringButton)

        startMonitoringButton.setOnClickListener {
            startMonitoring()
        }

        stopMonitoringButton.setOnClickListener {
            stopMonitoring()
        }

        findViewById<Button>(R.id.settingsButton).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, SETTINGS_REQUEST_CODE)
        }

        errorReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val errorMessage = intent?.getStringExtra("errorMessage")
                Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        }

        monitoringStatusReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                updateInfoDisplay()
            }
        }

        registerReceiver(errorReceiver, IntentFilter("com.example.kiinteraktion.API_ERROR"))
        registerReceiver(monitoringStatusReceiver, IntentFilter("com.example.kiinteraktion.MONITORING_STATUS_CHANGED"))

        updateInfoDisplay()
    }

    override fun onResume() {
        super.onResume()
        updateInfoDisplay()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(errorReceiver)
        unregisterReceiver(monitoringStatusReceiver)
    }

    private fun startMonitoring() {
        if (isConfigValid()) {
            val intent = Intent(this, APIMonitorService::class.java)
            intent.action = APIMonitorService.ACTION_START_MONITORING
            startService(intent)
            Log.d("MainActivity", "Attempting to start APIMonitorService")
            updateInfoDisplay()
            Toast.makeText(this, "Monitoring started", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Please configure API URL and Target Value in settings", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopMonitoring() {
        val intent = Intent(this, APIMonitorService::class.java)
        intent.action = APIMonitorService.ACTION_STOP_MONITORING
        startService(intent)
        updateInfoDisplay()
        Toast.makeText(this, "Monitoring stopped", Toast.LENGTH_SHORT).show()
    }

    private fun updateInfoDisplay() {
        val apiUrl = sharedPreferences.getString("apiUrl", "Not set") ?: "Not set"
        val targetValue = sharedPreferences.getString("targetValue", "Not set") ?: "Not set"
        val alarmSoundUri = sharedPreferences.getString("alarmSoundUri", "Default") ?: "Default"

        apiUrlTextView.text = "API URL: $apiUrl"
        targetValueTextView.text = "Target Value: $targetValue"
        alarmSoundTextView.text = "Alarm Sound: ${alarmSoundUri.substringAfterLast('/')}"

        val isServiceRunning = isServiceRunning(APIMonitorService::class.java)
        statusTextView.text = "Status: ${if (isServiceRunning) "Monitoring" else "Stopped"}"
        
        startMonitoringButton.isEnabled = !isServiceRunning
        stopMonitoringButton.isEnabled = isServiceRunning
    }

    private fun isConfigValid(): Boolean {
        val apiUrl = sharedPreferences.getString("apiUrl", "")
        val targetValue = sharedPreferences.getString("targetValue", "")
        return !apiUrl.isNullOrEmpty() && !targetValue.isNullOrEmpty()
    }

    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}
