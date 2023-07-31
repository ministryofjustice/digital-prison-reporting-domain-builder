package uk.gov.justice.digital.cli.output

import uk.gov.justice.digital.cli.output.Table.TableElements.HeadingSeparatorBottom
import uk.gov.justice.digital.cli.output.Table.TableElements.HeadingSeparatorTop
import uk.gov.justice.digital.cli.output.Table.TableElements.HorizontalConnectorLeft
import uk.gov.justice.digital.cli.output.Table.TableElements.HorizontalConnectorRight
import uk.gov.justice.digital.cli.output.Table.TableElements.HorizontalLine
import uk.gov.justice.digital.cli.output.Table.TableElements.TopLeftCorner
import uk.gov.justice.digital.cli.output.Table.TableElements.TopRightCorner
import uk.gov.justice.digital.cli.output.Table.TableElements.VerticalLine
import kotlin.math.max

class Table(private val headings: Array<String>, private val data: Array<Array<String>>) {

    // Determine maximum value lengths per column for formatting.
    private val maxColumnWidths = headings.mapIndexed { index: Int, value: String ->
        val longestColumnValue = data.maxOfOrNull { it[index].length } ?: Int.MIN_VALUE
        max(value.length, longestColumnValue)
    }

    private val columnFormatStrings = maxColumnWidths.map { " %-${it}s " }

    fun render(): String {
        return listOf(
            generateColumnHeadings(),
            generateDataRows(),
        ).joinToString("\n")
    }

    private fun generateColumnHeadings(): String {
        val headingsTopBar = maxColumnWidths
            .joinToString(prefix = TopLeftCorner, separator = HeadingSeparatorTop, postfix = TopRightCorner) {
                HorizontalLine.repeat(it + 2)
            }
        val headingsLine = headings.mapIndexed { index, value ->
            columnFormatStrings[index].format(value)
        }
        .joinToString(prefix = VerticalLine, separator = VerticalLine, postfix = VerticalLine)


        val headingsBottomBar = maxColumnWidths
            .joinToString(prefix = HorizontalConnectorLeft, separator = HeadingSeparatorBottom, postfix = HorizontalConnectorRight) {
                HorizontalLine.repeat(it + 2)
            }

        return listOf(headingsTopBar, headingsLine, headingsBottomBar).joinToString("\n")
    }

    private fun generateDataRows(): String {
        val rowSeparator = maxColumnWidths
            .joinToString(prefix = VerticalLine, separator = VerticalLine, postfix = VerticalLine) { " ".repeat(it + 2) }
        // Iterate over format strings to build up each line with appropriate delimiters.
        // Consider a function to do this so a row can easily be passed in.

        val dataRows = data.joinToString("\n") {
            val row = it.mapIndexed { index: Int, value: String ->
                columnFormatStrings[index].format(value)
            }.joinToString(prefix = VerticalLine, separator = VerticalLine, postfix = VerticalLine)
            listOf(row).joinToString("\n")
        }

        return dataRows
    }

    object TableElements {
        const val TopLeftCorner = "┌"
        const val TopRightCorner = "┐"
        const val BottomLeftCorner = "└"
        const val BottomRightCorner = "┘"

        const val HorizontalLine = "─"
        const val VerticalLine = "│"

        const val HorizontalConnectorLeft = "├"
        const val HorizontalConnectorRight = "┤"

        const val HeadingSeparatorTop = "┬"
        const val HeadingSeparatorBottom = "┴"

        const val HorizontalVerticalConnector = "┼"
    }
}