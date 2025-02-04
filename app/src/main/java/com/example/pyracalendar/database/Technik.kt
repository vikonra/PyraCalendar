package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Technik(
    val supervisor: String? = null, //Ansprechperson Soundline
    val angebot: Boolean? = null, //Angebot abgegeben?
    val ab: Boolean? = null, //Auftragsbestätigung erhalten?
    val staff: String? = null, //Anzahl Soundline Mitarbeiter
    val steiger: Boolean? = null, //Steiger benötigt?
    val bmz: Boolean? = null, //BMZ Abschaltung?
    val streaming: Boolean? = null, //Streaming Center auf feste Screens?
    val mobilScreen: String? = null, //Mobile Screens von uns?
    val notiz: String? = null, //Sonstiges
) {

}

