package net.chrissearle.huts.domain

import kotlinx.serialization.Serializable

/**
 * The admin's note on a booking, visible to the requester. Sending null or a
 * blank string clears it.
 */
@Serializable
data class AdminNotesInput(
    val adminNotes: String? = null,
)
