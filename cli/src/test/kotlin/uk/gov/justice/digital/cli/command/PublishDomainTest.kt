package uk.gov.justice.digital.cli.command

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.client.DomainNotFoundException
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.cli.session.InteractiveSession
import uk.gov.justice.digital.model.Status

class PublishDomainTest {

    private val mockDomainBuilder: DomainBuilder = mockk()
    private val mockDomainService: DomainService = mockk()
    private val mockInteractiveSession: InteractiveSession = mockk()

    private val underTest = PublishDomain(mockDomainService)

    @Test
    fun `preview domain should generate some output for a valid request`() {
        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder
        // Ensure properties that are set via the CLI are initialized
        underTest.domainNameElements = arrayOf("Domain 1")
        underTest.domainStatus = Status.DRAFT

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainBuilder.session() } answers { mockInteractiveSession }
        every { mockDomainBuilder.getInteractiveSession() } answers { mockInteractiveSession }
        every { mockDomainService.publishDomain(any(), any()) } returns Unit

        underTest.run()

        val expectedOutput = """
            
            @|bold,green Published domain Domain 1 with status DRAFT successfully|@
            
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

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        every { mockDomainBuilder.session() } answers { mockInteractiveSession }
        every { mockDomainBuilder.getInteractiveSession() } answers { mockInteractiveSession }
        every { mockDomainService.publishDomain(any(), any()) } throws(DomainNotFoundException("Domain with name: Domain 1 and status: DRAFT was not found"))

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