package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import picocli.CommandLine
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.command.ExceptionHandler.runAndHandleExceptions
import uk.gov.justice.digital.cli.output.Table
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.model.Status

@Singleton
@CommandLine.Command(
    name = "preview",
    description = ["Preview the data in a specific domain"]
)
class PreviewDomain(private val service: DomainService) : Runnable {

    @CommandLine.Option(
        names = ["-h", "--help"],
        description = [ "display this help message" ]
    )
    var usageHelpRequested = false

    @CommandLine.Option(
        names = ["-n", "--name"],
        description = [
            "the name of the domain to preview",
        ],
        arity = "1..*",
        required = true,
        paramLabel = "DOMAIN_NAME",
    )
    lateinit var domainNameElements: Array<String>

    @CommandLine.Option(
        names = ["-s", "--status"],
        description = [
            "the status of the domain to preview, either DRAFT or PUBLISHED"
        ],
        arity = "1",
        required = true,
        paramLabel = "STATUS",
    )
    lateinit var domainStatus: Status

    private fun domainName(): String {
        return domainNameElements.joinToString(" ")
    }

    @CommandLine.ParentCommand
    lateinit var parent: DomainBuilder

    override fun run() =
        runAndHandleExceptions(parent) {
            val result = service.previewDomain(domainName(), domainStatus, displayHeight())
            val output = listOf(
                "\n@|bold,green Previewing domain ${domainName()} with status ${domainStatus}|@\n",
                generateOutput(result),
                ""
            ).joinToString("\n")
            parent.print(output)
        }

    private fun generateOutput(data: List<List<String?>>) =
        Table(
            headings = data[0].map { it ?: "" },
            data = data.subList(1, data.size)
        ).render()

    private fun displayHeight() =
        if (parent.session().isInteractive()) parent.getInteractiveSession().terminal().height - 8
        else 10
}