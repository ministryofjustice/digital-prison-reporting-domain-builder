package uk.gov.justice.digital.cli.output

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TableTest {

    private val underTest = Table(
        arrayOf("heading1", "heading2", "heading3"),
        arrayOf(
            arrayOf("foo", "bar", "baz"),
            arrayOf("a", "this value is much longer than the other fields", "baz"),
        )
    )

    @Test
    fun `it should render the table correctly`() {
        val expectedOutput = """
           ┌──────────┬──────────┬──────────┐
           │ heading1 │ heading2 │ heading3 │
           ├──────────┴──────────┴──────────┤
        """.trimIndent()

        assertEquals(expectedOutput, underTest.render())
    }

}