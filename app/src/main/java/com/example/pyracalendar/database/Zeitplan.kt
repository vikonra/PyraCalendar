package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Zeitplan(
    val notiz: String? = null //zukünftig evtl. Liste etc.
) {

}

