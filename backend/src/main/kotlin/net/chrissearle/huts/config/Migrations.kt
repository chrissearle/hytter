package net.chrissearle.huts.config

import org.flywaydb.core.Flyway

fun runMigrations(config: DatabaseConfig) {
    Flyway
        .configure()
        .dataSource(config.jdbcUrl, config.user, config.password)
        .load()
        .migrate()
}
