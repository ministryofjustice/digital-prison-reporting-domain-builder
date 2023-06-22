package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import picocli.CommandLine.*
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.command.ExceptionHandler.runAndHandleExceptions
import kotlin.io.path.Path
import kotlin.io.path.exists

@Singleton
@Command(
    name = "create",
    description = ["Create a domain from a JSON definition"]
)
class CreateDomain : Runnable {

    @Option(
        names = ["-h", "--help"],
        description = [ "display this help message" ]
    )
    var usageHelpRequested = false

    @Option(
        names = ["-f", "--file"],
        description = [ "file that contains a JSON domain definition" ],
        arity = "1",
        required = true,
        paramLabel = "FILE_NAME",
    )
    lateinit var fileName: String

    @ParentCommand
    lateinit var parent: DomainBuilder

    override fun run() =
        runAndHandleExceptions(parent) {
            val filePath = Path(fileName)
            if (filePath.exists()) parent.print("File: $filePath exists")
            else parent.print("File $fileName not found")
        }

}