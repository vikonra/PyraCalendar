package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Catering(
    val anbieter: String? = null, //Caterer
    val notiz: String? = null //Beschreibung
) {

}

