package com.example.pyracalendar

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.example.pyracalendar.database.Veranstaltung
import com.example.pyracalendar.databinding.ActivityMainBinding
import com.example.pyracalendar.databinding.MonatBinding
import com.example.pyracalendar.databinding.TagBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.GregorianCalendar


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

        mAuth = FirebaseAuth.getInstance();
        checkCurrentUser()
        database = FirebaseDatabase.getInstance(Constants.DOMAIN).reference

        kalenderAbfragen()
    }

    private fun inTagen(datum: String?): Int {
        return if (!datum.isNullOrEmpty()) {
            val localDate: LocalDate = LocalDate.of(
                datum.substring(6, 10).toInt(),
                datum.substring(3, 5).toInt(),
                datum.substring(0, 2).toInt()
            )
            localDate.dayOfYear
        } else {
            0
        }
    }

    class KalenderEintrag {
        var tag: Int = 0
        var monat: Int = 0
        var jahr: Int = 0
        var name: String = ""
        var status: String = ""
    }

    private fun kalenderAbfragen() {
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
        databaseVeranstaltung.orderByChild("erstellDatum")
            .addValueEventListener(veranstaltungListener)
    }

    private fun kalenderSetzen(vaList: ArrayList<Veranstaltung>) {
        var kalenderListe: ArrayList<KalenderEintrag> = ArrayList()
        for (va in vaList) {

            //Aufbautage & Abbautage hinzufügen
            if (va.aufbautage == true) {
                if (!va.aufbauBeginn.isNullOrEmpty()) {
                    val tage: Int = (inTagen(va.datumBeginn) - inTagen(va.aufbauBeginn)) - 1
                    val date =
                        LocalDate.parse(va.aufbauBeginn, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    for (i in 0..tage) {
                        val k = KalenderEintrag()
                        k.tag = date.plusDays(i.toLong()).dayOfMonth
                        k.monat = date.plusDays(i.toLong()).monthValue
                        k.jahr = date.plusDays(i.toLong()).year
                        if (va.status != Cons.SONSTIGES) {
                            //k.name = "(A) " + va.kunde.toString() + " (" + va.raum + ")" //Alternative ausgeschriebene Räume
                            k.name =
                                "(A) " + va.kunde.toString() + " (" + raumsetzen(va.raum!!) + ")"
                        } else {
                            //k.name = "(A) " + va.kunde.toString() //Alternative ausgeschriebene Räume
                            k.name = "(A) " + va.kunde.toString()
                        }
                        k.status = va.status.toString()
                        kalenderListe.add(k)
                    }
                }
                if (!va.abbauEnde.isNullOrEmpty()) {
                    val tage: Int = (inTagen(va.abbauEnde) - inTagen(va.datumEnde)) - 1
                    val date =
                        LocalDate.parse(va.datumEnde, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            .plusDays(1)
                    for (i in 0..tage) {
                        val k = KalenderEintrag()
                        k.tag = date.plusDays(i.toLong()).dayOfMonth
                        k.monat = date.plusDays(i.toLong()).monthValue
                        k.jahr = date.plusDays(i.toLong()).year
                        if (va.status != Cons.SONSTIGES) {
                            //k.name = "(A) " + va.kunde.toString() + " (" + va.raum + ")" //Alternative ausgeschriebene Räume
                            k.name =
                                "(A) " + va.kunde.toString() + " (" + raumsetzen(va.raum!!) + ")"
                        } else {
                            //k.name = "(A) " + va.kunde.toString() //Alternative ausgeschriebene Räume
                            k.name =
                                "(A) " + va.kunde.toString()
                        }
                        k.status = va.status.toString()
                        kalenderListe.add(k)
                    }
                }
            }

            //Veranstaltungstage hinzufügen
            val tage: Int = (inTagen(va.datumEnde) - inTagen(va.datumBeginn))
            val date =
                LocalDate.parse(va.datumBeginn, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            for (i in 0..tage) {
                val k = KalenderEintrag()
                k.tag = date.plusDays(i.toLong()).dayOfMonth
                k.monat = date.plusDays(i.toLong()).monthValue
                k.jahr = date.plusDays(i.toLong()).year
                if (va.status != Cons.SONSTIGES) {
                    //k.name = va.kunde.toString() + " (" + va.raum + ")" //Alternative ausgeschriebene Räume
                    k.name = va.kunde.toString() + " (" + raumsetzen(va.raum!!) + ")"
                } else {
                    //k.name = va.kunde.toString() //Alternative ausgeschriebene Räume
                    k.name = va.kunde.toString()
                }
                k.status = va.status.toString()
                kalenderListe.add(k)
            }
        }
        updateUI(kalenderListe)
    }

    private fun raumsetzen(r: String): String {
        var raum = ""
        if (r.contains(Cons.C1500)) {
            raum = "C"
            if (r.contains(Cons.B800)) {
                raum += ", B"
                if (raum.contains(Cons.P250)) {
                    raum += ", P"
                }
            } else if (r.contains(Cons.P250)) {
                raum += ", P"
            }
        } else if (r.contains(Cons.B800)) {
            raum = "B"
            if (r.contains(Cons.P250)) {
                raum += ", P"
            }
        } else {
            raum = "P"
        }
        return raum
    }

    private fun updateUI(kalenderListe: ArrayList<KalenderEintrag>) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnRight.setOnClickListener {
            jahr++
            kalenderAbfragen()
        }
        binding.btnLeft.setOnClickListener {
            jahr--
            kalenderAbfragen()
        }
        setTagNummern()
        binding.txtYear.text = jahr.toString()

        for (t in kalenderListe) {
            when (t.monat) {
                1 -> {
                    val binding = binding.monat1
                    monatSetzen(binding, t)
                }
                2 -> {
                    val binding = binding.monat2
                    monatSetzen(binding, t)
                }
                3 -> {
                    val binding = binding.monat3
                    monatSetzen(binding, t)
                }
                4 -> {
                    val binding = binding.monat4
                    monatSetzen(binding, t)
                }
                5 -> {
                    val binding = binding.monat5
                    monatSetzen(binding, t)
                }
                6 -> {
                    val binding = binding.monat6
                    monatSetzen(binding, t)
                }
                7 -> {
                    val binding = binding.monat7
                    monatSetzen(binding, t)
                }
                8 -> {
                    val binding = binding.monat8
                    monatSetzen(binding, t)
                }
                9 -> {
                    val binding = binding.monat9
                    monatSetzen(binding, t)
                }
                10 -> {
                    val binding = binding.monat10
                    monatSetzen(binding, t)
                }
                11 -> {
                    val binding = binding.monat11
                    monatSetzen(binding, t)
                }
                12 -> {
                    val binding = binding.monat12
                    monatSetzen(binding, t)
                }
            }
        }
    }

    private fun monatSetzen(bindingMonat: MonatBinding, t: KalenderEintrag) {
        when (t.tag) {
            1 -> {
                val bindingTag = bindingMonat.tag1
                tagSetzen(bindingTag, t)
            }
            2 -> {
                val bindingTag = bindingMonat.tag2
                tagSetzen(bindingTag, t)
            }
            3 -> {
                val bindingTag = bindingMonat.tag3
                tagSetzen(bindingTag, t)
            }
            4 -> {
                val bindingTag = bindingMonat.tag4
                tagSetzen(bindingTag, t)
            }
            5 -> {
                val bindingTag = bindingMonat.tag5
                tagSetzen(bindingTag, t)
            }
            6 -> {
                val bindingTag = bindingMonat.tag6
                tagSetzen(bindingTag, t)
            }
            7 -> {
                val bindingTag = bindingMonat.tag7
                tagSetzen(bindingTag, t)
            }
            8 -> {
                val bindingTag = bindingMonat.tag8
                tagSetzen(bindingTag, t)
            }
            9 -> {
                val bindingTag = bindingMonat.tag9
                tagSetzen(bindingTag, t)
            }
            10 -> {
                val bindingTag = bindingMonat.tag10
                tagSetzen(bindingTag, t)
            }
            11 -> {
                val bindingTag = bindingMonat.tag11
                tagSetzen(bindingTag, t)
            }
            12 -> {
                val bindingTag = bindingMonat.tag12
                tagSetzen(bindingTag, t)
            }
            13 -> {
                val bindingTag = bindingMonat.tag13
                tagSetzen(bindingTag, t)
            }
            14 -> {
                val bindingTag = bindingMonat.tag14
                tagSetzen(bindingTag, t)
            }
            15 -> {
                val bindingTag = bindingMonat.tag15
                tagSetzen(bindingTag, t)
            }
            16 -> {
                val bindingTag = bindingMonat.tag16
                tagSetzen(bindingTag, t)
            }
            17 -> {
                val bindingTag = bindingMonat.tag17
                tagSetzen(bindingTag, t)
            }
            18 -> {
                val bindingTag = bindingMonat.tag18
                tagSetzen(bindingTag, t)
            }
            19 -> {
                val bindingTag = bindingMonat.tag19
                tagSetzen(bindingTag, t)
            }
            20 -> {
                val bindingTag = bindingMonat.tag20
                tagSetzen(bindingTag, t)
            }
            21 -> {
                val bindingTag = bindingMonat.tag21
                tagSetzen(bindingTag, t)
            }
            22 -> {
                val bindingTag = bindingMonat.tag22
                tagSetzen(bindingTag, t)
            }
            23 -> {
                val bindingTag = bindingMonat.tag23
                tagSetzen(bindingTag, t)
            }
            24 -> {
                val bindingTag = bindingMonat.tag24
                tagSetzen(bindingTag, t)
            }
            25 -> {
                val bindingTag = bindingMonat.tag25
                tagSetzen(bindingTag, t)
            }
            26 -> {
                val bindingTag = bindingMonat.tag26
                tagSetzen(bindingTag, t)
            }
            27 -> {
                val bindingTag = bindingMonat.tag27
                tagSetzen(bindingTag, t)
            }
            28 -> {
                val bindingTag = bindingMonat.tag28
                tagSetzen(bindingTag, t)
            }
            29 -> {
                val bindingTag = bindingMonat.tag29
                tagSetzen(bindingTag, t)
            }
            30 -> {
                val bindingTag = bindingMonat.tag30
                tagSetzen(bindingTag, t)
            }
            31 -> {
                val bindingTag = bindingMonat.tag31
                tagSetzen(bindingTag, t)
            }
        }
    }

    private fun tagSetzen(binding: TagBinding, t: KalenderEintrag) {
        if (binding.txt1.text.isNullOrEmpty()) {
            binding.txt1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6f)
            binding.txt1.text = t.name
            when (t.status) {
                Cons.BLOCKUNG -> binding.txt1.setBackgroundResource(R.drawable.blockung)
                Cons.BUCHUNG -> binding.txt1.setBackgroundResource(R.drawable.buchung)
                Cons.SONSTIGES -> binding.txt1.setBackgroundResource(R.drawable.sonstiges)
            }
        } else if (binding.txt2.text.isNullOrEmpty()) {
            binding.txt2.visibility = View.VISIBLE
            binding.txt1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 5f)
            binding.txt2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 5f)
            binding.txt2.text = t.name
            when (t.status) {
                Cons.BLOCKUNG -> binding.txt2.setBackgroundResource(R.drawable.blockung)
                Cons.BUCHUNG -> binding.txt2.setBackgroundResource(R.drawable.buchung)
                Cons.SONSTIGES -> binding.txt2.setBackgroundResource(R.drawable.sonstiges)
            }
        } else {
            binding.txt3.visibility = View.VISIBLE
            binding.txt3.text = t.name
            binding.txt1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 4f)
            binding.txt2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 4f)
            binding.txt3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 4f)
            when (t.status) {
                Cons.BLOCKUNG -> binding.txt3.setBackgroundResource(R.drawable.blockung)
                Cons.BUCHUNG -> binding.txt3.setBackgroundResource(R.drawable.buchung)
                Cons.SONSTIGES -> binding.txt3.setBackgroundResource(R.drawable.sonstiges)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setTagNummern() {
        var binding = this.binding.monat1
        setTagNummerMonat(binding)
        binding = this.binding.monat2
        setTagNummerMonat(binding)
        binding = this.binding.monat3
        setTagNummerMonat(binding)
        binding = this.binding.monat4
        setTagNummerMonat(binding)
        binding = this.binding.monat5
        setTagNummerMonat(binding)
        binding = this.binding.monat6
        setTagNummerMonat(binding)
        binding = this.binding.monat7
        setTagNummerMonat(binding)
        binding = this.binding.monat8
        setTagNummerMonat(binding)
        binding = this.binding.monat9
        setTagNummerMonat(binding)
        binding = this.binding.monat10
        setTagNummerMonat(binding)
        binding = this.binding.monat11
        setTagNummerMonat(binding)
        binding = this.binding.monat12
        setTagNummerMonat(binding)

        val cal = GregorianCalendar()
        if (cal.isLeapYear(jahr)) {
            this.binding.monat2.tag29.txtTag.visibility = View.VISIBLE
        } else {
            this.binding.monat2.tag29.txtTag.visibility = View.INVISIBLE
        }
        this.binding.monat2.tag30.txtTag.visibility = View.INVISIBLE
        this.binding.monat2.tag30.txt1.visibility = View.INVISIBLE
        this.binding.monat2.tag30.txt2.visibility = View.INVISIBLE
        this.binding.monat2.tag31.txtTag.visibility = View.INVISIBLE
        this.binding.monat2.tag31.txt1.visibility = View.INVISIBLE
        this.binding.monat2.tag31.txt2.visibility = View.INVISIBLE

        this.binding.monat4.tag31.txtTag.visibility = View.INVISIBLE
        this.binding.monat4.tag31.txt1.visibility = View.INVISIBLE
        this.binding.monat4.tag31.txt2.visibility = View.INVISIBLE

        this.binding.monat6.tag31.txtTag.visibility = View.INVISIBLE
        this.binding.monat6.tag31.txt1.visibility = View.INVISIBLE
        this.binding.monat6.tag31.txt2.visibility = View.INVISIBLE

        this.binding.monat9.tag31.txtTag.visibility = View.INVISIBLE
        this.binding.monat9.tag31.txt1.visibility = View.INVISIBLE
        this.binding.monat9.tag31.txt2.visibility = View.INVISIBLE

        this.binding.monat11.tag31.txtTag.visibility = View.INVISIBLE
        this.binding.monat11.tag31.txt1.visibility = View.INVISIBLE
        this.binding.monat11.tag31.txt2.visibility = View.INVISIBLE
    }

    @SuppressLint("SetTextI18n")
    private fun setTagNummerMonat(binding: MonatBinding) {
        binding.tag1.txtTag.text = "1"
        binding.tag2.txtTag.text = "2"
        binding.tag3.txtTag.text = "3"
        binding.tag4.txtTag.text = "4"
        binding.tag5.txtTag.text = "5"
        binding.tag6.txtTag.text = "6"
        binding.tag7.txtTag.text = "7"
        binding.tag8.txtTag.text = "8"
        binding.tag9.txtTag.text = "9"
        binding.tag10.txtTag.text = "10"
        binding.tag11.txtTag.text = "11"
        binding.tag12.txtTag.text = "12"
        binding.tag13.txtTag.text = "13"
        binding.tag14.txtTag.text = "14"
        binding.tag15.txtTag.text = "15"
        binding.tag16.txtTag.text = "16"
        binding.tag17.txtTag.text = "17"
        binding.tag18.txtTag.text = "18"
        binding.tag19.txtTag.text = "19"
        binding.tag20.txtTag.text = "20"
        binding.tag21.txtTag.text = "21"
        binding.tag22.txtTag.text = "22"
        binding.tag23.txtTag.text = "23"
        binding.tag24.txtTag.text = "24"
        binding.tag25.txtTag.text = "25"
        binding.tag26.txtTag.text = "26"
        binding.tag27.txtTag.text = "27"
        binding.tag28.txtTag.text = "28"
        binding.tag29.txtTag.text = "29"
        binding.tag30.txtTag.text = "30"
        binding.tag31.txtTag.text = "31"
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