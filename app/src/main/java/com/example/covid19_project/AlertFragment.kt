package com.example.covid19_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.Constants.MessagePayloadKeys.SENDER_ID
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.google.firebase.messaging.ktx.remoteMessage
import kotlinx.android.synthetic.main.fragment_alert.*
import java.text.SimpleDateFormat
import java.util.*


class AlertFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val alertView = inflater.inflate(R.layout.fragment_alert, container, false)
        val textDate2: TextView = alertView.findViewById(R.id.textDate2)

        return inflater.inflate(R.layout.fragment_alert, container, false)
    }

    //https://mainia.tistory.com/5650
    override fun onStart() {
        super.onStart()
        textDate2.setOnClickListener(){
            // https://the-illusionist.me/41
            val sdf = SimpleDateFormat("yyyy년 MM월 dd일 E요일 HH:mm:ss z", Locale.KOREA).format(Date())
            Log.d("tt", sdf)
            textDate2.text = sdf
        }
        buttonSend.setOnClickListener(){
            TODO("Alert 기능 추가하기")
        }


    }
}