package com.example.covid19_project

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action: String? = intent.action

        when (action) {
            BluetoothDevice.ACTION_FOUND -> {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                val device: BluetoothDevice =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                val deviceName = device.name
                val deviceHardwareAddress = device.address // MAC address
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                Log.d("condev", "$deviceName:$deviceHardwareAddress:$rssi")
            }
            BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                Log.d("condev", "isDiscovering")
            }
            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                Log.d("condev", "FinishedDiscovering")
            }
        }
    }
}
