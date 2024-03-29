package uk.gov.justice.digital.cli.command

import io.mockk.every
import io.mockk.mockk
import org.jline.terminal.Terminal
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.client.DomainNotFoundException
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.cli.session.InteractiveSession
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.test.Fixtures.domainPreviewData

class PreviewDomainTest {

    private val mockDomainBuilder: DomainBuilder = mockk()
    private val mockDomainService: DomainService = mockk()
    private val mockInteractiveSession: InteractiveSession = mockk()
    private val mockTerminal: Terminal = mockk()

    private val underTest = PreviewDomain(mockDomainService)

    @Test
    fun `preview domain should generate some output for a valid request`() {
        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder
        // Ensure properties that are set via the CLI are initialized
        underTest.domainNameElements = arrayOf("Domain 1")
        underTest.domainStatus = Status.DRAFT

        every { mockInteractiveSession.isInteractive() } answers { true }
        every { mockInteractiveSession.terminal() } answers { mockTerminal }
        every { mockTerminal.height } answers { 25 }

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainBuilder.session() } answers { mockInteractiveSession }
        every { mockDomainBuilder.getInteractiveSession() } answers { mockInteractiveSession }
        every { mockDomainService.previewDomain(any(), any(), any()) } answers { domainPreviewData }

        underTest.run()

        val expectedOutput = """
            
            @|bold,green Previewing domain Domain 1 with status DRAFT|@

            ┌─────┬─────┬─────┐
            │@|bold  foo |@│@|bold  bar |@│@|bold  baz |@│
            ├─────┼─────┼─────┤
            │ 1   │     │ 1   │
            └─────┴─────┴─────┘

        """.trimIndent()

        assertEquals(expectedOutput, capturedOutput.joinToString(""))
    }

    @Test
    fun `preview domain should display a suitable message if no data was returned by the query`() {
        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder
        // Ensure properties that are set via the CLI are initialized
        underTest.domainNameElements = arrayOf("Domain 1")
        underTest.domainStatus = Status.DRAFT

        every { mockInteractiveSession.isInteractive() } answers { true }
        every { mockInteractiveSession.terminal() } answers { mockTerminal }
        every { mockTerminal.height } answers { 25 }

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainBuilder.session() } answers { mockInteractiveSession }
        every { mockDomainBuilder.getInteractiveSession() } answers { mockInteractiveSession }
        every { mockDomainService.previewDomain(any(), any(), any()) } answers { domainPreviewData.subList(0, 1) }

        underTest.run()

        val expectedOutput = """
            
            @|bold,white Domain Domain 1 with status DRAFT is empty|@
            
            
        """.trimIndent()

        assertEquals(expectedOutput, capturedOutput.joinToString(""))
    }

    @Test
    fun `preview domain should display a suitable message if the domain was not found`() {
        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder
        // Ensure properties that are set via the CLI are initialized
        underTest.domainNameElements = arrayOf("Domain 1")
        underTest.domainStatus = Status.DRAFT

        every { mockInteractiveSession.isInteractive() } answers { true }
        every { mockInteractiveSession.terminal() } answers { mockTerminal }
        every { mockTerminal.height } answers { 25 }

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainBuilder.session() } answers { mockInteractiveSession }
        every { mockDomainBuilder.getInteractiveSession() } answers { mockInteractiveSession }
        every { mockDomainService.previewDomain(any(), any(), any()) } throws(DomainNotFoundException("Domain with name: Domain 1 and status: DRAFT was not found"))

        underTest.run()

        val expectedOutput = """
            
            @|red,bold There was a problem with your request|@

            @|white,bold Domain with name: Domain 1 and status: DRAFT was not found|@

            @|blue,bold Possible fixes|@

            1. Use the list command to confirm that the domain exists with the status you requested


        """.trimIndent()

        assertEquals(expectedOutput, capturedOutput.joinToString(""))
    }
}