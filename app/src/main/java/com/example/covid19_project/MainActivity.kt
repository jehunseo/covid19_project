package com.example.covid19_project

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import okio.IOException
import java.net.URL
import android.widget.Button
import android.widget.TextView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException


class MainActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private var requestQueue: RequestQueue? = null
    private fun jsonParse() {
        val url = "https://ajouycdcovid19.com/print.php"
        val request = JsonObjectRequest(Request.Method.GET, url, null, Response.Listener {
                response ->try {
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
        }, Response.ErrorListener { error -> error.printStackTrace() })
        requestQueue?.add(request)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.textViewResult)
        val button_parse: Button = findViewById(R.id.btnParse)
        requestQueue = Volley.newRequestQueue(this)
        button_parse.setOnClickListener {
            jsonParse()
        }

        val permission1 = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
        val permission2 = ContextCompat.checkSelfPermission(this,
            Manifest.permission.BLUETOOTH_ADMIN)
        val permission3 = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_COARSE_LOCATION)
        val permission4 = ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION)

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        val intent = Intent(this, BluetoothService::class.java)

        // URL을 만들어 주고
        val ipAddress = "https://ajouycdcovid19.com/dbADMIN/index.php"
        val url = URL(ipAddress)

        if (permission1 != PackageManager.PERMISSION_GRANTED
                || permission2 != PackageManager.PERMISSION_GRANTED
                || permission3 != PackageManager.PERMISSION_GRANTED
                || permission4 != PackageManager.PERMISSION_GRANTED) {
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
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show()
            finish()
        }

        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 5)
        }

        button.setOnClickListener() {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                this.startForegroundService(intent)
            }
            else{
                this.startService(intent)
            }
        }

        /*
        button2.setOnClickListener(){

            //데이터를 담아 보낼 바디를 만든다
            val requestBody : RequestBody = FormBody.Builder()
                .add("id","아이디")
                .build()

            // OkHttp Request 를 만들어준다.
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            // 클라이언트 생성
            val client = OkHttpClient()
            Log.d("전송 주소 ",ipAddress)
            // 요청 전송
            client.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    Log.d("요청","요청 완료")
                }
                override fun onFailure(call: Call, e: IOException) {
                    Log.d("요청","요청 실패 ")
                }
            })


        }
         */

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 5
                && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "취소 했습니다", Toast.LENGTH_SHORT).show()
            finish()
        } else if (requestCode == 5
                && resultCode == RESULT_OK) {
            Toast.makeText(this, "블루투스를 활성화합니다.", Toast.LENGTH_SHORT).show()
        }
    }
}

// Bluetooth Reference : https://developer.android.com/guide/topics/connectivity/bluetooth