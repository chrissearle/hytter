package net.chrissearle.huts.security

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.util.Base64

private const val CLIENT_ID = "hytter"

/** A JWT with no signature - decodeAccessTokenClaims never verifies one. */
private fun accessToken(
    subject: String = "sub-123",
    name: String = "Chris Searle",
    roles: List<String> = listOf("user"),
): String {
    val payload =
        """
        {"sub":"$subject","name":"$name","resource_access":{"$CLIENT_ID":{"roles":${roles.map { "\"$it\"" }}}}}
        """.trimIndent()
    val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
    return "header.$encoded.signature"
}

private fun tokenJson(
    token: String = accessToken(),
    expiresIn: Long = 300,
    refreshExpiresIn: Long? = 1800,
    refreshToken: String = "new-refresh-token",
) = buildString {
    append("""{"access_token":"$token","expires_in":$expiresIn,"refresh_token":"$refreshToken"""")
    if (refreshExpiresIn != null) append(""","refresh_expires_in":$refreshExpiresIn""")
    append("}")
}

private fun session(
    expiresAt: Long,
    refreshExpiresAt: Long,
    refreshToken: String = "old-refresh-token",
) = UserSession(
    subject = "sub-123",
    name = "Chris Searle",
    roles = setOf("user"),
    expiresAt = expiresAt,
    refreshToken = refreshToken,
    refreshExpiresAt = refreshExpiresAt,
)

private fun refresher(
    engine: MockEngine,
    now: Long,
) = TokenRefresher(
    issuer = "https://auth.example.com/realms/hytter",
    clientId = CLIENT_ID,
    clientSecret = "secret",
    httpClient =
        HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        },
    now = { now },
)

private fun jsonEngine(body: String) =
    MockEngine {
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
        )
    }

class TokenRefreshTest :
    FunSpec({
        test("a session inside the access token's lifetime is used as-is") {
            val engine = MockEngine { error("Keycloak must not be called") }

            val outcome =
                refresher(engine, now = 1000).ensureFresh(
                    session(expiresAt = 2000, refreshExpiresAt = 5000),
                )

            outcome.shouldBeInstanceOf<SessionOutcome.Valid>()
        }

        test("a session past the refresh token's expiry is over, and Keycloak is not called") {
            val engine = MockEngine { error("Keycloak must not be called") }

            val outcome =
                refresher(engine, now = 6000).ensureFresh(
                    session(expiresAt = 2000, refreshExpiresAt = 5000),
                )

            outcome shouldBe SessionOutcome.Expired
        }

        test("an expired refresh token ends the session even if the access token looks fresh") {
            val engine = MockEngine { error("Keycloak must not be called") }

            // Nonsensical but defensive: the refresh ceiling wins regardless.
            val outcome =
                refresher(engine, now = 6000).ensureFresh(
                    session(expiresAt = 9000, refreshExpiresAt = 5000),
                )

            outcome shouldBe SessionOutcome.Expired
        }

        test("an access token inside the skew window triggers a refresh") {
            val engine = jsonEngine(tokenJson())

            val outcome =
                refresher(engine, now = 1990).ensureFresh(
                    session(expiresAt = 2000, refreshExpiresAt = 5000),
                )

            outcome.shouldBeInstanceOf<SessionOutcome.Refreshed>()
        }

        test("refreshing re-reads name and roles from the new access token") {
            val engine =
                jsonEngine(
                    tokenJson(token = accessToken(name = "Kari Nordmann", roles = listOf("user", "admin"))),
                )

            val outcome =
                refresher(engine, now = 3000).ensureFresh(
                    session(expiresAt = 2000, refreshExpiresAt = 5000),
                )

            val refreshed = outcome.shouldBeInstanceOf<SessionOutcome.Refreshed>().session
            refreshed.name shouldBe "Kari Nordmann"
            refreshed.roles shouldBe setOf("user", "admin")
        }

        test("refreshing takes the new expiry ceiling from Keycloak, not from the old session") {
            val engine = jsonEngine(tokenJson(expiresIn = 300, refreshExpiresIn = 600))

            val outcome =
                refresher(engine, now = 3000).ensureFresh(
                    session(expiresAt = 2000, refreshExpiresAt = 5000),
                )

            val refreshed = outcome.shouldBeInstanceOf<SessionOutcome.Refreshed>().session
            refreshed.expiresAt shouldBe 3300
            refreshed.refreshExpiresAt shouldBe 3600
        }

        test("a rotated refresh token replaces the stored one") {
            val engine = jsonEngine(tokenJson(refreshToken = "rotated"))

            val outcome =
                refresher(engine, now = 3000).ensureFresh(
                    session(expiresAt = 2000, refreshExpiresAt = 5000),
                )

            outcome.shouldBeInstanceOf<SessionOutcome.Refreshed>().session.refreshToken shouldBe "rotated"
        }

        test("a refresh rejected by Keycloak ends the session") {
            val engine = MockEngine { respondError(HttpStatusCode.BadRequest) }

            val outcome =
                refresher(engine, now = 3000).ensureFresh(
                    session(expiresAt = 2000, refreshExpiresAt = 5000),
                )

            outcome shouldBe SessionOutcome.Expired
        }

        test("an unreachable Keycloak ends the session rather than leaving it authenticated") {
            val engine = MockEngine { throw java.io.IOException("connection refused") }

            val outcome =
                refresher(engine, now = 3000).ensureFresh(
                    session(expiresAt = 2000, refreshExpiresAt = 5000),
                )

            outcome shouldBe SessionOutcome.Expired
        }

        test("the refresh request sends the stored refresh token as a refresh_token grant") {
            var body = ""
            val engine =
                MockEngine { request ->
                    body = String((request.body as OutgoingContent.ByteArrayContent).bytes())
                    respond(
                        content = tokenJson(),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                }

            refresher(engine, now = 3000).ensureFresh(
                session(expiresAt = 2000, refreshExpiresAt = 5000, refreshToken = "stored-token"),
            )

            body.contains("grant_type=refresh_token") shouldBe true
            body.contains("refresh_token=stored-token") shouldBe true
        }

        test("a missing refresh_expires_in falls back to a short bound, never an unbounded login") {
            val engine = jsonEngine(tokenJson(refreshExpiresIn = null))

            val outcome =
                refresher(engine, now = 3000).ensureFresh(
                    session(expiresAt = 2000, refreshExpiresAt = 5000),
                )

            val refreshed = outcome.shouldBeInstanceOf<SessionOutcome.Refreshed>().session
            refreshed.refreshExpiresAt shouldBe 4800
        }
    })
