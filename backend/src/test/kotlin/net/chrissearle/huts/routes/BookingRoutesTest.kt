package net.chrissearle.huts.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.datetime.LocalDate
import net.chrissearle.huts.TEST_PRINCIPAL_HEADER
import net.chrissearle.huts.domain.Booking
import net.chrissearle.huts.domain.BookingData
import net.chrissearle.huts.domain.BookingInput
import net.chrissearle.huts.domain.BookingNameType
import net.chrissearle.huts.domain.BookingRecord
import net.chrissearle.huts.domain.BookingStatus
import net.chrissearle.huts.domain.Hut
import net.chrissearle.huts.repository.BookingRepository
import net.chrissearle.huts.testHytterApplication

private fun sampleRecord(
    id: Int = 1,
    createdBy: String? = "Some User",
    createdBySubject: String? = "user-subject",
    status: BookingStatus = BookingStatus.OPEN,
) = BookingRecord(
    id = id,
    nameType = BookingNameType.OPPHAVET,
    name = "Opphavet",
    numberOfPeople = 2,
    hut = Hut.HULDREBAKKEN,
    arrivalDate = LocalDate(2026, 6, 1),
    departureDate = LocalDate(2026, 6, 5),
    adminNotes = null,
    status = status,
    createdBy = createdBy,
    createdBySubject = createdBySubject,
)

private fun sampleInput() =
    BookingInput(
        nameType = BookingNameType.OPPHAVET,
        name = null,
        numberOfPeople = 2,
        hut = Hut.HULDREBAKKEN,
        arrivalDate = LocalDate(2026, 6, 1),
        departureDate = LocalDate(2026, 6, 5),
    )

