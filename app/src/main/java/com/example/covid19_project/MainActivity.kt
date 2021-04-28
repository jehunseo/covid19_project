package com.example.covid19_project

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    /*
    private lateinit var fusedLocation: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    */

    private var requestQueue: RequestQueue? = null

    private var nfcAdapter: NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null

    ////////////////////////////////////////////////////////////////////////////////////
    // function for php data parse
    ////////////////////////////////////////////////////////////////////////////////////
    /*
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
                    val tag_main = BLELOG.getString("tag_main")
                    val tag_sub = BLELOG.getString("tag_sub")
                    val TIME = BLELOG.getString("TIME")

                    Log.d("dat", "$tag_main, $tag_sub, $TIME\n\n")
                }


            } catch (e: JSONException) {
                e.printStackTrace()
            }
        },
        { error -> error.printStackTrace() },
    )

    requestQueue?.add(request)
}
*/
    //jsonParse()

    ////////////////////////////////////////////////////////////////////////////////////
    //permission for location
    ////////////////////////////////////////////////////////////////////////////////////
    /*
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
    */

    ////////////////////////////////////////////////////////////////////////////////////
    // for android DB
    ////////////////////////////////////////////////////////////////////////////////////
    private val helper = DataBaseHelper(this, "memo", 1)

    ////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////
    override fun onCreate(savedInstanceState: Bundle?) {
        val intent = Intent(this, MapsActivity::class.java)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fragmentAdapter = MyPagerAdapter(supportFragmentManager)
        sample_content_fragment.adapter = fragmentAdapter



        tabLayout.post {
            tabLayout.setupWithViewPager(sample_content_fragment)
            tabLayout.setTabsFromPagerAdapter(fragmentAdapter)
            tabLayout.getTabAt(1)?.setIcon(R.drawable.ic_baseline_warning_24)
        } //tap에서 mapfragment 제거 후 해당 위치 map Activity로 대체
        requestQueue = Volley.newRequestQueue(this)


        test.setOnClickListener({
            val gettag = GetTag(applicationContext)
            gettag.start()
        })


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
    }

    override fun onResume() {
        super.onResume()
        // Get all NDEF discovered intents
        // Makes sure the app gets all discovered NDEF messages as long as it's in the foreground.
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null);
        // Alternative: only get specific HTTP NDEF intent
        //nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, nfcIntentFilters, null);
    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch, as this activity is no longer in the foreground
        nfcAdapter?.disableForegroundDispatch(this);
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
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())

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
                            sdf
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
    val time: String,
) : Thread() {
    override fun run() {
        val tag = TagEntity(tag_main, tag_sub, time)
        TagDatabase
            .getInstance(context)!!
            .getTagDao()
            .insert(tag)
    }
}