package net.chrissearle.huts.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Booking(
    val id: Int,
    val name: String,
    val numberOfPeople: Int,
    val hutId: Int,
    val hutName: String,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val adminNotes: String?,
    val status: BookingStatus,
)
