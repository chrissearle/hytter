package net.chrissearle.huts.routes

import arrow.core.raise.catch
import arrow.core.raise.context.raise
import arrow.core.raise.either
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import net.chrissearle.huts.api.DatabaseUnavailable
import net.chrissearle.huts.api.respond
import java.sql.SQLException
import javax.sql.DataSource

private const val READY_QUERY_TIMEOUT_SECONDS = 3

@Serializable
data class HealthStatus(
    val status: String,
)

/**
 * Kubernetes probes. Not proxied by the frontend - these are for the cluster,
 * not the internet.
 *
 * Liveness deliberately does not touch the database: a database blip should not
 * get the pod killed and restarted, which would fix nothing. That belongs to
 * readiness, which takes the pod out of the service until the database is back.
 */
fun Route.healthRoutes(dataSource: DataSource) {
    get("/health") {
        call.respond(HealthStatus("UP"))
    }

    get("/ready") {
        either {
            catch({ dataSource.verifyConnection() }) { e: SQLException ->
                raise(DatabaseUnavailable(e.message))
            }
            HealthStatus("UP")
        }.respond()
    }
}

private suspend fun DataSource.verifyConnection() =
    withContext(Dispatchers.IO) {
        connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.queryTimeout = READY_QUERY_TIMEOUT_SECONDS
                statement.execute("SELECT 1")
            }
        }
    }
