package uk.gov.justice.digital.test.time

import uk.gov.justice.digital.time.ClockProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset.UTC

// Fixed clock for stable tests that involve temporal values.
class FixedClock : ClockProvider {
    override val clock: Clock = Clock.fixed(Instant.parse("2023-01-01T00:00:00Z"), UTC)
}