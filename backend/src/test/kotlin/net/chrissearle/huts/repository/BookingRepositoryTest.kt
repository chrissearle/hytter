package net.chrissearle.huts.repository

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import net.chrissearle.huts.domain.BookingData
import net.chrissearle.huts.domain.BookingNameType
import net.chrissearle.huts.domain.BookingStatus
import net.chrissearle.huts.domain.Hut
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

private fun bookingData(
    nameType: BookingNameType = BookingNameType.OPPHAVET,
    name: String = "Opphavet",
    hut: Hut = Hut.HULDREBAKKEN,
    arrivalDate: LocalDate = LocalDate(2026, 6, 1),
    departureDate: LocalDate = LocalDate(2026, 6, 5),
) = BookingData(
    nameType = nameType,
    name = name,
    numberOfPeople = 2,
    hut = hut,
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
            val id = repository.insert(bookingData(), createdBy = "Chris", createdBySubject = "sub-chris")

            val booking = repository.findById(id)

            booking?.id shouldBe id
            booking?.nameType shouldBe BookingNameType.OPPHAVET
            booking?.name shouldBe "Opphavet"
            booking?.numberOfPeople shouldBe 2
            booking?.hut shouldBe Hut.HULDREBAKKEN
            booking?.arrivalDate shouldBe LocalDate(2026, 6, 1)
            booking?.departureDate shouldBe LocalDate(2026, 6, 5)
            booking?.status shouldBe BookingStatus.OPEN
            booking?.createdBy shouldBe "Chris"
            booking?.createdBySubject shouldBe "sub-chris"
        }

        test("every hut value survives a round-trip through the check constraint") {
            Hut.entries.forEach { hut ->
                val id = repository.insert(bookingData(hut = hut), createdBy = null, createdBySubject = null)

                repository.findById(id)?.hut shouldBe hut
            }
        }

        test("every name type survives a round-trip through the check constraint") {
            BookingNameType.entries.forEach { nameType ->
                val id =
                    repository.insert(
                        bookingData(nameType = nameType, name = "Someone"),
                        createdBy = null,
                        createdBySubject = null,
                    )

                repository.findById(id)?.nameType shouldBe nameType
            }
        }

        test("findById returns null for an unknown id") {
            repository.findById(-1) shouldBe null
        }

        test("insert with no logged-in creator stores a null created_by") {
            val id = repository.insert(bookingData(), createdBy = null, createdBySubject = null)

            repository.findById(id)?.createdBy shouldBe null
        }

        test("findInRange only returns bookings overlapping the window") {
            repository.insert(
                bookingData(
                    name = "inside",
                    arrivalDate = LocalDate(2026, 6, 10),
                    departureDate = LocalDate(2026, 6, 15),
                ),
                createdBy = null,
                createdBySubject = null,
            )
            repository.insert(
                bookingData(
                    name = "before",
                    arrivalDate = LocalDate(2026, 1, 1),
                    departureDate = LocalDate(2026, 1, 5),
                ),
                createdBy = null,
                createdBySubject = null,
            )
            repository.insert(
                bookingData(
                    name = "overlaps-start",
                    arrivalDate = LocalDate(2026, 6, 5),
                    departureDate = LocalDate(2026, 6, 12),
                ),
                createdBy = null,
                createdBySubject = null,
            )

            val results = repository.findInRange(LocalDate(2026, 6, 10), LocalDate(2026, 6, 20))

            results.map { it.name }.toSet() shouldBe setOf("inside", "overlaps-start")
        }

        test("findInRange carries the hut through to the summary") {
            repository.insert(bookingData(hut = Hut.TENT_HAMMOCK), createdBy = null, createdBySubject = null)

            val results = repository.findInRange(LocalDate(2026, 6, 1), LocalDate(2026, 6, 30))

            results.map { it.hut } shouldBe listOf(Hut.TENT_HAMMOCK)
        }

        test("update reverts an approved booking to OPEN") {
            val id = repository.insert(bookingData(), createdBy = null, createdBySubject = null)
            repository.approve(id)
            repository.findById(id)?.status shouldBe BookingStatus.APPROVED

            repository.update(id, bookingData(name = "Updated"))

            val updated = repository.findById(id)
            updated?.name shouldBe "Updated"
            updated?.status shouldBe BookingStatus.OPEN
        }

        test("approve sets status to APPROVED") {
            val id = repository.insert(bookingData(), createdBy = null, createdBySubject = null)

            repository.approve(id)

            repository.findById(id)?.status shouldBe BookingStatus.APPROVED
        }

        test("delete removes the booking") {
            val id = repository.insert(bookingData(), createdBy = null, createdBySubject = null)

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
