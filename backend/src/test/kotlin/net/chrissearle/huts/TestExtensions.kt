package net.chrissearle.huts

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.AuthenticationProvider
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import net.chrissearle.huts.security.AUTH_PROVIDER_NAME
import net.chrissearle.huts.security.HytterPrincipal

/** Header used only by [testAuthentication] to pick a fixed principal for a request. */
const val TEST_PRINCIPAL_HEADER = "X-Test-Principal"

val adminPrincipal =
    HytterPrincipal(subject = "admin-subject", name = "Admin", roles = setOf("admin", "user"))
val userPrincipal =
    HytterPrincipal(subject = "user-subject", name = "Some User", roles = setOf("user"))

/**
 * Authenticated against the shared realm but granted no `hytter` client role -
 * holding an account in the realm must not by itself grant access.
 */
val noRolePrincipal =
    HytterPrincipal(subject = "stranger-subject", name = "Realm Stranger", roles = setOf("monit-access"))

private val testPrincipals =
    mapOf("admin" to adminPrincipal, "user" to userPrincipal, "norole" to noRolePrincipal)

/**
 * Stands in for the real Keycloak session auth in tests: reads [TEST_PRINCIPAL_HEADER]
 * ("admin" / "user") and sets the matching principal, or leaves the request anonymous
 * when the header is absent - so route authorization branches can be exercised directly.
 */
private class TestAuthenticationProvider(
    config: Config,
) : AuthenticationProvider(config) {
    class Config(
        name: String?,
    ) : AuthenticationProvider.Config(name)

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val header = context.call.request.headers[TEST_PRINCIPAL_HEADER] ?: return
        val principal = testPrincipals[header] ?: return
        context.principal(name, principal)
    }
}

private fun AuthenticationConfig.test(name: String?) {
    register(TestAuthenticationProvider(TestAuthenticationProvider.Config(name)))
}

fun Application.testHytterApplication(block: Application.() -> Unit) {
    install(ContentNegotiation) {
        json()
    }
    install(Authentication) {
        test(AUTH_PROVIDER_NAME)
    }
    block()
}
