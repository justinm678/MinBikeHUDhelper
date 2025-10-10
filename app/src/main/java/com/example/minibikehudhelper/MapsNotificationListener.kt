package com.example.minibikehudhelper

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.bluetooth.*
import android.util.Log
import java.io.IOException
import java.util.*

class MapsNotificationListener : NotificationListenerService() {

    private var btAdapter: BluetoothAdapter? = null
    private var btSocket: BluetoothSocket? = null
    private val ESP32_NAME = "ESP32_Bike"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard SPP

    override fun onCreate() {
        super.onCreate()
        btAdapter = BluetoothAdapter.getDefaultAdapter()

        // Connect in a background thread
        Thread { connectToESP32() }.start()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.google.android.apps.maps") {
            val text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            text?.let { sendToESP32(it) }
        }
    }

    private fun connectToESP32() {
        try {
            val pairedDevices = btAdapter?.bondedDevices
            val device = pairedDevices?.firstOrNull { it.name == ESP32_NAME }
            device?.let {
                btSocket = it.createRfcommSocketToServiceRecord(SPP_UUID)
                btAdapter?.cancelDiscovery()
                btSocket?.connect()
                Log.d("BT", "Connected to ESP32")
            }
        } catch (e: IOException) {
            Log.e("BT", "Connection failed", e)
        }
    }

    private fun sendToESP32(message: String) {
        try {
            if (btSocket?.isConnected == true) {
                val outStream = btSocket?.outputStream
                outStream?.write((message + "\n").toByteArray())
            }
        } catch (e: IOException) {
            Log.e("BT", "Send failed", e)
        }
    }
}
