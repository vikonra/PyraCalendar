package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Datum(
    val event: VonBis? = null, //Event Beginn und Ende
    val altenativ: String? = null, //Alternativdaten?
    val nichtExistent: Boolean? = null, //Noch kein Datum vorhanden (z.B. nur KW oder Monat vorgegeben)
    val erstellung: Long? = null, //Erstelldatum, wird bei Bearbeitung nicht überschrieben
    val aenderung: Long? = null, //Erstelldatum, wird bei Bearbeitung überschrieben
    val aufbau: VonBis? = null, //Aufbau Beginn und Ende
    val zeitTV: VonBis? = null, //TV Zeiten
    val zeitAV: VonBis? = null, //AV Zeiten
    val zeitAufbau: VonBis? = null, //Aufbau Zeiten
    val zeitAbbau: VonBis? = null //Abbau Zeiten
) {

}

