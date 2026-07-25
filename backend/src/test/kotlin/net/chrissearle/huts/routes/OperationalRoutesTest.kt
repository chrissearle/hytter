package net.chrissearle.huts.routes

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import net.chrissearle.huts.config.DEVELOPMENT_VERSION
import net.chrissearle.huts.testHytterApplication
import java.sql.Connection
import java.sql.SQLException
import java.sql.Statement
import javax.sql.DataSource

private fun workingDataSource(): DataSource {
    val statement = mockk<Statement>(relaxed = true)
    every { statement.execute("SELECT 1") } returns true

    val connection = mockk<Connection>(relaxed = true)
    every { connection.createStatement() } returns statement

    return mockk<DataSource>().also { every { it.connection } returns connection }
}

private fun brokenDataSource(): DataSource =
    mockk<DataSource>().also {
        every { it.connection } throws SQLException("connection refused")
    }

class OperationalRoutesTest :
    FunSpec({
        test("GET /version reports the development fallback when no image tag is baked in") {
            testApplication {
                application { testHytterApplication { routing { versionRoute() } } }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/version")

                response.status shouldBe HttpStatusCode.OK
                response.body<VersionInfo>().version shouldBe DEVELOPMENT_VERSION
            }
        }

        test("GET /health is up without touching the database") {
            testApplication {
                // A data source that would blow up if used at all.
                application {
                    testHytterApplication { routing { healthRoutes(brokenDataSource()) } }
                }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/health")

                response.status shouldBe HttpStatusCode.OK
                response.body<HealthStatus>().status shouldBe "UP"
            }
        }

        test("GET /ready is up when the database answers") {
            testApplication {
                application {
                    testHytterApplication { routing { healthRoutes(workingDataSource()) } }
                }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/ready")

                response.status shouldBe HttpStatusCode.OK
                response.body<HealthStatus>().status shouldBe "UP"
            }
        }

        test("GET /ready is unavailable when the database cannot be reached") {
            testApplication {
                application {
                    testHytterApplication { routing { healthRoutes(brokenDataSource()) } }
                }
                val client = createClient { install(ContentNegotiation) { json() } }

                val response = client.get("/ready")

                response.status shouldBe HttpStatusCode.ServiceUnavailable
            }
        }

        test("GET /ready does not leak the database failure to the caller") {
            testApplication {
                application {
                    testHytterApplication { routing { healthRoutes(brokenDataSource()) } }
                }
                val client = createClient { install(ContentNegotiation) { json() } }

                val body = client.get("/ready").bodyAsText()

                body shouldContain "Database unavailable"
                body.contains("connection refused") shouldBe false
            }
        }
    })
