package com.example.pyracalendar

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import com.example.pyraapp.database.Event
import com.example.pyracalendar.database.Veranstaltung
import com.example.pyracalendar.databinding.ActivityMainBinding
import com.example.pyracalendar.databinding.MonatBinding
import com.example.pyracalendar.databinding.TagBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.GregorianCalendar


class MainActivity : FragmentActivity() {

    private var user: FirebaseUser? = null
    private var mAuth: FirebaseAuth? = null
    private lateinit var database: DatabaseReference

    private var jahr: Int = 0
    private var month: Int = 0
    private var monthOffset: Int = 0
    private lateinit var startDate: LocalDate
    private lateinit var endDate: LocalDate

    private var monthList: ArrayList<LocalDate> = ArrayList()
    private var monthListString: ArrayList<String> = ArrayList()

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
        month = LocalDate.now().monthValue

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
        var datum: LocalDate = LocalDate.now()
        var name: String = ""
        var status: String = ""
        var blockDatum: String = ""
    }

    private fun kalenderAbfragen() {
        updateMonthList()
        val databaseEvent: DatabaseReference =
            FirebaseDatabase.getInstance(Constants.DOMAIN).getReference("Event")
        val eventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val vaList: ArrayList<Event> = ArrayList()

                for (eventListener in dataSnapshot.children) {
                    val event: Event? =
                        eventListener.getValue(Event::class.java)
                    if (event != null) {
                        if (!event.datum?.event?.beginn.isNullOrEmpty()) {
                            if (event.typ?.status != Cons.ANFRAGE){
                                if (event.datum?.event?.beginn?.substring(6, 10)
                                        ?.toInt() == monthList[0].year || event.datum?.event?.beginn?.substring(
                                        6,
                                        10
                                    )
                                        ?.toInt() == monthList[11].year
                                ) {
                                    vaList.add(event)
                                }
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
        databaseEvent.orderByChild("erstellDatum")
            .addValueEventListener(eventListener)
    }

    private fun kalenderSetzen(vaList: ArrayList<Event>) {
        var kalenderListe: ArrayList<KalenderEintrag> = ArrayList()
        for (e in vaList) {

            //Aufbautage & Abbautage hinzufügen
            if (e.typ?.aufbautage == true) {
                if (!e.datum?.aufbau?.beginn.isNullOrEmpty()) {
                    val tage: Int = (inTagen(e.datum?.event?.beginn) - inTagen(e.datum?.aufbau?.beginn)) - 1
                    val date =
                        LocalDate.parse(e.datum?.aufbau?.beginn, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    for (i in 0..tage) {
                        val k = KalenderEintrag()
                        k.datum = date.plusDays(i.toLong())
                        if ((k.datum.isAfter(startDate) || k.datum.isEqual(startDate)) && (k.datum.isBefore(
                                endDate
                            ) || k.datum.isEqual(endDate))
                        ) {
                            if (e.typ.status != Cons.SONSTIGES) {
                                k.name =
                                    "(A) " + e.name.toString() + " (" + raumsetzen(e.raum?.summe!!) + ")"
                            } else {
                                k.name = "(A) " + e.name.toString()
                            }
                            k.status = e.typ.status.toString()
                            k.blockDatum = e.blockung?.datum.toString()
                            kalenderListe.add(k)
                        }
                    }
                }
            }
            if (!e.datum?.aufbau?.ende.isNullOrEmpty()) {
                val tage: Int = (inTagen(e.datum?.aufbau?.ende) - inTagen(e.datum?.event?.ende)) - 1
                val date =
                    LocalDate.parse(e.datum?.event?.ende, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                        .plusDays(1)
                for (i in 0..tage) {
                    val k = KalenderEintrag()
                    k.datum = date.plusDays(i.toLong())
                    if ((k.datum.isAfter(startDate) || k.datum.isEqual(startDate)) && (k.datum.isBefore(
                            endDate
                        ) || k.datum.isEqual(endDate))
                    ) {
                        if (e.typ?.status != Cons.SONSTIGES) {
                            k.name =
                                "(A) " + e.name.toString() + " (" + raumsetzen(e.raum?.summe!!) + ")"
                        } else {
                            k.name =
                                "(A) " + e.name.toString()
                        }
                        k.status = e.typ?.status.toString()
                        k.blockDatum = e.blockung?.datum.toString()
                        kalenderListe.add(k)
                    }
                }
            }


            //Veranstaltungstage hinzufügen
            val tage: Int = (inTagen(e.datum?.event?.ende) - inTagen(e.datum?.event?.beginn))
            val date =
                LocalDate.parse(e.datum?.event?.beginn, DateTimeFormatter.ofPattern("dd/MM/yyyy"))

            for (i in 0..tage) {
                val k = KalenderEintrag()
                k.datum = date.plusDays(i.toLong())
                if ((k.datum.isAfter(startDate) || k.datum.isEqual(startDate)) && (k.datum.isBefore(
                        endDate
                    ) || k.datum.isEqual(endDate))
                ) {
                    if (e.typ?.status != Cons.SONSTIGES) {
                        k.name = e.name.toString() + " (" + raumsetzen(e.raum?.summe!!) + ")"
                    } else {
                        k.name = e.name.toString()
                    }
                    k.status = e.typ?.status.toString()
                    k.blockDatum = e.blockung?.datum.toString()
                    kalenderListe.add(k)
                }
            }
        }
        updateUI(kalenderListe)
    }

    fun stringToDate(datum: String?): LocalDate {
        val localDate: LocalDate = LocalDate.of(
            datum?.substring(6, 10)!!.toInt(),
            datum.substring(3, 5).toInt(),
            datum.substring(0, 2).toInt()
        )
        return localDate
    }

    private fun setMonth(localDate: LocalDate): LocalDate {
        var currentDate = localDate
        val position: Int
        val sum = currentDate.monthValue - month
        position = if (sum >= 0) {
            sum + 1
        } else {
            13 + sum
        }
        currentDate = currentDate.withMonth(position)
        return currentDate
    }

    private fun raumsetzen(r: String): String {
        var raum = ""
        if (r.contains(Cons.C1500)) {
            raum = "C"
            if (r.contains(Cons.B800)) {
                raum += ", ◭"
                if (r.contains(Cons.P250)) {
                    raum += ", P"
                }
            } else if (r.contains(Cons.P250)) {
                raum += ", P"
            }
        } else if (r.contains(Cons.B800)) {
            raum = "◭"
            if (r.contains(Cons.P250)) {
                raum += ", P"
            }
        } else {
            raum = "P"
        }
        if (raum == "C, ◭, P") {
            raum = "g. H."
        }
        return raum
    }

    @SuppressLint("SetTextI18n")
    private fun updateUI(kalenderListe: ArrayList<KalenderEintrag>) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRight.setOnClickListener { monthOffset++; kalenderAbfragen() }
        binding.btnLeft.setOnClickListener { monthOffset--; kalenderAbfragen() }

        setTagNummern()
        binding.txtYear.text = "${monthListString[0]}  ${monthList[0].year}"

        val monate = listOf(
            binding.monat1, binding.monat2, binding.monat3, binding.monat4,
            binding.monat5, binding.monat6, binding.monat7, binding.monat8,
            binding.monat9, binding.monat10, binding.monat11, binding.monat12
        )

        for (t in kalenderListe) {
            val index = dateTransform(t.datum) - 1
            if (index in monate.indices) {
                monatSetzen(monate[index], t)
            }
        }
    }

    private fun dateTransform(date: LocalDate): Int {
        var currentDate = date
        var position: Int
        val sum = currentDate.monthValue - LocalDate.now().monthValue
        position = if (sum >= 0) {
            sum + 1
        } else {
            13 + sum
        }
        position = if (monthOffset > 0) {
            if (position - monthOffset > 0) {
                position - monthOffset
            } else {
                12 + position - monthOffset
            }
        } else {
            if (position - monthOffset < 13) {
                position - monthOffset
            } else {
                position - monthOffset - 12
            }
        }
        currentDate = currentDate.withMonth(position)
        return currentDate.monthValue
    }

    private fun monatSetzen(bindingMonat: MonatBinding, t: KalenderEintrag) {
        val tage = listOf(
            bindingMonat.tag1, bindingMonat.tag2, bindingMonat.tag3, bindingMonat.tag4, bindingMonat.tag5,
            bindingMonat.tag6, bindingMonat.tag7, bindingMonat.tag8, bindingMonat.tag9, bindingMonat.tag10,
            bindingMonat.tag11, bindingMonat.tag12, bindingMonat.tag13, bindingMonat.tag14, bindingMonat.tag15,
            bindingMonat.tag16, bindingMonat.tag17, bindingMonat.tag18, bindingMonat.tag19, bindingMonat.tag20,
            bindingMonat.tag21, bindingMonat.tag22, bindingMonat.tag23, bindingMonat.tag24, bindingMonat.tag25,
            bindingMonat.tag26, bindingMonat.tag27, bindingMonat.tag28, bindingMonat.tag29, bindingMonat.tag30,
            bindingMonat.tag31
        )

        val index = t.datum.dayOfMonth - 1
        if (index in tage.indices) {
            tagSetzen(tage[index], t)
        }
    }

    @SuppressLint("ResourceAsColor")
    private fun tagSetzen(binding: TagBinding, t: KalenderEintrag) {
        urlaubColor(binding, t.status)
        if (binding.txt1.text.isNullOrEmpty()) {
            if (t.status != Cons.URLAUB && t.status != Cons.FERIEN && t.status != Cons.SEASON) {
                binding.txt1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 6f)
                binding.txt1.text = t.name
            }
            when (t.status) {
                Cons.BLOCKUNG -> {
                    binding.txt1.setBackgroundResource(R.drawable.blockung)
                    if (t.blockDatum.isNotEmpty()) {
                        if (stringToDate(t.blockDatum).isBefore(LocalDate.now())) {
                            blinkTxt(binding.txt1)
                        }
                    }
                }

                Cons.BUCHUNG -> binding.txt1.setBackgroundResource(R.drawable.buchung)
                Cons.SONSTIGES -> binding.txt1.setBackgroundResource(R.drawable.sonstiges)
                Cons.URLAUB -> binding.txtUrlaub.text =
                    urlaubSetzen(binding.txtUrlaub.text.toString(), t.name)
                Cons.FERIEN -> binding.root.setBackgroundResource(
                    if (isWeekend(t.datum)) R.drawable.tag_ferien_wochenende else R.drawable.tag_ferien
                )
                Cons.SEASON -> binding.root.setBackgroundResource(
                    if (isWeekend(t.datum)) R.drawable.tag_season_wochenende else R.drawable.tag_season
                )
            }
        } else if (binding.txt2.text.isNullOrEmpty()) {
            if (t.status != Cons.URLAUB && t.status != Cons.FERIEN && t.status != Cons.SEASON) {
                binding.txt2.visibility = View.VISIBLE
                binding.txt1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 5f)
                binding.txt2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 5f)
                binding.txt2.text = t.name
            }
            when (t.status) {
                Cons.BLOCKUNG -> {
                    binding.txt2.setBackgroundResource(R.drawable.blockung)
                    if (t.blockDatum.isNotEmpty()) {
                        if (stringToDate(t.blockDatum).isBefore(LocalDate.now())) {
                            blinkTxt(binding.txt2)
                        }
                    }
                }

                Cons.BUCHUNG -> binding.txt2.setBackgroundResource(R.drawable.buchung)
                Cons.SONSTIGES -> binding.txt2.setBackgroundResource(R.drawable.sonstiges)
                Cons.URLAUB -> binding.txtUrlaub.text =
                    urlaubSetzen(binding.txtUrlaub.text.toString(), t.name)
                Cons.FERIEN -> binding.root.setBackgroundResource(
                    if (isWeekend(t.datum)) R.drawable.tag_ferien_wochenende else R.drawable.tag_ferien
                )
                Cons.SEASON -> binding.root.setBackgroundResource(
                    if (isWeekend(t.datum)) R.drawable.tag_season_wochenende else R.drawable.tag_season
                )
            }
        } else {
            if (t.status != Cons.URLAUB && t.status != Cons.FERIEN && t.status != Cons.SEASON) {
                binding.txt3.visibility = View.VISIBLE
                binding.txt3.text = t.name
                binding.txt1.setTextSize(TypedValue.COMPLEX_UNIT_SP, 4f)
                binding.txt2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 4f)
                binding.txt3.setTextSize(TypedValue.COMPLEX_UNIT_SP, 4f)
            }
            when (t.status) {
                Cons.BLOCKUNG -> {
                    binding.txt3.setBackgroundResource(R.drawable.blockung)
                    if (t.blockDatum.isNotEmpty()) {
                        if (stringToDate(t.blockDatum).isBefore(LocalDate.now())) {
                            blinkTxt(binding.txt3)
                        }
                    }
                }

                Cons.BUCHUNG -> binding.txt3.setBackgroundResource(R.drawable.buchung)
                Cons.SONSTIGES -> binding.txt3.setBackgroundResource(R.drawable.sonstiges)
                Cons.URLAUB -> binding.txtUrlaub.text =
                    urlaubSetzen(binding.txtUrlaub.text.toString(), t.name)
                Cons.FERIEN -> binding.root.setBackgroundResource(
                    if (isWeekend(t.datum)) R.drawable.tag_ferien_wochenende else R.drawable.tag_ferien
                )
                Cons.SEASON -> binding.root.setBackgroundResource(
                    if (isWeekend(t.datum)) R.drawable.tag_season_wochenende else R.drawable.tag_season
                )
            }
        }
    }


    private fun urlaubColor(binding: TagBinding, status: String) {
        if (status != Cons.URLAUB && status != Cons.FERIEN && status != Cons.SEASON) {
            binding.txtUrlaub.setTextColor(Color.WHITE)
        }
    }

    private fun urlaubSetzen(current: String, toAdd: String): String {
        return if (current.isEmpty()) {
            toAdd.substring(0, 1)
        } else {
            current + ", " + toAdd.substring(0, 1)
        }
    }

    @SuppressLint("ResourceType")
    private fun blinkTxt(txt: TextView) {
        val animation = AnimatorInflater.loadAnimator(this, R.anim.blink_animation)
        animation.setTarget(txt)
        animation.start()
    }

    private fun updateMonthList() {
        monthList.clear()
        for (i in 0..11) {
            monthList.add(
                getDateWithMonthOffset(
                    LocalDate.now(),
                    monthOffset + i
                ).withDayOfMonth(1)
            )
        }
        startDate = monthList[0]
        endDate = monthList[11].withDayOfMonth(monthList[11].month.length(monthList[11].isLeapYear))
        val test = endDate
        test.monthValue
    }

    @SuppressLint("SetTextI18n")
    private fun setTagNummern() {
        val monthAbbreviations = listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")

        monthListString.clear()
        monthListString.addAll(monthList.map { monthAbbreviations[it.monthValue - 1] })

        val monthTextViews = listOf(
            binding.titel.txtMonat1, binding.titel.txtMonat2, binding.titel.txtMonat3, binding.titel.txtMonat4,
            binding.titel.txtMonat5, binding.titel.txtMonat6, binding.titel.txtMonat7, binding.titel.txtMonat8,
            binding.titel.txtMonat9, binding.titel.txtMonat10, binding.titel.txtMonat11, binding.titel.txtMonat12
        )

        monthTextViews.forEachIndexed { index, textView ->
            textView.text = monthListString[index]
        }

        val monthBindings = listOf(
            binding.monat1, binding.monat2, binding.monat3, binding.monat4,
            binding.monat5, binding.monat6, binding.monat7, binding.monat8,
            binding.monat9, binding.monat10, binding.monat11, binding.monat12
        )

        monthBindings.forEachIndexed { index, monatBinding ->
            setTagNummerMonat(monatBinding, monthList[index].year, monthList[index].monthValue)
        }
    }


    @SuppressLint("SetTextI18n")
    private fun setTagNummerMonat(binding: MonatBinding, jahr: Int, monat: Int) {
        for (i in 0 until binding.root.childCount) {
            val tagLayout = binding.root.getChildAt(i) as ConstraintLayout
            val tagTextView = tagLayout.getChildAt(0) as TextView
            tagTextView.text = (i + 1).toString()
            try {
                val datum = LocalDate.of(jahr, monat, (i + 1))
                if (isWeekend(datum)) {
                    tagLayout.setBackgroundResource(R.drawable.tag_wochenende)
                }
            } catch (_: DateTimeException) {

            }
        }
        if (monat == 4 || monat == 6 || monat == 9 || monat == 11) {
            binding.tag31.txtTag.visibility = View.INVISIBLE
            binding.tag31.txt1.visibility = View.INVISIBLE
            binding.tag31.txt2.visibility = View.INVISIBLE
        } else if (monat == 2) {
            val cal = GregorianCalendar()
            if (cal.isLeapYear(jahr)) {
                binding.tag29.txtTag.visibility = View.VISIBLE
            } else {
                binding.tag29.txtTag.visibility = View.INVISIBLE
            }
            binding.tag30.txtTag.visibility = View.INVISIBLE
            binding.tag30.txt1.visibility = View.INVISIBLE
            binding.tag30.txt2.visibility = View.INVISIBLE
            binding.tag31.txtTag.visibility = View.INVISIBLE
            binding.tag31.txt1.visibility = View.INVISIBLE
            binding.tag31.txt2.visibility = View.INVISIBLE
        }
    }

    fun isWeekend(date: LocalDate): Boolean {
        return date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    }

    fun getDateWithMonthOffset(localDate: LocalDate, monthOffset: Int): LocalDate {
        // Get the current date
        var currentDate = localDate
        val monthsum = monthOffset + currentDate.monthValue

        if (monthsum < 1) { //minus Jahr
            currentDate = currentDate.withYear(currentDate.year - 1)
            currentDate = currentDate.withMonth(currentDate.monthValue + 12 + monthOffset)
        } else if (monthsum <= 12) { //gleiches Jahr
            currentDate = currentDate.withMonth(currentDate.monthValue + monthOffset)
        } else { //plus Jahr
            currentDate = currentDate.withYear(currentDate.year + 1)
            currentDate = currentDate.withMonth(currentDate.monthValue + monthOffset - 12)
        }

        return currentDate
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
}