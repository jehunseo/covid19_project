package com.example.covid19_project

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permission1 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
        val permission2 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)
        val permission3 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        val permission4 = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val receiver = BluetoothReceiver()
        //val discoverableIntent: Intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        val intent = Intent(this, BluetoothService::class.java)

        if (permission1 != PackageManager.PERMISSION_GRANTED
                || permission2 != PackageManager.PERMISSION_GRANTED
                || permission3 != PackageManager.PERMISSION_GRANTED
                || permission4 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION),
                    642)
        } else {
            Log.d("DISCOVERING-PERMISSIONS", "Permissions Granted")
        }

        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 5)
        }

        button.setOnClickListener() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                this.startForegroundService(intent)
            }
            else{
                this.startService(intent)
            }

            /*
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }

            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)

            var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            this.registerReceiver(receiver, filter)
            filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            this.registerReceiver(receiver, filter)
            filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            this.registerReceiver(receiver, filter)
            bluetoothAdapter?.startDiscovery()
             */
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5
                && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "취소 했습니다", Toast.LENGTH_SHORT).show()
            finish()
        } else if (requestCode == 5
                && resultCode == RESULT_OK) {
            Toast.makeText(this, "블루투스를 활성화합니다.", Toast.LENGTH_SHORT).show()
        }
    }
}

// Bluetooth Reference : https://developer.android.com/guide/topics/connectivity/bluetooth