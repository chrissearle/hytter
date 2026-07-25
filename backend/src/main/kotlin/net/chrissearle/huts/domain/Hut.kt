package net.chrissearle.huts.domain

import kotlinx.serialization.Serializable

/**
 * The three bookable spaces. The enum name is the persisted value (and the
 * value on the wire); [displayName] is the Bokmål label the GUI renders.
 */
@Serializable
enum class Hut(
    val displayName: String,
) {
    HULDREBAKKEN("Huldrebakken"),
    TROLLHAUGEN("Trollhaugen"),
    TENT_HAMMOCK("Telt/hengekøye"),
}
