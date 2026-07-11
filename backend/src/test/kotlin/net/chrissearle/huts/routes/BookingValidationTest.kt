package net.chrissearle.huts.routes

import arrow.core.raise.either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import net.chrissearle.huts.api.HutRequired
import net.chrissearle.huts.api.InvalidDateRange
import net.chrissearle.huts.api.InvalidNumberOfPeople
import net.chrissearle.huts.api.NameRequired
import net.chrissearle.huts.domain.BookingInput

private fun validInput() =
    BookingInput(
        name = "Opphavet",
        numberOfPeople = 2,
        hutId = 1,
        arrivalDate = LocalDate(2026, 6, 1),
        departureDate = LocalDate(2026, 6, 5),
    )

class BookingValidationTest :
    FunSpec({
        test("valid input passes") {
            val result = either { validateBookingInput(validInput()) }

            result.isRight() shouldBe true
        }

        test("blank name is rejected") {
            val result = either { validateBookingInput(validInput().copy(name = "  ")) }

            result shouldBe arrow.core.Either.Left(NameRequired)
        }

        test("name over 100 chars is rejected") {
            val result = either { validateBookingInput(validInput().copy(name = "a".repeat(101))) }

            result shouldBe arrow.core.Either.Left(NameRequired)
        }

        test("name of exactly 100 chars is accepted") {
            val result = either { validateBookingInput(validInput().copy(name = "a".repeat(100))) }

            result.isRight() shouldBe true
        }

        test("zero people is rejected") {
            val result = either { validateBookingInput(validInput().copy(numberOfPeople = 0)) }

            result shouldBe arrow.core.Either.Left(InvalidNumberOfPeople)
        }

        test("negative people is rejected") {
            val result = either { validateBookingInput(validInput().copy(numberOfPeople = -1)) }

            result shouldBe arrow.core.Either.Left(InvalidNumberOfPeople)
        }

        test("hutId of zero is rejected") {
            val result = either { validateBookingInput(validInput().copy(hutId = 0)) }

            result shouldBe arrow.core.Either.Left(HutRequired)
        }

        test("departure before arrival is rejected") {
            val input = validInput().copy(arrivalDate = LocalDate(2026, 6, 5), departureDate = LocalDate(2026, 6, 1))

            val result = either { validateBookingInput(input) }

            result shouldBe
                arrow.core.Either.Left(InvalidDateRange("'departureDate' must not be before 'arrivalDate'"))
        }

        test("same-day arrival and departure is accepted") {
            val input = validInput().copy(arrivalDate = LocalDate(2026, 6, 5), departureDate = LocalDate(2026, 6, 5))

            val result = either { validateBookingInput(input) }

            result.isRight() shouldBe true
        }
    })
