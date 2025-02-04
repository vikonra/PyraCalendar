package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Raum(
    val summe: String? = null, //Summe aus TV und AV
    val tv: String? = null, //Räume TV
    val av: String? = null, //Räume AV
    val bestuhlungTV: String? = null, //Bestuhlung bei TV
    val bestuhlungAV: String? = null, //Bestuhlung bei AV
    val zusatz: String? = null //Staffräume etc.
) {

}

