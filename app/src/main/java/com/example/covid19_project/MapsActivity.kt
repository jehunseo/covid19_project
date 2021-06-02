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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import kotlinx.android.synthetic.main.activity_maps.*

private val db = FirebaseFirestore.getInstance()  //firestore db

//좌표를 담을 데이터 클래스 생성
data class DataPosition (var id:String, var Lat:Double, var long:Double)

var dataPosition = DataPosition ("1", 36.5, 128.1)
var dataPosition2 = DataPosition ("2", 37.4, 127.1)
var dataPosition3 = DataPosition ("3", 38.0, 128.4)
var dataPosition4 = DataPosition ( "4", 37.2, 128.5)
var dataPosition5 = DataPosition ( "5", 37.3, 129.0)
var dataPosition6 = DataPosition ( "6", 37.9, 127.4)
var dataPosition7 = DataPosition ( "7", 37.7, 126.9)
var myPos =(LatLng(myLatitude, myLongitude))
var posArray = arrayListOf<DataPosition>(dataPosition, dataPosition2, dataPosition3, dataPosition4,
    dataPosition5, dataPosition6, dataPosition7)

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
    private lateinit var jsonArray: JSONArray
    private lateinit var clusterManager: ClusterManager<ClusterItem>
    private val markerList = mutableListOf<MarkerOptions>()  //밀집도 표시를 위한 리스트
    private val markerList2 = mutableListOf<MarkerOptions>()  //접촉자 표시를 위한 리스트
    val PERM_FLAG = 99

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        //jsonArray = readAssets()
        if (isPermitted()){
            startProcess()
        }else{
            ActivityCompat.requestPermissions(this, permissions, PERM_FLAG)
        }
    }

    //json 파일 읽는 기능도 구현 가능 (아래 주석 = addmarkers, json파일로 읽어서 마커 생성하는 방법)

