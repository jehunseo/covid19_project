package com.example.covid19_project

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class ScheduledWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val intent = Intent(applicationContext, BluetoothService::class.java)
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
            applicationContext.startForegroundService(discoverableIntent)
        }
        else {
        applicationContext.startService(intent)
        }

        Log.d("worker", "!!")
        //성공
        return Result.success()
    }
}