package com.example.minibikehudhelper

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check notification access
        val enabledListeners =
            Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!enabledListeners.contains(packageName)) {
            Toast.makeText(this, "Please enable notification access for this app", Toast.LENGTH_LONG).show()
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        } else {
            // Start the notification listener service
            val intent = Intent(this, MapsNotificationListener::class.java)
            startService(intent)
        }
    }
}