package net.chrissearle.huts.domain

import kotlinx.serialization.Serializable

@Serializable
data class Hut(
    val id: Int,
    val name: String,
)
