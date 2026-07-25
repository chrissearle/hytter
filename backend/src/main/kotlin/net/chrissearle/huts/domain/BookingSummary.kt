package net.chrissearle.huts.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class BookingSummary(
    val id: Int,
    val name: String,
    val hut: Hut,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val status: BookingStatus,
)
