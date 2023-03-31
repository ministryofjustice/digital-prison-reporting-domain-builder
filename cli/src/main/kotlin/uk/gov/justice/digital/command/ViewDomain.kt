package uk.gov.justice.digital.command

import jakarta.inject.Singleton
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import uk.gov.justice.digital.service.DomainService

@Singleton
@Command(
    name = "view",
    description = ["View details for a specific domain"]
)
class ViewDomain(private val service: DomainService) : CommandBase(), Runnable {

    @Option(
        names = ["-n", "--name"],
        description = ["The name of the domain to view"],
        required = true
    )
    var domainName: String = ""

    override fun run() {
        val domain = service.getDomainWithName(domainName)
        if (domain == null) {
            printAnsiString("@|red,bold ERROR|@ - no domain with name @|yellow'$domainName'|@ was found")
        }
        else {
            println("Found domain with name: '$domainName'")
        }
    }

}