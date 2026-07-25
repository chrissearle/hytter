package net.chrissearle.huts.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.datetime.LocalDate
import net.chrissearle.huts.TEST_PRINCIPAL_HEADER
import net.chrissearle.huts.domain.AdminNotesInput
import net.chrissearle.huts.domain.BookingNameType
import net.chrissearle.huts.domain.BookingRecord
import net.chrissearle.huts.domain.BookingStatus
import net.chrissearle.huts.domain.Hut
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.security.AUTH_PROVIDER_NAME
import net.chrissearle.huts.testHytterApplication

private fun sampleRecord() =
    BookingRecord(
        id = 1,
        nameType = BookingNameType.OPPHAVET,
        name = "Opphavet",
        numberOfPeople = 2,
        hut = Hut.HULDREBAKKEN,
        arrivalDate = LocalDate(2026, 6, 1),
        departureDate = LocalDate(2026, 6, 5),
        adminNotes = null,
        status = BookingStatus.APPROVED,
        createdBy = "Some User",
        createdBySubject = "user-subject",
    )

private fun Route.adminRouting(repository: BookingRepository) {
    authenticate(AUTH_PROVIDER_NAME, optional = true) { bookingAdminRoutes(repository) }
}

class BookingAdminRoutesTest :
    FunSpec({
        test("approve without a principal is unauthorized") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                client.post("/api/bookings/1/approve").status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("approve as a non-admin user is forbidden") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.post("/api/bookings/1/approve") {
                        header(TEST_PRINCIPAL_HEADER, "user")
                    }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        test("approve as an admin succeeds") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.approve(1) } returns 1
                coEvery { repository.findById(1) } returns sampleRecord()

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.post("/api/bookings/1/approve") {
                        header(TEST_PRINCIPAL_HEADER, "admin")
                    }

                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("setting admin notes without a principal is unauthorized") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.patch("/api/bookings/1/notes") {
                        contentType(ContentType.Application.Json)
                        setBody(AdminNotesInput("Husk å tømme utedoen"))
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("setting admin notes as a non-admin user is forbidden") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.patch("/api/bookings/1/notes") {
                        header(TEST_PRINCIPAL_HEADER, "user")
                        contentType(ContentType.Application.Json)
                        setBody(AdminNotesInput("Husk å tømme utedoen"))
                    }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        test("an admin can set a note") {
            testApplication {
                val repository = mockk<BookingRepository>()
                val stored = slot<String?>()
                coEvery { repository.updateAdminNotes(1, captureNullable(stored)) } returns 1
                coEvery { repository.findById(1) } returns sampleRecord()

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.patch("/api/bookings/1/notes") {
                        header(TEST_PRINCIPAL_HEADER, "admin")
                        contentType(ContentType.Application.Json)
                        setBody(AdminNotesInput("  Du må ha telt de første 2 dagene  "))
                    }

                response.status shouldBe HttpStatusCode.OK
                stored.captured shouldBe "Du må ha telt de første 2 dagene"
            }
        }

        test("a blank note clears it rather than storing whitespace") {
            testApplication {
                val repository = mockk<BookingRepository>()
                val stored = slot<String?>()
                coEvery { repository.updateAdminNotes(1, captureNullable(stored)) } returns 1
                coEvery { repository.findById(1) } returns sampleRecord()

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                client.patch("/api/bookings/1/notes") {
                    header(TEST_PRINCIPAL_HEADER, "admin")
                    contentType(ContentType.Application.Json)
                    setBody(AdminNotesInput("   "))
                }

                stored.captured shouldBe null
            }
        }

        test("an over-long note is rejected before reaching the repository") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.patch("/api/bookings/1/notes") {
                        header(TEST_PRINCIPAL_HEADER, "admin")
                        contentType(ContentType.Application.Json)
                        setBody(AdminNotesInput("a".repeat(ADMIN_NOTES_MAX_LENGTH + 1)))
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("setting notes on a non-existent booking returns 404") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.updateAdminNotes(99, any()) } returns 0
                coEvery { repository.findById(99) } returns null

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.patch("/api/bookings/99/notes") {
                        header(TEST_PRINCIPAL_HEADER, "admin")
                        contentType(ContentType.Application.Json)
                        setBody(AdminNotesInput("Nope"))
                    }

                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        test("delete as a non-admin user is forbidden") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.delete("/api/bookings/1") {
                        header(TEST_PRINCIPAL_HEADER, "user")
                    }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        test("delete of a non-existent booking returns 404") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.delete(1) } returns 0

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.delete("/api/bookings/1") {
                        header(TEST_PRINCIPAL_HEADER, "admin")
                    }

                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        test("delete as an admin succeeds") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.delete(1) } returns 1

                application { testHytterApplication { routing { adminRouting(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.delete("/api/bookings/1") {
                        header(TEST_PRINCIPAL_HEADER, "admin")
                    }

                response.status shouldBe HttpStatusCode.OK
            }
        }
    })
