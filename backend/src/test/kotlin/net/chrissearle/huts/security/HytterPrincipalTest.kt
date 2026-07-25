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

        test("hasAccess is true with the user role") {
            HytterPrincipal(subject = "sub-1", name = "Chris", roles = setOf("user")).hasAccess shouldBe true
        }

        test("admin implies access even without the user role") {
            HytterPrincipal(subject = "sub-1", name = "Chris", roles = setOf("admin")).hasAccess shouldBe true
        }

        test("hasAccess is false with no roles") {
            HytterPrincipal(subject = "sub-1", name = "Chris", roles = emptySet()).hasAccess shouldBe false
        }

        test("realm roles from other apps do not grant access") {
            val principal =
                HytterPrincipal(
                    subject = "sub-1",
                    name = "Chris",
                    roles = setOf("monit-access", "metrics-access", "offline_access"),
                )

            principal.hasAccess shouldBe false
        }
    })
