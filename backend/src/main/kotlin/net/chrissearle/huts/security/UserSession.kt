package net.chrissearle.huts.security

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val name: String,
    val roles: Set<String>,
)
