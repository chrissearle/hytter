package net.chrissearle.huts.routes

import arrow.core.raise.either
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import net.chrissearle.huts.api.GroupNotAllowed
import net.chrissearle.huts.api.InvalidDateRange
import net.chrissearle.huts.api.InvalidNumberOfPeople
import net.chrissearle.huts.api.NameRequired
import net.chrissearle.huts.domain.BookingInput
import net.chrissearle.huts.domain.BookingNameType
import net.chrissearle.huts.domain.Hut
import net.chrissearle.huts.security.GROUP_ROLES
import net.chrissearle.huts.security.HytterPrincipal

private fun validInput(nameType: BookingNameType = BookingNameType.OPPHAVET) =
    BookingInput(
        nameType = nameType,
        name = null,
        numberOfPeople = 2,
        hut = Hut.HULDREBAKKEN,
        arrivalDate = LocalDate(2026, 6, 1),
        departureDate = LocalDate(2026, 6, 5),
    )

/** A logged-in principal, optionally an admin and/or a member of one fixed group. */
private fun principal(
    name: String,
    isAdmin: Boolean = false,
    group: BookingNameType? = null,
) = HytterPrincipal(
    subject = "sub-$name",
    name = name,
    roles =
        buildSet {
            add(if (isAdmin) "admin" else "user")
            group?.let { g -> add(GROUP_ROLES.entries.first { it.value == g }.key) }
        },
)

class BookingValidationTest :
    FunSpec({
        test("valid input passes") {
            val result = either { validInput().resolve(null) }

            result.isRight() shouldBe true
        }

        test("a fixed group stores its own display name, ignoring any supplied name") {
            val input = validInput(BookingNameType.SORKISRAMPEN).copy(name = "Something Else")

            val result = either { input.resolve(principal("Chris", group = BookingNameType.SORKISRAMPEN)) }

            result.getOrNull()?.name shouldBe "Sørkisrampen"
        }

        test("personal booking by a logged-in user takes the name from the principal") {
            val result = either { validInput(BookingNameType.PERSONAL).resolve(principal("Chris Searle")) }

            result.getOrNull()?.name shouldBe "Chris Searle"
        }

        test("personal booking by an anonymous visitor takes the supplied name") {
            val input = validInput(BookingNameType.PERSONAL).copy(name = "Kari Nordmann")

            val result = either { input.resolve(null) }

            result.getOrNull()?.name shouldBe "Kari Nordmann"
        }

        test("an admin may record a personal booking under someone else's name") {
            val input = validInput(BookingNameType.PERSONAL).copy(name = "Mormor")

            val result = either { input.resolve(principal("Chris Searle", isAdmin = true)) }

            result.getOrNull()?.name shouldBe "Mormor"
        }

        test("an admin with no name supplied still books as themselves") {
            val result =
                either { validInput(BookingNameType.PERSONAL).resolve(principal("Chris", isAdmin = true)) }

            result.getOrNull()?.name shouldBe "Chris"
        }

        test("a regular user cannot register a personal booking under another name") {
            val input = validInput(BookingNameType.PERSONAL).copy(name = "Someone Else")

            val result = either { input.resolve(principal("Chris Searle")) }

            result.getOrNull()?.name shouldBe "Chris Searle"
        }

        test("an admin naming someone else on a fixed group is still ignored") {
            val input = validInput(BookingNameType.HA12).copy(name = "Not HA12")

            val result = either { input.resolve(principal("Chris", isAdmin = true)) }

            result.getOrNull()?.name shouldBe "HA12"
        }

        test("a user may book under their own group") {
            val result =
                either { validInput(BookingNameType.HA12).resolve(principal("Chris", group = BookingNameType.HA12)) }

            result.getOrNull()?.name shouldBe "HA12"
        }

        test("a user may not book under a group that is not theirs") {
            val chris = principal("Chris", group = BookingNameType.HA12)

            val result = either { validInput(BookingNameType.OPPHAVET).resolve(chris) }

            result shouldBe arrow.core.Either.Left(GroupNotAllowed)
        }

        test("a groupless user may not book under any fixed group") {
            val result = either { validInput(BookingNameType.OPPHAVET).resolve(principal("Chris")) }

            result shouldBe arrow.core.Either.Left(GroupNotAllowed)
        }

        test("an admin may book under any group") {
            val result =
                either { validInput(BookingNameType.OPPHAVET).resolve(principal("Chris", isAdmin = true)) }

            result.getOrNull()?.name shouldBe "Opphavet"
        }

        test("an anonymous visitor may book under any fixed group") {
            val result = either { validInput(BookingNameType.HA12).resolve(null) }

            result.getOrNull()?.name shouldBe "HA12"
        }

        test("personal booking by an anonymous visitor with no name is rejected") {
            val result = either { validInput(BookingNameType.PERSONAL).resolve(null) }

            result shouldBe arrow.core.Either.Left(NameRequired)
        }

        test("other with a blank name is rejected") {
            val input = validInput(BookingNameType.OTHER).copy(name = "  ")

            val result = either { input.resolve(null) }

            result shouldBe arrow.core.Either.Left(NameRequired)
        }

        test("a logged-in non-admin user may not book as 'other'") {
            val input = validInput(BookingNameType.OTHER).copy(name = "Naboene")

            val result = either { input.resolve(principal("Chris", group = BookingNameType.HA12)) }

            result shouldBe arrow.core.Either.Left(GroupNotAllowed)
        }

        test("other keeps the supplied name for an admin") {
            val input = validInput(BookingNameType.OTHER).copy(name = "Naboene")

            val result = either { input.resolve(principal("Chris", isAdmin = true)) }

            result.getOrNull()?.name shouldBe "Naboene"
        }

        test("supplied name is trimmed") {
            val input = validInput(BookingNameType.OTHER).copy(name = "  Naboene  ")

            val result = either { input.resolve(null) }

            result.getOrNull()?.name shouldBe "Naboene"
        }

        test("name over the max length is rejected") {
            val input = validInput(BookingNameType.OTHER).copy(name = "a".repeat(NAME_MAX_LENGTH + 1))

            val result = either { input.resolve(null) }

            result shouldBe arrow.core.Either.Left(NameRequired)
        }

        test("name of exactly the max length is accepted") {
            val input = validInput(BookingNameType.OTHER).copy(name = "a".repeat(NAME_MAX_LENGTH))

            val result = either { input.resolve(null) }

            result.isRight() shouldBe true
        }

        test("zero people is rejected") {
            val result = either { validInput().copy(numberOfPeople = 0).resolve(null) }

            result shouldBe arrow.core.Either.Left(InvalidNumberOfPeople)
        }

        test("negative people is rejected") {
            val result = either { validInput().copy(numberOfPeople = -1).resolve(null) }

            result shouldBe arrow.core.Either.Left(InvalidNumberOfPeople)
        }

        test("departure before arrival is rejected") {
            val input =
                validInput().copy(arrivalDate = LocalDate(2026, 6, 5), departureDate = LocalDate(2026, 6, 1))

            val result = either { input.resolve(null) }

            result shouldBe
                arrow.core.Either.Left(InvalidDateRange("'departureDate' must not be before 'arrivalDate'"))
        }

        test("same-day arrival and departure is accepted") {
            val input =
                validInput().copy(arrivalDate = LocalDate(2026, 6, 5), departureDate = LocalDate(2026, 6, 5))

            val result = either { input.resolve(null) }

            result.isRight() shouldBe true
        }
    })
