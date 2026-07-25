package net.chrissearle.huts.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.Url

class AuthRoutesTest :
    FunSpec({
        test("logout url points at Keycloak's end-session endpoint") {
            val url = logoutUrl("https://auth.example.com/realms/hytter", "hytter", "https://hytter.example.com")

            Url(url).encodedPath shouldBe "/realms/hytter/protocol/openid-connect/logout"
        }

        test("logout url encodes the post-logout redirect rather than concatenating it raw") {
            val url = logoutUrl("https://auth.example.com/realms/hytter", "hytter", "https://hytter.example.com")

            url.contains("post_logout_redirect_uri=https%3A%2F%2Fhytter.example.com") shouldBe true
        }

        test("logout url survives a client id needing escaping") {
            val url = logoutUrl("https://auth.example.com/realms/hytter", "my client&x", "https://h.example.com")

            Url(url).parameters["client_id"] shouldBe "my client&x"
        }
    })
