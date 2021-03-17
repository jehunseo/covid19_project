package com.example.covid19_project

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.android.synthetic.main.fragment_qr.*
import org.json.JSONException

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocation: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback


    private var requestQueue: RequestQueue? = null

    ////////////////////////////////////////////////////////////////////////////////////
    // function for php data parse
    ////////////////////////////////////////////////////////////////////////////////////
    private val url = "https://ajouycdcovid19.com/print.php"
    private fun jsonParse() {
        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
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
            { error -> error.printStackTrace() },
        )

        requestQueue?.add(request)
    }
    ////////////////////////////////////////////////////////////////////////////////////
    //permission for location
    ////////////////////////////////////////////////////////////////////////////////////
    private val permissionLocation = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, permissionLocation, 99)
    }

    private fun checkPermission() {
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
    private val helper = DataBaseHelper(this, "memo", 1)

    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentAdapter = MyPagerAdapter(supportFragmentManager)
        viewPager.adapter = fragmentAdapter

        tabLayout.post {
            tabLayout.setupWithViewPager(viewPager)
            tabLayout.setTabsFromPagerAdapter(fragmentAdapter)
            tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_baseline_map_24)
            tabLayout.getTabAt(2)?.setIcon(R.drawable.ic_baseline_warning_24)
        }
        requestQueue = Volley.newRequestQueue(this)

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

    /* 뒤로가기 버튼 버그 수정*/
    private var backPressedTime:Long = 0
    lateinit var backToast:Toast
    override fun onBackPressed() {
        backToast = Toast.makeText(this, "한번 더 누르면 종료됩니다", Toast.LENGTH_LONG)
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast.cancel()
            ActivityCompat.finishAffinity(this)
            System.runFinalization()
            System.exit(0)
        } else {
            backToast.show()
        }
        backPressedTime = System.currentTimeMillis()
    }

}


// Bluetooth Reference : https://developer.android.com/guide/topics/connectivity/bluetooth