package uk.gov.justice.digital.cli.command

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.gov.justice.digital.cli.DomainBuilder
import java.io.File
import java.util.*
import kotlin.text.Charsets.UTF_8

class CreateDomainTest {

    private val mockDomainBuilder: DomainBuilder = mockk()

    private val underTest = CreateDomain()

    @TempDir
    var tempDir: File? = null

    @Test
    fun `create domain should create a new domain given a valid domain definition file`() {
        val sourceFile = "/domains/valid-domain.json"

        val data = this::class.java.getResourceAsStream(sourceFile)
            ?.readAllBytes()
            ?.joinToString()

        val validFilename = "$tempDir/${UUID.randomUUID()}.json"

        File(validFilename).writeText(data!!, UTF_8)

        val capturedOutput = captureCommandOutput()

        underTest.parent = mockDomainBuilder
        underTest.fileName = validFilename

        underTest.run()

        val expectedOutput = """File: $validFilename exists"""

        assertEquals(expectedOutput, capturedOutput.joinToString())
    }

    @Test
    fun `create domain should display a file not found error given an invalid file name`() {
        val invalidFilename = "file_that_does_not_exist"
        val capturedOutput = captureCommandOutput()

        underTest.parent = mockDomainBuilder
        underTest.fileName = invalidFilename

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