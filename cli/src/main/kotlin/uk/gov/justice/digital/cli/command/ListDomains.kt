package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import picocli.CommandLine.*
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.command.ExceptionHandler.runAndHandleExceptions
import uk.gov.justice.digital.cli.output.Table
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

        val heading = "\n@|bold,green Found ${data.size} domains|@\n"

        val tableData = data.map {
            arrayOf(it.name, it.status.name, it.description ?: "")
        }.toTypedArray()

        val renderedTable = Table(arrayOf("Name", "Status", "Description"), tableData).render()

        return listOf(
            heading,
            renderedTable,
            "\n"
        ).joinToString("\n")
    }

}
