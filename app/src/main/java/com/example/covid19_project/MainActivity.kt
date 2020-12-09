package com.example.covid19_project

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocation: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var requestQueue: RequestQueue? = null

    ////////////////////////////////////////////////////////////////////////////////////
    // function for php data parse
    ////////////////////////////////////////////////////////////////////////////////////
    val url = "https://ajouycdcovid19.com/print.php"
    private fun jsonParse() {
        val request = JsonObjectRequest(Request.Method.GET,
            url,
            null,
            Response.Listener { response ->
                try {
                    val jsonArray = response.getJSONArray("BLELOG")

                    for (i in 0 until jsonArray.length()) {
                        val BLELOG = jsonArray.getJSONObject(i)
                        val id = BLELOG.getInt("id")
                        val searched_id = BLELOG.getInt("searched_id")
                        val MAC = BLELOG.getString("MAC")

                        Log.d("dat", "$id, $searched_id, $MAC\n\n")
                    }


                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error -> error.printStackTrace() })
        requestQueue?.add(request)
    }


    ////////////////////////////////////////////////////////////////////////////////////
    //permission for location
    ////////////////////////////////////////////////////////////////////////////////////
    val permissionLocation = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissionLocation, 99)
    }

    fun checkPermission() {
        for (p in permissionLocation) {
            val res = ContextCompat.checkSelfPermission(this, p)
            if (res != PackageManager.PERMISSION_DENIED) {
                requestPermission()
                break
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun updateLocation() {
        val locationRequest = LocationRequest.create()
        locationRequest.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
                    for ((i, location) in it.locations.withIndex()) {
                        Log.d("Location", "$i ${location.longitude}, ${location.latitude}")
                    }
                }
            }
        }
        fusedLocation.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }


    ////////////////////////////////////////////////////////////////////////////////////
    // for android DB
    ////////////////////////////////////////////////////////////////////////////////////
    val helper = DataBaseHelper(this, "memo", 1)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentAdapter = MyPagerAdapter(supportFragmentManager)
        viewPager.adapter = fragmentAdapter


        requestQueue = Volley.newRequestQueue(this)

        //val intent_map = Intent(this, MapsActivity::class.java)  //지도 화면으로 이동하기 위한 intent객체 생성
        //startActivity(intent_map)

        /////////////////////////////////////////////////////////////////////////////////////////////
        //Bluetooth Permission Check/////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////
        val permission1 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
        val permission2 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_ADMIN
        )
        val permission3 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val permission4 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val intent = Intent(this, BluetoothService::class.java)
        if (permission1 != PackageManager.PERMISSION_GRANTED
            || permission2 != PackageManager.PERMISSION_GRANTED
            || permission3 != PackageManager.PERMISSION_GRANTED
            || permission4 != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                642
            )
        } else {
            Log.d("DISCOVERING-PERMISSIONS", "Permissions Granted")
        }
        if (bluetoothAdapter == null) {
            makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 5)
        }
        if (Data_Class.size != 0) {
            for (i in 0 until Data_Class.size) {
                Log.d(
                    "dat2",
                    "${Data_Class.name}, ${Data_Class.bluetoothMacAddress}, ${Data_Class.bluetoothRssi}\n\n"
                )
            }
        }
        val saveRequest =
            PeriodicWorkRequestBuilder<ScheduledWorker>(
                1, TimeUnit.MINUTES
            ).build()
        WorkManager.getInstance(this).enqueue(saveRequest)
        /////////////////////////////////////////////////////////////////////////////////////////////
        //Button Listener////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////
        checkPermission()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            this.startForegroundService(intent)
        } else {
            this.startService(intent)
        }

        jsonParse()

        fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        updateLocation()

        for (i in (0..(Data_Class.size - 1))) {
            val memo = Memo(
                Data_Class.name[i],
                Data_Class.bluetoothMacAddress[i],
                Data_Class.bluetoothRssi[i]
            )
            helper.insertMemo(memo)
        }

        for (l in helper.selectMemo()) {
            Log.d("dbdata", "${l.name}, ${l.MAC}, ${l.bluetoothRssi}\n")
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5
            && resultCode == RESULT_CANCELED
        ) {
            makeText(this, "취소 했습니다", Toast.LENGTH_SHORT).show()
            finish()
        } else if (requestCode == 5
            && resultCode == RESULT_OK
        ) {
            makeText(this, "블루투스를 활성화합니다.", Toast.LENGTH_SHORT).show()
        }
    }
}


// Bluetooth Reference : https://developer.android.com/guide/topics/connectivity/bluetooth