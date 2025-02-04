package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Dienstleister(
    val deko: String? = null, //Kontakt und Beschreibung
    val film: String? = null, //Kontakt und Beschreibung
    val dj: String? = null, //Kontakt und Beschreibung
    val shuttle: String? = null, ////Kontakt und Beschreibung
    val sonstiges: String? = null //Sonstige Dienstleistungen wie Casino, WLAN, ...
) {

}

