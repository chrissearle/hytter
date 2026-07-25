package net.chrissearle.huts.routes

import arrow.core.raise.catch
import arrow.core.raise.context.raise
import arrow.core.raise.either
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import net.chrissearle.huts.api.VersionNotReadable
import net.chrissearle.huts.api.respond
import net.chrissearle.huts.config.BuildInfo

@Serializable
data class VersionInfo(
    val version: String,
)

/**
 * Proxied through the frontend (unlike /metrics, /health and /ready) so the
 * footer can show what is actually deployed.
 */
fun Route.versionRoute() {
    get("/version") {
        either {
            val version =
                catch({ BuildInfo.version() }) { e: Throwable ->
                    raise(VersionNotReadable(e))
                }
            VersionInfo(version)
        }.respond()
    }
}
