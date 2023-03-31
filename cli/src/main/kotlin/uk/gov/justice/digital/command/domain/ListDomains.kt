package uk.gov.justice.digital.command.domain

import jakarta.inject.Singleton
import picocli.CommandLine.Command

@Command(
    name = "list",
    description = ["List all available domains"]
)
@Singleton
class ListDomains : Runnable {
    override fun run() {
        println("TODO - domains list goes here")
    }
}
