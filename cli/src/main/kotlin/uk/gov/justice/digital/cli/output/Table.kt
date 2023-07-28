package uk.gov.justice.digital.cli.output

import uk.gov.justice.digital.cli.output.Table.TableElements.HeadingSeparatorBottom
import uk.gov.justice.digital.cli.output.Table.TableElements.HeadingSeparatorTop
import uk.gov.justice.digital.cli.output.Table.TableElements.HorizontalConnectorLeft
import uk.gov.justice.digital.cli.output.Table.TableElements.HorizontalConnectorRight
import uk.gov.justice.digital.cli.output.Table.TableElements.HorizontalLine
import uk.gov.justice.digital.cli.output.Table.TableElements.TopLeftCorner
import uk.gov.justice.digital.cli.output.Table.TableElements.TopRightCorner
import uk.gov.justice.digital.cli.output.Table.TableElements.VerticalLine

class Table(private val headings: Array<String>, private val data: Array<Array<String>>) {

    private val maxColumnWidths = headings.map { it.length }

    fun render(): String {
        return generateColumnHeadings()
    }

    fun generateColumnHeadings(): String {
        val headingsTopBar = headings
            .joinToString(prefix = TopLeftCorner, separator = HeadingSeparatorTop, postfix = TopRightCorner) {
                HorizontalLine.repeat(it.length + 2)
            }
        val headingsLine = headings.joinToString(prefix = VerticalLine, separator = VerticalLine, postfix = VerticalLine) { " $it " }
        val headingsBottomBar = headings
            .joinToString(prefix = HorizontalConnectorLeft, separator = HeadingSeparatorBottom, postfix = HorizontalConnectorRight) {
                HorizontalLine.repeat(it.length + 2)
            }

        return listOf(headingsTopBar, headingsLine, headingsBottomBar).joinToString("\n")
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