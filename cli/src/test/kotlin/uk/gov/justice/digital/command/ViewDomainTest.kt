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
    fun viewDomainGeneratesExpectedOutput() {

        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder
        underTest.domainNameElements = arrayOf("Domain 1")

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainService.getDomainWithName(any()) } answers { domain1 }

        underTest.run()

        val expectedOutput = """
            
            @|green,bold Found domain with name: 'Domain 1'|@

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
}
