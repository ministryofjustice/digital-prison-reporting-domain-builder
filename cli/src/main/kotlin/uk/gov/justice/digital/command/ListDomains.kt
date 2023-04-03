package uk.gov.justice.digital.command

import jakarta.inject.Singleton
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import uk.gov.justice.digital.service.DomainService

@Singleton
@Command(
    name = "list",
    description = ["List all available domains"],
)
class ListDomains(private val service: DomainService) : CommandBase(), Runnable {

    @Option(names = ["-h", "--help"], usageHelp = true, description = [ "display this help message" ])
    var usageHelpRequested = false

    private val NAME_WIDTH = 20
    private val DESCRIPTION_WIDTH = 40
    private val PADDING = 2

    override fun run() {
        fetchAndDisplayDomains()
    }

    private fun fetchAndDisplayDomains() {

        val result = service.getAllDomains()

        if (result.isNotEmpty()) {

            printlnAnsi("\n@|bold,green Found ${result.size} domains|@\n")

            // Table headings
            val tableRowLine = tableRowDelimiter(NAME_WIDTH, DESCRIPTION_WIDTH)

            println(tableRowLine)
            printlnAnsi(String.format("| @|bold %-20s|@ | @|bold %-40s|@ |", "Name", "Description"))
            println(tableRowLine)

            // Table rows
            result.forEach {
                println(String.format("| %-20s | %-40s |", it.name, it.description))
            }

            println("$tableRowLine\n")
        }
        else printlnAnsi("@|red,bold ERROR|@ No domains were found")
    }

    private fun tableRowDelimiter(vararg columnWidth: Int): String {
        return columnWidth
            .joinToString(separator = "+", prefix = "+", postfix = "+") { "-".repeat(it + PADDING) }
    }

}
