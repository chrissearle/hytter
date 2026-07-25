package net.chrissearle.huts.domain

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class Booking(
    val id: Int,
    val nameType: BookingNameType,
    val name: String,
    val numberOfPeople: Int,
    val hut: Hut,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val adminNotes: String?,
    val status: BookingStatus,
    /** Display name of whoever requested it - shown to admins, not used for authorization. */
    val createdBy: String?,
    /**
     * Whether the requesting principal may edit this booking. Resolved server-side
     * so the client never needs the owner's identity to work it out.
     */
    val canEdit: Boolean,
)
