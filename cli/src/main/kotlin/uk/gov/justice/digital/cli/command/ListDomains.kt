package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import picocli.CommandLine.*
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.command.ExceptionHandler.runAndHandleExceptions
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.cli.service.DomainService

@Singleton
@Command(
    name = "list",
    description = ["List all available domains"],
)
class ListDomains(private val service: DomainService) : Runnable {

    @Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = [ "display this help message" ]
    )
    var usageHelpRequested = false

    @ParentCommand
    lateinit var parent: DomainBuilder

    private val padding = 2
    private val defaultNameWidth = 4
    private val defaultStatusWidth = 6
    private val defaultDescriptionWidth = 10

    override fun run() =
        runAndHandleExceptions(parent) {
            fetchAndDisplayDomains()
        }

    private fun fetchAndDisplayDomains() {
        val result = service.getAllDomains()
        if (result.isEmpty()) parent.print("\n@|bold No domains were found|@\n\n")
        else parent.print(generateOutput(result))
    }

    private fun generateOutput(data: Array<Domain>): String {
        // Format name and description widths dynamically
        val nameWidth = data.maxOf { it.name.length }.let { if (it > defaultNameWidth) it else defaultNameWidth }
        val statusWidth = data.maxOf { it.status.name.length }.let { if (it > defaultStatusWidth) it else defaultStatusWidth }
        val descriptionWidth = data.maxOf { it.description?.length ?: 0 }.let { if (it > defaultDescriptionWidth) it else defaultDescriptionWidth }

        val tableBorder = tableRowBorder(nameWidth, statusWidth, descriptionWidth)

        val heading = listOf(
            "\n@|bold,green Found ${data.size} domains|@\n",
            tableBorder,
            String.format("| @|bold %-${nameWidth}s|@ | @|bold %-${statusWidth}s|@ | @|bold %-${descriptionWidth}s|@ |", "Name", "Status", "Description"),
            tableBorder,
        )
        val dataRows = data.map {
            String.format(
                "| %-${nameWidth}s | %-${statusWidth}s | %-${descriptionWidth}s |", it.name, it.status, it.description) }
        val footer = listOf("$tableBorder\n\n")

        return listOf(heading, dataRows, footer)
            .flatten()
            .joinToString("\n")
    }

    /**
     * Creates a horizontal table row line for the given number of column widths.
     * For example tableRowDelimiter(2, 2, 2) will return
     * +----+----+----+
     * which can be used between table rows e.g.
     * +----+----+----+
     * | C1 | C2 | C3 |
     * +----+----+----+
     */
    private fun tableRowBorder(vararg columnWidths: Int): String {
        return columnWidths
            .joinToString(separator = "+", prefix = "+", postfix = "+") { "-".repeat(it + padding) }
    }

}
