package net.chrissearle.huts.api

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable

@Serializable
private data class Payload(
    val value: String,
)

class StatusPagesTest :
    FunSpec({
        test("a malformed JSON body is a 400 in the standard error envelope") {
            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    configureStatusPages()
                    routing { post("/echo") { call.receive<Payload>() } }
                }

                val response =
                    client.post("/echo") {
                        contentType(ContentType.Application.Json)
                        setBody("{ this is not json")
                    }

                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldContain "\"error\""
            }
        }

        test("an unhandled exception is a 500 in the standard error envelope") {
            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    configureStatusPages()
                    routing { get("/boom") { error("something went badly wrong") } }
                }

                val response = client.get("/boom")

                response.status shouldBe HttpStatusCode.InternalServerError
                response.bodyAsText() shouldContain "Internal server error"
            }
        }

        test("an unhandled exception does not leak its message to the caller") {
            testApplication {
                application {
                    install(ContentNegotiation) { json() }
                    configureStatusPages()
                    routing { get("/boom") { error("jdbc://user:password@host/db is unreachable") } }
                }

                val body = client.get("/boom").bodyAsText()

                body.contains("password") shouldBe false
            }
        }
    })
