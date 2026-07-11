package net.chrissearle.huts.security

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import java.util.Base64

private fun fakeJwt(payloadJson: String): String {
    val header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".toByteArray())
    val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.toByteArray())
    return "$header.$payload.signature"
}

class AccessTokenClaimsTest :
    FunSpec({
        test("decodes claims from an unpadded base64url payload") {
            val token = fakeJwt("""{"name":"Chris Searle"}""")

            val claims = decodeAccessTokenClaims(token)

            claims.displayName() shouldBe "Chris Searle"
        }

        test("displayName is null when name claim absent") {
            val claims = Json.parseToJsonElement("{}").let { it as kotlinx.serialization.json.JsonObject }

            claims.displayName() shouldBe null
        }

        test("clientRoles extracts roles for the matching client") {
            val token =
                fakeJwt(
                    """
                    {"resource_access":{"hytter":{"roles":["admin","user"]},"other-client":{"roles":["ignored"]}}}
                    """.trimIndent(),
                )

            val claims = decodeAccessTokenClaims(token)

            claims.clientRoles("hytter") shouldBe setOf("admin", "user")
        }

        test("clientRoles is empty when client is not present") {
            val token = fakeJwt("""{"resource_access":{"other-client":{"roles":["admin"]}}}""")

            val claims = decodeAccessTokenClaims(token)

            claims.clientRoles("hytter") shouldBe emptySet()
        }

        test("clientRoles is empty when resource_access is missing entirely") {
            val token = fakeJwt("""{"name":"Chris"}""")

            val claims = decodeAccessTokenClaims(token)

            claims.clientRoles("hytter") shouldBe emptySet()
        }
    })
