package net.chrissearle.huts.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

private const val BASE64_BLOCK_SIZE = 4

/**
 * Keycloak access tokens are JWTs. We only ever see this token immediately after
 * exchanging it directly with Keycloak over TLS, so the claims are decoded without
 * re-verifying the signature.
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
