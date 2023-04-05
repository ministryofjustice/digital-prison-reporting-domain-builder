package uk.gov.justice.digital.command

import jakarta.inject.Singleton
import org.jline.builtins.Less
import org.jline.builtins.Source
import picocli.CommandLine.*
import uk.gov.justice.digital.DomainBuilder
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.service.DomainService
import java.io.ByteArrayInputStream
import java.io.PrintWriter


@Singleton
@Command(
    name = "list",
    description = ["List all available domains"],
)
class ListDomains(private val service: DomainService) : CommandBase(), Runnable {

    @Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = [ "display this help message" ]
    )
    var usageHelpRequested = false

    @ParentCommand
    lateinit var parent: DomainBuilder

    override fun getPrintWriter(): PrintWriter {
        return parent.out
    }

    private val NAME_WIDTH = 20
    private val DESCRIPTION_WIDTH = 40
    private val PADDING = 2

    override fun run() {
        fetchAndDisplayDomains()
    }

    // TODO - pagination
    private fun fetchAndDisplayDomains() {

        val result = service.getAllDomains()

        val output = generateOutput(result)

        if (parent.interactive && result.size + 4 > parent.terminalHeight) {
            val ansiOutput = Help.Ansi.AUTO.string(output)
            val src: Source = Source.InputStreamSource(ByteArrayInputStream(ansiOutput.toByteArray()), true, "pager")
            val less = Less(parent.terminal, null)
            less.run(src)
        }
        else if (result.isNotEmpty()) {
            printAnsi(output)
        }
        else printAnsi("@|red,bold ERROR|@ No domains were found")
    }

    private fun generateOutput(data: List<Domain>): String {
        // Table headings
        val tableRowLine = tableRowDelimiter(NAME_WIDTH, DESCRIPTION_WIDTH)

        val lines = listOf(
            "\n@|bold,green Found ${data.size} domains|@\n",
            tableRowLine,
            String.format("| @|bold %-20s|@ | @|bold %-40s|@ |", "Name", "Description"),
            tableRowLine,
        ) +
            data.map { String.format("| %-20s | %-40s |", it.name, it.description) } +
                listOf(tableRowLine, "\n")

        return lines.joinToString("\n")
    }

    private fun tableRowDelimiter(vararg columnWidth: Int): String {
        return columnWidth
            .joinToString(separator = "+", prefix = "+", postfix = "+") { "-".repeat(it + PADDING) }
    }

}
