package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import picocli.CommandLine.*
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.command.ExceptionHandler.runAndHandleExceptions
import uk.gov.justice.digital.model.Domain
import uk.gov.justice.digital.model.Status
import uk.gov.justice.digital.cli.service.DomainService

@Singleton
@Command(
    name = "view",
    description = ["View information for a specific domain"],
    synopsisHeading = "Usage:\n",
    customSynopsis = [
        "   @|bold view -n Domain Name|@",
        "   @|bold view -n Domain Name -s DRAFT|@",
        "Examples:",
        "   View information for the domain 'Example Domain'",
        "   @|cyan view -n Example Domain|@",
        "   View information for the domain 'Example Domain' with the status PUBLISHED",
        "   @|cyan view -n Example Domain -s PUBLISHED|@",
    ]
)
class ViewDomain(private val service: DomainService) : Runnable {

    @Option(
        names = ["-h", "--help"],
        description = [ "display this help message" ]
    )
    var usageHelpRequested = false

    @Option(
        names = ["-n", "--name"],
        description = [
            "the name of the domain to view",
        ],
        arity = "1..*",
        required = true,
        paramLabel = "DOMAIN_NAME",
    )
    lateinit var domainNameElements: Array<String>

    @Option(
        names = ["-s", "--status"],
        description = [
            "the status of the domain to view, either DRAFT or PUBLISHED"
        ],
        arity = "1",
        required = false,
        paramLabel = "STATUS",
    )
    var domainStatus: Status? = null

    private fun domainName(): String {
        return domainNameElements.joinToString(" ")
    }

    @ParentCommand
    lateinit var parent: DomainBuilder

    override fun run() =
        runAndHandleExceptions(parent) {
            service.getDomains(domainName(), domainStatus).let { domains ->
                if (domains.isNotEmpty()) {
                    val domainCount = domains.count()
                    val countText = if (domainCount == 1) "1 domain" else "$domainCount domains"
                    val statusText = domainStatus?.let { " and status $it" } ?: ""
                    parent.print("\n@|green,bold Found $countText with name: '${domainName()}'$statusText|@\n\n")
                    domains.map {
                        parent.print(generateOutput(it))
                    }
                }
                else {
                    val statusText = domainStatus?.let { " and status @|bold $it|@" } ?: ""
                    parent.print("""
                        
                        @|red,bold ERROR|@ - no domain with name '@|bold ${domainName()}|@'$statusText was found
                        
                        
                    """.trimIndent())
                }
            }
        }

    private fun generateOutput(domain: Domain): String {
        val heading = listOf(
            "@|cyan,bold Domain '${domain.name}' with status ${domain.status}|@\n",
            """
               @|bold Name        |@│ ${domain.name} 
               @|bold Status      |@│ ${domain.status.name.lowercase()}
               @|bold Description |@│ ${domain.description}
               @|bold Owner       |@│ ${domain.owner}
               @|bold Author      |@│ ${domain.author}
            """.trimIndent(),
            "\n@|yellow,bold Tables in this domain|@\n"
        )
        val tableData =
            domain
                .tables
                ?.map {table ->
                    val formattedTableFields = """
                        @|bold Table       |@│ ${table.name}
                        @|bold Description |@│ ${table.description}
                        @|bold Sources     |@│ ${table.transform.sources.joinToString()}
                    """.trimIndent()

                    val formattedQueryString = "\n@|bold Query       |@│ " +
                        table.transform.viewText.split("\n").joinToString("\n@|bold             |@│")

                    "$formattedTableFields$formattedQueryString\n"
                } ?: emptyList()

        return listOf(heading, tableData)
            .flatten()
            .joinToString("\n")
            .let { "$it\n" }
    }

}