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
import com.example.covid19_project.Extensions.toast
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*

private val db = FirebaseFirestore.getInstance()  //firestore db

//좌표를 담을 데이터 클래스 생성
data class DataPosition (var id:String, var Lat:Double, var long:Double)

var dataPosition = DataPosition ("1", 33.01, 40.0)
var dataPosition2 = DataPosition ("2", 33.02, 40.0)
var dataPosition3 = DataPosition ("3", 33.03, 40.01)
var dataPosition4 = DataPosition ( "4", 33.04, 40.0)
var dataPosition5 = DataPosition ( "5", 51.0, 41.0)
var dataPosition6 = DataPosition ( "6", 33.05, 40.0)
var dataPosition7 = DataPosition ( "7", 52.0, 43.0)

var posArray = arrayListOf<DataPosition>(dataPosition, dataPosition2, dataPosition3, dataPosition4,
    dataPosition5, dataPosition6, dataPosition7)

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    private lateinit var jsonArray: JSONArray
    private lateinit var clusterManager: ClusterManager<ClusterItem>
    private val markerList = mutableListOf<MarkerOptions>()
    val PERM_FLAG = 99


    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        jsonArray = readAssets()
        if (isPermitted()){
            startProcess()
        }else{
            ActivityCompat.requestPermissions(this, permissions, PERM_FLAG)
        }
    }

    //json 파일 읽는 기능도 구현 가능 (아래 주석 = addmarkers, json파일로 읽어서 마커 생성하는 방법)
    private fun readAssets(): JSONArray{
        val json = assets.open("places.json")
            .bufferedReader()
            .readText()
        return JSONArray(json)
    }

    private fun addMarkers(){
        for (index in 0 until jsonArray.length()){
            val jsonObject = jsonArray.getJSONObject(index)
            val name = jsonObject.getString("name")
            val lat = jsonObject.getDouble("lat")
            val lng = jsonObject.getDouble("lng")
            val marker = MarkerOptions().position(LatLng(lat, lng)).title(name)
            markerList.add(marker)
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerList[0].position, 13.0f))
        var i = 0
    }
    /*
    private fun addMarkers(){
        mMap.clear()
        for (index in 0 until posArray.size){  //DataPosition 클래스를 담아둔 배열을 돌림
            val name = posArray.get(index).id
            val lat = posArray.get(index).Lat
            val lng = posArray.get(index).long
            val marker = MarkerOptions().position(LatLng(lat, lng)).title(name)
            markerList.add(marker)
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerList[0].position, 13.0f))
    }
     */

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
        clusterManager = ClusterManager(this, googleMap)
        addMarkers()
        setupClusterManager()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupdateLocationListner()
    }

    private fun setupClusterManager() {
        clusterManager.renderer = MarkerClusterRenderer(this, mMap, clusterManager)
        addClusters()
        setClusterManagerClickListener()
        mMap.setOnCameraIdleListener(clusterManager)
        mMap.setOnMarkerClickListener(clusterManager)
        clusterManager.cluster()
    }

    private fun addClusters() {
        for (marker in markerList){
            val clusterItem = MarkerClusterItem(marker.position, marker.title)
            clusterManager.addItem(clusterItem)
        }
    }

    private fun setClusterManagerClickListener() {
        clusterManager.setOnClusterClickListener {
            val items = it.items
            val itemsList = mutableListOf<String>()
            for (item in items){
                itemsList.add((item as MarkerClusterItem).markerTitle)
            }
            true
        }
    }
    //https://github.com/MChehab94/Google-Maps-Marker-Clustering 참고

    // -- 내 위치를 가져오는 코드
    lateinit var fusedLocationClient:FusedLocationProviderClient  //배터리소모, 정확도 관리
    lateinit var locationCallback:LocationCallback //위를 통해 받은 좌표를 callback하는 역할


    // -- 주기적으로 좌표를 업뎃해주는 역할
    @SuppressLint("MissingPermission")
    fun setupdateLocationListner() {
        val locationRequest = LocationRequest.create()
        locationRequest.run {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY //정확도를 높이 하겠다.
            interval = 10000 //10초
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
                    for ((i, location) in it.locations.withIndex()) {//튜플기능으로 index와 함께 꺼내쓸수 있음
                        Log.d("로케이션", "$i ${location.latitude} ${location.longitude}")
                        setLastLocation(location)
                        val docRef = db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid)
                        docRef.get()
                            .addOnSuccessListener { document ->
                                if (document != null) {
                                    val Loc_Store_Agree_value = document.getBoolean("Loc_Store_Agree")
                                    if(Loc_Store_Agree_value == true)
                                    {
                                        val data = hashMapOf("Lat" to location.latitude,"long" to location.longitude, "check" to "true")   //Firestore 필드 : 위도, 경도 (내 위치)
                                        // 기생성된 Users 컬렉션의 각 멤버 문서에 종속되는 컬렉션 Location에 위치 데이터 저장하기
                                        db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid).collection("Location").document("Current").set(data)  //firestore에 data 삽입
                                        //toast("익명의 위치정보가 안전하게 저장되었어요")
                                    }else{
                                        //toast("위치정보는 저장되지 않아요")
                                    }
                                } else {
                                    //toast("가입 정보가 DB에 없어요")
                                }
                            }
                            .addOnFailureListener { exception ->
                                //toast("DB 접근 실패")
                            }


                    }
                }
            }
        }

        // 로케이션 요청 함수 호출 (locationRequest, locationCallback
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    //마커 , 카메라이동
    fun setLastLocation(location : Location) {
        //mMap.clear()
        /*
        var i = 0
        for(i in 0 until posArray.size ){
            val locations = LatLng(posArray.get(i).Lat, posArray.get(i).long)
            val marker = MarkerOptions()
                .position(locations)
            val cameraOption = CameraPosition.Builder()
                .target(LatLng(37.0, 137.0))
                .zoom(15.0f)
                .build()
            val camera = CameraUpdateFactory.newCameraPosition(cameraOption)
            mMap.addMarker(marker)
        }
        */
        val mymarker = LatLng(location.latitude,location.longitude) //내 좌표
        val descriptor = getDescriptorFromDrawable(R.drawable.marker)
        val marker = MarkerOptions()
            .position(mymarker)
            .title("myPosition")
            .icon(descriptor)
        mMap.addMarker(marker)



        /*
        val myLocation = LatLng(posarray.get(0).Lat, posarray.get(0).long)
        val marker = MarkerOptions()
            .position(myLocation)
        val cameraOption = CameraPosition.Builder()
            .target(myLocation)
            .zoom(15.0f)
            .build()
        val camera = CameraUpdateFactory.newCameraPosition(cameraOption)
         */
        //mMap.addMarker(marker)
        //mMap.moveCamera(camera)
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