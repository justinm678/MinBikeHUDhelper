package com.example.minibikehudhelper

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import kotlin.concurrent.thread
import java.io.IOException
import java.util.*

class MainActivity : ComponentActivity() {

    private val ESP32_NAME = "ESP32_Bike"
    private var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val testButton = Button(this).apply { text = "Send Test Message" }
        val notifButton = Button(this).apply { text = "Enable Notification Access" }

        layout.addView(testButton)
        layout.addView(notifButton)
        setContentView(layout)

        // Request Bluetooth permissions on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                1
            )
        }

        notifButton.setOnClickListener {
            // Opens system settings so you can enable your app for notification access
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        testButton.setOnClickListener {
            val message = "Test #${counter++}"
            thread { sendOnceReliable(message) }
        }
    }

    private fun sendOnceReliable(message: String) {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val device: BluetoothDevice? =
            btAdapter.bondedDevices.firstOrNull { it.name == ESP32_NAME }

        if (device == null) {
            runOnUiThread {
                Toast.makeText(this, "ESP32 not paired", Toast.LENGTH_SHORT).show()
            }
            return
        }

        var socket: BluetoothSocket? = null
        try {
            btAdapter.cancelDiscovery()
            socket = device.javaClass
                .getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                .invoke(device, 1) as BluetoothSocket

            Thread.sleep(150)
            socket.connect()
            Thread.sleep(200)

            socket.outputStream.write((message + "\n").toByteArray())
            socket.outputStream.flush()

            Thread.sleep(200)
            runOnUiThread {
                Toast.makeText(this, "Sent: $message", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
