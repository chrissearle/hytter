package net.chrissearle.huts.domain

import kotlinx.serialization.Serializable

/**
 * The dropdown contents for the booking form. Served from the enums above so
 * the GUI never hard-codes hut names or booking groups.
 */
@Serializable
data class HutItem(
    val value: Hut,
    val displayName: String,
)

@Serializable
data class NameTypeItem(
    val value: BookingNameType,
    val displayName: String,
    val isFreeText: Boolean,
)

@Serializable
data class Reference(
    val huts: List<HutItem>,
    val nameTypes: List<NameTypeItem>,
) {
    companion object {
        val current =
            Reference(
                huts = Hut.entries.map { HutItem(value = it, displayName = it.displayName) },
                nameTypes =
                    BookingNameType.entries.map {
                        NameTypeItem(value = it, displayName = it.displayName, isFreeText = it.isFreeText)
                    },
            )
    }
}
