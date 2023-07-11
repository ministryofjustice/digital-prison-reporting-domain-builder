package uk.gov.justice.digital.cli.command

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.test.Fixtures.domain1
import uk.gov.justice.digital.test.Fixtures.domain2
import uk.gov.justice.digital.test.Fixtures.domain3

class ListDomainsTest {

    private val mockDomainService: DomainService = mockk()
    private val mockDomainBuilder: DomainBuilder = mockk()

    private val underTest = ListDomains(mockDomainService)

    @Test
    fun `should generate a list of domains when domains exist`() {

        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainService.getAllDomains() } answers { arrayOf(domain1, domain2, domain3) }

        underTest.run()

        val expectedOutput = """
    
            @|bold,green Found 3 domains|@
            
            +----------+--------+--------------------+
            | @|bold Name    |@ | @|bold Status|@ | @|bold Description       |@ |
            +----------+--------+--------------------+
            | Domain 1 | DRAFT  | A domain           |
            | Domain 2 | DRAFT  | Another domain     |
            | Domain 3 | DRAFT  | Yet another domain |
            +----------+--------+--------------------+
    
    
""".trimIndent()

        assertEquals(expectedOutput, capturedOutput.joinToString(""))
    }

    @Test
    fun `should show a no domains found message when the api returns an empty list`() {

        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainService.getAllDomains() } answers { emptyArray() }

        underTest.run()

        val expectedOutput = """
    
            @|bold No domains were found|@
            
    
""".trimIndent()

        assertEquals(expectedOutput, capturedOutput.joinToString(""))
    }
}
