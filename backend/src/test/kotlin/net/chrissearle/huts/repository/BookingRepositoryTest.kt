package net.chrissearle.huts.repository

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import net.chrissearle.huts.domain.BookingInput
import net.chrissearle.huts.domain.BookingStatus
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

private fun bookingInput(
    name: String = "Opphavet",
    hutId: Int = 1,
    arrivalDate: LocalDate = LocalDate(2026, 6, 1),
    departureDate: LocalDate = LocalDate(2026, 6, 5),
) = BookingInput(
    name = name,
    numberOfPeople = 2,
    hutId = hutId,
    arrivalDate = arrivalDate,
    departureDate = departureDate,
)

class BookingRepositoryTest :
    FunSpec({
        val container = PostgreSQLContainer<Nothing>(DockerImageName.parse("postgres:18-alpine"))

        lateinit var dataSource: DataSource
        lateinit var repository: BookingRepository

        beforeSpec {
            container.start()
            dataSource =
                org.postgresql.ds.PGSimpleDataSource().apply {
                    setUrl(container.jdbcUrl)
                    user = container.username
                    password = container.password
                }
            Flyway
                .configure()
                .dataSource(container.jdbcUrl, container.username, container.password)
                .load()
                .migrate()
            repository = BookingRepository(dataSource)
        }

        afterSpec {
            container.stop()
        }

        beforeEach {
            dataSource.connection.use { it.prepareStatement("DELETE FROM bookings").execute() }
        }

        test("insert then findById round-trips the booking") {
            val id = repository.insert(bookingInput(), createdBy = "Chris")

            val booking = repository.findById(id)

            booking shouldBe
                booking?.copy(
                    id = id,
                    name = "Opphavet",
                    numberOfPeople = 2,
                    hutId = 1,
                    arrivalDate = LocalDate(2026, 6, 1),
                    departureDate = LocalDate(2026, 6, 5),
                    status = BookingStatus.OPEN,
                    createdBy = "Chris",
                )
        }

        test("findById returns null for an unknown id") {
            repository.findById(-1) shouldBe null
        }

        test("insert with no logged-in creator stores a null created_by") {
            val id = repository.insert(bookingInput(), createdBy = null)

            repository.findById(id)?.createdBy shouldBe null
        }

        test("findInRange only returns bookings overlapping the window") {
            repository.insert(
                bookingInput(
                    name = "inside",
                    arrivalDate = LocalDate(2026, 6, 10),
                    departureDate = LocalDate(2026, 6, 15),
                ),
                createdBy = null,
            )
            repository.insert(
                bookingInput(
                    name = "before",
                    arrivalDate = LocalDate(2026, 1, 1),
                    departureDate = LocalDate(2026, 1, 5),
                ),
                createdBy = null,
            )
            repository.insert(
                bookingInput(
                    name = "overlaps-start",
                    arrivalDate = LocalDate(2026, 6, 5),
                    departureDate = LocalDate(2026, 6, 12),
                ),
                createdBy = null,
            )

            val results = repository.findInRange(LocalDate(2026, 6, 10), LocalDate(2026, 6, 20))

            results.map { it.name }.toSet() shouldBe setOf("inside", "overlaps-start")
        }

        test("update reverts an approved booking to OPEN") {
            val id = repository.insert(bookingInput(), createdBy = null)
            repository.approve(id)
            repository.findById(id)?.status shouldBe BookingStatus.APPROVED

            repository.update(id, bookingInput(name = "Updated"))

            val updated = repository.findById(id)
            updated?.name shouldBe "Updated"
            updated?.status shouldBe BookingStatus.OPEN
        }

        test("approve sets status to APPROVED") {
            val id = repository.insert(bookingInput(), createdBy = null)

            repository.approve(id)

            repository.findById(id)?.status shouldBe BookingStatus.APPROVED
        }

        test("delete removes the booking") {
            val id = repository.insert(bookingInput(), createdBy = null)

            val deletedRows = repository.delete(id)

            deletedRows shouldBe 1
            repository.findById(id) shouldBe null
        }

        test("delete of an unknown id affects zero rows") {
            repository.delete(-1) shouldBe 0
        }

        test("findInRange returns an empty list when nothing overlaps") {
            repository.findInRange(LocalDate(2099, 1, 1), LocalDate(2099, 1, 5)) shouldHaveSize 0
        }
    })
