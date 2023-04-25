package uk.gov.justice.digital.service

import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.client.DomainClient
import uk.gov.justice.digital.test.Fixtures.domain1
import uk.gov.justice.digital.test.Fixtures.domains

@MicronautTest
class DomainServiceTest {

    @Inject
    private lateinit var underTest: DomainService

    private val mockDomainClient: DomainClient = mockk()

    @MockBean(DomainClient::class)
    fun getMockDomainClient() = mockDomainClient

    @Test
    fun `getDomains should return a list of domains`() {
        every { mockDomainClient.getDomains() } returns domains.toTypedArray()
        assertEquals(3, underTest.getAllDomains().size)
        verify { mockDomainClient.getDomains() }
    }

    @Test
    fun `getDomainWithName should return a Domain given a name that exists`() {
        every { mockDomainClient.getDomainWithName(any()) } returns domain1

        val name = "Domain 1"
        val result = underTest.getDomainWithName(name)

        assertEquals(name, result?.name)
    }

    @Test
    fun `getDomainWithName should return null given a name that does not exist`() {
        every { mockDomainClient.getDomainWithName(any()) } returns null
        assertNull(underTest.getDomainWithName("This is not a valid domain name"))
    }

}