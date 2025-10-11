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
import kotlin.concurrent.thread
import java.io.IOException
import java.util.*

class MainActivity : ComponentActivity() {

    private val ESP32_NAME = "ESP32_Bike"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var btSocket: BluetoothSocket? = null
    private var counter = 0  // 🔢 added counter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button = Button(this)
        button.text = "Send Increasing Number"
        setContentView(button)

        // Request permissions on Android 12+
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

        button.setOnClickListener {
            // Increment and send the new number
            val message = "Count: ${counter++}"
            thread { connectAndSend(message) }
        }
    }

    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        // Force channel 1 using reflection
        return device.javaClass
            .getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            .invoke(device, 1) as BluetoothSocket
    }

    private fun connectAndSend(message: String) {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val device: BluetoothDevice? =
            btAdapter.bondedDevices.firstOrNull { it.name == ESP32_NAME }

        if (device == null) {
            runOnUiThread {
                Toast.makeText(this, "ESP32 not paired", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            btAdapter.cancelDiscovery()
            btSocket = createBluetoothSocket(device)
            Thread.sleep(200) // brief pause before connect

            btSocket!!.connect()
            Thread.sleep(200) // brief pause to stabilize connection

            val out = btSocket!!.outputStream
            out.write((message + "\n").toByteArray())
            out.flush()

            Thread.sleep(200) // allow ESP32 time to read
            runOnUiThread {
                Toast.makeText(this, "Sent: $message", Toast.LENGTH_SHORT).show()
            }

        } catch (e: IOException) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } finally {
            try { btSocket?.close() } catch (_: Exception) {}
        }
    }
}
