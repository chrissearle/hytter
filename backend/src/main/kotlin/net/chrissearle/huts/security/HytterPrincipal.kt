package net.chrissearle.huts.security

data class HytterPrincipal(
    val name: String,
    val roles: Set<String>,
) {
    val isAdmin: Boolean get() = "admin" in roles
}
