package uk.gov.justice.digital.command.domain

import jakarta.inject.Inject
import jakarta.inject.Singleton
import picocli.CommandLine.Command
import uk.gov.justice.digital.service.DomainService

@Singleton
@Command(
    name = "list",
    description = ["List all available domains"]
)
class ListDomains: Runnable {

    @Inject
    private lateinit var service: DomainService

    override fun run() {
        val result = service.getAllDomains()
        println("Domains\n")
        println(result)
    }

}
