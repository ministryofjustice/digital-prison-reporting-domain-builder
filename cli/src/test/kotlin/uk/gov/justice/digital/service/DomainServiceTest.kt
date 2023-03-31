package uk.gov.justice.digital.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DomainServiceTest {

    private val underTest = DomainService()

    @Test
    fun getDomainsShouldReturnAListOfDomains() {
        val result = underTest.getAllDomains()
        assertEquals(3, result.size)
    }

}