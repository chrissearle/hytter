package net.chrissearle.huts.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.datetime.LocalDate
import net.chrissearle.huts.TEST_PRINCIPAL_HEADER
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

class BookingAdminRoutesTest :
    FunSpec({
        test("approve without a principal is unauthorized") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application {
                    testHytterApplication {
                        routing {
                            authenticate(
                                AUTH_PROVIDER_NAME,
                                optional = true,
                            ) { bookingAdminRoutes(repository) }
                        }
                    }
                }
                val client = createClient { install(ContentNegotiation) { json() } }

                client.post("/api/bookings/1/approve").status shouldBe HttpStatusCode.Unauthorized
            }
        }

        test("approve as a non-admin user is forbidden") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application {
                    testHytterApplication {
                        routing {
                            authenticate(
                                AUTH_PROVIDER_NAME,
                                optional = true,
                            ) { bookingAdminRoutes(repository) }
                        }
                    }
                }
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

                application {
                    testHytterApplication {
                        routing {
                            authenticate(
                                AUTH_PROVIDER_NAME,
                                optional = true,
                            ) { bookingAdminRoutes(repository) }
                        }
                    }
                }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.post("/api/bookings/1/approve") {
                        header(TEST_PRINCIPAL_HEADER, "admin")
                    }

                response.status shouldBe HttpStatusCode.OK
            }
        }

        test("delete as a non-admin user is forbidden") {
            testApplication {
                val repository = mockk<BookingRepository>()

                application {
                    testHytterApplication {
                        routing {
                            authenticate(
                                AUTH_PROVIDER_NAME,
                                optional = true,
                            ) { bookingAdminRoutes(repository) }
                        }
                    }
                }
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

                application {
                    testHytterApplication {
                        routing {
                            authenticate(
                                AUTH_PROVIDER_NAME,
                                optional = true,
                            ) { bookingAdminRoutes(repository) }
                        }
                    }
                }
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

                application {
                    testHytterApplication {
                        routing {
                            authenticate(
                                AUTH_PROVIDER_NAME,
                                optional = true,
                            ) { bookingAdminRoutes(repository) }
                        }
                    }
                }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response =
                    client.delete("/api/bookings/1") {
                        header(TEST_PRINCIPAL_HEADER, "admin")
                    }

                response.status shouldBe HttpStatusCode.OK
            }
        }
    })
