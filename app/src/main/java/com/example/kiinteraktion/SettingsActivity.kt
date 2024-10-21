
package com.example.kiinteraktion

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_OK)
        super.onBackPressed()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            findPreference<Preference>("alarmSound")?.setOnPreferenceClickListener {
                openAudioFilePicker()
                true
            }

            // Debug: Log current alarm sound URI
            val currentUri = preferenceManager.sharedPreferences?.getString("alarmSoundUri", null)
            Log.d("SettingsActivity", "Current alarm sound URI: $currentUri")
        }

        private fun openAudioFilePicker() {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
            }
            startActivityForResult(intent, PICK_AUDIO_FILE)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == PICK_AUDIO_FILE && resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    try {
                        saveAlarmSoundUri(uri)
                    } catch (e: Exception) {
                        Log.e("SettingsActivity", "Error saving alarm sound URI", e)
                        Toast.makeText(context, "Error saving alarm sound", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun saveAlarmSoundUri(uri: Uri) {
            context?.contentResolver?.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )

            val documentFile = DocumentFile.fromSingleUri(requireContext(), uri)
            if (documentFile == null || !documentFile.exists()) {
                Toast.makeText(context, "Invalid audio file", Toast.LENGTH_SHORT).show()
                return
            }

            preferenceManager.sharedPreferences?.edit()?.apply {
                putString("alarmSoundUri", uri.toString())
                apply()
            }

            // Debug: Log the selected URI
            Log.d("SettingsActivity", "Selected alarm sound URI: $uri")
            Toast.makeText(context, "Alarm sound updated", Toast.LENGTH_SHORT).show()
        }

        companion object {
            private const val PICK_AUDIO_FILE = 1
        }
    }
}
