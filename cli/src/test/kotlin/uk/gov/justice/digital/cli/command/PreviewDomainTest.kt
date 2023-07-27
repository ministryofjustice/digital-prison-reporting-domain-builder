package uk.gov.justice.digital.cli.command

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.model.Status

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
        every { mockDomainService.previewDomain(any(), any()) } answers {
            arrayOf(mapOf("foo" to "1", "bar" to "1", "baz" to "1"))
        }

        underTest.run()

        val expectedOutput = "{foo=1, bar=1, baz=1}i\n"

        assertEquals(expectedOutput, capturedOutput.joinToString(""))
    }
}