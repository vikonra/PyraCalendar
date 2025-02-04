package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Typ(
    val status: String? = null, //Anfrage, Blockung, Buchung, Sonstiges (Sonstiges, Urlaub, Ferien)
    val mehrtaegig: Boolean? = null, //mehrere Veranstaltungstage?
    val aufbautage: Boolean? = null, //Aufbautage vorhanden?
    val vaArt: String? = null, //Business, Fremdveranstalter, Privat
    val vaZeitraum: String? = null //TV, AV, TV & AV
) {

}

