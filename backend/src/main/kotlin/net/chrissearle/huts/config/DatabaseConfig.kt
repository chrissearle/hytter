package net.chrissearle.huts.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource

private const val MAX_POOL_SIZE = 10

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
) {
    val jdbcUrl: String
        get() = "jdbc:postgresql://$host:$port/$name"

    fun dataSource(): DataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = this@DatabaseConfig.jdbcUrl
                username = user
                password = this@DatabaseConfig.password
                maximumPoolSize = MAX_POOL_SIZE
            },
        )

    companion object {
        fun fromEnv(): DatabaseConfig =
            DatabaseConfig(
                host = System.getenv("DB_HOST") ?: error("DB_HOST not set"),
                port = System.getenv("DB_PORT")?.toInt() ?: error("DB_PORT not set"),
                name = System.getenv("DB_NAME") ?: error("DB_NAME not set"),
                user = System.getenv("DB_USER") ?: error("DB_USER not set"),
                password = System.getenv("DB_PASSWORD") ?: error("DB_PASSWORD not set"),
            )
    }
}
