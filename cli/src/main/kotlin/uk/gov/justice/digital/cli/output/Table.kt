package uk.gov.justice.digital.cli.output

import uk.gov.justice.digital.cli.output.Table.TableElements.BottomLeftCorner
import uk.gov.justice.digital.cli.output.Table.TableElements.BottomRightCorner
import uk.gov.justice.digital.cli.output.Table.TableElements.ColumnSeparatorBottom
import uk.gov.justice.digital.cli.output.Table.TableElements.ColumnSeparatorTop
import uk.gov.justice.digital.cli.output.Table.TableElements.HorizontalConnectorLeft
import uk.gov.justice.digital.cli.output.Table.TableElements.HorizontalConnectorRight
import uk.gov.justice.digital.cli.output.Table.TableElements.HorizontalLine
import uk.gov.justice.digital.cli.output.Table.TableElements.HorizontalVerticalConnector
import uk.gov.justice.digital.cli.output.Table.TableElements.TopLeftCorner
import uk.gov.justice.digital.cli.output.Table.TableElements.TopRightCorner
import uk.gov.justice.digital.cli.output.Table.TableElements.VerticalLine
import kotlin.math.max

class Table(private val headings: List<String>, private val data: List<List<String?>>) {

    // Determine maximum value lengths per column for formatting.
    private val maxColumnWidths = headings.mapIndexed { index: Int, value: String ->
        val longestColumnValue = data.maxOf { it[index]?.length ?: Int.MIN_VALUE }
        max(value.length, longestColumnValue)
    }

    private val columnFormatStrings = maxColumnWidths.map { " %-${it}s " }

    fun render(): String {
        return listOf(
            generateColumnHeadings(),
            generateDataRows(),
            generateClosingRow(),
        ).joinToString("\n")
    }

    private fun generateColumnHeadings(): String {
        val headingsTopBar = maxColumnWidths
            .joinToString(prefix = TopLeftCorner, separator = ColumnSeparatorTop, postfix = TopRightCorner) {
                HorizontalLine.repeat(it + 2)
            }

        val headingsLine = headings.mapIndexed { index, value ->
            // Render headings in bold
            "@|bold ${columnFormatStrings[index].format(value)}|@"
        }
        .joinToString(prefix = VerticalLine, separator = VerticalLine, postfix = VerticalLine)


        val headingsBottomBar = maxColumnWidths
            .joinToString(prefix = HorizontalConnectorLeft, separator = HorizontalVerticalConnector, postfix = HorizontalConnectorRight) {
                HorizontalLine.repeat(it + 2)
            }

        return listOf(headingsTopBar, headingsLine, headingsBottomBar).joinToString("\n")
    }

    private fun generateDataRows(): String =
        data.joinToString("\n") {
            it.mapIndexed { index: Int, value: String? ->
                columnFormatStrings[index].format(value ?: "")
            }.joinToString(prefix = VerticalLine, separator = VerticalLine, postfix = VerticalLine)
        }

    private fun generateClosingRow(): String =
        maxColumnWidths
            .joinToString(prefix = BottomLeftCorner, separator = ColumnSeparatorBottom, postfix = BottomRightCorner) {
                HorizontalLine.repeat(it + 2)
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

        const val ColumnSeparatorTop = "┬"
        const val ColumnSeparatorBottom = "┴"

        const val HorizontalVerticalConnector = "┼"
    }
}