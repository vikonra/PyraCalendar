package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class VonBis(
    val beginn: String? = null,
    val ende: String? = null
) {

}

