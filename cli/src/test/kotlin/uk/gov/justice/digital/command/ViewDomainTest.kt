package uk.gov.justice.digital.command

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.DomainBuilder
import uk.gov.justice.digital.service.DomainService
import uk.gov.justice.digital.test.Fixtures.domain1

class ViewDomainTest {

    private val mockDomainService: DomainService = mockk()
    private val mockDomainBuilder: DomainBuilder = mockk()

    private val underTest = ViewDomain(mockDomainService)

    @Test
    fun `view domain displays an existing domain correctly`() {

        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder
        underTest.domainNameElements = arrayOf("Domain 1")

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainService.getDomains(any(), any()) } answers { arrayOf(domain1) }

        underTest.run()

        val expectedOutput = """
            
            @|green,bold Found 1 domain with name: 'Domain 1'|@

            @|cyan,bold Domain 'Domain 1' with status DRAFT|@
            
            @|bold Name        |@| Domain 1 
            @|bold Status      |@| draft
            @|bold Description |@| A domain
            @|bold Owner       |@| someone@example.com
            @|bold Author      |@| someone@example.com

            @|yellow,bold Tables in this domain|@

            @|bold Table       |@| Table 1
            @|bold Description |@| A table containing some data
            @|bold Sources     |@| source.table
            @|bold Query       |@| SELECT source.table.field1, source.table.field2 FROM source.table
            
            
        """.trimIndent()

        assertEquals(expectedOutput, capturedOutput.joinToString(""))
    }

    @Test
    fun `view domain displays an error message if no domain is found`() {

        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder
        underTest.domainNameElements = arrayOf("Domain 1")

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainService.getDomains(any(), any()) } answers { emptyArray() }

        underTest.run()

        val expectedOutput = """
            
            @|red,bold ERROR|@ - no domain with name '@|bold Domain 1|@' was found
            
            
        """.trimIndent()

        assertEquals(expectedOutput, capturedOutput.joinToString(""))

    }
}
