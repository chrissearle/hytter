package net.chrissearle.huts.security

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.sessions.SessionTransportTransformerEncrypt
import io.ktor.server.sessions.Sessions
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.serialization.KotlinxSessionSerializer
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.routes.SessionInfo
import net.chrissearle.huts.routes.bookingRoutes
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

private val ENCRYPT_KEY = ByteArray(16) { it.toByte() }
private val SIGN_KEY = ByteArray(16) { (it + 1).toByte() }

private const val NOW = 1_000_000L

private fun sessionExpiringAt(
    refreshExpiresAt: Long,
    expiresAt: Long = refreshExpiresAt,
) = UserSession(
    subject = "user-subject",
    name = "Some User",
    roles = setOf("user"),
    expiresAt = expiresAt,
    refreshToken = "refresh-token",
    refreshExpiresAt = refreshExpiresAt,
)

/**
 * Exercises the real Keycloak session provider over HTTP.
 *
 * The rest of the suite installs a stub provider that leaves the request
 * anonymous by simply returning, which reports `NoCredentials` - so the
 * "cookie present but rejected" path was structurally unreachable, and an
 * expired session 401-ing the public calendar shipped unnoticed.
 *
 * The refresher's clock is injectable, so an expired session is produced
 * without Keycloak: `ensureFresh` short-circuits to `Expired` on the
 * `refreshExpiresAt` check before any HTTP call is made.
 */
class SessionAuthenticationTest :
    FunSpec({
        fun refresherAt(now: Long) =
            TokenRefresher(
                issuer = "https://keycloak.example.com/realms/hytter",
                clientId = "hytter",
                clientSecret = "secret",
                httpClient = mockk(),
                now = { now },
            )

        suspend fun io.ktor.server.testing.ApplicationTestBuilder.setup(
            session: UserSession,
            now: Long,
        ) = run {
            val repository = mockk<BookingRepository>()
            coEvery { repository.findInRange(any(), any()) } returns emptyList()

            application {
                install(ServerContentNegotiation) { json() }
                install(Sessions) {
                    cookie<UserSession>(SESSION_COOKIE_NAME) {
                        cookie.path = "/"
                        serializer = KotlinxSessionSerializer(Json)
                        transform(SessionTransportTransformerEncrypt(ENCRYPT_KEY, SIGN_KEY))
                    }
                }
                install(Authentication) { keycloakSession(AUTH_PROVIDER_NAME, refresherAt(now)) }
                routing {
                    // Stands in for /callback: mints a real, correctly encrypted cookie.
                    get("/test-login") {
                        call.sessions.set(session)
                        call.respond(HttpStatusCode.OK)
                    }
                    bookingRoutes(repository)
                }
            }

            createClient {
                install(ContentNegotiation) { json() }
                install(HttpCookies)
            }
        }

        test("an expired session leaves the public calendar readable instead of 401-ing it") {
            testApplication {
                val client = setup(sessionExpiringAt(refreshExpiresAt = NOW - 1), now = NOW)

                client.get("/test-login").status shouldBe HttpStatusCode.OK

                // Regression: this returned 401 because Ktor reported
                // InvalidCredentials for a present-but-rejected cookie, which
                // defeats the `optional = true` short-circuit.
                client.get("/api/bookings").status shouldBe HttpStatusCode.OK
            }
        }

        test("an expired session reports itself as anonymous rather than 401-ing /api/session") {
            testApplication {
                val client = setup(sessionExpiringAt(refreshExpiresAt = NOW - 1), now = NOW)

                client.get("/test-login")

                val response = client.get("/api/session")

                response.status shouldBe HttpStatusCode.OK
                response.body<SessionInfo>().authenticated shouldBe false
            }
        }

        test("an expired session clears the cookie so the browser stops sending it") {
            testApplication {
                val client = setup(sessionExpiringAt(refreshExpiresAt = NOW - 1), now = NOW)

                client.get("/test-login")

                val cleared =
                    client
                        .get("/api/bookings")
                        .headers
                        .getAll("Set-Cookie")
                        .orEmpty()
                        .any { it.startsWith(SESSION_COOKIE_NAME) && it.contains("Max-Age=0") }

                cleared.shouldBeTrue()
            }
        }

        test("a live session still authenticates and is reported as logged in") {
            testApplication {
                val client =
                    setup(
                        sessionExpiringAt(refreshExpiresAt = NOW + 3600, expiresAt = NOW + 3600),
                        now = NOW,
                    )

                client.get("/test-login")

                val session = client.get("/api/session").body<SessionInfo>()

                session.authenticated shouldBe true
                session.name shouldBe "Some User"
                session.hasAccess shouldBe true
            }
        }

        test("no cookie at all is still anonymous, not a 401") {
            testApplication {
                val client = setup(sessionExpiringAt(refreshExpiresAt = NOW + 3600), now = NOW)

                client.get("/api/bookings").status shouldBe HttpStatusCode.OK
                client.get("/api/session").body<SessionInfo>().authenticated shouldBe false
            }
        }
    })
