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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button = Button(this)
        button.text = "Send Test Message"
        setContentView(button)

        // Request permissions on Android 12+
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
        // Force channel 1 using reflection
        return device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
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
            btAdapter.cancelDiscovery() // cancel discovery BEFORE connecting
            btSocket = createBluetoothSocket(device)
            Thread.sleep(100)    // small delay to ensure ESP32 is ready

            runOnUiThread {
                Toast.makeText(this, "socket created!", Toast.LENGTH_SHORT).show()
            }
            btSocket!!.connect() // connect
            Thread.sleep(200)    // small delay to ensure ESP32 is ready

            runOnUiThread {
                Toast.makeText(this, "socket connected!", Toast.LENGTH_SHORT).show()
            }
            btSocket!!.outputStream.write((message + "\n").toByteArray()) // send message
            Thread.sleep(300) // give ESP32 time to read

            runOnUiThread {
                Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show()
            }

            btSocket!!.close() // close after sending
        } catch (e: IOException) {
            e.printStackTrace()
            runOnUiThread {
                Toast.makeText(this, "Failed to connect/send", Toast.LENGTH_SHORT).show()
            }
        }
    }
}