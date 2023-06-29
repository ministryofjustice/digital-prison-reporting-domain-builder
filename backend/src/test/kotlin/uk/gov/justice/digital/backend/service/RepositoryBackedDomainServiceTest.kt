package uk.gov.justice.digital.backend.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertThrows
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.backend.validator.InvalidSparkSql
import uk.gov.justice.digital.backend.validator.SparkSqlValidator
import uk.gov.justice.digital.backend.validator.ValidSparkSql
import uk.gov.justice.digital.model.WriteableDomain
import uk.gov.justice.digital.test.Fixtures.mapping
import uk.gov.justice.digital.test.Fixtures.writeableDomain
import java.util.*

class RepositoryBackedDomainServiceTest {

    private val mockRepository: DomainRepository = mockk()
    private val mockValidator: SparkSqlValidator = mockk()
    private val fixedUUID = UUID.randomUUID()

    private val underTest = RepositoryBackedDomainService(mockRepository, mockValidator)

    @Test
    fun `create should create a domain containing valid spark sql`() {
        every { mockRepository.createDomain(any<WriteableDomain>()) } returns fixedUUID
        every { mockValidator.validate(any()) } returns ValidSparkSql()
        val result = underTest.createDomain(writeableDomain)
        assertEquals(fixedUUID, result)
    }

    @Test
    fun `create should throw an exception given a domain containing invalid spark sql`() {
        every { mockValidator.validate(any()) } returns InvalidSparkSql("Parse failure message")

        val writeableDomainWithInvalidSql = writeableDomain.copy(
            tables = writeableDomain.tables.map {
                it.copy(mapping = mapping.copy(viewText = "This is not valid SQL"))
            }
        )

        assertThrows(InvalidSparkSqlException::class.java) {
            underTest.createDomain(writeableDomainWithInvalidSql)
        }
    }

}