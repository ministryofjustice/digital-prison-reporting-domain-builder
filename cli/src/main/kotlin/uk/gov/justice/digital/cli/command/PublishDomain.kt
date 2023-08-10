package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import picocli.CommandLine
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.command.ExceptionHandler.runAndHandleExceptions
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.model.Status

@Singleton
@CommandLine.Command(
    name = "publish",
    description = ["Publish a domain with a given status"]
)
class PublishDomain(private val service: DomainService) : Runnable {

    @CommandLine.Option(
        names = ["-h", "--help"],
        description = [ "display this help message" ]
    )
    var usageHelpRequested = false

    @CommandLine.Option(
        names = ["-n", "--name"],
        description = [
            "the name of the domain to publish",
        ],
        arity = "1..*",
        required = true,
        paramLabel = "DOMAIN_NAME",
    )
    lateinit var domainNameElements: Array<String>

    @CommandLine.Option(
        names = ["-s", "--status"],
        description = [
            "the status of the domain to publish, either DRAFT or PUBLISHED"
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
            service.publishDomain(domainName(), domainStatus)
            parent.print("\n@|bold,green Published domain Domain 1 with status DRAFT successfully|@\n")
        }

}