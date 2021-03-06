package com.example.covid19_project

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.print.PrintHelper
import androidx.print.PrintHelper.COLOR_MODE_MONOCHROME
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.zxing.BarcodeFormat
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.BarcodeEncoder
import org.jetbrains.anko.find
import java.util.*

class QRFragment : Fragment() {
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val qrView = inflater.inflate(R.layout.fragment_qr, container, false)

        val qrButton : Button = qrView.findViewById(R.id.qrbutton)
        val nfcToggleButton : ToggleButton = qrView.findViewById(R.id.nfcToggleButton)
        val printButton : Button = qrView.find(R.id.printButton)
        val nfcWriteButton : Button = qrView.findViewById(R.id.nfcWriteButton)

        val imageViewQrCode: ImageView = qrView.findViewById(R.id.qrView)
        val fragment = CardReaderFragment()
        val bundle = Bundle()

        // QR코드 생성시 현재 유저의 UID를 담은 다이나믹 링크를 바로 생성한다
        val DEEPLINK_URL = "https://ajouycdcovid19.com/"
        val SHORT_DYNAMIC_LINK = "https://ajouunivcovid19.page.link"
        val PACKAGE_NAME = "com.example.covid19_project"

        val myUid = FirebaseUtils.firebaseAuth.currentUser.uid

        fun createDynamicLink(): String {
            return FirebaseDynamicLinks.getInstance()
                .createDynamicLink()
                .setLink(Uri.parse(DEEPLINK_URL))
                .setDomainUriPrefix(SHORT_DYNAMIC_LINK)
                .setAndroidParameters(
                    DynamicLink.AndroidParameters.Builder(PACKAGE_NAME)
                        .build()
                )
                .buildDynamicLink()
                .uri.toString() + "meet/" +  myUid //  https://ajoucovid19.com/meet/접촉UID
        }
        val myDynamicLink = createDynamicLink()

        // 다이나믹 링크 생성 부분 끝
        val barcodeEncoder = BarcodeEncoder()
        val bitmap = barcodeEncoder.encodeBitmap(myDynamicLink,
                BarcodeFormat.QR_CODE,
                400,
                400)
            // UID를 QR코드로 만들면, QR 리더에서 이를 인식하면 해당 UID에 접속했다는 기록을 남기도록 구현해야지!
        imageViewQrCode.setImageBitmap(bitmap)

        //QR촬영버튼 동작
        qrButton.setOnClickListener(){
            val integrator = IntentIntegrator.forSupportFragment(this);
            integrator.setOrientationLocked(false); //세로
            integrator.setPrompt("QR을 인식해주세요!!"); //QR코드 화면이 되면 밑에 표시되는 글씨 바꿀수있음
            integrator.initiateScan();
        }

        //NFC 리더기 동작
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

                        // 디바이스 내부 DB에도 저장하고, 그리고 각 유저의 클라우드 DB에도 저장해보자
                        // 현재 QR코드를 촬영한 유저의 클라우드 DB에 접촉 기록을 저장
                        val db = Firebase.firestore
                        // 위치정보 수집 동의 값 가져오기
                        val log_scanning = hashMapOf(
                            "Who" to result,
                            "When" to getNowTime().let{Date(it)},
                            "Lat" to myLatitude,
                            "Long" to myLongitude,
                            "Loc_Store_Agree" to true
                        )
                        db.collection("Users").document(myUid).collection(
                            "Contacts").add(log_scanning)
                            .addOnSuccessListener { Toast.makeText(requireActivity(),
                                "접촉 기록이 저장되었어요",
                                Toast.LENGTH_SHORT).show() }
                            .addOnFailureListener { Toast.makeText(requireActivity(),
                                "접촉 기록 저장 실패",
                                Toast.LENGTH_SHORT).show() }

                        //피접촉 기록 저장
                        log_scanning.replace("Who", myUid)

