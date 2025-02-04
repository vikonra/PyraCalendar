package com.example.pyraapp.database

import com.google.firebase.database.IgnoreExtraProperties

// [START rtdb_user_class]
@IgnoreExtraProperties
data class Event(
    val name: String? = null, //Kunde
    val personen: Int? = null, //Gästeanzahl
    val datum: Datum? = null, //Veranstaltungsdatum, Sortierung und Zeiten
    val blockung: Blockung? = null, //Reservierungsdatum und Reihenfolge
    val typ: Typ? = null, //Grundangaben Status, Art und Zeiträume
    val raum: Raum? = null, //Räume und Bestuhlung
    val kontakt: Kontakt? = null, //Ansprechperson und Beschreibung
    val zeitplan: Zeitplan? = null, //Aktuell String, evtl. zukünftig Liste
    val getraenke: Getraenke? = null, //Getränkepauschalen
    val catering: Catering? = null, //Kontakt und Beschreibung
    val technik: Technik? = null, //Kontakt, Personenanzahl und Schnittstelle
    val dienstleister: Dienstleister? = null, //Deko, Film, DJ, Shuttle
    val sonstiges: Sonstiges? = null, //Aktuell String, evtl. zukünftig Liste
) {

}

