package uk.gov.justice.digital.cli.command

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.test.DomainJsonResources
import java.io.File
import java.util.*
import kotlin.text.Charsets.UTF_8

class CreateDomainTest {

    private val mockDomainBuilder: DomainBuilder = mockk()
    private val mockDomainService: DomainService = mockk()

    private val underTest = CreateDomain(mockDomainService)

    @TempDir
    var tempDir: File? = null

    @Test
    fun `create domain should create a new domain given a valid domain definition file`() {
        val validFilename = "$tempDir/${UUID.randomUUID()}.json"

        File(validFilename).writeText(DomainJsonResources.validDomain, UTF_8)

        val domainPath = "/domain/12345"

        every { mockDomainService.createDomain(any()) } answers { domainPath }

        val capturedOutput = captureCommandOutput()

        underTest.parent = mockDomainBuilder
        underTest.filename = validFilename

        underTest.run()

        val expectedOutput = "@|bold,green Domain successfully created with id: $domainPath|@"

        assertEquals(expectedOutput, capturedOutput.joinToString())
    }

    @Test
    fun `create domain should display an error given a domain definition that could not be parsed before sending`() {
        val validFilename = "$tempDir/${UUID.randomUUID()}.json"

        File(validFilename).writeText(DomainJsonResources.invalidDomain, UTF_8)

        every { mockDomainService.createDomain(any()) } throws(RuntimeException("Failed to parse JSON file"))

        val capturedOutput = captureCommandOutput()

        underTest.parent = mockDomainBuilder
        underTest.filename = validFilename

        underTest.run()

        val expectedOutput = """
            
            @|red,bold There was a problem with your request.|@

            Please try again later.

            Cause: @|red Failed to parse JSON file|@
             
        """.trimIndent()

        assertEquals(expectedOutput, capturedOutput.joinToString())
    }

    @Test
    fun `create domain should display a file not found error given an invalid file name`() {
        val invalidFilename = "file_that_does_not_exist"
        val capturedOutput = captureCommandOutput()

        underTest.parent = mockDomainBuilder
        underTest.filename = invalidFilename

        underTest.run()

        val expectedOutput = """File $invalidFilename not found"""

        assertEquals(expectedOutput, capturedOutput.joinToString())
    }

    private fun captureCommandOutput(): MutableList<String> {
        val capturedOutput = mutableListOf<String>()
        every { mockDomainBuilder.print(capture(capturedOutput)) } answers {  }
        return capturedOutput
    }

}