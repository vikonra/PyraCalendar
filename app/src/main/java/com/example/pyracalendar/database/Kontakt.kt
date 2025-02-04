package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Kontakt(
    val name: String? = null, //Ansprechperson
    val mail: String? = null, //Mailadresse Kunde
    val telefon: String? = null, //Telefon Kunde
    val notiz: String? = null, //Beschreibung VA
    val supervisor: String? = null //Sachbearbeiter und Durchführung (z.B. bdnf)
) {

}

