package net.chrissearle.huts.domain

import kotlinx.serialization.Serializable

@Serializable
enum class BookingStatus {
    OPEN,
    APPROVED,
}
