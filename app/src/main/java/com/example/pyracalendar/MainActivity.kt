package com.example.pyracalendar

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import com.example.pyracalendar.database.Test
import com.example.pyracalendar.database.Veranstaltung
import com.example.pyracalendar.databinding.ActivityMainBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date

class MainActivity : FragmentActivity() {

    private var user: FirebaseUser? = null
    private var mAuth: FirebaseAuth? = null
    private lateinit var database: DatabaseReference

    private var jahr: Int = 0

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, MainFragment())
                .commitNow()
        }

        jahr = LocalDate.now().year
        //TODO Funktion, um das Jahr zu ändern (Pfeilbutton etc.)


        mAuth = FirebaseAuth.getInstance();
        checkCurrentUser()
        database = FirebaseDatabase.getInstance(Constants.DOMAIN).reference

        //Liste Veranstaltungen abfragen und setzen
        val databaseVeranstaltung: DatabaseReference =
            FirebaseDatabase.getInstance(Constants.DOMAIN).getReference("Veranstaltung")
        val veranstaltungListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                var vaList: ArrayList<Veranstaltung> = ArrayList()

                for (veranstaltungSnapshot in dataSnapshot.children) {
                    val veranstaltung: Veranstaltung? =
                        veranstaltungSnapshot.getValue(Veranstaltung::class.java)
                    if (veranstaltung != null) {
                        if (veranstaltung.datumBeginn != null) {
                            if (veranstaltung.datumBeginn.substring(6, 10).toInt() == jahr) {
                                vaList.add(veranstaltung)
                            }
                        }
                    }
                }
                kalenderSetzen(vaList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w(ContentValues.TAG, "loadPost:onCancelled", databaseError.toException())
            }
        }
        databaseVeranstaltung.orderByChild("sortierung").addValueEventListener(veranstaltungListener)
    }

    private fun inTagen(datum: String?): Long {
        return if (!datum.isNullOrEmpty()) {
            val tag: Long = datum.substring(0, 2).toLong()
            val monat: Long = datum.substring(3, 5).toLong()
            val jahr: Long = datum.substring(6, 10).toLong()
            tag + monat * 12 + jahr * 365
        } else {
            0
        }
    }

    class KalenderEintrag() {
        var tag: Int = 0
        var monat: Int = 0
        var jahr: Int = 0
        var name: String = ""
        var status: String = ""
    }

    private fun kalenderSetzen(vaList: ArrayList<Veranstaltung>) {
        var kalenderListe: ArrayList<KalenderEintrag> = ArrayList()
        for (va in vaList) {

            //Aufbautage & Abbautage hinzufügen
            if (va.aufbautage == true) {
                if (!va.aufbauBeginn.isNullOrEmpty()) {
                    val tage: Int = (inTagen(va.datumBeginn) - inTagen(va.aufbauBeginn)).toInt() - 1
                    val date =
                        LocalDate.parse(va.aufbauBeginn, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    for (i in 0..tage) {
                        val k = KalenderEintrag()
                        k.tag = date.plusDays(i.toLong()).dayOfMonth
                        k.monat = date.plusDays(i.toLong()).monthValue
                        k.jahr = date.plusDays(i.toLong()).year
                        k.name = "(A) " + va.kunde.toString() + " (" + va.raum + ")"
                        k.status = va.status.toString()
                        kalenderListe.add(k)
                    }
                }
                if (!va.abbauEnde.isNullOrEmpty()) {
                    val tage: Int = (inTagen(va.abbauEnde) - inTagen(va.datumEnde)).toInt() - 1
                    val date =
                        LocalDate.parse(va.datumEnde, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            .plusDays(1)
                    for (i in 0..tage) {
                        val k = KalenderEintrag()
                        k.tag = date.plusDays(i.toLong()).dayOfMonth
                        k.monat = date.plusDays(i.toLong()).monthValue
                        k.jahr = date.plusDays(i.toLong()).year
                        k.name = "(A) " + va.kunde.toString() + " (" + va.raum + ")"
                        k.status = va.status.toString()
                        kalenderListe.add(k)
                    }
                }
            }

            //Veranstaltungstage hinzufügen
            val tage: Int = (inTagen(va.datumEnde) - inTagen(va.datumBeginn)).toInt()
            val date =
                LocalDate.parse(va.datumBeginn, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            for (i in 0..tage) {
                val k = KalenderEintrag()
                k.tag = date.plusDays(i.toLong()).dayOfMonth
                k.monat = date.plusDays(i.toLong()).monthValue
                k.jahr = date.plusDays(i.toLong()).year
                k.name = va.kunde.toString() + " (" + va.raum + ")"
                k.status = va.status.toString()
                kalenderListe.add(k)
            }
        }
        updateUI(kalenderListe)
    }

    private fun updateUI(kalenderListe: ArrayList<KalenderEintrag>) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setTagNummern()
        for (t in kalenderListe) {
            when (t.monat) {
                1 -> {
                    when (t.tag) {
                        1 -> {
                            if (binding.monat1.tag1.txt1.text.isNullOrEmpty()) {
                                binding.monat1.tag1.txt1.text = t.name
                                binding.monat1.tag1.txt2.visibility = View.GONE
                                binding.monat1.tag1.txt3.visibility = View.GONE
                                when (t.status) {
                                    Cons.BLOCKUNG -> binding.monat1.tag1.txt1.setBackgroundResource(
                                        R.drawable.blockung
                                    )
                                    Cons.BUCHUNG -> binding.monat1.tag1.txt1.setBackgroundResource(
                                        R.drawable.buchung
                                    )
                                    Cons.SONSTIGES -> binding.monat1.tag1.txt1.setBackgroundResource(
                                        R.drawable.sonstiges
                                    )
                                }
                            } else if (binding.monat1.tag1.txt2.text.isNullOrEmpty()) {
                                binding.monat1.tag1.txt2.visibility = View.VISIBLE
                                binding.monat1.tag1.txt2.text = t.name
                                binding.monat1.tag1.txt3.visibility = View.GONE
                                when (t.status) {
                                    Cons.BLOCKUNG -> binding.monat1.tag1.txt2.setBackgroundResource(
                                        R.drawable.blockung
                                    )
                                    Cons.BUCHUNG -> binding.monat1.tag1.txt2.setBackgroundResource(
                                        R.drawable.buchung
                                    )
                                    Cons.SONSTIGES -> binding.monat1.tag1.txt2.setBackgroundResource(
                                        R.drawable.sonstiges
                                    )
                                }
                            } else {
                                binding.monat1.tag1.txt3.visibility = View.VISIBLE
                                binding.monat1.tag1.txt3.text = t.name
                                when (t.status) {
                                    Cons.BLOCKUNG -> binding.monat1.tag1.txt3.setBackgroundResource(
                                        R.drawable.blockung
                                    )
                                    Cons.BUCHUNG -> binding.monat1.tag1.txt3.setBackgroundResource(
                                        R.drawable.buchung
                                    )
                                    Cons.SONSTIGES -> binding.monat1.tag1.txt3.setBackgroundResource(
                                        R.drawable.sonstiges
                                    )
                                }
                            }
                        }
                        2 -> {
                            if (binding.monat1.tag2.txt1.text.isNullOrEmpty()) {
                                binding.monat1.tag2.txt1.text = t.name
                                binding.monat1.tag2.txt2.visibility = View.GONE
                                binding.monat1.tag2.txt3.visibility = View.GONE
                                when (t.status) {
                                    Cons.BLOCKUNG -> binding.monat1.tag2.txt1.setBackgroundResource(
                                        R.drawable.blockung
                                    )
                                    Cons.BUCHUNG -> binding.monat1.tag2.txt1.setBackgroundResource(
                                        R.drawable.buchung
                                    )
                                    Cons.SONSTIGES -> binding.monat1.tag2.txt1.setBackgroundResource(
                                        R.drawable.sonstiges
                                    )
                                }
                            } else if (binding.monat1.tag2.txt2.text.isNullOrEmpty()) {
                                binding.monat1.tag2.txt2.visibility = View.VISIBLE
                                binding.monat1.tag2.txt2.text = t.name
                                binding.monat1.tag2.txt3.visibility = View.GONE
                                when (t.status) {
                                    Cons.BLOCKUNG -> binding.monat1.tag2.txt2.setBackgroundResource(
                                        R.drawable.blockung
                                    )
                                    Cons.BUCHUNG -> binding.monat1.tag2.txt2.setBackgroundResource(
                                        R.drawable.buchung
                                    )
                                    Cons.SONSTIGES -> binding.monat1.tag2.txt2.setBackgroundResource(
                                        R.drawable.sonstiges
                                    )
                                }
                            } else {
                                binding.monat1.tag2.txt3.visibility = View.VISIBLE
                                binding.monat1.tag2.txt3.text = t.name
                                when (t.status) {
                                    Cons.BLOCKUNG -> binding.monat1.tag2.txt3.setBackgroundResource(
                                        R.drawable.blockung
                                    )
                                    Cons.BUCHUNG -> binding.monat1.tag2.txt3.setBackgroundResource(
                                        R.drawable.buchung
                                    )
                                    Cons.SONSTIGES -> binding.monat1.tag2.txt3.setBackgroundResource(
                                        R.drawable.sonstiges
                                    )
                                }
                            }
                        }
                        3 -> {
                            if (binding.monat1.tag3.txt1.text.isNullOrEmpty()) {
                                binding.monat1.tag3.txt1.text = t.name
                                binding.monat1.tag3.txt2.visibility = View.GONE
                                binding.monat1.tag3.txt3.visibility = View.GONE
                                when (t.status) {
                                    Cons.BLOCKUNG -> binding.monat1.tag3.txt1.setBackgroundResource(
                                        R.drawable.blockung
                                    )
                                    Cons.BUCHUNG -> binding.monat1.tag3.txt1.setBackgroundResource(
                                        R.drawable.buchung
                                    )
                                    Cons.SONSTIGES -> binding.monat1.tag3.txt1.setBackgroundResource(
                                        R.drawable.sonstiges
                                    )
                                }
                            } else if (binding.monat1.tag3.txt2.text.isNullOrEmpty()) {
                                binding.monat1.tag3.txt2.visibility = View.VISIBLE
                                binding.monat1.tag3.txt2.text = t.name
                                binding.monat1.tag3.txt3.visibility = View.GONE
                                when (t.status) {
                                    Cons.BLOCKUNG -> binding.monat1.tag3.txt2.setBackgroundResource(
                                        R.drawable.blockung
                                    )
                                    Cons.BUCHUNG -> binding.monat1.tag3.txt2.setBackgroundResource(
                                        R.drawable.buchung
                                    )
                                    Cons.SONSTIGES -> binding.monat1.tag3.txt2.setBackgroundResource(
                                        R.drawable.sonstiges
                                    )
                                }
                            } else {
                                binding.monat1.tag3.txt3.visibility = View.VISIBLE
                                binding.monat1.tag3.txt3.text = t.name
                                when (t.status) {
                                    Cons.BLOCKUNG -> binding.monat1.tag3.txt3.setBackgroundResource(
                                        R.drawable.blockung
                                    )
                                    Cons.BUCHUNG -> binding.monat1.tag3.txt3.setBackgroundResource(
                                        R.drawable.buchung
                                    )
                                    Cons.SONSTIGES -> binding.monat1.tag3.txt3.setBackgroundResource(
                                        R.drawable.sonstiges
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setTagNummern() {
        binding.monat1.tag1.txtTag.text = "1"
        binding.monat1.tag2.txtTag.text = "2"
        binding.monat1.tag3.txtTag.text = "3"
        binding.monat1.tag4.txtTag.text = "4"
        binding.monat1.tag5.txtTag.text = "5"
        binding.monat1.tag6.txtTag.text = "6"
        binding.monat1.tag7.txtTag.text = "7"
        binding.monat1.tag8.txtTag.text = "8"
        binding.monat1.tag9.txtTag.text = "9"
        binding.monat1.tag10.txtTag.text = "10"
        binding.monat1.tag11.txtTag.text = "11"
        binding.monat1.tag12.txtTag.text = "12"
        binding.monat1.tag13.txtTag.text = "13"
        binding.monat1.tag14.txtTag.text = "14"
        binding.monat1.tag15.txtTag.text = "15"
        binding.monat1.tag16.txtTag.text = "16"
        binding.monat1.tag17.txtTag.text = "17"
        binding.monat1.tag18.txtTag.text = "18"
        binding.monat1.tag19.txtTag.text = "19"
        binding.monat1.tag20.txtTag.text = "20"
        binding.monat1.tag21.txtTag.text = "21"
        binding.monat1.tag22.txtTag.text = "22"
        binding.monat1.tag23.txtTag.text = "23"
        binding.monat1.tag24.txtTag.text = "24"
        binding.monat1.tag25.txtTag.text = "25"
        binding.monat1.tag26.txtTag.text = "26"
        binding.monat1.tag27.txtTag.text = "27"
        binding.monat1.tag28.txtTag.text = "28"
        binding.monat1.tag29.txtTag.text = "29"
        binding.monat1.tag30.txtTag.text = "30"
        binding.monat1.tag31.txtTag.text = "31"

        binding.monat2.tag1.txtTag.text = "1"
        binding.monat2.tag2.txtTag.text = "2"
        binding.monat2.tag3.txtTag.text = "3"
        binding.monat2.tag4.txtTag.text = "4"
        binding.monat2.tag5.txtTag.text = "5"
        binding.monat2.tag6.txtTag.text = "6"
        binding.monat2.tag7.txtTag.text = "7"
        binding.monat2.tag8.txtTag.text = "8"
        binding.monat2.tag9.txtTag.text = "9"
        binding.monat2.tag10.txtTag.text = "10"
        binding.monat2.tag11.txtTag.text = "11"
        binding.monat2.tag12.txtTag.text = "12"
        binding.monat2.tag13.txtTag.text = "13"
        binding.monat2.tag14.txtTag.text = "14"
        binding.monat2.tag15.txtTag.text = "15"
        binding.monat2.tag16.txtTag.text = "16"
        binding.monat2.tag17.txtTag.text = "17"
        binding.monat2.tag18.txtTag.text = "18"
        binding.monat2.tag19.txtTag.text = "19"
        binding.monat2.tag20.txtTag.text = "20"
        binding.monat2.tag21.txtTag.text = "21"
        binding.monat2.tag22.txtTag.text = "22"
        binding.monat2.tag23.txtTag.text = "23"
        binding.monat2.tag24.txtTag.text = "24"
        binding.monat2.tag25.txtTag.text = "25"
        binding.monat2.tag26.txtTag.text = "26"
        binding.monat2.tag27.txtTag.text = "27"
        binding.monat2.tag28.txtTag.text = "28"
        binding.monat2.tag29.txtTag.text = "29"
        binding.monat2.tag30.txtTag.visibility = View.INVISIBLE
        binding.monat2.tag30.txt1.visibility = View.INVISIBLE
        binding.monat2.tag30.txt2.visibility = View.INVISIBLE
        binding.monat2.tag31.txtTag.visibility = View.INVISIBLE
        binding.monat2.tag31.txt1.visibility = View.INVISIBLE
        binding.monat2.tag31.txt2.visibility = View.INVISIBLE

        binding.monat3.tag1.txtTag.text = "1"
        binding.monat3.tag2.txtTag.text = "2"
        binding.monat3.tag3.txtTag.text = "3"
        binding.monat3.tag4.txtTag.text = "4"
        binding.monat3.tag5.txtTag.text = "5"
        binding.monat3.tag6.txtTag.text = "6"
        binding.monat3.tag7.txtTag.text = "7"
        binding.monat3.tag8.txtTag.text = "8"
        binding.monat3.tag9.txtTag.text = "9"
        binding.monat3.tag10.txtTag.text = "10"
        binding.monat3.tag11.txtTag.text = "11"
        binding.monat3.tag12.txtTag.text = "12"
        binding.monat3.tag13.txtTag.text = "13"
        binding.monat3.tag14.txtTag.text = "14"
        binding.monat3.tag15.txtTag.text = "15"
        binding.monat3.tag16.txtTag.text = "16"
        binding.monat3.tag17.txtTag.text = "17"
        binding.monat3.tag18.txtTag.text = "18"
        binding.monat3.tag19.txtTag.text = "19"
        binding.monat3.tag20.txtTag.text = "20"
        binding.monat3.tag21.txtTag.text = "21"
        binding.monat3.tag22.txtTag.text = "22"
        binding.monat3.tag23.txtTag.text = "23"
        binding.monat3.tag24.txtTag.text = "24"
        binding.monat3.tag25.txtTag.text = "25"
        binding.monat3.tag26.txtTag.text = "26"
        binding.monat3.tag27.txtTag.text = "27"
        binding.monat3.tag28.txtTag.text = "28"
        binding.monat3.tag29.txtTag.text = "29"
        binding.monat3.tag30.txtTag.text = "30"
        binding.monat3.tag31.txtTag.text = "31"

        binding.monat4.tag1.txtTag.text = "1"
        binding.monat4.tag2.txtTag.text = "2"
        binding.monat4.tag3.txtTag.text = "3"
        binding.monat4.tag4.txtTag.text = "4"
        binding.monat4.tag5.txtTag.text = "5"
        binding.monat4.tag6.txtTag.text = "6"
        binding.monat4.tag7.txtTag.text = "7"
        binding.monat4.tag8.txtTag.text = "8"
        binding.monat4.tag9.txtTag.text = "9"
        binding.monat4.tag10.txtTag.text = "10"
        binding.monat4.tag11.txtTag.text = "11"
        binding.monat4.tag12.txtTag.text = "12"
        binding.monat4.tag13.txtTag.text = "13"
        binding.monat4.tag14.txtTag.text = "14"
        binding.monat4.tag15.txtTag.text = "15"
        binding.monat4.tag16.txtTag.text = "16"
        binding.monat4.tag17.txtTag.text = "17"
        binding.monat4.tag18.txtTag.text = "18"
        binding.monat4.tag19.txtTag.text = "19"
        binding.monat4.tag20.txtTag.text = "20"
        binding.monat4.tag21.txtTag.text = "21"
        binding.monat4.tag22.txtTag.text = "22"
        binding.monat4.tag23.txtTag.text = "23"
        binding.monat4.tag24.txtTag.text = "24"
        binding.monat4.tag25.txtTag.text = "25"
        binding.monat4.tag26.txtTag.text = "26"
        binding.monat4.tag27.txtTag.text = "27"
        binding.monat4.tag28.txtTag.text = "28"
        binding.monat4.tag29.txtTag.text = "29"
        binding.monat4.tag30.txtTag.text = "30"
        binding.monat4.tag31.txtTag.visibility = View.INVISIBLE
        binding.monat4.tag31.txt1.visibility = View.INVISIBLE
        binding.monat4.tag31.txt2.visibility = View.INVISIBLE

        binding.monat5.tag1.txtTag.text = "1"
        binding.monat5.tag2.txtTag.text = "2"
        binding.monat5.tag3.txtTag.text = "3"
        binding.monat5.tag4.txtTag.text = "4"
        binding.monat5.tag5.txtTag.text = "5"
        binding.monat5.tag6.txtTag.text = "6"
        binding.monat5.tag7.txtTag.text = "7"
        binding.monat5.tag8.txtTag.text = "8"
        binding.monat5.tag9.txtTag.text = "9"
        binding.monat5.tag10.txtTag.text = "10"
        binding.monat5.tag11.txtTag.text = "11"
        binding.monat5.tag12.txtTag.text = "12"
        binding.monat5.tag13.txtTag.text = "13"
        binding.monat5.tag14.txtTag.text = "14"
        binding.monat5.tag15.txtTag.text = "15"
        binding.monat5.tag16.txtTag.text = "16"
        binding.monat5.tag17.txtTag.text = "17"
        binding.monat5.tag18.txtTag.text = "18"
        binding.monat5.tag19.txtTag.text = "19"
        binding.monat5.tag20.txtTag.text = "20"
        binding.monat5.tag21.txtTag.text = "21"
        binding.monat5.tag22.txtTag.text = "22"
        binding.monat5.tag23.txtTag.text = "23"
        binding.monat5.tag24.txtTag.text = "24"
        binding.monat5.tag25.txtTag.text = "25"
        binding.monat5.tag26.txtTag.text = "26"
        binding.monat5.tag27.txtTag.text = "27"
        binding.monat5.tag28.txtTag.text = "28"
        binding.monat5.tag29.txtTag.text = "29"
        binding.monat5.tag30.txtTag.text = "30"
        binding.monat5.tag31.txtTag.text = "31"

        binding.monat6.tag1.txtTag.text = "1"
        binding.monat6.tag2.txtTag.text = "2"
        binding.monat6.tag3.txtTag.text = "3"
        binding.monat6.tag4.txtTag.text = "4"
        binding.monat6.tag5.txtTag.text = "5"
        binding.monat6.tag6.txtTag.text = "6"
        binding.monat6.tag7.txtTag.text = "7"
        binding.monat6.tag8.txtTag.text = "8"
        binding.monat6.tag9.txtTag.text = "9"
        binding.monat6.tag10.txtTag.text = "10"
        binding.monat6.tag11.txtTag.text = "11"
        binding.monat6.tag12.txtTag.text = "12"
        binding.monat6.tag13.txtTag.text = "13"
        binding.monat6.tag14.txtTag.text = "14"
        binding.monat6.tag15.txtTag.text = "15"
        binding.monat6.tag16.txtTag.text = "16"
        binding.monat6.tag17.txtTag.text = "17"
        binding.monat6.tag18.txtTag.text = "18"
        binding.monat6.tag19.txtTag.text = "19"
        binding.monat6.tag20.txtTag.text = "20"
        binding.monat6.tag21.txtTag.text = "21"
        binding.monat6.tag22.txtTag.text = "22"
        binding.monat6.tag23.txtTag.text = "23"
        binding.monat6.tag24.txtTag.text = "24"
        binding.monat6.tag25.txtTag.text = "25"
        binding.monat6.tag26.txtTag.text = "26"
        binding.monat6.tag27.txtTag.text = "27"
        binding.monat6.tag28.txtTag.text = "28"
        binding.monat6.tag29.txtTag.text = "29"
        binding.monat6.tag30.txtTag.text = "30"
        binding.monat6.tag31.txtTag.visibility = View.INVISIBLE
        binding.monat6.tag31.txt1.visibility = View.INVISIBLE
        binding.monat6.tag31.txt2.visibility = View.INVISIBLE

        binding.monat7.tag1.txtTag.text = "1"
        binding.monat7.tag2.txtTag.text = "2"
        binding.monat7.tag3.txtTag.text = "3"
        binding.monat7.tag4.txtTag.text = "4"
        binding.monat7.tag5.txtTag.text = "5"
        binding.monat7.tag6.txtTag.text = "6"
        binding.monat7.tag7.txtTag.text = "7"
        binding.monat7.tag8.txtTag.text = "8"
        binding.monat7.tag9.txtTag.text = "9"
        binding.monat7.tag10.txtTag.text = "10"
        binding.monat7.tag11.txtTag.text = "11"
        binding.monat7.tag12.txtTag.text = "12"
        binding.monat7.tag13.txtTag.text = "13"
        binding.monat7.tag14.txtTag.text = "14"
        binding.monat7.tag15.txtTag.text = "15"
        binding.monat7.tag16.txtTag.text = "16"
        binding.monat7.tag17.txtTag.text = "17"
        binding.monat7.tag18.txtTag.text = "18"
        binding.monat7.tag19.txtTag.text = "19"
        binding.monat7.tag20.txtTag.text = "20"
        binding.monat7.tag21.txtTag.text = "21"
        binding.monat7.tag22.txtTag.text = "22"
        binding.monat7.tag23.txtTag.text = "23"
        binding.monat7.tag24.txtTag.text = "24"
        binding.monat7.tag25.txtTag.text = "25"
        binding.monat7.tag26.txtTag.text = "26"
        binding.monat7.tag27.txtTag.text = "27"
        binding.monat7.tag28.txtTag.text = "28"
        binding.monat7.tag29.txtTag.text = "29"
        binding.monat7.tag30.txtTag.text = "30"
        binding.monat7.tag31.txtTag.text = "31"

        binding.monat8.tag1.txtTag.text = "1"
        binding.monat8.tag2.txtTag.text = "2"
        binding.monat8.tag3.txtTag.text = "3"
        binding.monat8.tag4.txtTag.text = "4"
        binding.monat8.tag5.txtTag.text = "5"
        binding.monat8.tag6.txtTag.text = "6"
        binding.monat8.tag7.txtTag.text = "7"
        binding.monat8.tag8.txtTag.text = "8"
        binding.monat8.tag9.txtTag.text = "9"
        binding.monat8.tag10.txtTag.text = "10"
        binding.monat8.tag11.txtTag.text = "11"
        binding.monat8.tag12.txtTag.text = "12"
        binding.monat8.tag13.txtTag.text = "13"
        binding.monat8.tag14.txtTag.text = "14"
        binding.monat8.tag15.txtTag.text = "15"
        binding.monat8.tag16.txtTag.text = "16"
        binding.monat8.tag17.txtTag.text = "17"
        binding.monat8.tag18.txtTag.text = "18"
        binding.monat8.tag19.txtTag.text = "19"
        binding.monat8.tag20.txtTag.text = "20"
        binding.monat8.tag21.txtTag.text = "21"
        binding.monat8.tag22.txtTag.text = "22"
        binding.monat8.tag23.txtTag.text = "23"
        binding.monat8.tag24.txtTag.text = "24"
        binding.monat8.tag25.txtTag.text = "25"
        binding.monat8.tag26.txtTag.text = "26"
        binding.monat8.tag27.txtTag.text = "27"
        binding.monat8.tag28.txtTag.text = "28"
        binding.monat8.tag29.txtTag.text = "29"
        binding.monat8.tag30.txtTag.text = "30"
        binding.monat8.tag31.txtTag.text = "31"

        binding.monat9.tag1.txtTag.text = "1"
        binding.monat9.tag2.txtTag.text = "2"
        binding.monat9.tag3.txtTag.text = "3"
        binding.monat9.tag4.txtTag.text = "4"
        binding.monat9.tag5.txtTag.text = "5"
        binding.monat9.tag6.txtTag.text = "6"
        binding.monat9.tag7.txtTag.text = "7"
        binding.monat9.tag8.txtTag.text = "8"
        binding.monat9.tag9.txtTag.text = "9"
        binding.monat9.tag10.txtTag.text = "10"
        binding.monat9.tag11.txtTag.text = "11"
        binding.monat9.tag12.txtTag.text = "12"
        binding.monat9.tag13.txtTag.text = "13"
        binding.monat9.tag14.txtTag.text = "14"
        binding.monat9.tag15.txtTag.text = "15"
        binding.monat9.tag16.txtTag.text = "16"
        binding.monat9.tag17.txtTag.text = "17"
        binding.monat9.tag18.txtTag.text = "18"
        binding.monat9.tag19.txtTag.text = "19"
        binding.monat9.tag20.txtTag.text = "20"
        binding.monat9.tag21.txtTag.text = "21"
        binding.monat9.tag22.txtTag.text = "22"
        binding.monat9.tag23.txtTag.text = "23"
        binding.monat9.tag24.txtTag.text = "24"
        binding.monat9.tag25.txtTag.text = "25"
        binding.monat9.tag26.txtTag.text = "26"
        binding.monat9.tag27.txtTag.text = "27"
        binding.monat9.tag28.txtTag.text = "28"
        binding.monat9.tag29.txtTag.text = "29"
        binding.monat9.tag30.txtTag.text = "30"
        binding.monat9.tag31.txtTag.visibility = View.INVISIBLE
        binding.monat9.tag31.txt1.visibility = View.INVISIBLE
        binding.monat9.tag31.txt2.visibility = View.INVISIBLE

        binding.monat10.tag1.txtTag.text = "1"
        binding.monat10.tag2.txtTag.text = "2"
        binding.monat10.tag3.txtTag.text = "3"
        binding.monat10.tag4.txtTag.text = "4"
        binding.monat10.tag5.txtTag.text = "5"
        binding.monat10.tag6.txtTag.text = "6"
        binding.monat10.tag7.txtTag.text = "7"
        binding.monat10.tag8.txtTag.text = "8"
        binding.monat10.tag9.txtTag.text = "9"
        binding.monat10.tag10.txtTag.text = "10"
        binding.monat10.tag11.txtTag.text = "11"
        binding.monat10.tag12.txtTag.text = "12"
        binding.monat10.tag13.txtTag.text = "13"
        binding.monat10.tag14.txtTag.text = "14"
        binding.monat10.tag15.txtTag.text = "15"
        binding.monat10.tag16.txtTag.text = "16"
        binding.monat10.tag17.txtTag.text = "17"
        binding.monat10.tag18.txtTag.text = "18"
        binding.monat10.tag19.txtTag.text = "19"
        binding.monat10.tag20.txtTag.text = "20"
        binding.monat10.tag21.txtTag.text = "21"
        binding.monat10.tag22.txtTag.text = "22"
        binding.monat10.tag23.txtTag.text = "23"
        binding.monat10.tag24.txtTag.text = "24"
        binding.monat10.tag25.txtTag.text = "25"
        binding.monat10.tag26.txtTag.text = "26"
        binding.monat10.tag27.txtTag.text = "27"
        binding.monat10.tag28.txtTag.text = "28"
        binding.monat10.tag29.txtTag.text = "29"
        binding.monat10.tag30.txtTag.text = "30"
        binding.monat10.tag31.txtTag.text = "31"

        binding.monat11.tag1.txtTag.text = "1"
        binding.monat11.tag2.txtTag.text = "2"
        binding.monat11.tag3.txtTag.text = "3"
        binding.monat11.tag4.txtTag.text = "4"
        binding.monat11.tag5.txtTag.text = "5"
        binding.monat11.tag6.txtTag.text = "6"
        binding.monat11.tag7.txtTag.text = "7"
        binding.monat11.tag8.txtTag.text = "8"
        binding.monat11.tag9.txtTag.text = "9"
        binding.monat11.tag10.txtTag.text = "10"
        binding.monat11.tag11.txtTag.text = "11"
        binding.monat11.tag12.txtTag.text = "12"
        binding.monat11.tag13.txtTag.text = "13"
        binding.monat11.tag14.txtTag.text = "14"
        binding.monat11.tag15.txtTag.text = "15"
        binding.monat11.tag16.txtTag.text = "16"
        binding.monat11.tag17.txtTag.text = "17"
        binding.monat11.tag18.txtTag.text = "18"
        binding.monat11.tag19.txtTag.text = "19"
        binding.monat11.tag20.txtTag.text = "20"
        binding.monat11.tag21.txtTag.text = "21"
        binding.monat11.tag22.txtTag.text = "22"
        binding.monat11.tag23.txtTag.text = "23"
        binding.monat11.tag24.txtTag.text = "24"
        binding.monat11.tag25.txtTag.text = "25"
        binding.monat11.tag26.txtTag.text = "26"
        binding.monat11.tag27.txtTag.text = "27"
        binding.monat11.tag28.txtTag.text = "28"
        binding.monat11.tag29.txtTag.text = "29"
        binding.monat11.tag30.txtTag.text = "30"
        binding.monat11.tag31.txtTag.visibility = View.INVISIBLE
        binding.monat11.tag31.txt1.visibility = View.INVISIBLE
        binding.monat11.tag31.txt2.visibility = View.INVISIBLE

        binding.monat12.tag1.txtTag.text = "1"
        binding.monat12.tag2.txtTag.text = "2"
        binding.monat12.tag3.txtTag.text = "3"
        binding.monat12.tag4.txtTag.text = "4"
        binding.monat12.tag5.txtTag.text = "5"
        binding.monat12.tag6.txtTag.text = "6"
        binding.monat12.tag7.txtTag.text = "7"
        binding.monat12.tag8.txtTag.text = "8"
        binding.monat12.tag9.txtTag.text = "9"
        binding.monat12.tag10.txtTag.text = "10"
        binding.monat12.tag11.txtTag.text = "11"
        binding.monat12.tag12.txtTag.text = "12"
        binding.monat12.tag13.txtTag.text = "13"
        binding.monat12.tag14.txtTag.text = "14"
        binding.monat12.tag15.txtTag.text = "15"
        binding.monat12.tag16.txtTag.text = "16"
        binding.monat12.tag17.txtTag.text = "17"
        binding.monat12.tag18.txtTag.text = "18"
        binding.monat12.tag19.txtTag.text = "19"
        binding.monat12.tag20.txtTag.text = "20"
        binding.monat12.tag21.txtTag.text = "21"
        binding.monat12.tag22.txtTag.text = "22"
        binding.monat12.tag23.txtTag.text = "23"
        binding.monat12.tag24.txtTag.text = "24"
        binding.monat12.tag25.txtTag.text = "25"
        binding.monat12.tag26.txtTag.text = "26"
        binding.monat12.tag27.txtTag.text = "27"
        binding.monat12.tag28.txtTag.text = "28"
        binding.monat12.tag29.txtTag.text = "29"
        binding.monat12.tag30.txtTag.text = "30"
        binding.monat12.tag31.txtTag.text = "31"
    }

    fun checkCurrentUser() {
        user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            signIn("vk@pyramidemainz.de", "Pyraappvk")
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun signIn(email: String, password: String) {
        // [START sign_in_with_email]
        mAuth!!.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(
                this
            ) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        this@MainActivity, "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        // [END sign_in_with_email]
    }

    private fun signOut() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                // ...
            }
    }
}