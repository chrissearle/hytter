package net.chrissearle.huts

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import net.chrissearle.huts.config.DatabaseConfig
import net.chrissearle.huts.config.runMigrations

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(wait = true)
}

fun Application.module() {
    runMigrations(DatabaseConfig.fromEnv())
}
