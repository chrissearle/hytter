package net.chrissearle.huts.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * What a client sends when creating or editing a booking. [name] is only read
 * for the free-text name types; for everything else the stored name is derived
 * server-side, so a client cannot label a booking as a group it isn't.
 */
@Serializable
data class BookingInput(
    val nameType: BookingNameType,
    val name: String? = null,
    val numberOfPeople: Int,
    val hut: Hut,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
)
