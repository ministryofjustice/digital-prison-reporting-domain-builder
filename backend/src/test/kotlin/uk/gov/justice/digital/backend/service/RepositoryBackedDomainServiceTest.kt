package uk.gov.justice.digital.backend.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.backend.client.domain.DomainRegistryClient
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.backend.validator.InvalidSparkSqlResult
import uk.gov.justice.digital.backend.validator.SparkSqlValidator
import uk.gov.justice.digital.backend.validator.ValidSparkSqlResult
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.WriteableDomain
import uk.gov.justice.digital.test.Fixtures.domain1
import uk.gov.justice.digital.test.Fixtures.domains
import uk.gov.justice.digital.test.Fixtures.writeableDomain
import uk.gov.justice.digital.test.Fixtures.writeableDomainWithInvalidMappingSql
import uk.gov.justice.digital.test.Fixtures.writeableDomainWithInvalidTransformSql
import java.util.*

class RepositoryBackedDomainServiceTest {

    private val mockRepository: DomainRepository = mockk()
    private val mockValidator: SparkSqlValidator = mockk()
    private val mockRegistryClient: DomainRegistryClient = mockk()

    private val fixedUUID = UUID.randomUUID()

    private val underTest = RepositoryBackedDomainService(mockRepository, mockValidator, mockRegistryClient)

    @Test
    fun `create should create a domain containing valid spark sql`() {
        every { mockRepository.createDomain(any<WriteableDomain>()) } returns fixedUUID
        every { mockValidator.validate(any()) } returns ValidSparkSqlResult()
        val result = underTest.createDomain(writeableDomain)
        assertEquals(fixedUUID, result)
    }

    @Test
    fun `create should throw an exception given a domain containing invalid mapping sql`() {
        every { mockValidator.validate(any()) } returns InvalidSparkSqlResult("Parse failure message")

        assertThrows(InvalidSparkSqlException::class.java) {
            underTest.createDomain(writeableDomainWithInvalidMappingSql)
        }
    }

    @Test
    fun `create should throw an exception given a domain containing invalid transform sql`() {
        every { mockValidator.validate(any()) } returns InvalidSparkSqlResult("Parse failure message")

        assertThrows(InvalidSparkSqlException::class.java) {
            underTest.createDomain(writeableDomainWithInvalidTransformSql)
        }
    }

    @Test
    fun `publish should return the UUID of the published domain when successful`() {
        val publishedDomain = domain1.copy(status = Status.PUBLISHED)

        every { mockValidator.validate(any()) } returns ValidSparkSqlResult()
        every { mockRepository.withinTransaction<Unit>(any()) } returns Unit
        every { mockRepository.getDomains(any(), any()) } returns listOf(domain1)
        every { mockRepository.updateDomain(publishedDomain) } returns Unit
        every { mockRepository.deleteDomain(domain1.id) } returns Unit
        every { mockRegistryClient.publish(any()) } returns Unit

        assertDoesNotThrow {
            underTest.publishDomain("domain1", Status.DRAFT)
        }
    }

    @Test
    fun `publish should throw an exception on attempt to publish domain that is already published`() {
        assertThrows(InvalidStatusException::class.java) {
            underTest.publishDomain("domain1", Status.PUBLISHED)
        }
    }

    @Test
    fun `publish should throw an exception if the requested domain could not be found`() {
        every { mockRepository.getDomains(any(), any()) } returns emptyList()

        assertThrows(PublishDomainNotFoundException::class.java) {
            underTest.publishDomain("domain1", Status.DRAFT)
        }
    }

    @Test
    fun `publish should throw an exception if more than one matching domain was found`() {
        every { mockRepository.getDomains(any(), any()) } returns domains

        assertThrows(MultipleDomainsFoundException::class.java) {
            underTest.publishDomain("domain1", Status.DRAFT)
        }
    }

}