package net.chrissearle.huts.domain

import kotlinx.datetime.LocalDate

/**
 * A booking row as stored. Deliberately not serializable: it carries
 * [createdBySubject], which is an internal authorization key and has no business
 * reaching the browser. Routes map this to [Booking] for the wire, resolving
 * `canEdit` against the requesting principal on the way out.
 */
data class BookingRecord(
    val id: Int,
    val nameType: BookingNameType,
    val name: String,
    val numberOfPeople: Int,
    val hut: Hut,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val adminNotes: String?,
    val status: BookingStatus,
    val createdBy: String?,
    val createdBySubject: String?,
) {
    fun toBooking(canEdit: Boolean) =
        Booking(
            id = id,
            nameType = nameType,
            name = name,
            numberOfPeople = numberOfPeople,
            hut = hut,
            arrivalDate = arrivalDate,
            departureDate = departureDate,
            adminNotes = adminNotes,
            status = status,
            createdBy = createdBy,
            canEdit = canEdit,
        )
}
