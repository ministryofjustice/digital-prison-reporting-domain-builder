package uk.gov.justice.digital.cli.output

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TableTest {

    private val underTest = Table(
        listOf("heading1", "heading2", "heading3"),
        listOf(
            listOf("foo", "bar", "baz"),
            listOf("a", "this value is much longer than the other fields", "1"),
            listOf("b", null, "foo"),
        )
    )

    @Test
    fun `it should render data as a table correctly`() {
        val expectedOutput = """
            ┌──────────┬─────────────────────────────────────────────────┬──────────┐
            │@|bold  heading1 |@│@|bold  heading2                                        |@│@|bold  heading3 |@│
            ├──────────┼─────────────────────────────────────────────────┼──────────┤
            │ foo      │ bar                                             │ baz      │
            │ a        │ this value is much longer than the other fields │ 1        │
            │ b        │                                                 │ foo      │
            └──────────┴─────────────────────────────────────────────────┴──────────┘
        """.trimIndent()

        assertEquals(expectedOutput, underTest.render())
    }

}