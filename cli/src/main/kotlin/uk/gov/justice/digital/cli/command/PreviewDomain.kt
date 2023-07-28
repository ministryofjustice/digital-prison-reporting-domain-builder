package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import picocli.CommandLine
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.command.ExceptionHandler.runAndHandleExceptions
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
            // TODO - look at the best way to make the limit dynamic (interactive only :/)
            val result = service.previewDomain(domainName(), domainStatus, 25)
            // TODO - generate tabulated output
            result.forEach { parent.print("$it\n") }
        }

}