package net.chrissearle.huts.security

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HytterPrincipalTest :
    FunSpec({
        test("isAdmin is true when admin role present") {
            HytterPrincipal(subject = "sub-1", name = "Chris", roles = setOf("admin", "user")).isAdmin shouldBe true
        }

        test("isAdmin is false when only user role present") {
            HytterPrincipal(subject = "sub-1", name = "Chris", roles = setOf("user")).isAdmin shouldBe false
        }

        test("isAdmin is false with no roles") {
            HytterPrincipal(subject = "sub-1", name = "Chris", roles = emptySet()).isAdmin shouldBe false
        }
    })
