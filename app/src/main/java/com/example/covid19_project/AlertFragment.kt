package com.example.covid19_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_alert.*
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*

class AlertFragment : Fragment() {
    lateinit var sdf : String
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
        sdf = ""
        textDate2.setOnClickListener(){
            // https://the-illusionist.me/41
            sdf = SimpleDateFormat("yyyy-MM-dd HH", Locale.KOREA).format(Date())
            Log.d("tt", sdf)
            textDate2.text = sdf
        }

        buttonSend.setOnClickListener(){
            if(sdf == ""){
                Toast.makeText(activity, "날짜를 지정해주세요", Toast.LENGTH_LONG).show()
            }
            else{
                val sdf = textDate2.text.toString()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH")
                val currentDay = dateFormat.parse(sdf, ParsePosition(0))
                val currentLong = currentDay.time - 1210000000 // 2주 전

                val db = Firebase.firestore

                db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid)
                    .collection("Contacts")
                    .whereGreaterThan("When", currentLong.let { Date(it) })
                    .get()
                    .addOnSuccessListener { documents ->
                        Toast.makeText(activity, "접촉기록이 있는 ${documents.size()}명에게 알림을 보냈습니다.", Toast.LENGTH_LONG).show()
                        for (document in documents) {
                            val timestamp = document.data.get("When") as Timestamp
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH").format(timestamp.toDate())
                            Log.d("DBtest", "${document.data.get("Who")}, ${dateFormat}")
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.w("DBtest", "Error getting documents: ", exception)
                    }

                Toast.makeText(activity, "날짜를 지정해주세요", Toast.LENGTH_LONG)
            }
        }


    }
}