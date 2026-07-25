package net.chrissearle.huts.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import net.chrissearle.huts.domain.BookingNameType
import net.chrissearle.huts.domain.Hut
import net.chrissearle.huts.domain.Reference
import net.chrissearle.huts.testHytterApplication

class ReferenceRoutesTest :
    FunSpec({
        test("GET /api/reference is available without a principal") {
            testApplication {
                application { testHytterApplication { routing { referenceRoutes() } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                client.get("/api/reference").status shouldBe HttpStatusCode.OK
            }
        }

        test("GET /api/reference returns every hut with its Bokmål label") {
            testApplication {
                application { testHytterApplication { routing { referenceRoutes() } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val reference = client.get("/api/reference").body<Reference>()

                reference.huts.map { it.value } shouldBe Hut.entries
                reference.huts.single { it.value == Hut.TENT_HAMMOCK }.displayName shouldBe "Telt/hengekøye"
            }
        }

        test("GET /api/reference marks the free-text name types") {
            testApplication {
                application { testHytterApplication { routing { referenceRoutes() } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val reference = client.get("/api/reference").body<Reference>()

                reference.nameTypes.map { it.value } shouldBe BookingNameType.entries
                reference.nameTypes.filter { it.isFreeText }.map { it.value } shouldBe
                    listOf(BookingNameType.PERSONAL, BookingNameType.OTHER)
            }
        }
    })
