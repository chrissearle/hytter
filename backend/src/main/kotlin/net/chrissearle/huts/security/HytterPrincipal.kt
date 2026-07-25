package net.chrissearle.huts.security

import net.chrissearle.huts.domain.BookingNameType

const val ROLE_ADMIN = "admin"
const val ROLE_USER = "user"

/**
 * Which fixed booking group a user belongs to, expressed as `hytter` client
 * roles rather than a realm-level user attribute - group membership is scoped to
 * this app exactly like [ROLE_ADMIN]/[ROLE_USER], and rides the same
 * `resource_access.hytter.roles` claim. The mapping is explicit (not a prefix
 * convention) so the coupling between role name and enum is greppable.
 */
val GROUP_ROLES =
    mapOf(
        "group-opphavet" to BookingNameType.OPPHAVET,
        "group-sorkisrampen" to BookingNameType.SORKISRAMPEN,
        "group-ha12" to BookingNameType.HA12,
    )

data class HytterPrincipal(
    /** The Keycloak `sub` claim - stable across name changes, unlike [name]. */
    val subject: String,
    val name: String,
    val roles: Set<String>,
) {
    val isAdmin: Boolean get() = ROLE_ADMIN in roles

    /**
     * Whether this account is entitled to the app at all. The Keycloak realm is
     * shared with other sites, so merely holding an account there is not enough -
     * a `hytter` client role has to have been granted. Admin implies access.
     */
    val hasAccess: Boolean get() = isAdmin || ROLE_USER in roles

    /**
     * The single fixed group this user may book under, derived from the
     * `group-*` client roles. `null` for admins (who may book any group) and for
     * users with no group assigned. If more than one group role is somehow
     * present we treat it as none rather than guess - "exactly one" is the
     * intended shape, and a misconfiguration should not silently grant a group.
     */
    val group: BookingNameType?
        get() = roles.mapNotNull { GROUP_ROLES[it] }.singleOrNull()
}
