package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Blockung(
    val datum: String? = null, //Datum, bis wann geblockt ist
    val reihenfolge: Int? = null // Optionsreihenfolge
) {

}

