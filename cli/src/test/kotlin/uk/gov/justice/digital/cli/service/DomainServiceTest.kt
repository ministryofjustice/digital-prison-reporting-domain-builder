package uk.gov.justice.digital.cli.service

import io.micronaut.test.annotation.MockBean
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.cli.client.DomainClient
import uk.gov.justice.digital.cli.test.DomainJsonResources
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
        every { mockDomainClient.getDomains() } returns domains
        assertEquals(3, underTest.getAllDomains().size)
        verify { mockDomainClient.getDomains() }
    }

    @Test
    fun `getDomains should return a Domain given a name that exists`() {
        every { mockDomainClient.getDomains(any(), any()) } returns listOf(domain1)

        val name = "Domain 1"
        val result = underTest.getDomains(name)

        assertEquals(1, result.size)
        assertEquals(name, result[0].name)
    }

    @Test
    fun `getDomains should return null given a name that does not exist`() {
        every { mockDomainClient.getDomains(any(), any()) } returns emptyList()
        assertTrue(underTest.getDomains("This is not a valid domain name").isEmpty())
    }

    @Test
    fun `createDomain should return the value of the response location header on success`() {
        val domainLocation = "/domain/12345"
        every { mockDomainClient.createDomain(any()) } returns domainLocation
        val validJson = DomainJsonResources.validDomain
        assertEquals(domainLocation, underTest.createDomain(validJson))
    }

    @Test
    fun `createDomain should throw an exception given invalid json`() {
        assertThrows(JsonParsingFailedException::class.java) {
            underTest.createDomain(DomainJsonResources.invalidDomain)
        }
    }

}