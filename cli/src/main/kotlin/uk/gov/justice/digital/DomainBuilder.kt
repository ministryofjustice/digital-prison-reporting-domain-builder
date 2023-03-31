package uk.gov.justice.digital

import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Singleton
import picocli.CommandLine.Command
import uk.gov.justice.digital.command.ListDomains
import uk.gov.justice.digital.command.ViewDomain

@Command(
    name = "domain-builder",
    mixinStandardHelpOptions = true,
    version = ["domain-builder 0.0.1"],
    subcommands = [
        ListDomains::class,
        ViewDomain::class,
    ],
)
@Singleton
class DomainBuilder : Runnable {

    override fun run() {}

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("picocli.ansi", "true")
            PicocliRunner.execute(DomainBuilder::class.java, *args);
        }
    }

}
