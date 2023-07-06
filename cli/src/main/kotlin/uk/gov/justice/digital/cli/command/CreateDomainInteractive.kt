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
import java.lang.Integer.min
import kotlin.math.max

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
    fun render(width: Int): String
}

class Blank: Element {
    override val canSelect = false
    override fun render(width: Int): String { return " ".repeat(width) }
}

// TODO - consider providing a prompt method here that can be used for generating the list view and for edits.
data class InputField(val name: String,
                      val value: String,
                      val selected: Boolean = false,
                      val margin: Int = name.length): Element {
    override val canSelect = true
    override fun render(width: Int): String {
        val formattedName = String.format("%-${margin}s",  name)
        // TODO - fix these magic numbers
        val padding = " ".repeat(width - margin - 36)
        // TODO - the 30 padding here needs to be specified elsewhere
        val formattedValue = String.format("%-30s%s", value, padding)
        return if (selected) "@|bold  $formattedName |@│ @|underline $formattedValue |@"
            else "@|bold,faint  $formattedName |@│ $formattedValue"

    }
}

data class Heading(val heading: String, val color: String, val backgroundColor: String? = null): Element {
    override val canSelect = false
    override fun render(width: Int): String {
        val bgString = backgroundColor?.let { ",bg($it)" } ?: ""
        val padding = " ".repeat(width - heading.length - 1)
        return "@|fg($color)$bgString,bold  $heading$padding|@"
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
    // TODO - rename this - editable field position
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

    // TODO - tags, allow key value pair entry / deletion
    // TODO - the following fits into an 80 x 24 window - if more entries are required we will need to handle this
    //        by defining a window or offset to only show those values that fit as the user moves up and down.
    private fun emptyPageElements() = listOf(
        Blank(),
        Heading("Create New Domain", "black", "green"),
        Blank(),
        Heading("Domain Properties", "yellow"),
        Blank(),
        InputField("Name", ""),
        InputField("Description", ""),
        InputField("Location", ""),
        InputField("Owner", ""),
        InputField("Author", ""),
        Blank(),
        Heading("Table Properties", "yellow"),
        Blank(),
        InputField("Name", ""),
        InputField("Description", ""),
        InputField("Location", ""),
        InputField("Owner", ""),
        InputField("Author", ""),
        InputField("Primary Key", ""),
        InputField("Sources", ""),
        InputField("Spark Query", ""),
        Blank(),
        // TODO - use some sort of dynamic status line?
        Heading("Use cursor up/down keys to select field. Press enter to edit. Press q to Quit", "black", "white"),
    )

    private fun updateDisplay(input: String? = null) {
        val width = terminalSize.columns
        // TODO - handle case where there's too much output to display.
        val height = terminalSize.rows

        clearDisplay()

        // Update state - TODO - separate method
        // TODO - clean this up
        val selectedElementIndex = selectableElementIndexes[position]
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
                                margin = inputFieldMargin ?: 20, // TODO - define default elsewhere
                                value = if (selected && input != null) input else element.value
                        )
                    }
                }

        pageElements.forEach { e ->
            val content = e.render(width)
            if (content.isNotEmpty()) println(session.toAnsi(content))
            else println()
        }

        // Reset cursor position and move to the currently selected element
        terminal.puts(Capability.cursor_home)

        // TODO - should be possible to do these moves in one go by specifying repetitions in the escape sequence.
        for (i in 1..selectedElementIndex) { terminal.puts(Capability.cursor_down) }
        // TODO - don't use hardcoded margin value
        // TODO - cursor should appear at end of input where present
        val selectedElement = pageElements[selectedElementIndex]
        val inputLength = if (selectedElement is InputField) selectedElement.value.length else 0
        // TODO - should be possible to do these moves in one go by specifying repetitions in the escape sequence.
        for (i in 1..(15 + inputLength)) { terminal.puts(Capability.cursor_right) }

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
                    // TODO - provide a method to get the current input field or null?
                    val selectedElementIndex = selectableElementIndexes[position]
                    val selectedElement = pageElements[selectedElementIndex] as? InputField

                    // For dumb terminals, we need to make sure that CR are ignored
                    val attr = Attributes(originalAttributes)
                    attr.setInputFlag(Attributes.InputFlag.ICRNL, true)
                    terminal.attributes = attr

                    val inputLength = selectedElement?.value?.length ?: 0

                    // Move the cursor to the start of the value string before we accept input since it will be in the
                    // wrong place if we already have a value (we default to placing the cursor at the end of the string).
                    for (i in 1..(inputLength)) { terminal.puts(Capability.cursor_left) }

                    // TODO - can we set a margin to prevent deletion of the labels?
                    val lineReader = LineReaderBuilder.builder()
                        .terminal(this.terminal)
                        .build()

                    // TODO - this code is a bit messy - provide helper methods
                    val currentValue = selectedElement?.value ?: ""

                    // TODO - better way to do this? We set prompt to ' ' so we need to move the cursor one char
                    //        left before initiating edit.
                    terminal.puts(Capability.cursor_left)

                    val input = lineReader.readLine(" ", null, currentValue)

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
        // Ensure the updated value remains within the bounds minPosition < position < maxPosition
        // TODO - check kotlin syntax - does it provide anything to express this?
        val newPosition = min(maxPosition, max(minPosition, position + i))
        // Only update the display if the position has changed
        if (newPosition != position) {
            position = newPosition
            updateDisplay()
        }
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