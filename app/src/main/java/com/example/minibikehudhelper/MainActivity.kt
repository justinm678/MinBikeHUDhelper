package com.example.minibikehudhelper


import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class MainActivity : ComponentActivity() {

    private val ESP32_NAME = "ESP32_Bike"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var btSocket: BluetoothSocket? = null
    private val messageQueue = ConcurrentLinkedQueue<String>()
    @Volatile private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button = Button(this)
        button.text = "Send Test"
        setContentView(button)

        // Request permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_SCAN),
                1
            )
        }

        // Connect in background
        Thread { connectToESP32() }.start()

        button.setOnClickListener {
            messageQueue.add("Hello ESP32!")
        }

        // Background thread to send queued messages
        Thread {
            while (true) {
                if (isConnected && btSocket != null && messageQueue.isNotEmpty()) {
                    val msg = messageQueue.poll()
                    try {
                        btSocket?.outputStream?.write((msg + "\n").toByteArray())
                    } catch (e: IOException) {
                        e.printStackTrace()
                        isConnected = false
                        connectToESP32()
                    }
                }
                Thread.sleep(50)
            }
        }.start()
    }

    private fun connectToESP32() {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val device: BluetoothDevice? = btAdapter.bondedDevices.firstOrNull { it.name == ESP32_NAME }
        if (device == null) {
            runOnUiThread {
                Toast.makeText(this, "ESP32 not paired", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            btAdapter.cancelDiscovery()
            btSocket?.connect()
            isConnected = true
            runOnUiThread {
                Toast.makeText(this, "Connected to ESP32", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            isConnected = false
        }
    }
}