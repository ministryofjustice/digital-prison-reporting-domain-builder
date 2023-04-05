package uk.gov.justice.digital

import io.micronaut.configuration.picocli.MicronautFactory
import io.micronaut.configuration.picocli.PicocliRunner
import jakarta.inject.Singleton
import org.fusesource.jansi.AnsiConsole
import org.jline.console.SystemRegistry
import org.jline.console.impl.DefaultPrinter
import org.jline.console.impl.SystemRegistryImpl
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader.*
import org.jline.reader.LineReaderBuilder
import org.jline.reader.Parser
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.widget.AutosuggestionWidgets
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.shell.jline3.PicocliCommands
import uk.gov.justice.digital.command.CommandBase
import uk.gov.justice.digital.command.ListDomains
import uk.gov.justice.digital.command.ViewDomain
import java.io.PrintWriter

@Command(
    name = "domain-builder",
    subcommands = [
        ListDomains::class,
        ViewDomain::class,
    ],
)
@Singleton
class DomainBuilder : CommandBase(), Runnable {

    @Option(
        names = ["-i", "--interactive"],
        description = ["Run domain-builder in interactive mode"],
        required = false
    )
    var interactive = false

    lateinit var out: PrintWriter
    lateinit var terminal: Terminal

    override fun getPrintWriter(): PrintWriter {
        return out
    }

    var terminalWidth: Int = 0
    var terminalHeight: Int = 0

    override fun run() {
        if (interactive) {
            AnsiConsole.systemInstall()

            try {
                TerminalBuilder.builder()
                    .build()
                    .use(this::interactiveSession)
            } finally {
                AnsiConsole.systemUninstall()
            }
        }
    }

    private fun interactiveSession(terminal: Terminal) {
        out = terminal.writer()
        this.terminal = terminal

        printAnsi(launchText)

        val commandLine = CommandLine(this, MicronautFactory())

        val parser: Parser = DefaultParser()
        val systemRegistry: SystemRegistry = SystemRegistryImpl(parser, terminal, null, null)

        systemRegistry
            .setCommandRegistries(PicocliCommands(commandLine))

        val reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(systemRegistry.completer())
            .parser(parser)
            .variable(LIST_MAX, tabCompletionMaxCandidates)
            .variable(HISTORY_FILE, historyFileLocation)
            .variable(HISTORY_SIZE, historySize)
            .variable(HISTORY_FILE_SIZE, historySize)
            .build()

        // Enable tab completion.
        AutosuggestionWidgets(reader).enable()

        // Start the interactive shell and process input until the user types exit or hits CTRL-D.
        while (true) {
            try {
                terminalWidth = terminal.width
                terminalHeight = terminal.height

                systemRegistry.cleanUp()
                systemRegistry.execute(reader.readLine(prompt))
            }
            catch (e: EndOfFileException) {
                break
            }
            catch (e: Exception) {
                systemRegistry.trace(e)
                break
            }
        }

    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            System.setProperty("picocli.ansi", "true")
            // TODO - is there a better way to launch help?
            if (args.isEmpty()) {
                val fakeArgs = arrayOf("--help")
                PicocliRunner.execute(DomainBuilder::class.java, *fakeArgs)
            }
            else PicocliRunner.execute(DomainBuilder::class.java, *args)
        }

        private const val prompt = "domain-builder> "
        private const val tabCompletionMaxCandidates = 50
        private val historyFileLocation = "${System.getProperty("user.home")}/.domain-builder_history"
        private const val historySize = 100

        private val launchText = """
        
            @|bold,cyan  ____                                  ____          _     _               |@
            @|bold,cyan |  _ \  ___  _ __ ___   __ _(_)_ __   | __ ) _   _(_) | __| | ___ _ __     |@
            @|bold,cyan | | | |/ _ \| '_ ` _ \ / _` | | '_ \  |  _ \| | | | | |/ _` |/ _ \ '__|    |@
            @|bold,cyan | |_| | (_) | | | | | | (_| | | | | | | |_) | |_| | | | (_| |  __/ |       |@
            @|bold,cyan |____/ \___/|_| |_| |_|\__,_|_|_| |_| |____/ \__,_|_|_|\__,_|\___|_|       |@
    
            Type @|bold help|@ to view available commands.
            
            Type @|bold <command> --help|@ to view help for a specific command.
            
            Type @|bold exit|@ to exit the domain builder.
            
            Press the @|bold TAB|@ key to view available commands or autocomplete commands as you type.
        
        """.trimIndent()

    }

}
