package net.chrissearle.huts

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.routing.routing
import net.chrissearle.huts.config.DatabaseConfig
import net.chrissearle.huts.config.runMigrations
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.routes.bookingRoutes
import net.chrissearle.huts.security.AuthConfig
import net.chrissearle.huts.security.configureHytterAuth

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    val databaseConfig = DatabaseConfig.fromEnv()
    runMigrations(databaseConfig)
    val dataSource = databaseConfig.dataSource()
    val bookingRepository = BookingRepository(dataSource)

    install(ContentNegotiation) {
        json()
    }

    install(CORS) {
        anyHost()
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
    }

    install(Authentication) {
        configureHytterAuth(AuthConfig.fromEnv())
    }

    routing {
        bookingRoutes(bookingRepository)
    }
}
