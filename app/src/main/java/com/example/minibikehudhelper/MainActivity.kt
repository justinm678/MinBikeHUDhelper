package com.example.minibikehudhelper

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.widget.Button
import android.widget.Toast
import java.io.IOException
import java.util.*

class MainActivity : ComponentActivity() {

    private val ESP32_NAME = "ESP32_Bike"
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var btSocket: BluetoothSocket? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val button = Button(this)
        button.text = "Send Test"
        setContentView(button)

        button.setOnClickListener {
            Thread {
                sendTestMessage("Hello ESP32!")
            }.start()
        }
    }

    private fun sendTestMessage(message: String) {
        val btAdapter = BluetoothAdapter.getDefaultAdapter()
        val device: BluetoothDevice? = btAdapter.bondedDevices.firstOrNull { it.name == ESP32_NAME }

        if (device == null) {
            runOnUiThread {
                Toast.makeText(this, "ESP32 not paired", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            if (btSocket == null || !btSocket!!.isConnected) {
                btSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                btSocket?.connect()
            }

            btSocket?.outputStream?.write((message + "\n").toByteArray())
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