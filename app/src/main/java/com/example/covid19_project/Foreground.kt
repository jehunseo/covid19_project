package com.example.covid19_project

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.concurrent.thread
private val db = FirebaseFirestore.getInstance()  //firestore db
class Foreground : Service() {
    lateinit var fusedLocationClient: FusedLocationProviderClient  //배터리소모, 정확도 관리
    lateinit var locationCallback: LocationCallback //위를 통해 받은 좌표를 callback하는 역할


    // -- 주기적으로 좌표를 업뎃해주는 역할
    @SuppressLint("MissingPermission")
    fun setupdateLocationListner() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
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
    }

    val CHANNEL_ID = "FGS153"  //단순히 channel id 생성
    val NOTI_ID = 153

    fun createNotificationChannel() {  //notification channel 생성
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {  //오레오버전 이상
            val serviceChannel = NotificationChannel(CHANNEL_ID, "FOREGROUND",NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java) //notificationmanager를 시스템 서비스를 통해서 생성
            manager.createNotificationChannel(serviceChannel) //manager로 내 서비스를 이 channel에서 쓰겠다는 것을 알림
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this,CHANNEL_ID)
            .setContentTitle("Ajou Covid Foreground Service")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .build()
        runBackground()
        startForeground(NOTI_ID,notification) //내가 사용하는 service가 foreground로 동작함을 알려줌, 호출되는 순간 notification 뜸

        return super.onStartCommand(intent, flags, startId)
    }

    fun runBackground(){
        thread(start=true){
            for(i in 0..100) {
                Thread.sleep(1000)
                setupdateLocationListner()
                Log.d("background","$myPos")
            }
        }
    }


    override fun onBind(intent: Intent): IBinder {
        //onBindrk 실행되면 TODO가 실행되는데 TODO가 실행되면 앱이 죽는다.
        return Binder()
    }
}