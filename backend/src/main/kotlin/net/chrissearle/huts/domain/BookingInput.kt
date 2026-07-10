package net.chrissearle.huts.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class BookingInput(
    val name: String,
    val numberOfPeople: Int,
    val hutId: Int,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
)
