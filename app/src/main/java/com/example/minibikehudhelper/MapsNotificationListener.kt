package com.example.minibikehudhelper

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.bluetooth.*
import android.util.Log
import java.io.IOException
import java.util.*

class MapsNotificationListener : NotificationListenerService() {

    private val ESP32_NAME = "ESP32_Bike"
    private var btAdapter: BluetoothAdapter? = null

    override fun onCreate() {
        super.onCreate()
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        Log.d("BikeHUD", "MapsNotificationListener started")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != "com.google.android.apps.maps") return

        val extras = sbn.notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()

        // Only react to navigation updates, not general Maps notifications
        if (text.isNullOrBlank()) return
        if (!text.contains("•") && !text.contains(" mi") && !text.contains(" ft") && !text.contains(" km"))
            return

        val message = "$title: $text"
        Log.d("BikeHUD", "Maps message: $message")

        Thread { sendOnceReliable(message) }.start()
    }

    private fun sendOnceReliable(message: String) {
        val adapter = btAdapter ?: return
        val device = adapter.bondedDevices.firstOrNull { it.name == ESP32_NAME }

        if (device == null) {
            Log.e("BikeHUD", "ESP32 not paired!")
            return
        }

        var socket: BluetoothSocket? = null
        try {
            adapter.cancelDiscovery()
            socket = device.javaClass
                .getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                .invoke(device, 1) as BluetoothSocket

            Thread.sleep(150) // Let radio settle
            socket.connect()
            Thread.sleep(200) // Let link stabilize

            val out = socket.outputStream
            out.write((message + "\n").toByteArray())
            out.flush()

            Thread.sleep(200) // Give ESP32 time to read before closing
            Log.d("BikeHUD", "Sent to ESP32: $message")

        } catch (e: IOException) {
            Log.e("BikeHUD", "Send failed", e)
        } catch (e: Exception) {
            Log.e("BikeHUD", "Unexpected error", e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
