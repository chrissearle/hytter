package net.chrissearle.huts

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import net.chrissearle.huts.config.DatabaseConfig
import net.chrissearle.huts.config.runMigrations
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.routes.authRoutes
import net.chrissearle.huts.routes.bookingRoutes
import net.chrissearle.huts.security.AuthConfig
import net.chrissearle.huts.security.configureHytterAuth
import net.chrissearle.huts.security.configureSessions

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    val databaseConfig = DatabaseConfig.fromEnv()
    runMigrations(databaseConfig)
    val dataSource = databaseConfig.dataSource()
    val bookingRepository = BookingRepository(dataSource)
    val authConfig = AuthConfig.fromEnv()

    // No CORS: the browser only ever talks to the frontend's own origin. The
    // frontend proxies /api, /login, /callback and /logout to this backend over
    // the in-cluster service address, so this is same-origin as far as the browser
    // (and Keycloak's redirect_uri validation) is concerned.

    install(ContentNegotiation) {
        json()
    }

    configureSessions()

    val httpClient = HttpClient(CIO)

    install(Authentication) {
        configureHytterAuth(authConfig, httpClient)
    }

    routing {
        bookingRoutes(bookingRepository)

        if (!authConfig.disabled) {
            val issuer = requireNotNull(authConfig.issuer) { "KEYCLOAK_ISSUER not set" }
            val clientId = requireNotNull(authConfig.clientId) { "KEYCLOAK_CLIENT_ID not set" }
            authRoutes(publicUrl = authConfig.publicUrl, issuer = issuer, clientId = clientId)
        }
    }
}
