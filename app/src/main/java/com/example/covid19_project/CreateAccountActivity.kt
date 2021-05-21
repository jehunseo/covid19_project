package com.example.covid19_project

import android.content.ContentValues.TAG
import kotlinx.android.synthetic.main.activity_create_account.*
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.covid19_project.Extensions.toast
import com.example.covid19_project.FirebaseUtils.firebaseAuth
import com.example.covid19_project.FirebaseUtils.firebaseUser
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CreateAccountActivity : AppCompatActivity() {
    lateinit var userEmail: String
    lateinit var userPassword: String
    lateinit var createAccountInputsArray: Array<EditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)
        createAccountInputsArray = arrayOf(etEmail, etPassword, etConfirmPassword)
        btnCreateAccount.setOnClickListener {
            signIn()
        }

        btnSignIn2.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            toast("로그인 해 주세요")
            finish()
        }

        val checkBox = findViewById<CheckBox>(R.id.Loc_Store_Agree)
        checkBox?.setOnCheckedChangeListener { buttonView, isChecked ->
           if (isChecked) {
               toast("익명의 위치정보를 저장해요");
           }
            else {
                toast("익명의 위치정보를 저장하지 않아요")
           }
        }
    }

    /* check if there's a signed-in user*/
    override fun onStart() {
        super.onStart()
        val user: FirebaseUser? = firebaseAuth.currentUser
        user?.let {
            startActivity(Intent(this, MainActivity::class.java))
            toast("자동 로그인 되었습니다")
        }
    }

    /*입력칸 공백 확인*/
    private fun notEmpty(): Boolean = etEmail.text.toString().trim().isNotEmpty() &&
            etPassword.text.toString().trim().isNotEmpty() &&
            etConfirmPassword.text.toString().trim().isNotEmpty()
    /*동일하게 패스워드 입력되었는지 확인*/
    private fun identicalPassword(): Boolean {
        var identical = false
        if (notEmpty() &&
            etPassword.text.toString().trim() == etConfirmPassword.text.toString().trim()
        ) {
            identical = true
        } else if (!notEmpty()) {
            createAccountInputsArray.forEach { input ->
                if (input.text.toString().trim().isEmpty()) {
                    input.error = "${input.hint} 를 확인해주세요"
                }
            }
        } else {
            toast("비밀번호를 동일하게 두번 입력해주세요 !")
        }
        return identical
    }
    /*비밀번호 동일하게 입력되었을 경우 변수에 입력한 이메일, 비밀번호 초기화*/
    private fun signIn() {
        if (identicalPassword()) {
            // identicalPassword() returns true only  when inputs are not empty and passwords are identical
            userEmail = etEmail.text.toString().trim()
            userPassword = etPassword.text.toString().trim()

            /*create a user*/
            firebaseAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        toast("회원가입 성공 !")
                        sendEmailVerification()
                        startActivity(Intent(this, MainActivity::class.java))
                        // firestore 저장하기 위해 코드 추가
                        val db = Firebase.firestore
                        // 위치정보 수집 동의 값 가져오기
                        val Loc_Store_Agree_value = findViewById<CheckBox>(R.id.Loc_Store_Agree).isChecked()

                        val signupdefault = hashMapOf(
                            "email" to userEmail,
                            "Loc_Store_Agree" to Loc_Store_Agree_value
                        )
                        /*현재는 email 필드만 만들면 된다. Contacts sub Collection은 NFC태깅 또는 QR코드 촬영시 생성하면 됨!*/
                        val user = Firebase.auth.currentUser
                        db.collection("Users").document(user.uid).set(signupdefault)
                            .addOnSuccessListener { toast("DB 생성 완료 !") }
                            .addOnFailureListener { toast("DB 생성 실패 ("+task.exception+")") }

                        // firestore 저장 코드 끝
                        finish()
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        toast("가입 오류("+task.exception+")")
                        /* 오류 메세지 표시하도록 강제 */
                    }
                }
        }
    }

    /* send verification email to the new user. This will only
    *  work if the firebase user is not null.
    */

    private fun sendEmailVerification() {
        firebaseUser?.let {
            it.sendEmailVerification().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    toast("email sent to $userEmail")
                }
            }
        }
    }
}