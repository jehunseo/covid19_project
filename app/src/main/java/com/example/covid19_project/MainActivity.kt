package com.example.covid19_project

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import android.widget.Toast.makeText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.*
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONException


class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocation : FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var textView: TextView

    private var requestQueue: RequestQueue? = null

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
                        textView.append("$id, $searched_id, $MAC\n\n")
                    }


                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            Response.ErrorListener { error -> error.printStackTrace() })
        requestQueue?.add(request)
    }

    val permissionLocation = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    fun requestPermission(){
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
    fun updateLocation(){
        val locationRequest = LocationRequest.create()
        locationRequest.run{
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
        }

        locationCallback = object : LocationCallback(){
            override fun onLocationResult(locationResult : LocationResult?){
                locationResult?.let{
                    for((i, location) in it.locations.withIndex()){
                        Log.d("Location","$i ${location.longitude}, ${location.latitude}")
                    }
                }
            }
        }
        fusedLocation.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textViewResult)
        val button_parse: Button = findViewById(R.id.btnParse)
        requestQueue = Volley.newRequestQueue(this)

        /////////////////////////////////////////////////////////////////////////////////////////////
        //Bluetooth Permission Check/////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////
        val permission1 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
        val permission2 = ContextCompat.checkSelfPermission(this,
            Manifest.permission.BLUETOOTH_ADMIN)
        val permission3 = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)
        val permission4 = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val intent = Intent(this, BluetoothService::class.java)
        if (permission1 != PackageManager.PERMISSION_GRANTED
            || permission2 != PackageManager.PERMISSION_GRANTED
            || permission3 != PackageManager.PERMISSION_GRANTED
            || permission4 != PackageManager.PERMISSION_GRANTED
        ) {
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
            makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 5)
        }
        if (Data_Class.size != 0) {
            for (i in 0 until Data_Class.size) {
                textView.append("${Data_Class.name}, ${Data_Class.bluetoothMacAddress}, ${Data_Class.bluetoothRssi}\n\n")
            }
        }

        /////////////////////////////////////////////////////////////////////////////////////////////
        //Button Listener////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////


        checkPermission()

        button.setOnClickListener() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                this.startForegroundService(intent)
            } else {
                this.startService(intent)
            }
        }

        button_parse.setOnClickListener {
            jsonParse()
        }

        btnGPS.setOnClickListener {
            fusedLocation = LocationServices.getFusedLocationProviderClient(this)
            updateLocation()
        }

        btnlogging.setOnClickListener {

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