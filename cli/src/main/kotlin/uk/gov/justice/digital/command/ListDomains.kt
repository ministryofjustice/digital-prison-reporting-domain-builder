package uk.gov.justice.digital.command

import jakarta.inject.Singleton
import picocli.CommandLine.*
import uk.gov.justice.digital.DomainBuilder
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.service.DomainService


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

    private val nameWidth = 20
    private val descriptionWidth = 40
    private val padding = 2

    override fun run() {
        fetchAndDisplayDomains()
    }

    private fun fetchAndDisplayDomains() {
        val result = service.getAllDomains()

        val output = generateOutput(result)

        if (result.isEmpty()) parent.print("@|red,bold ERROR|@ No domains were found")
        else parent.print(output)
    }

    private fun generateOutput(data: List<Domain>): String {
        val tableBorder = tableRowBorder(nameWidth, descriptionWidth)

        val heading = listOf(
            "\n@|bold,green Found ${data.size} domains|@\n",
            tableBorder,
            String.format("| @|bold %-20s|@ | @|bold %-40s|@ |", "Name", "Description"),
            tableBorder,
        )
        val dataRows = data.map { String.format("| %-20s | %-40s |", it.name, it.description) }
        val footer = listOf("$tableBorder\n")

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
