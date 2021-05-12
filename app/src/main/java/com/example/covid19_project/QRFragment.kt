package com.example.covid19_project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.android.synthetic.main.fragment_qr.*
import java.text.SimpleDateFormat
import java.util.*

class QRFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val qrView = inflater.inflate(R.layout.fragment_qr, container, false)
        val qrButton:Button = qrView.findViewById(R.id.qrbutton)
        val nfcToggleButton : ToggleButton = qrView.findViewById(R.id.nfcToggleButton)
        val imageViewQrCode: ImageView = qrView.findViewById(R.id.qrView)
        val fragment = CardReaderFragment()
        val bundle = Bundle()
        //https://dwfox.tistory.com/79

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(FirebaseUtils.firebaseAuth.currentUser.uid,
                BarcodeFormat.QR_CODE,
                400,
                400)
            // UID를 QR코드로 만들면, QR 리더에서 이를 인식하면 해당 UID에 접속했다는 기록을 남기도록 구현해야지!
            imageViewQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
        }
        qrButton.setOnClickListener(){
            val integrator = IntentIntegrator.forSupportFragment(this);
            integrator.setOrientationLocked(false); //세로
            integrator.setPrompt("QR을 인식해주세요!!"); //QR코드 화면이 되면 밑에 표시되는 글씨 바꿀수있음
            integrator.initiateScan();
        }
        nfcToggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (savedInstanceState == null) {
                    bundle.putString("Account", "")
                    fragment.arguments = bundle

                    activity?.supportFragmentManager!!.beginTransaction()
                        .add(R.id.data_view, fragment)
                        .commit()

                    setFragmentResultListener("account") { key, bundle ->
                        val result = bundle.getString("account")
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
                        // 디바이스 내부 DB에도 저장하고, 그리고 각 유저의 클라우드 DB에도 저장해보자
                        // 현재 QR코드를 촬영한 유저의 클라우드 DB에 접촉 기록을 저장
                        val db = Firebase.firestore
                        // 위치정보 수집 동의 값 가져오기
                        val log_scanning = hashMapOf(
                            "Who" to result,
                            "When" to sdf
                        )
                        db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid).collection(
                            "Contacts").add(log_scanning)
                            .addOnSuccessListener { Toast.makeText(requireActivity(),
                                "접촉 기록이 저장되었어요",
                                Toast.LENGTH_SHORT).show() }
                            .addOnFailureListener { Toast.makeText(requireActivity(),
                                "접촉 기록 저장 실패",
                                Toast.LENGTH_SHORT).show() }

                        val addtag = AddTagQR(
                            requireContext(),
                            FirebaseUtils.firebaseAuth.currentUser.uid,
                            result,
                            sdf
                        )
                        Log.d("QRFragment", "${FirebaseUtils.firebaseAuth.currentUser.uid}||${result}")
                        addtag.start()
                    }

                }
            } else {
                activity?.supportFragmentManager?.beginTransaction()
                    ?.remove(fragment)
                    ?.commit()
                AccountStorage.SetAccount(activity, FirebaseUtils.firebaseAuth.currentUser.uid)
            }
        }


        return qrView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        val result =
            IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents == null) {
                Toast.makeText(getActivity(), "Cancelled", Toast.LENGTH_LONG).show()
            } else {
                val scanned_uid = result.getContents() // 스캔한 유저의 UID저장
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
                // 디바이스 내부 DB에도 저장하고, 그리고 각 유저의 클라우드 DB에도 저장해보자
                // 현재 QR코드를 촬영한 유저의 클라우드 DB에 접촉 기록을 저장
                val db = Firebase.firestore
                // 위치정보 수집 동의 값 가져오기
                val log_scanning = hashMapOf(
                    "Who" to scanned_uid,
                    "When" to sdf
                )
                db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid).collection(
                    "Contacts").add(scanned_uid)
                    .addOnSuccessListener { Toast.makeText(requireActivity(),
                        "접촉 기록이 저장되었어요",
                        Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(requireActivity(),
                        "접촉 기록 저장 실패",
                        Toast.LENGTH_SHORT).show() }
                // 클라우드 DB부분 코드 끝!

                val addtag = AddTagQR(
                    requireContext(),
                    FirebaseUtils.firebaseAuth.currentUser.uid,
                    scanned_uid,
                    sdf
                )
                addtag.start()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}

class AddTagQR(
    val context: Context,
    val tag_main: String,
    val tag_sub: String?,
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