/*
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
        for (i in 0 until markerList.size){
            Log.d("마킹2", "위치 : ${markerList[i].position}, 이름 : ${markerList[i].title}")
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerList[0].position, 13.0f))
    }
*/




    val locRef = db.collection("Users")
    val query = locRef.whereEqualTo("check", true)

    private fun addMarkers(){
        //var lat : Double = 0.0
        //var lng : Double = 0.0

        //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myPos, 13.0f))
        db.collection("Users")                       //Users 콜렉션에서
           .whereEqualTo("Loc_Store_Agree",true)     //field에 만들어놓은 위치정보 동의여부가 true인지 체크
           .get().addOnSuccessListener { documents ->            //위의 조건을 만족한 document(User uid)들을 불러와서
               for(document in documents) { //document를 하나씩 반복문으로 돌린다.
                   val name = document.getString("email")  //name 변수에 현재 돌리는 document의 email을 넣는다
                   if(document.getDouble("Lat")!=null && document.getDouble("Long")!=null) {
                       // field의 Lat, Lng 가 null이 아닌경우에 제한, 이렇게 하지 않으면 지도가 튕김....
                   val lat = document.getDouble("Lat")!!
                   val lng = document.getDouble("Long")!!
                   val marker = MarkerOptions().position(LatLng(lat,lng)).title(name) //marker 정의
                   markerList.add(marker)
                      // mMap.addMarker(marker)
                       Log.d("마킹", "위도: ${lat}, 경도: ${lng}, email: ${name}")
                       //위치 정보 수락한 uid에 한하여 잘 불러오고 있는지 확인
                   }
                }
                setupClusterManager()

                for (i in 0 until markerList.size){
                    Log.d("마킹2", "위치 : ${markerList[i].position}, 이름 : ${markerList[i].title}")
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myPos, 13.0f))
           }

    }




    private fun ContactsMmarker(){
        db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid).collection("Contacts")
            .whereEqualTo("Loc_Store_Agree",true)
            .get().addOnSuccessListener { documents ->            //위의 조건을 만족한 document(User uid)들을 불러와서
                for(document in documents) { //document를 하나씩 반복문으로 돌린다.
                    val name = document.getString("Who")  //name 변수에 현재 돌리는 document의 who을 넣는다
                    val contactTime = document.getTimestamp("When")
                    if(document.getDouble("Lat")!=null && document.getDouble("Long")!=null) {
                        // field의 Lat, Lng 가 null이 아닌경우에 제한, 이렇게 하지 않으면 지도가 튕김....
                        val lat = document.getDouble("Lat")!!
                        val lng = document.getDouble("Long")!!
                        val descriptor = getDescriptorFromDrawable(R.drawable.redcirlce) //접촉자 마커 모양 설정
                        val marker = MarkerOptions().position(LatLng(lat,lng)).title(null).icon(descriptor) //marker 정의
                        markerList2.add(marker)
                        //mMap.addMarker(marker)
                        Log.d("마킹3", "위도: ${lat}, 경도: ${lng}, contact: ${name}")
                        //위치 정보 수락한 uid에 한하여 잘 불러오고 있는지 확인
                    }
                }
                for (i in 0 until markerList2.size){
                    Log.d("마킹4", "위치 : ${markerList2[i].position}, 이름 : ${markerList2[i].title}")
                }
                for (i in 0 until markerList2.size){
                   mMap.addMarker(markerList2[i])
                }
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
        clusterManager = ClusterManager(this, googleMap)
        addMarkers()
        mypos_btn.setOnCheckedChangeListener{ _, ischecked ->
            if(ischecked) {
                setupdateLocationListner()
                val descriptor = getDescriptorFromDrawable(R.drawable.marker)
                val marker = MarkerOptions()
                    .position(myPos)
                    .title("myPosition")
                    .icon(descriptor)
                mMap.addMarker(marker)
            }
            else{
                if(contact_btn.isChecked){
                    mMap.clear()
                    markerList.clear()
                    markerList2.clear()
                    clusterManager = ClusterManager(this, googleMap)
                    addMarkers()
                    ContactsMmarker()
                }
                else{
                    mMap.clear()
                    markerList.clear()
                    markerList2.clear()
                    clusterManager = ClusterManager(this, googleMap)
                    addMarkers()
                }
            }
        }
        contact_btn.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ContactsMmarker()
            }
            else{
                if(mypos_btn.isChecked){
                    mMap.clear()
                    markerList.clear()
                    markerList2.clear()
                    setupdateLocationListner()
                    val descriptor = getDescriptorFromDrawable(R.drawable.marker)
                    val marker = MarkerOptions()
                        .position(myPos)
                        .title("myPosition")
                        .icon(descriptor)
                    mMap.addMarker(marker)
                    clusterManager = ClusterManager(this, googleMap)
                    addMarkers()
                }
                else{
                    mMap.clear()
                    markerList.clear()
                    markerList2.clear()
                    clusterManager = ClusterManager(this, googleMap)
                    addMarkers()
                }
            }
        }

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
                        myPos = LatLng(location.latitude,location.longitude)
                        //setLastLocation(location)
                        val docRef = db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid)
                        docRef.get()
                            .addOnSuccessListener { document ->
                                if (document != null) {
                                    val Loc_Store_Agree_value = document.getBoolean("Loc_Store_Agree")
                                    if(Loc_Store_Agree_value == true)
                                    {
                                        val data = hashMapOf("Lat" to location.latitude,"Long" to location.longitude)   //Firestore 필드 : 위도, 경도 (내 위치)
                                        // 기생성된 Users 컬렉션의 각 멤버 문서에 종속되는 컬렉션 Location에 위치 데이터 저장하기
                                        db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid).set(data, SetOptions.merge())  //firestore에 data 삽입
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
        val mymarker = LatLng(location.latitude,location.longitude) //내 좌표
        myPos = mymarker
        val descriptor = getDescriptorFromDrawable(R.drawable.marker)
        val marker = MarkerOptions()
            .position(mymarker)
            .title("myPosition")
            .icon(descriptor)
        mMap.addMarker(marker)
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