class BookingRoutesTest :
    FunSpec({
        test("GET /api/bookings/{id} returns 404 for a missing booking") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findById(99) } returns null

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/bookings/99") { header(TEST_PRINCIPAL_HEADER, "user") }

                response.status shouldBe HttpStatusCode.NotFound
            }
        }

        test("GET /api/bookings/{id} returns the booking when found") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findById(1) } returns sampleRecord()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/bookings/1") { header(TEST_PRINCIPAL_HEADER, "user") }

                response.status shouldBe HttpStatusCode.OK
                response.body<Booking>().name shouldBe "Opphavet"
            }
        }

        test("GET /api/bookings/{id} with a non-numeric id is a bad request, not a 404 or a 500") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.get("/api/bookings/not-a-number") { header(TEST_PRINCIPAL_HEADER, "user") }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/bookings/{id} is not available to anonymous visitors") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/bookings/1")

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("GET /api/bookings/{id} is forbidden for a realm account with no hytter role") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/bookings/1") { header(TEST_PRINCIPAL_HEADER, "norole") }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        test("PUT /api/bookings/{id} is forbidden for a realm account with no hytter role") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.put("/api/bookings/1") {
                        header(TEST_PRINCIPAL_HEADER, "norole")
                        contentType(ContentType.Application.Json)
                        setBody(sampleInput())
                    }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        test("a booking is never editable by an account without a hytter role") {
            testApplication {
                val repository = mockk<BookingRepository>()
                // Owns it by subject, but has no role granting access.
                coEvery { repository.findById(1) } returns
                    sampleRecord(createdBySubject = "stranger-subject")

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/bookings/1") { header(TEST_PRINCIPAL_HEADER, "norole") }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        test("GET /api/session reports access for a user with the role") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val session =
                    client.get("/api/session") { header(TEST_PRINCIPAL_HEADER, "user") }.body<SessionInfo>()

                session.authenticated shouldBe true
                session.hasAccess shouldBe true
                session.isAdmin shouldBe false
            }
        }

        test("GET /api/session reports authenticated but without access for a realm stranger") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val session =
                    client.get("/api/session") { header(TEST_PRINCIPAL_HEADER, "norole") }.body<SessionInfo>()

                session.authenticated shouldBe true
                session.hasAccess shouldBe false
                session.name shouldBe "Realm Stranger"
            }
        }

        test("GET /api/bookings stays public for the calendar") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findInRange(any(), any()) } returns emptyList()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                client.get("/api/bookings").status shouldBe HttpStatusCode.OK
            }
        }

        test("canEdit is true for the owner, matched on subject") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findById(1) } returns
                    sampleRecord(createdBy = "Renamed Since", createdBySubject = "user-subject")

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/bookings/1") { header(TEST_PRINCIPAL_HEADER, "user") }

                response.body<Booking>().canEdit shouldBe true
            }
        }

        test("canEdit is false for a different subject even when the display name matches") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findById(1) } returns
                    sampleRecord(createdBy = "Some User", createdBySubject = "someone-else")

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/bookings/1") { header(TEST_PRINCIPAL_HEADER, "user") }

                response.body<Booking>().canEdit shouldBe false
            }
        }

        test("canEdit falls back to the display name for rows predating the subject column") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findById(1) } returns
                    sampleRecord(createdBy = "Some User", createdBySubject = null)

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/bookings/1") { header(TEST_PRINCIPAL_HEADER, "user") }

                response.body<Booking>().canEdit shouldBe true
            }
        }

        test("canEdit is true for an admin on someone else's booking") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findById(1) } returns sampleRecord(createdBySubject = "someone-else")

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/bookings/1") { header(TEST_PRINCIPAL_HEADER, "admin") }

                response.body<Booking>().canEdit shouldBe true
            }
        }

        test("a booking never carries the owner's subject on the wire") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findById(1) } returns sampleRecord(createdBySubject = "secret-subject")

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val body = client.get("/api/bookings/1") { header(TEST_PRINCIPAL_HEADER, "user") }.bodyAsText()

                body.contains("secret-subject") shouldBe false
            }
        }

        test("POST /api/bookings rejects invalid input before hitting the repository") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.post("/api/bookings") {
                        contentType(ContentType.Application.Json)
                        setBody(sampleInput().copy(numberOfPeople = 0))
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("POST /api/bookings creates a booking and records the anonymous creator as null") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.insert(any(), null, null) } returns 7
                coEvery { repository.findById(7) } returns sampleRecord(id = 7, createdBy = null)

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.post("/api/bookings") {
                        contentType(ContentType.Application.Json)
                        setBody(sampleInput())
                    }

                response.status shouldBe HttpStatusCode.Created
            }
        }

        test("POST /api/bookings created by a logged-in user records their name as creator") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.insert(any(), "Some User", "user-subject") } returns 7
                coEvery { repository.findById(7) } returns sampleRecord(id = 7)

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.post("/api/bookings") {
                        header(TEST_PRINCIPAL_HEADER, "user")
                        contentType(ContentType.Application.Json)
                        setBody(sampleInput())
                    }

                response.status shouldBe HttpStatusCode.Created
            }
        }

        test("POST /api/bookings resolves a personal booking to the logged-in user's name") {
            testApplication {
                val repository = mockk<BookingRepository>()
                val stored = slot<BookingData>()
                coEvery { repository.insert(capture(stored), any(), any()) } returns 7
                coEvery { repository.findById(7) } returns sampleRecord(id = 7)

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                client.post("/api/bookings") {
                    header(TEST_PRINCIPAL_HEADER, "user")
                    contentType(ContentType.Application.Json)
                    setBody(sampleInput().copy(nameType = BookingNameType.PERSONAL, name = "Ignored"))
                }

                stored.captured.name shouldBe "Some User"
            }
        }

        test("POST /api/bookings ignores a client-supplied name for a fixed group") {
            testApplication {
                val repository = mockk<BookingRepository>()
                val stored = slot<BookingData>()
                coEvery { repository.insert(capture(stored), any(), any()) } returns 7
                coEvery { repository.findById(7) } returns sampleRecord(id = 7)

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                client.post("/api/bookings") {
                    contentType(ContentType.Application.Json)
                    setBody(sampleInput().copy(nameType = BookingNameType.HA12, name = "Not HA12"))
                }

                stored.captured.name shouldBe "HA12"
            }
        }

        test("POST /api/bookings is forbidden when a user books under a group that is not theirs") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                // The test "user" principal belongs to Opphavet, so HA12 is not theirs.
                val response =
                    client.post("/api/bookings") {
                        header(TEST_PRINCIPAL_HEADER, "user")
                        contentType(ContentType.Application.Json)
                        setBody(sampleInput().copy(nameType = BookingNameType.HA12))
                    }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        test("POST /api/bookings with an unknown hut is a bad request") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.post("/api/bookings") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {"nameType":"OPPHAVET","numberOfPeople":2,"hut":"CASTLE",
                             "arrivalDate":"2026-06-01","departureDate":"2026-06-05"}
                            """.trimIndent(),
                        )
                    }

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("PUT /api/bookings/{id} without a principal is rejected") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.put("/api/bookings/1") {
                        contentType(ContentType.Application.Json)
                        setBody(sampleInput())
                    }

                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("PUT /api/bookings/{id} by a non-owning, non-admin user is forbidden") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findById(1) } returns
                    sampleRecord(createdBy = "Someone Else", createdBySubject = "someone-else")

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.put("/api/bookings/1") {
                        header(TEST_PRINCIPAL_HEADER, "user")
                        contentType(ContentType.Application.Json)
                        setBody(sampleInput())
                    }

                response.status shouldBe HttpStatusCode.Forbidden
            }
        }

        test("PUT /api/bookings/{id} by the owning user succeeds") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findById(1) } returns sampleRecord(createdBy = "Some User")
                coEvery { repository.update(1, any(), any()) } returns 1

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.put("/api/bookings/1") {
                        header(TEST_PRINCIPAL_HEADER, "user")
                        contentType(ContentType.Application.Json)
                        setBody(sampleInput())
                    }

                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("PUT /api/bookings/{id} by the owner sends it back for re-approval") {
            testApplication {
                val repository = mockk<BookingRepository>()
                val keepStatus = slot<Boolean>()
                coEvery { repository.findById(1) } returns sampleRecord(createdBy = "Some User")
                coEvery { repository.update(1, any(), capture(keepStatus)) } returns 1

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                client.put("/api/bookings/1") {
                    header(TEST_PRINCIPAL_HEADER, "user")
                    contentType(ContentType.Application.Json)
                    setBody(sampleInput())
                }

                keepStatus.captured shouldBe false
            }
        }

        test("PUT /api/bookings/{id} by an admin does not un-approve the booking") {
            testApplication {
                val repository = mockk<BookingRepository>()
                val keepStatus = slot<Boolean>()
                coEvery { repository.findById(1) } returns sampleRecord(status = BookingStatus.APPROVED)
                coEvery { repository.update(1, any(), capture(keepStatus)) } returns 1

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                client.put("/api/bookings/1") {
                    header(TEST_PRINCIPAL_HEADER, "admin")
                    contentType(ContentType.Application.Json)
                    setBody(sampleInput())
                }

                keepStatus.captured shouldBe true
            }
        }

        test("PUT /api/bookings/{id} by an admin on someone else's booking succeeds") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findById(1) } returns sampleRecord(createdBy = "Someone Else")
                coEvery { repository.update(1, any(), any()) } returns 1

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.put("/api/bookings/1") {
                        header(TEST_PRINCIPAL_HEADER, "admin")
                        contentType(ContentType.Application.Json)
                        setBody(sampleInput())
                    }

                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("GET /api/bookings with 'to' before 'from' is a bad request") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/api/bookings?from=2026-06-10&to=2026-06-01")

                response.status shouldBe HttpStatusCode.BadRequest
            }
        }

        test("GET /api/bookings defaults to the current year's June-August season") {
            testApplication {
                val repository = mockk<BookingRepository>()
                coEvery { repository.findInRange(any(), any()) } returns emptyList()

                application { testHytterApplication { routing { bookingRoutes(repository) } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                client.get("/api/bookings").status shouldBe HttpStatusCode.OK
            }
        }
    })
