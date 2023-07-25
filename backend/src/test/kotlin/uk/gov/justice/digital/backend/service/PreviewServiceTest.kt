package uk.gov.justice.digital.backend.service

import io.mockk.every
import io.mockk.verify
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.backend.client.preview.PreviewClient
import uk.gov.justice.digital.backend.converter.DomainToPreviewQueryConverter
import uk.gov.justice.digital.backend.repository.DomainRepository
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.model.Table
import uk.gov.justice.digital.model.Transform
import uk.gov.justice.digital.test.Fixtures.domain1
import uk.gov.justice.digital.test.Fixtures.domain2
import uk.gov.justice.digital.test.Fixtures.table1

class PreviewServiceTest {

    private val mockRepository = mockk<DomainRepository>()
    private val mockClient = mockk<PreviewClient>()
    private val mockConverter = mockk<DomainToPreviewQueryConverter>()

    private val underTest by lazy { PreviewService(mockClient, mockRepository, mockConverter) }

    @Test
    fun `it should execute the query and return the results for a valid request`() {
        val queryString = "select * from test_domain"

        val domainWithTestDomainQuery =  domain1.withTables(
            listOf(table1.copy(transform = Transform(queryString, emptyList())))
        )

        val mockResult = listOf(mapOf("foo" to "bar"))

        every { mockRepository.getDomains(any(), any()) } answers { listOf(domainWithTestDomainQuery) }
        every { mockClient.runQuery(any()) } answers { mockResult }
        every { mockConverter.convertQuery(queryString, 50) } answers { queryString }

        assertEquals(mockResult, underTest.preview("Test Domain", Status.DRAFT, 50))
    }

    @Test
    fun `it should use the specified limit when it falls below the allowed maximum`() {
        val queryString = "select * from test_domain"

        val domainWithTestDomainQuery =  domain1.withTables(
            listOf(table1.copy(transform = Transform(queryString, emptyList())))
        )

        val mockResult = listOf(mapOf("foo" to "bar"))

        every { mockRepository.getDomains(any(), any()) } answers { listOf(domainWithTestDomainQuery) }
        every { mockClient.runQuery(any()) } answers { mockResult }
        every { mockConverter.convertQuery(queryString, 50) } answers { queryString }

        assertEquals(mockResult, underTest.preview("Test Domain", Status.DRAFT, 50))

        verify(exactly = 1) { mockConverter.convertQuery(queryString, 50) }
    }

    @Test
    fun `it should use the maximum allowed limit when the requested limit is greater than this value`() {
        val queryString = "select * from test_domain"

        val domainWithTestDomainQuery =  domain1.withTables(
            listOf(table1.copy(transform = Transform(queryString, emptyList())))
        )

        val mockResult = listOf(mapOf("foo" to "bar"))

        every { mockRepository.getDomains(any(), any()) } answers { listOf(domainWithTestDomainQuery) }
        every { mockClient.runQuery(any()) } answers { mockResult }
        every { mockConverter.convertQuery(queryString, PreviewService.MaximumLimit ) } answers { queryString }

        assertEquals(mockResult, underTest.preview("Test Domain", Status.DRAFT, 500))

        verify(exactly = 1) { mockConverter.convertQuery(queryString, PreviewService.MaximumLimit) }
    }

    @Test
    fun `it should throw a DomainNotFoundException if no matching domains are returned`() {
        every { mockRepository.getDomains(any(), any()) } answers { emptyList() }
        assertThrows(DomainNotFoundException::class.java) { underTest.preview("Test Domain", Status.DRAFT, 50) }
    }

    @Test
    fun `it should throw a MultipleDomainsFoundException if more than one matching domain is returned`() {
        every { mockRepository.getDomains(any(), any()) } answers { listOf(domain1 ,domain2) }
        assertThrows(MultipleDomainsFoundException::class.java) { underTest.preview("Test Domain", Status.DRAFT, 50) }
    }

    @Test
    fun `it should throw a NoTablesInDomainException if the domain has no tables defined`() {
        every { mockRepository.getDomains(any(), any()) } answers { listOf(domain1.withTables(emptyList())) }
        assertThrows(NoTablesInDomainException::class.java) { underTest.preview("Test Domain", Status.DRAFT, 50) }
    }

    private fun Domain.withTables(tables: List<Table>) = this.copy(tables = tables)

}