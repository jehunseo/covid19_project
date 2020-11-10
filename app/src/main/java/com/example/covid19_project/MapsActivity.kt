package com.example.covid19_project

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import okhttp3.internal.ignoreIoExceptions
import java.util.HashSet
import java.util.jar.Manifest

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this) //getMapAsync 함수 호출시 안드로이드가 onMapReady 함수 호출
    }

    //내 위치를 가져오는 코드
    lateinit var fusedLocationClient:FusedLocationProviderClient //배터리소모, 좌표 정확도 등등을 자동으로 해주는 역할
    lateinit var locationCallback:LocationCallback //위에서 받아온 응답값을 처리해주는 역할

    @SuppressLint("MissingPermission") //이관련 문법 무시하는 용도

    fun setUpdateLocationListner() {
        val locationRequest = LocationRequest.create()
        locationRequest.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000 // 1초에 한번씩 위치 요청
        }
        locationCallback = object : LocationCallback () {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
                    for((i, location) in it.locations.withIndex()){
                        Log.d("로케이션", "$i ${location.latitude}, ${location.longitude}")
                        setLastLocation(location)
                    }
                }
            }
        }
        // 로케이션 요청 함수 호출 (locationRequest, locationCallback)
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    fun setLastLocation(location : Location) {
        val myLocation = LatLng(location.latitude, location.longitude)
        val marker = MarkerOptions()
            .position(myLocation)
            .title("I am here!")
        val cameraOption = CameraPosition.Builder()
            .target(myLocation)
            .zoom(15.0f)
            .build()
        val camera = CameraUpdateFactory.newCameraPosition(cameraOption)

        mMap.clear()
        mMap.addMarker(marker)
        mMap.moveCamera(camera)
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Add a marker in Sydney and move the camera
        val ajou = LatLng(37.280286, 127.045242)  //아주대 좌표

        // 마커 아이콘 만들기
        val descriptor = getDescriptorFromDrawable(R.drawable.marker)
        //drawable 폴더에 저장한 marker 라능 이름의 png 파일로 마커 모양 변경

        // 마커 생성 및 위치, title, icon 설정
        val marker = MarkerOptions()
            .position(ajou)
            .title("Marker in Ajou University")
            .icon(descriptor)
        mMap.addMarker(marker)
        setUpdateLocationListner()
        /*
        val cameraOption = CameraPosition.Builder()
            .target(ajou)
            .zoom(12f).build()
        val camera = CameraUpdateFactory.newCameraPosition(cameraOption)
        // 카메라 생성
        mMap.moveCamera(camera)
        */
    }


    fun getDescriptorFromDrawable(drawableID : Int) : BitmapDescriptor{
        var bitmapDrawable:BitmapDrawable
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            bitmapDrawable = getDrawable(drawableID) as BitmapDrawable
        } else {
            bitmapDrawable = resources.getDrawable(drawableID) as BitmapDrawable
        }
        //마커 크기 변환
        val scaledBitmap = Bitmap.createScaledBitmap(bitmapDrawable.bitmap, 100,100, false)
        return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
    } //마커 모양 및 크기 변경 함수
}