package com.example.pyracalendar.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Test(val testID: String? = null, val testName: String? = null) {

}