package de.gleex.graphdb.playground.model

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.*
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.haveMessage
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filterNot
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class VersionTest : StringSpec() {
    init {
        "Valid versions" {
            table(
                Headers4("Version string", "expected major", "expected minor", "expected patch"),
                row("1.0.0", 1, 0, 0),
                row("1.0", 1, 0, 0),
                row("1", 1, 0, 0),
                row("1.000.00", 1, 0, 0),
                row("1.2.3", 1, 2, 3),
                row("123.456.0789", 123, 456, 789)
            ).forAll { version, expectedMajor, expectedMinor, expectedPatch ->
                val newVersion = Version(version)
                assertSoftly(newVersion) {
                    major shouldBe expectedMajor
                    minor shouldBe expectedMinor
                    patch shouldBe expectedPatch
                    suffix shouldBe null
                    isSnapshot shouldBe false
                }
            }
        }
        "Valid snapshot versions" {
            table(
                Headers5("Version string", "expected major", "expected minor", "expected patch", "expected suffix"),
                row("1.0.0-SNAPSHOT", 1, 0, 0, "-SNAPSHOT"),
                row("1.0-SNAPSHOT", 1, 0, 0, "-SNAPSHOT"),
                row("1-SNAPSHOT", 1, 0, 0, "-SNAPSHOT"),
                row("1.000.00-R C1 ", 1, 0, 0, "-R C1"),
                row("1.2.3.5", 1, 2, 3, ".5"),
                row("123.456.0789:!  ", 123, 456, 789, ":!")
            ).forAll { version, expectedMajor, expectedMinor, expectedPatch, expectedSuffix ->
                val newVersion = Version(version)
                assertSoftly(newVersion) {
                    major shouldBe expectedMajor
                    minor shouldBe expectedMinor
                    patch shouldBe expectedPatch
                    suffix shouldBe expectedSuffix
                    isSnapshot shouldBe true
                }
            }
        }

        "Invalid versions" {
            Arb.string().filterNot { it.matches(Version.VERSION_REGEX) }
                .checkAll { invalidVersionString ->
                    shouldThrow<IllegalArgumentException> {
                        Version(invalidVersionString)
                    } should haveMessage("No version detected in given string '$invalidVersionString'")
                }
        }
    }
}