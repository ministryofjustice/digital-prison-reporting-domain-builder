package uk.gov.justice.digital.cli.command

import jakarta.inject.Singleton
import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Attributes
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.Terminal.Signal
import org.jline.utils.Curses
import org.jline.utils.Display
import org.jline.utils.InfoCmp.Capability
import picocli.CommandLine.Command
import picocli.CommandLine.ParentCommand
import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.cli.session.ConsoleSession

@Singleton
@Command(
        name = "create-interactive",
        description = ["Create a new domain"],
)
class CreateDomainInteractive(private val service: DomainService) : Runnable {

    @ParentCommand
    lateinit var parent: DomainBuilder

    // TODO - revise to pass in the session only
    private val editor: DomainEditor by lazy {
        DomainEditor(parent.getInteractiveSession().terminal(),
                     parent.getInteractiveSession())
    }

    // TODO - this needs to reinit the form data
    override fun run() {
        editor.run()
    }

}

sealed interface Element {
    val canSelect: Boolean
    fun render(): String
}
class Blank: Element {
    override val canSelect = false
    override fun render(): String { return "" }
}
data class InputField(val name: String,
                      val value: String,
                      val selected: Boolean = false,
                      val margin: Int = name.length): Element {
    override val canSelect = true
    override fun render(): String {
        val formattedName = String.format("%-${margin}s",  name)
        val formattedValue = String.format("%-30s", value)
        return if (selected) "@|bold $formattedName |@| @|bg(yellow),fg(black) $formattedValue |@"
            else "@|bold $formattedName |@| $formattedValue"

    }
}
data class Heading(val heading: String, val color: String): Element {
    override val canSelect = false
    override fun render(): String {
        return "@|$color,bold $heading|@"
    }
}