                        db.collection("Users").document(result!!).collection(
                            "Contacts").add(log_scanning)
                            .addOnSuccessListener { Toast.makeText(requireActivity(),
                                "접촉 기록이 저장되었어요",
                                Toast.LENGTH_SHORT).show() }
                            .addOnFailureListener { Toast.makeText(requireActivity(),
                                "접촉 기록 저장 실패",
                                Toast.LENGTH_SHORT).show() }

                        AddTagQR(
                            requireContext(),
                            myUid,
                            result,
                            getNowTime().let{Date(it)}
                        ).start()
                    }

                }
            } else {
                activity?.supportFragmentManager?.beginTransaction()
                    ?.remove(fragment)
                    ?.commit()
                AccountStorage.SetAccount(activity, myUid)
            }
        }

        //QR 출력
        printButton.setOnClickListener() {
            activity?.also { context ->
                PrintHelper(context).apply {
                    scaleMode = PrintHelper.SCALE_MODE_FIT
                }.also { printHelper ->
                    val bitmap = (imageViewQrCode.drawable as BitmapDrawable).bitmap

                    printHelper.colorMode = COLOR_MODE_MONOCHROME
                    printHelper.printBitmap("test print", bitmap)
                }
            }
        }
        //NFC 태그 기록
        nfcWriteButton.setOnClickListener(){
            val nfcWriteIntent = Intent(activity, NFCWriteActivity::class.java)
            nfcWriteIntent.apply{
                this.putExtra("myLink", myDynamicLink)
            }
            startActivity(nfcWriteIntent)
        }

        return qrView
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.N)
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
                val scanned = result.getContents()
                val scanned_uid = scanned.replace(
                    "https://ajouunivcovid19.page.link?apn=com.example.covid19_project&link=https%3A%2F%2Fajouycdcovid19.com%2Fmeet/",
                    ""
                )

                // 디바이스 내부 DB에도 저장하고, 그리고 각 유저의 클라우드 DB에도 저장해보자
                // 현재 QR코드를 촬영한 유저의 클라우드 DB에 접촉 기록을 저장
                val db = Firebase.firestore
                // 위치정보 수집 동의 값 가져오기
                // 접촉기록 collection
                var log_scanning = hashMapOf(
                    "Who" to scanned_uid,
                    "When" to getNowTime().let{Date(it)},
                    "Lat" to myLatitude,
                    "Long" to myLongitude,
                    "Loc_Store_Agree" to true
                )
                db.collection("Users").document(FirebaseUtils.firebaseAuth.currentUser.uid).collection(
                    "Contacts").add(log_scanning)
                    .addOnSuccessListener { Toast.makeText(requireActivity(),
                        "접촉 기록이 저장되었어요",
                        Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(requireActivity(),
                        "접촉 기록 저장 실패",
                        Toast.LENGTH_SHORT).show() }

                //피접촉 collection - Who와 document uid를 맞바꿈
                log_scanning.replace("Who", FirebaseUtils.firebaseAuth.currentUser.uid)

                db.collection("Users").document(scanned_uid).collection(
                    "Contacts").add(log_scanning)
                    .addOnSuccessListener { Toast.makeText(requireActivity(),
                        "접촉 기록이 저장되었어요",
                        Toast.LENGTH_SHORT).show() }
                    .addOnFailureListener { Toast.makeText(requireActivity(),
                        "접촉 기록 저장 실패",
                        Toast.LENGTH_SHORT).show() }

                // 클라우드 DB부분 코드 끝!
                AddTagQR(
                    requireContext(),
                    FirebaseUtils.firebaseAuth.currentUser.uid,
                    scanned_uid,
                    getNowTime().let{Date(it)}
                ).start()
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
    val time: Date?,
) : Thread() {
    override fun run() {
        val tag = TagEntity(tag_main, tag_sub, time)
        TagDatabase
            .getInstance(context)!!
            .getTagDao()
            .insert(tag)
    }


}