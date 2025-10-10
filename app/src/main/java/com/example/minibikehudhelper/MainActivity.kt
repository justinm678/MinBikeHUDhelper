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
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private val ESP32_NAME = "ESP32_Bike"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var btSocket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button = Button(this)
        button.text = "Send Test Message"
        setContentView(button)

        // Request permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                1
            )
        }

        button.setOnClickListener {
            thread { connectAndSend("Hello ESP32!") }
        }
    }

    private fun createBluetoothSocket(device: BluetoothDevice): BluetoothSocket {
        return try {
            // Reflection method: forces channel 1
            device.javaClass.getMethod(
                "createRfcommSocket",
                Int::class.javaPrimitiveType
            ).invoke(device, 1) as BluetoothSocket
        } catch (e: Exception) {
            // fallback
            device.createRfcommSocketToServiceRecord(SPP_UUID)
        }
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
            val socket = createBluetoothSocket(device)
            btAdapter.cancelDiscovery()
            socket.connect()
            socket.outputStream.write((message + "\n").toByteArray())
            socket.close()

            runOnUiThread {
                Toast.makeText(this, "Connected and sent message!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Failed to connect or send", Toast.LENGTH_SHORT).show()
            }
        }
    }
}