package net.chrissearle.huts.routes

import arrow.core.raise.either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import net.chrissearle.huts.api.InvalidDateRange
import net.chrissearle.huts.api.InvalidNumberOfPeople
import net.chrissearle.huts.api.NameRequired
import net.chrissearle.huts.domain.BookingInput
import net.chrissearle.huts.domain.BookingNameType
import net.chrissearle.huts.domain.Hut

private fun validInput(nameType: BookingNameType = BookingNameType.OPPHAVET) =
    BookingInput(
        nameType = nameType,
        name = null,
        numberOfPeople = 2,
        hut = Hut.HULDREBAKKEN,
        arrivalDate = LocalDate(2026, 6, 1),
        departureDate = LocalDate(2026, 6, 5),
    )

class BookingValidationTest :
    FunSpec({
        test("valid input passes") {
            val result = either { validInput().resolve(principalName = null) }

            result.isRight() shouldBe true
        }

        test("a fixed group stores its own display name, ignoring any supplied name") {
            val input = validInput(BookingNameType.SORKISRAMPEN).copy(name = "Something Else")

            val result = either { input.resolve(principalName = "Chris") }

            result.getOrNull()?.name shouldBe "Sørkisrampen"
        }

        test("personal booking by a logged-in user takes the name from the principal") {
            val result = either { validInput(BookingNameType.PERSONAL).resolve(principalName = "Chris Searle") }

            result.getOrNull()?.name shouldBe "Chris Searle"
        }

        test("personal booking by an anonymous visitor takes the supplied name") {
            val input = validInput(BookingNameType.PERSONAL).copy(name = "Kari Nordmann")

            val result = either { input.resolve(principalName = null) }

            result.getOrNull()?.name shouldBe "Kari Nordmann"
        }

        test("personal booking by an anonymous visitor with no name is rejected") {
            val result = either { validInput(BookingNameType.PERSONAL).resolve(principalName = null) }

            result shouldBe arrow.core.Either.Left(NameRequired)
        }

        test("other with a blank name is rejected") {
            val input = validInput(BookingNameType.OTHER).copy(name = "  ")

            val result = either { input.resolve(principalName = "Chris") }

            result shouldBe arrow.core.Either.Left(NameRequired)
        }

        test("other keeps the supplied name even when logged in") {
            val input = validInput(BookingNameType.OTHER).copy(name = "Naboene")

            val result = either { input.resolve(principalName = "Chris") }

            result.getOrNull()?.name shouldBe "Naboene"
        }

        test("supplied name is trimmed") {
            val input = validInput(BookingNameType.OTHER).copy(name = "  Naboene  ")

            val result = either { input.resolve(principalName = null) }

            result.getOrNull()?.name shouldBe "Naboene"
        }

        test("name over the max length is rejected") {
            val input = validInput(BookingNameType.OTHER).copy(name = "a".repeat(NAME_MAX_LENGTH + 1))

            val result = either { input.resolve(principalName = null) }

            result shouldBe arrow.core.Either.Left(NameRequired)
        }

        test("name of exactly the max length is accepted") {
            val input = validInput(BookingNameType.OTHER).copy(name = "a".repeat(NAME_MAX_LENGTH))

            val result = either { input.resolve(principalName = null) }

            result.isRight() shouldBe true
        }

        test("zero people is rejected") {
            val result = either { validInput().copy(numberOfPeople = 0).resolve(principalName = null) }

            result shouldBe arrow.core.Either.Left(InvalidNumberOfPeople)
        }

        test("negative people is rejected") {
            val result = either { validInput().copy(numberOfPeople = -1).resolve(principalName = null) }

            result shouldBe arrow.core.Either.Left(InvalidNumberOfPeople)
        }

        test("departure before arrival is rejected") {
            val input =
                validInput().copy(arrivalDate = LocalDate(2026, 6, 5), departureDate = LocalDate(2026, 6, 1))

            val result = either { input.resolve(principalName = null) }

            result shouldBe
                arrow.core.Either.Left(InvalidDateRange("'departureDate' must not be before 'arrivalDate'"))
        }

        test("same-day arrival and departure is accepted") {
            val input =
                validInput().copy(arrivalDate = LocalDate(2026, 6, 5), departureDate = LocalDate(2026, 6, 5))

            val result = either { input.resolve(principalName = null) }

            result.isRight() shouldBe true
        }
    })
