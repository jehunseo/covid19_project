package com.example.covid19_project

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.covid19_project.Extensions.toast
import com.google.android.gms.location.*
import com.google.firebase.Timestamp
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.locationManager
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
    val permissions = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)

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
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if(location == null) {
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
        requestPermission()
        setContentView(R.layout.activity_main)
        initLocation()

        ////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////
        //test
        val sdf = SimpleDateFormat("yyyy-MM-dd HH", Locale.KOREA).format(Date())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH")
        val currentDay = dateFormat.parse(sdf, ParsePosition(0))
        val currentLong = currentDay.time - 1210000000 // 2주 전

        val db = Firebase.firestore
        db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid).collection("Contacts")
            .whereGreaterThan("When", currentLong.let{Date(it)})
            .get()
            .addOnSuccessListener { documents ->
                Log.d("DBtest", "this user has ${documents.size()} contact record(s)")
                for (document in documents) {
                    val timestamp = document.data.get("When") as Timestamp
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH").format(timestamp.toDate())
                    Log.d("DBtest", "${document.data.get("Who")}, ${dateFormat}")
                }
            }
            .addOnFailureListener { exception ->
                Log.w("DBtest", "Error getting documents: ", exception)
            }
        //delete old data
        db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid).collection("Contacts")
            .whereLessThan("When", currentLong.let{Date(it)})
            .get()
            .addOnSuccessListener { documents ->
                Log.d("DBtest", "this user has ${documents.size()} old record(s)")
                for (document in documents) {
                    db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid)
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

        /////////////////////////////////////////////////////////////////////////////////////////////
        //NFC Check////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////////////
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
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH", Locale.KOREA).format(Date())
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH")
                    val currentDay = dateFormat.parse(sdf, ParsePosition(0))
                    val currentLong = currentDay.time

                    val db = Firebase.firestore
                    val log_scanning = hashMapOf(
                        "Who" to who,
                        "When" to currentLong.let{Date(it)}
                    )
                    db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid).collection(
                        "Contacts").add(log_scanning)
                        .addOnSuccessListener {toast(who +"와의 접촉 기록이 저장되었어요")}
                        .addOnFailureListener {toast("접촉 기록 저장 실패")}
                } else {
                }

            }
            .addOnFailureListener {
                toast("다이나믹 링크 동작 에러")
            }
    }

    override fun onResume() {
        super.onResume()
        // Get all NDEF discovered intents
        // Makes sure the app gets all discovered NDEF messages as long as it's in the foreground.
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null);
        // Alternative: only get specific HTTP NDEF intent
        //nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, nfcIntentFilters, null);


        locationRequest.run{
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
        }
        locationCallback = object: LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult?.let {
                    for((i, location) in it.locations.withIndex()) {
                        Log.d("mainloc", "#$i ${location.latitude} , ${location.longitude}")
                        myLatitude = location.latitude
                        myLongitude = location.longitude
                    }
                }
            }
        }
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
        if (intent != null) processIntent(intent)
    }

    private fun processIntent(checkIntent: Intent) {
        // Check if intent has the action of a discovered NFC tag
        // with NDEF formatted contents
        if (checkIntent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            // Retrieve the raw NDEF message from the tag
            val rawMessages = checkIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            Log.d("NFC2", "Raw messages" + rawMessages?.size.toString())

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

    /**
     * Parse the NDEF message contents and print these to the on-screen log.
     */
    private fun processNdefMessages(ndefMessages: Array<NdefMessage?>) {
        // Go through all NDEF messages found on the NFC tag
        for (curMsg in ndefMessages) {
            if (curMsg != null) {
                // Loop through all the records contained in the message
                for (curRecord in curMsg.records) {
                    if (curRecord.toUri() != null) {
                        // URI NDEF Tag
                        var url = curRecord.toUri().toString()
                        Log.d("NFC5", "- URI : " + url)

                    } else {
                        // Other NDEF Tags - simply print the payload
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH", Locale.KOREA).format(Date())
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH")
                        val currentDay = dateFormat.parse(sdf, ParsePosition(0))
                        val currentLong = currentDay.time

                        var text = ""
                        text = curRecord.payload.contentToString()
                        text = text.substring(1, text.length - 1).replace(",", "")
                        var tmp = text.split(" ")
                        var content : String = ""

                        for (t in tmp.slice(IntRange(3, tmp.size - 1))) {
                            content += t.toInt().toChar()
                        }
                        Log.d("NFC6", "- URI : " + content)
                        val addtag = AddTag(applicationContext,
                            FirebaseUtils.firebaseAuth.currentUser.uid,
                            content,
                            currentLong.let{Date(it)}
                        )
                        addtag.start()
                    }
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



}

// Bluetooth Reference : https://developer.android.com/guide/topics/connectivity/bluetooth

class GetTag(val context: Context) : Thread() {
    override fun run() {
        val items = TagDatabase
            .getInstance(context)!!
            .getTagDao()
            .getAll()

        for (i in items) {
            Log.d("bookList", "${i.tag_main} | ${i.tag_sub} | ${i.time}")
        }
    }
}

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