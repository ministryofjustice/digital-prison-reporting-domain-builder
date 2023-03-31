package uk.gov.justice.digital.command

import jakarta.inject.Singleton
import picocli.CommandLine.*
import picocli.CommandLine.Model.CommandSpec
import uk.gov.justice.digital.command.domain.ListDomains

@Command(name = "domain", subcommands = [ListDomains::class] )
@Singleton
class Domain : Runnable {

    @Spec
    var spec: CommandSpec? = null

    override fun run() {
        throw ParameterException(spec?.commandLine(), "ERROR - no subcommand was specified")
    }

}