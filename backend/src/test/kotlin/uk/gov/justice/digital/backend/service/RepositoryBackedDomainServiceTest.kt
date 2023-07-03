package uk.gov.justice.digital.backend.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.backend.validator.InvalidSparkSqlResult
import uk.gov.justice.digital.backend.validator.SparkSqlValidator
import uk.gov.justice.digital.backend.validator.ValidSparkSqlResult
import uk.gov.justice.digital.model.WriteableDomain
import uk.gov.justice.digital.test.Fixtures.writeableDomain
import uk.gov.justice.digital.test.Fixtures.writeableDomainWithInvalidMappingSql
import uk.gov.justice.digital.test.Fixtures.writeableDomainWithInvalidTransformSql
import java.util.*

class RepositoryBackedDomainServiceTest {

    private val mockRepository: DomainRepository = mockk()
    private val mockValidator: SparkSqlValidator = mockk()
    private val fixedUUID = UUID.randomUUID()

    private val underTest = RepositoryBackedDomainService(mockRepository, mockValidator)

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

}