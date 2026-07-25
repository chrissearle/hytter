package net.chrissearle.huts.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode

class ApiErrorTest :
    FunSpec({
        test("messageMap for a plain ApiError only contains 'error'") {
            BookingNotFound(id = 5).messageMap().keys shouldBe setOf("error")
        }

        test("messageMap for an UpstreamError includes both 'upstream' and 'error'") {
            val upstream = ErrorResponse(status = HttpStatusCode.BadGateway, message = "boom")
            val error = DatabaseCallFailed(upstream)

            val map = error.messageMap()

            map.keys shouldBe setOf("upstream", "error")
            map["upstream"] shouldBe upstream
            map["error"] shouldBe error.response
        }

        test("status() returns the response's status code") {
            AdminRequired.status() shouldBe HttpStatusCode.Forbidden
        }

        test("RequiredField subtypes report their field name in the message") {
            NameRequired.response.message shouldBe "name required"
        }
    })
