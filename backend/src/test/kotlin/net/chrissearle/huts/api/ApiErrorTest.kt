package net.chrissearle.huts.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode

class ApiErrorTest :
    FunSpec({
        test("messageMap for a plain ApiError only contains 'error'") {
            BookingNotFound(id = 5).messageMap().keys shouldBe setOf("error")
        }

        test("messageMap for an UpstreamError keeps the upstream detail server-side") {
            val upstream =
                ErrorResponse(
                    status = HttpStatusCode.BadGateway,
                    message = """ERROR: insert violates foreign key constraint "bookings_hut_fkey"""",
                )
            val error = DatabaseCallFailed(upstream)

            val map = error.messageMap()

            map.keys shouldBe setOf("error")
            map["error"] shouldBe error.response
        }

        test("an UpstreamError's client-facing message names no internals") {
            val upstream = ErrorResponse(status = HttpStatusCode.BadGateway, message = "relation does not exist")

            DatabaseCallFailed(upstream).response.message shouldBe "call to Database failed"
        }

        test("status() returns the response's status code") {
            AdminRequired.status() shouldBe HttpStatusCode.Forbidden
        }

        test("RequiredField subtypes report their field name in the message") {
            NameRequired.response.message shouldBe "name required"
        }
    })
