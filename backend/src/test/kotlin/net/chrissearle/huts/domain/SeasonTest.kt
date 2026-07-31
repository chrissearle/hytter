package net.chrissearle.huts.domain

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class SeasonTest :
    FunSpec({
        test("seasonStart resolves the quarter a date falls in") {
            seasonStart(LocalDate(2026, 7, 15)) shouldBe LocalDate(2026, 6, 1)
            seasonStart(LocalDate(2026, 10, 3)) shouldBe LocalDate(2026, 9, 1)
            seasonStart(LocalDate(2027, 4, 20)) shouldBe LocalDate(2027, 3, 1)
        }

        test("seasonStart puts December in the winter that ends the following February") {
            seasonStart(LocalDate(2026, 12, 1)) shouldBe LocalDate(2026, 12, 1)
            seasonStart(LocalDate(2026, 12, 31)) shouldBe LocalDate(2026, 12, 1)
            seasonStart(LocalDate(2027, 1, 15)) shouldBe LocalDate(2026, 12, 1)
            seasonStart(LocalDate(2027, 2, 28)) shouldBe LocalDate(2026, 12, 1)
        }

        test("seasonStart moves to spring on 1 March") {
            seasonStart(LocalDate(2027, 3, 1)) shouldBe LocalDate(2027, 3, 1)
        }

        test("seasonEnd closes each quarter") {
            seasonEnd(LocalDate(2026, 6, 1)) shouldBe LocalDate(2026, 8, 31)
            seasonEnd(LocalDate(2026, 9, 1)) shouldBe LocalDate(2026, 11, 30)
            seasonEnd(LocalDate(2027, 3, 1)) shouldBe LocalDate(2027, 5, 31)
        }

        test("seasonEnd stays leap-safe in February") {
            seasonEnd(LocalDate(2026, 12, 1)) shouldBe LocalDate(2027, 2, 28)
            seasonEnd(LocalDate(2027, 12, 1)) shouldBe LocalDate(2028, 2, 29)
        }

        test("every day of a year maps into the season it belongs to") {
            var date = LocalDate(2027, 1, 1)
            while (date < LocalDate(2028, 1, 1)) {
                val start = seasonStart(date)
                val end = seasonEnd(start)
                withClue("$date should fall within $start..$end") {
                    (date >= start && date <= end) shouldBe true
                }
                date = date.plus(1, DateTimeUnit.DAY)
            }
        }
    })
