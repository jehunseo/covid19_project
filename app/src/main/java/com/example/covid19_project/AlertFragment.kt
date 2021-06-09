package com.example.covid19_project

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.covid19_project.Extensions.toast
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.okhttp.RequestBody
import kotlinx.android.synthetic.main.fragment_alert.*
import okhttp3.*
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*


class AlertFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_alert, container, false)
    }

    //https://mainia.tistory.com/5650
    override fun onStart() {
        super.onStart()
        var state = ""
        var range = ""
        val description = "가까운 선별검사소에서 검사를 받으세요"

        buttonSend.setOnClickListener() {
            state = when (radioGroup.checkedRadioButtonId) {
                R.id.radioSympton -> "증상"
                R.id.radioIsolation -> "격리"
                R.id.radioPositive -> "접촉"
                else -> "미선택"
            }
            range = when (radioTypes.checkedRadioButtonId) {
                R.id.radioContact -> "밀접접촉"
                R.id.radioPlace -> "경로일치"
                else -> "미선택"
            }
            if (!(checkAllow.isChecked)) {
                Toast.makeText(activity,
                    "위치정보를 허용해주세요",
                    Toast.LENGTH_LONG).show()
            }
            else if (state == "미선택" || range == "미선택") {
                Toast.makeText(activity,
                    "증상과 접촉여부를 선택해주세요!",
                    Toast.LENGTH_LONG).show()
            } else {
                val myUid = FirebaseUtils.firebaseAuth.currentUser.uid
                val db = Firebase.firestore

                //쿼리시작
                db.collection("Users").document(myUid)
                    .collection("Contacts")
                    .whereGreaterThan("When", (getNowTime() - 1210000000).let { Date(it) })
                    .get()
                    .addOnSuccessListener { documents ->
                        Toast.makeText(activity,
                            "${state}(으)로 인해 ${range}한 ${documents.size()}명에게 알림을 보냈습니다.",
                            Toast.LENGTH_LONG).show()
                        // 쿼리결과에 따른 반복문문
                       for (document in documents) {
                            val timestamp = document.data.get("When") as Timestamp
                            val dateFormat =
                                SimpleDateFormat("yyyy-MM-dd HH").format(timestamp.toDate())
                            val targetuid = document.data.get("Who") as String
                           // 해당 문서의 who에 기록된 id를 통해 해당 id의 푸시아이디를 불러와야함
                           db.collection("Users").document(targetuid).get().addOnSuccessListener { document ->
                               val target_PID = document.data?.get("Push_ID")
                               val url = URL("https://ajouycdcovid19.com/push.php?target=${target_PID}&title=${state}&content=${description}")
                               val request = Request.Builder().url(url).build()
                               val client = OkHttpClient()
                               client.newCall(request).enqueue(object : Callback {
                                   override fun onResponse(call: Call, response: Response) {
                                       Log.d("요청","요청 완료")
                                   }
                                   override fun onFailure(call: Call, e: IOException) {
                                       Log.d("요청","요청 실패 ")
                                   }
                               })
                           }

                        }
                    }
            }
        }
    }
}