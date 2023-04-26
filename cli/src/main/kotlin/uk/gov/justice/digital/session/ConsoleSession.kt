package uk.gov.justice.digital.session

import jakarta.inject.Singleton
import org.fusesource.jansi.AnsiConsole
import org.jline.builtins.Less
import org.jline.builtins.Source
import org.jline.console.SystemRegistry
import org.jline.console.impl.SystemRegistryImpl
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.Parser
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.widget.AutosuggestionWidgets
import picocli.CommandLine
import picocli.shell.jline3.PicocliCommands
import java.io.ByteArrayInputStream

interface ConsoleSession {

    fun print(s: String)

    fun toAnsi(s: String): String = CommandLine.Help.Ansi.AUTO.string(s)

}

@Singleton
class BatchSession: ConsoleSession {

    override fun print(s: String) {
        println(toAnsi(s))
    }

}

@Singleton
class InteractiveSession: ConsoleSession {

    private val pagerText = "use arrow keys to move up and down | press q to exit this view | press h for help"

    private lateinit var terminal: Terminal

    private val less by lazy {
        val l = Less(terminal, null)
        // Do not show pager if output will fit on the current screen
        l.quitIfOneScreen = true
        l
    }

    override fun print(s: String) {
        less.run(Source.InputStreamSource(ByteArrayInputStream(toAnsi(s).toByteArray()), true, pagerText))
    }

    fun start(commandLine: CommandLine) {
        AnsiConsole.systemInstall()

        try {
            this.terminal = TerminalBuilder.builder().build()
            this.terminal.use { interactiveSession(commandLine, it) }
        } finally {
            AnsiConsole.systemUninstall()
        }
    }

    private fun interactiveSession(commandLine: CommandLine, terminal: Terminal) {
        print(launchText)

        val parser: Parser = DefaultParser()
        val systemRegistry: SystemRegistry = SystemRegistryImpl(parser, terminal, null, null)

        systemRegistry
            .setCommandRegistries(PicocliCommands(commandLine))

        val reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(systemRegistry.completer())
            .parser(parser)
            .variable(LineReader.LIST_MAX, tabCompletionMaxCandidates)
            .variable(LineReader.HISTORY_FILE, historyFileLocation)
            .variable(LineReader.HISTORY_SIZE, historySize)
            .variable(LineReader.HISTORY_FILE_SIZE, historySize)
            .build()

        // Enable tab completion.
        AutosuggestionWidgets(reader).enable()

        // Start the interactive shell and process input until the user types exit or hits CTRL-D.
        while (true) {
            try {
                systemRegistry.cleanUp()
                systemRegistry.execute(reader.readLine(prompt))
            }
            catch (e: UserInterruptException) {
                // Ignore this exception to allow the session to continue.
            }
            catch (e: EndOfFileException) {
                break
            }
            catch (e: Exception) {
                systemRegistry.trace(e)
            }
        }

    }

    companion object {
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
            
            Press the @|bold TAB ->||@ key to view available commands or autocomplete commands as you type.
            
            
        """.trimIndent()
    }

}