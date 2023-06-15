package uk.gov.justice.digital.command

import jakarta.inject.Singleton
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.ParentCommand
import uk.gov.justice.digital.DomainBuilder
import uk.gov.justice.digital.command.ExceptionHandler.runAndHandleExceptions
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.service.DomainService

@Singleton
@Command(
    name = "view",
    description = ["View details for a specific domain"]
)
class ViewDomain(private val service: DomainService) : Runnable {

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

    private fun domainName(): String {
        return domainNameElements.joinToString(" ")
    }

    @ParentCommand
    lateinit var parent: DomainBuilder

    override fun run() =
        runAndHandleExceptions(parent) {
            service.getDomainWithName(domainName())?.let {
                val output = generateOutput(it)
                parent.print("$output\n")
            } ?: parent.print("""
                
            @|red,bold ERROR|@ - no domain with name '@|bold ${domainName()}|@' was found
            
            
            """.trimIndent())
        }

    private fun generateOutput(domain: Domain): String {
        val heading = listOf(
            "\n@|green,bold Found domain with name: '${domainName()}'|@\n",
            """
               @|bold Name        |@| ${domain.name} 
               @|bold Description |@| ${domain.description}
               @|bold Owner       |@| ${domain.owner}
               @|bold Author  |@| ${domain.author}
            """.trimIndent(),
            "\n@|yellow,bold Tables in this domain|@\n"
        )
        val tableData =
            domain
                .tables
                .map {
                    """
                        @|bold Table       |@| ${it.name}
                        @|bold Description |@| ${it.description}
                        @|bold Sources     |@| ${it.transform.sources.joinToString()}
                        @|bold Query       |@| ${it.transform.viewText}
                        
                    """.trimIndent()
            }

        return listOf(heading, tableData)
            .flatten()
            .joinToString("\n")
    }

}