// This is modelled after the Less command which shows how we can take over the console and handle user key presses
// directly.
class DomainEditor(private val terminal: Terminal,
                   private val session: ConsoleSession) {
    private val bindingReader: BindingReader = BindingReader(terminal.reader())
    private val terminalSize: Size = Size()
    private val display = Display(terminal, true)

    // Initial position when editing - defaults to first field
    private var position = 0

    private var pageElements = emptyPageElements()

    private val inputFieldMargin = pageElements
            .filter { it.canSelect && it is InputField }
            .maxOfOrNull { if (it is InputField) it.name.length else 0 }

    private val selectableElementIndexes = pageElements
            .withIndex()
            .filter { it.value.canSelect }
            .map { it.index }

    // Bounds for user cursor up/down
    private val minPosition = 0
    private val maxPosition = selectableElementIndexes.size - 1

    private fun emptyPageElements() = listOf(
        Blank(),
        Heading("Create New Domain", "green"),
        Blank(),
        Heading("Domain Properties", "yellow"),
        Blank(),
        InputField("Name", ""),
        InputField("Description", ""),
        InputField("Location", ""),
        // TODO - tags, allow key value pair entry / deletion
        InputField("Owner", ""),
        InputField("Author", ""),
        Blank(),
        Heading("Table Properties", "yellow"),
        Blank(),
        InputField("Name", ""),
        InputField("Description", ""),
        InputField("Location", ""),
        // TODO - tags, allow key value pair entry / deletion
        InputField("Owner", ""),
        InputField("Author", ""),
        InputField("Primary Key", ""),
        InputField("Sources", ""),
        InputField("Spark Query", ""),
        Blank(),
        Heading("Use cursor up/down keys to move up and down. Press enter to edit a field. Press q to Quit", "white"),
        Blank(),
    )

    private fun updateDisplay(input: String = "") {
        val width = terminalSize.columns
        // TODO - handle case where there's too much output to display.
        val height = terminalSize.rows

        clearDisplay()

        // Update state - TODO - separate method
        val selectedElementIndex = selectableElementIndexes.get(position)
        pageElements = pageElements
                .withIndex()
                .map { item ->
                    val selected = item.index == selectedElementIndex
                    when(val element = item.value) {
                        is Blank -> element
                        is Heading -> element
                        // TODO - fix this - ideally the margin isn't nullable
                        // TODO - cursor position should be updated too
                        is InputField -> element.copy(
                                selected = selected,
                                margin = inputFieldMargin ?: 20,
                                value = if (selected && input.isNotBlank()) input else element.value
                        )
                    }
                }

        pageElements.forEach { e ->
            val content = e.render()
            if (content.isNotEmpty()) println(session.toAnsi(content))
            else println()
        }

        // Reset cursor position and move to the currently selected element
        terminal.puts(Capability.cursor_home)

        // TODO - should be possible to do these moves in one go by specifying repetitions in the escape sequence.
        for (i in 1..selectedElementIndex) { terminal.puts(Capability.cursor_down) }
        // TODO - don't use hardcoded margin value
        for (i in 1..14) { terminal.puts(Capability.cursor_right) }

        terminal.flush()
    }

    // TODO - clearDisplay causes flicker - replace this with fullwidth strings printed for each line on each refresh
    private fun clearDisplay() {
        terminal.puts(Capability.clear_screen)
        terminal.flush()
    }

    private fun handleSignal(signal: Signal) {
        terminalSize.copy(terminal.size)
        updateDisplay()
    }

    // Allow ctrl-c to interrupt the command.
    private fun checkInterrupted() {
        Thread.yield()
        if (Thread.currentThread().isInterrupted) {
            throw InterruptedException()
        }
    }

    // TODO - does anything else need to be reset?
    private fun resetState() {
        pageElements = emptyPageElements()
        position = 0
    }

    fun run() {
        resetState()

        // TODO - can we clean this up using an immutable map instead?
        val keys = KeyMap<Operation>()
        bindKeys(keys)

        terminalSize.copy(terminal.size)
        terminal.handle(Signal.WINCH, this::handleSignal)
        display.reset()

        val originalAttributes = terminal.enterRawMode()
        terminal.flush()
        terminal.writer().flush()

        updateDisplay()

        var continueRunning = true

        do {
            checkInterrupted()

            when (bindingReader.readBinding(keys, null, true)) {
                Operation.EXIT -> {
                    continueRunning = false
                    clearDisplay()
                    terminal.attributes = originalAttributes
                }
                Operation.UP -> updatePositionAndRefreshDisplay(-1)
                Operation.DOWN -> updatePositionAndRefreshDisplay(1)
                // TODO - can we add ESC to abort editing?
                Operation.EDIT -> {
                    // For dumb terminals, we need to make sure that CR are ignored
                    val attr = Attributes(originalAttributes)
                    attr.setInputFlag(Attributes.InputFlag.ICRNL, true)
                    terminal.attributes = attr
                    // TODO - can we set a margin to prevent deletion of the labels?
                    val lineReader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .build()
                    val input = lineReader.readLine()
                    terminal.attributes = originalAttributes
                    terminal.enterRawMode()
                    updateDisplay(input)
                }
                else -> {
                    /** NO-OP **/
                }
            }

        } while (continueRunning)
    }

    private fun updatePositionAndRefreshDisplay(i: Int) {
        val newPosition = position + i
        position = if (newPosition < minPosition) minPosition
            else if (newPosition > maxPosition) maxPosition
            else newPosition
        updateDisplay()
    }

    private fun bindKeys(map: KeyMap<Operation>) {
        map.bind(Operation.EXIT, "q")
        map.bind(Operation.UP, "\u001B[A", "k")
        map.bind(Operation.DOWN, "\u001B[B", "j")
        map.bind(Operation.EDIT, "\r")
    }

    private fun key(capability: Capability): String {
        return Curses.tputs(terminal.getStringCapability(capability))
    }

    private enum class Operation {
        // General
        EXIT,
        // Motion
        UP,
        DOWN,
        // Editing
        EDIT
    }

}