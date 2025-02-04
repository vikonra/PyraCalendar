package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Getraenke(
    val alkfrei: Boolean? = null, //Alkoholfreie Pauschale
    val bw: Boolean? = null, //Bier Wein Pauschale
    val cl: Boolean? = null, //Cocktail Longdring Pauschale
    val sekt: Boolean? = null, //Sektempfang
    val notiz: String? = null //Sonstige Sonderregelungen
) {

}

