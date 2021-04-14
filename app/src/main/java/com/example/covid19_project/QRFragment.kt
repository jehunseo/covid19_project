package com.example.covid19_project

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
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
        val qrGenButton: Button = qrView.findViewById(R.id.qrgenbutton)
        val imageViewQrCode: ImageView = qrView.findViewById(R.id.qrView)
        //https://dwfox.tistory.com/79
        qrButton.setOnClickListener(){
            val integrator = IntentIntegrator.forSupportFragment(this);

            integrator.setOrientationLocked(false); //세로
            integrator.setPrompt("QR을 인식해주세요!!"); //QR코드 화면이 되면 밑에 표시되는 글씨 바꿀수있음
            integrator.initiateScan();
        }

        qrGenButton.setOnClickListener(){
            try {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(FirebaseUtils.firebaseAuth.currentUser.uid, BarcodeFormat.QR_CODE, 400, 400)
                imageViewQrCode.setImageBitmap(bitmap)
            } catch (e: Exception) {
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

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())
                val addtag = AddTagQR(
                    requireContext(),
                    FirebaseUtils.firebaseAuth.currentUser.uid,
                    result.getContents(),
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
    val tag_sub: String,
    val time: String) : Thread() {
    override fun run() {
        val tag = TagEntity(tag_main, tag_sub, time)
        TagDatabase
            .getInstance(context)!!
            .getTagDao()
            .insert(tag)
    }
}