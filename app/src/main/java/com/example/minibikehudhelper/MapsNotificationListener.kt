package com.example.minibikehudhelper

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.os.Handler
import android.os.Looper
import android.bluetooth.*
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import java.io.IOException
import java.util.*

class MapsNotificationListener : NotificationListenerService() {

    private val ESP32_NAME = "ESP32_Bike"
    private val TAG = "BikeHUD"
    private var btAdapter: BluetoothAdapter? = null
    private val handler = Handler(Looper.getMainLooper())
    private val pollIntervalMs: Long = 15000 // 15 seconds
//    private val sentMessages = mutableSetOf<String>() // keep track of already sent messages

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                val active = activeNotifications
                var foundGMAPSnotification: Boolean = false
                for (sbn in active) {
                    if (sbn.packageName == "com.google.android.apps.maps") {
                        foundGMAPSnotification = true
                    }
                    else continue
                    val extras = sbn.notification.extras
//                    val text = extras.getCharSequence("android.text")?.toString()
//                    val title = extras.getCharSequence("android.title")?.toString()
                    val title = extras.getCharSequence("android.title")?.toString() ?: ""
                    val text = extras.getCharSequence("android.text")?.toString() ?: ""
                    val bigText = extras.getCharSequence("android.bigText")?.toString()
                    val subText = extras.getCharSequence("android.subText")?.toString()
                    val infoText = extras.getCharSequence("android.infoText")?.toString()
                    val summaryText = extras.getCharSequence("android.summaryText")?.toString()

                    Log.d("BikeHUD", "Title: $title")
                    Log.d("BikeHUD", "Text (android.text): $text")
                    Log.d("BikeHUD", "Big Text: $bigText")
                    Log.d("BikeHUD", "Sub Text: $subText")
                    Log.d("BikeHUD", "Info Text: $infoText")
                    Log.d("BikeHUD", "Summary Text: $summaryText")

                    // Icons
                    val smallIcon = sbn.notification.smallIcon
                    Log.d("BikeHUD", "Small Icon: $smallIcon")



                    val largeIcon: Icon? = sbn.notification.getLargeIcon()
                    var directionSymbol = "?"

                    if (largeIcon != null) {
                        val bitmap: Bitmap? = try {
                            largeIcon.loadDrawable(this@MapsNotificationListener)?.let { drawable ->
                                val bmp = Bitmap.createBitmap(
                                    drawable.intrinsicWidth,
                                    drawable.intrinsicHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = android.graphics.Canvas(bmp)
                                drawable.setBounds(0, 0, canvas.width, canvas.height)
                                drawable.draw(canvas)
                                bmp
                            }
                        } catch (e: Exception) {
                            null
                        }

                        if (bitmap != null) {
                            val iconHash = hashBitmap(bitmap) // implement a simple hash function
                            Log.d("BikeHUD", "Large Icon hash: $iconHash")

                            // Example lookup table
                            directionSymbol = when (iconHash) {
                                "2f6ae0e0" -> "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"   // left
                                "ec6d98e0" -> ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>"   // right
                                "e7acc840" -> "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^"   // forward
                                else -> ""
                            }
                        }
                    }

                    val message = "$title:--$directionSymbol: $text "

                    Log.d(TAG, "Final HUD message: $message")
                    Thread { sendOnceReliable(message) }.start()
                }
                if (!foundGMAPSnotification) {

                    val message = "no Gmaps notification"

                    Log.d(TAG, "Warning: $message")
                    Thread { sendOnceReliable(message) }.start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error polling notifications", e)
            } finally {
                handler.postDelayed(this, pollIntervalMs)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        btAdapter = BluetoothAdapter.getDefaultAdapter()
        Log.d(TAG, "MapsNotificationListener started")
        handler.post(pollRunnable) // start periodic polling
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
        Log.d(TAG, "MapsNotificationListener stopped")
    }
    private fun cleanMapsMessage(message: String): String {
        var result = message

        // Replace words with short symbols
        result = result.replace("toward", "tw", ignoreCase = true)
        result = result.replace("left", "<", ignoreCase = true)
        result = result.replace("right", ">", ignoreCase = true)

        // Replace non-breaking spaces or weird unicode spaces with normal space
        result = result.replace("\u00A0", " ")

        // Only keep ASCII characters 32–126
        result = result.map { c ->
            if (c.code in 32..126) c else '?'
        }.joinToString("")

        // Trim leading/trailing spaces
        result = result.trim()

        return result
    }
    private fun hashBitmap(bitmap: Bitmap): String {
        // Downscale to make it stable even if anti-aliased differently
        val scaled = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
        var hash = 0
        for (y in 0 until scaled.height) {
            for (x in 0 until scaled.width) {
                hash = 31 * hash + scaled.getPixel(x, y)
            }
        }
        return Integer.toHexString(hash)
    }
    private fun sendOnceReliable(message: String) {
        val adapter = btAdapter ?: return
        val device = adapter.bondedDevices.firstOrNull { it.name == ESP32_NAME } ?: run {
            Log.e(TAG, "ESP32 not paired!")
            return
        }
        val cleanedMessage = cleanMapsMessage(message)

        repeat(3) { attempt ->
            var socket: BluetoothSocket? = null
            try {
                adapter.cancelDiscovery()
                socket = device.javaClass
                    .getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                    .invoke(device, 1) as BluetoothSocket

                Thread.sleep(150)
                socket.connect()
                Thread.sleep(200)

                socket.outputStream.write((cleanedMessage + "\n").toByteArray(Charsets.UTF_8))
                socket.outputStream.flush()

                Log.d(TAG, "Sent to ESP32: $message")
                return
            } catch (e: IOException) {
                Log.e(TAG, "Attempt ${attempt + 1} failed", e)
                try { socket?.close() } catch (_: Exception) {}
                Thread.sleep(1000)
            }
        }
    }
}
