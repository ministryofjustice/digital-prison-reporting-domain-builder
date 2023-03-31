package uk.gov.justice.digital.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DomainServiceTest {

    private val underTest = DomainService()

    @Test
    fun `getDomains should return a list of domains`() {
        val result = underTest.getAllDomains()
        assertEquals(3, result.size)
    }

    @Test
    fun `getDomainWithName should return a Domain given a name that exists`() {
        val name = "Domain 1"
        val result = underTest.getDomainWithName(name)
        assertEquals(name, result?.name)
    }

    @Test
    fun `getDomainWithName should return null given a name that does not exist`() {
        assertNull(underTest.getDomainWithName("This is not a valid domain name"))
    }

}