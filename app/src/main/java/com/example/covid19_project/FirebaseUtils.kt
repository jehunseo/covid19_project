package com.example.covid19_project

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/* 개발 편의를 위해 공통변수 정의 */
object FirebaseUtils {
    val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
}