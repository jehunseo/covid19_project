package com.example.covid19_project

import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import okhttp3.internal.ignoreIoExceptions
import org.jetbrains.anko.zoomControls
import java.util.HashSet
import java.util.jar.Manifest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    val PERM_FLAG = 99
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if (isPermitted()){
            startProcess()
        }else{
            ActivityCompat.requestPermissions(this, permissions, PERM_FLAG)
        }
    }

    fun isPermitted() : Boolean {
        for (perm in permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun startProcess() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap){
        mMap = googleMap

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupdateLocationListner()
    }

    // -- 내 위치를 가져오는 코드
    lateinit var fusedLocationClient:FusedLocationProviderClient  //배터리소모, 정확도 관리
    lateinit var locationCallback:LocationCallback //위를 통해 받은 좌표를 callback하는 역할

    // -- 주기적으로 좌표계를 업뎃해주는 역할
    @SuppressLint("MissingPermission")
    fun setupdateLocationListner() {
        val locationRequest = LocationRequest.create()
        locationRequest.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY //정확도를 높이 하겠다.
            interval = 10000 //1초
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
                    for ((i, location) in it.locations.withIndex()) {//튜플기능으로 index와 함께 꺼내쓸수 있음
                        Log.d("로케이션", "$i ${location.latitude} ${location.longitude}")
                        setLastLocation(location)
                    }
                }
            }
        }

        // 로케이션 요청 함수 호출 (locationRequest, locationCallback
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    //마커 , 카메라이동
    fun setLastLocation(location : Location) {
        val myLocation = LatLng(location.latitude, location.longitude)
        val marker = MarkerOptions()
            .position(myLocation)
            .title("I am here")
        val cameraOption = CameraPosition.Builder()
            .target(myLocation)
            .zoom(15.0f)
            .build()
        val camera = CameraUpdateFactory.newCameraPosition(cameraOption)

        mMap.clear()
        mMap.addMarker(marker)
        mMap.moveCamera(camera)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERM_FLAG -> {
                var check = true
                for(grant in grantResults) {
                    if(grant != PERMISSION_GRANTED){
                        check = false
                        break
                    }
                }
                if(check) {
                    startProcess()
                }else{
                    Toast.makeText(this,"권한을 승인해야지만 앱을 사용할 수 있습니다.",Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

}