package net.chrissearle.huts.domain

import kotlinx.datetime.LocalDate

/**
 * A validated booking with its display name already resolved from the
 * [BookingNameType] and the requesting principal. This - not the raw
 * [BookingInput] - is what the repository persists.
 */
data class BookingData(
    val nameType: BookingNameType,
    val name: String,
    val numberOfPeople: Int,
    val hut: Hut,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
)
