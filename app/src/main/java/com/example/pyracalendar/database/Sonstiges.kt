package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Sonstiges(
    val notiz: String? = null //aktuell nicht benutzt, evtl. zukünftig benötigt, kann beliebig angepasst werden da eigene unbenutzte Klasse
) {

}

