package uk.gov.justice.digital.cli.command

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.test.Fixtures.domainPreviewData

class PreviewDomainTest {

    private val mockDomainBuilder: DomainBuilder = mockk()
    private val mockDomainService: DomainService = mockk()

    private val underTest = PreviewDomain(mockDomainService)

    @Test
    fun `preview domain should generate some output`() {
        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder
        // Ensure properties that are set via the CLI are initialized
        underTest.domainNameElements = arrayOf("Domain 1")
        underTest.domainStatus = Status.DRAFT

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainService.previewDomain(any(), any(), any()) } answers { domainPreviewData }

        underTest.run()

        val expectedOutput = """
            
            @|bold, green Previewing domain Domain 1 with status DRAFT|@

            ┌─────┬─────┬─────┐
            │@|bold  foo |@│@|bold  bar |@│@|bold  baz |@│
            ├─────┼─────┼─────┤
            │ 1   │ 1   │ 1   │
            └─────┴─────┴─────┘

        """.trimIndent()

        assertEquals(expectedOutput, capturedOutput.joinToString(""))
    }
}