package net.chrissearle.huts.domain

import kotlinx.serialization.Serializable

/**
 * Who a booking is for. The first three are fixed groups whose booking name is
 * always their [displayName]; [PERSONAL] and [OTHER] carry a name supplied
 * elsewhere - see `BookingInput.resolve`.
 */
@Serializable
enum class BookingNameType(
    val displayName: String,
    val isFreeText: Boolean,
) {
    OPPHAVET("Opphavet", false),
    SORKISRAMPEN("Sørkisrampen", false),
    HA12("HA12", false),

    // Booking just for yourself: the name comes from the logged-in user's `name`
    // claim, or is typed in by an anonymous visitor who has no claim to draw on.
    PERSONAL("Personlig", true),
    OTHER("Annet", true),
}
