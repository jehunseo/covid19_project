package com.example.covid19_project

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.covid19_project.Extensions.toast
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

var myLatitude = 0.0
var myLongitude = 0.0
class MainActivity : AppCompatActivity() {
    lateinit var fusedLocationClient: FusedLocationProviderClient  //배터리소모, 정확도 관리
    lateinit var locationCallback: LocationCallback //위를 통해 받은 좌표를 callback하는 역할

    private var requestQueue: RequestQueue? = null
    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null

    private var locationRequest = LocationRequest.create()
    val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION)
    private val db = FirebaseFirestore.getInstance()  //firestore db

    ////////////////////////////////////////////////////////////////////////////////////
    //permission for location
    ////////////////////////////////////////////////////////////////////////////////////
    val PERM_FLAG = 99

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
    ////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////

    private fun initLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location == null) {
                    Log.e("mainloc", "location get fail")
                } else {
                    Log.d("mainloc", "${location.latitude} , ${location.longitude}")
                    myLatitude = location.latitude
                    myLongitude = location.longitude
                    Log.d("myloc", "lat : $myLatitude, Long: $myLongitude")
                }
            }
            .addOnFailureListener {
                Log.e("mainloc", "location error is ${it.message}")
                it.printStackTrace()
            }
    }

    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
        initLocation()

        ////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////
        val myUid = FirebaseUtils.firebaseAuth.currentUser.uid
        val db = Firebase.firestore
        Log.d("myuid", myUid)
        //delete old data
        db.collection("Users").document(myUid)
            .collection("Contacts")
            .whereLessThan("When", getNowTime().let { Date(it) })
            .get()
            .addOnSuccessListener { documents ->
                Log.d("DBtest", "this user has ${documents.size()} old record(s)")
                for (document in documents) {
                    db.collection("Users").document(myUid)
                        .collection("Contacts").document(document.id)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("DBtest",
                                "DocumentSnapshot successfully deleted!")
                        }
                        .addOnFailureListener { e -> Log.w("DBtest", "Error deleting document", e) }
                }
            }
            .addOnFailureListener { exception ->
                Log.w("DBtest", "Error getting documents: ", exception)
            }
        ////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////
        val intent = Intent(this, MapsActivity::class.java)
        val fragmentAdapter = MyPagerAdapter(supportFragmentManager)
        sample_content_fragment.adapter = fragmentAdapter

        tabLayout.post {
            tabLayout.setupWithViewPager(sample_content_fragment)
            tabLayout.setTabsFromPagerAdapter(fragmentAdapter)
            tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_baseline_warning_24)
        } //tap에서 mapfragment 제거 후 해당 위치 map Activity로 대체
        requestQueue = Volley.newRequestQueue(this)

        ///////////////////////////////////////////////////////////////////////////////////////
        //NFC Check////////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        nfcPendingIntent = PendingIntent.getActivity(this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

        mapbtn.setOnClickListener({
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        })//MAP 버튼 클릭시 구글맵으로 넘어감

        //checkPermission()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            this.startForegroundService(intent)
        } else {
            this.startService(intent)
        }

        //fusedLocation = LocationServices.getFusedLocationProviderClient(this)
        //updateLocation()

        /* 다이나믹 링크 값을 얻고, 저장한다 */
        FirebaseDynamicLinks.getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener {
                var deppLink: Uri? = null
                if (it != null) {
                    deppLink = it.link
                    val who = deppLink!!.path!!.replace("/meet/", "")
                    // 접촉 기록 저장 시작
                    val log_scanning = hashMapOf(
                        "Who" to who,
                        "When" to getNowTime().let { Date(it) }
                    )
                    db.collection("Users").document(myUid)
                        .collection(
                            "Contacts").add(log_scanning)
                        .addOnSuccessListener { toast(who + "와의 접촉 기록이 저장되었어요") }
                        .addOnFailureListener { toast("접촉 기록 저장 실패") }
                }
            }
            .addOnFailureListener {
                toast("다이나믹 링크 동작 에러")
            }

        // 만약 토큰이 DB에 저장되어 있지 않으면 DB에 저장한다. 이미 저장되어있으면 아무런 작업도하지 않는다
        db.collection("Users").document(myUid).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    if (document.get("Push_ID") == null) {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener(
                            OnCompleteListener { task ->
                                if (!task.isSuccessful) {
                                    toast("PUSH알림 토큰을 가져올때 문제가 발생했어요")
                                    return@OnCompleteListener
                                }
                                // token=현재 디바이스의 푸시 토큰
                                val token = task.result

                                db.collection("Users").document(myUid)
                                    .update("Push_ID", token)
                                    .addOnSuccessListener { toast("푸시 ID 를 DB에 등록하였어요") }
                                    .addOnFailureListener { toast("푸시 ID 를 DB에 등록하지 못했어요") }
                            })
                    }
                } else {
                    toast("사용자의 DB가 존재하지 않아요")
                }
            }
            .addOnFailureListener { exception ->
                toast("DB접근에 실패하였어요")
            }
    }

    override fun onResume() {
        super.onResume()
        setupdateLocationListner()
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null);

        Log.d("myloc ", "lat:$myLatitude,Long:$myLongitude")
    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch, as this activity is no longer in the foreground
        nfcAdapter?.disableForegroundDispatch(this);
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }



    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
                // Retrieve the raw NDEF message from the tag
                val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                // Complete variant: parse NDEF messages
                if (rawMessages != null) {
                    val messages =
                        arrayOfNulls<NdefMessage?>(rawMessages.size)// Array<NdefMessage>(rawMessages.size, {})
                    for (i in rawMessages.indices) {
                        messages[i] = rawMessages[i] as NdefMessage;
                    }
                    // Process the messages array.
                    processNdefMessages(messages)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        setupdateLocationListner()
    }

    private fun processNdefMessages(ndefMessages: Array<NdefMessage?>) {
        // Go through all NDEF messages found on the NFC tag
        for (curMsg in ndefMessages) {
            if (curMsg != null) {
                // Loop through all the records contained in the message
                for (curRecord in curMsg.records) {
                    var text = curRecord.payload.contentToString()
                    text = text.substring(1, text.length - 1).replace(",", "")
                    var tmp = text.split(" ")
                    var content: String = ""

                    for (t in tmp.slice(IntRange(3, tmp.size - 1))) {
                        content += t.toInt().toChar()
                    }
                    val addtag = AddTag(applicationContext,
                        FirebaseUtils.firebaseAuth.currentUser.uid,
                        content,
                        getNowTime().let { Date(it) }
                    )
                    addtag.start()
                }
            }
        }
    }

    /* 뒤로가기 버튼 버그 수정*/
    private var backPressedTime: Long = 0
    lateinit var backToast: Toast
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
                        myLatitude = location.latitude
                        myLongitude = location.longitude
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
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    fun serviceStart(view:View){
        val intent = Intent(this, Foreground::class.java)
        ContextCompat.startForegroundService(this,intent)
    }

    fun serviceStop(view:View){
        val intent = Intent(this, Foreground::class.java)
        stopService(intent)
    }
}

// Bluetooth Reference : https://developer.android.com/guide/topics/connectivity/bluetooth
class AddTag(
    val context: Context,
    val tag_main: String,
    val tag_sub: String,
    val time: Date?
) : Thread() {
    override fun run() {
        val tag = TagEntity(tag_main, tag_sub, time)
        TagDatabase
            .getInstance(context)!!
            .getTagDao()
            .insert(tag)
    }
}

fun getNowTime():Long{
    var sdf = SimpleDateFormat("yyyy-MM-dd HH", Locale.KOREA).format(Date())
    var currentLong = SimpleDateFormat("yyyy-MM-dd HH")
        .parse(sdf, ParsePosition(0)).time

    return currentLong
}
