package com.example.minibikehudhelper


import android.Manifest
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

class MainActivity : ComponentActivity() {

    private val ESP32_NAME = "ESP32_Bike"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Button to send test message
        val button = Button(this)
        button.text = "Send Test Message"
        setContentView(button)

        // Request Bluetooth permissions on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                1
            )
        }

        button.setOnClickListener {
            Thread { sendTestMessage("Hello ESP32!") }.start()
        }
    }

    private fun sendTestMessage(message: String) {
        val btAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (btAdapter == null) {
            runOnUiThread {
                Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val device: BluetoothDevice? =
            btAdapter.bondedDevices.firstOrNull { it.name == ESP32_NAME }

        if (device == null) {
            runOnUiThread {
                Toast.makeText(this, "ESP32 not paired", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            btAdapter.cancelDiscovery()
            socket.connect()
            socket.outputStream.write((message + "\n").toByteArray())
            socket.close()

            runOnUiThread {
                Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
        }
    }
}