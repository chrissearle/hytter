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
import kotlinx.serialization.json.Json
import net.chrissearle.huts.config.DatabaseConfig
import net.chrissearle.huts.config.runMigrations
import net.chrissearle.huts.monitoring.configureMonitoring
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.routes.authRoutes
import net.chrissearle.huts.routes.bookingRoutes
import net.chrissearle.huts.security.AuthConfig
import net.chrissearle.huts.security.TokenRefresher
import net.chrissearle.huts.security.configureHytterAuth
import net.chrissearle.huts.security.configureSessions
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

private const val SERVER_PORT = 8080

fun main() {
    embeddedServer(Netty, port = SERVER_PORT, module = Application::module).start(wait = true)
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

    configureMonitoring()

    install(ContentNegotiation) {
        json()
    }

    configureSessions(authConfig)

    val httpClient =
        HttpClient(CIO) {
            install(ClientContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    val refresher =
        if (authConfig.disabled) {
            null
        } else {
            TokenRefresher(
                issuer = requireNotNull(authConfig.issuer) { "KEYCLOAK_ISSUER not set" },
                clientId = requireNotNull(authConfig.clientId) { "KEYCLOAK_CLIENT_ID not set" },
                clientSecret = requireNotNull(authConfig.clientSecret) { "KEYCLOAK_CLIENT_SECRET not set" },
                httpClient = httpClient,
            )
        }

    install(Authentication) {
        configureHytterAuth(authConfig, httpClient, refresher)
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
