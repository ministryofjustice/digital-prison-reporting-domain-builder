package uk.gov.justice.digital.command

import jakarta.inject.Singleton
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.ParentCommand
import uk.gov.justice.digital.DomainBuilder
import uk.gov.justice.digital.service.DomainService
import java.io.PrintWriter

@Singleton
@Command(
    name = "view",
    description = ["View details for a specific domain"]
)
class ViewDomain(private val service: DomainService) : CommandBase(), Runnable {

    @Option(
        names = ["-h", "--help"],
        usageHelp = true,
        description = [ "display this help message" ]
    )
    var usageHelpRequested = false

    @Option(
        names = ["-n", "--name"],
        description = [
            "the name of the domain to view",
        ],
        arity = "1..*",
        required = true
    )
    lateinit var domainNameElements: Array<String>

    @ParentCommand
    lateinit var parent: DomainBuilder

    override fun getPrintWriter(): PrintWriter {
        return parent.out
    }

    override fun run() {
        val domainName = domainNameElements.joinToString(" ")
        val domain = service.getDomainWithName(domainName)
        if (domain == null) {
            printAnsi("@|red,bold ERROR|@ - no domain with name '@|bold $domainName|@' was found")
        }
        else {
            printAnsi("\n@|green,bold Found domain with name: '$domainName'|@\n")

            printAnsi("""
               @|bold Name        |@| ${domain.name} 
               @|bold Description |@| ${domain.description}
               @|bold Originator  |@| ${domain.originator}
            """.trimIndent())

            printAnsi("\n@|yellow,bold Tables in this domain|@\n")

            domain
                .tables
                .forEach {
                    printAnsi("""
                        @|bold Table       |@| ${it.name}
                        @|bold Description |@| ${it.description}
                        @|bold Sources     |@| ${it.transform.sources.joinToString()}
                        @|bold Query       |@| ${it.transform.viewText}
                        
                    """.trimIndent())
                }
        }
    }

}