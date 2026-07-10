package net.chrissearle.huts.config

data class DatabaseConfig(
    val host: String,
    val port: Int,
    val name: String,
    val user: String,
    val password: String,
) {
    val jdbcUrl: String
        get() = "jdbc:postgresql://$host:$port/$name"

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
