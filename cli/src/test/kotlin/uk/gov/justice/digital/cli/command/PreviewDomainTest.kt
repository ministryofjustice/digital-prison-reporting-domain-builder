package uk.gov.justice.digital.cli.command

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.cli.DomainBuilder

class PreviewDomainTest {

    private val mockDomainBuilder: DomainBuilder = mockk()

    private val underTest = PreviewDomain()

    @Test
    fun `preview domain should generate some output`() {
        val capturedOutput = mutableListOf<String>()

        underTest.parent = mockDomainBuilder
        underTest.domainNameElements = arrayOf("Domain 1")

        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }

        underTest.run()

        assertEquals("TODO\n", capturedOutput.joinToString(""))
    }
}