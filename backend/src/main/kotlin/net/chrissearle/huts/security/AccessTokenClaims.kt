package net.chrissearle.huts.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

private const val BASE64_BLOCK_SIZE = 4

/**
 * Keycloak access tokens are JWTs. The signature is deliberately not verified:
 * a token only ever reaches this function straight out of a TLS call the backend
 * made to Keycloak itself - the authorization-code exchange, or a refresh - so
 * its provenance is already established by the transport.
 *
 * This holds only as long as the backend stays a confidential client doing the
 * code exchange server-side (the BFF pattern). If tokens ever start arriving
 * from the browser as bearer credentials, this becomes a critical hole and full
 * JWKS signature, issuer and audience validation is required first.
 */
fun decodeAccessTokenClaims(accessToken: String): JsonObject {
    val payload = accessToken.split(".").getOrElse(1) { "" }
    val decoded = Base64.getUrlDecoder().decode(payload.padBase64())
    return Json.parseToJsonElement(decoded.decodeToString()).jsonObject
}

private fun String.padBase64(): String {
    val remainder = length % BASE64_BLOCK_SIZE
    return if (remainder == 0) this else this + "=".repeat(BASE64_BLOCK_SIZE - remainder)
}

fun JsonObject.clientRoles(clientId: String): Set<String> =
    this["resource_access"]
        ?.jsonObject
        ?.get(clientId)
        ?.jsonObject
        ?.get("roles")
        ?.jsonArray
        ?.map { it.jsonPrimitive.content }
        ?.toSet()
        .orEmpty()

fun JsonObject.displayName(): String? = this["name"]?.jsonPrimitive?.content

/** The `sub` claim: stable for a user even if they change their display name. */
fun JsonObject.subject(): String? = this["sub"]?.jsonPrimitive?.content
