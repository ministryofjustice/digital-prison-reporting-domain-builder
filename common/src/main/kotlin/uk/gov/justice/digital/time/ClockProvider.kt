package uk.gov.justice.digital.time

import jakarta.inject.Singleton
import java.time.Clock

interface ClockProvider {
    val clock: Clock
}

@Singleton
class DefaultClockProvider : ClockProvider {
    override val clock: Clock = Clock.systemUTC()
}