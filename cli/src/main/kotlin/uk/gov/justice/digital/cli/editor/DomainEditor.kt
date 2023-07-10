package uk.gov.justice.digital.cli.editor

import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.utils.Display
import uk.gov.justice.digital.cli.client.BadRequestException
import uk.gov.justice.digital.cli.client.ConflictException
import uk.gov.justice.digital.cli.service.DomainService
import uk.gov.justice.digital.cli.session.InteractiveSession
import uk.gov.justice.digital.model.Mapping
import uk.gov.justice.digital.model.Table
import uk.gov.justice.digital.model.Transform
import uk.gov.justice.digital.model.WriteableDomain
import kotlin.math.max

class DomainEditor(private val session: InteractiveSession, private val service: DomainService) {

    private val keyReaderTimeout = 250L

    private val terminal = session.terminal()

    private val bindingReader: BindingReader = BindingReader(terminal.reader())
    private val terminalSize: Size = Size()
    private val display = Display(terminal, true)
    private val originalAttributes = terminal.attributes

    // Initial position when editing - defaults to first editable field
    private var selectedField = 0

    private var pageElements = emptyPageElements()

    private val inputFieldMargin = pageElements
        .filter { it.canSelect && it is Field }
        .map { it as Field }
        .maxOf { it.name.length }

    private val selectableElementIndexes = pageElements
        .withIndex()
        .filter { it.value.canSelect }
        .map { it.index }

    // Bounds for user cursor up/down
    private val minPosition = 0
    private val maxPosition = selectableElementIndexes.size - 1

    private fun emptyPageElements() = listOf(
        Blank(),
        Heading("Create New Domain", "black", "green"),
        Blank(),
        Heading("Domain Properties", "yellow"),
        Blank(),
        Field("Name", ""),
        Field("Description", ""),
        Field("Location", ""),
        Field("Owner", ""),
        Field("Author", ""),
        Blank(),
        Heading("Table Properties", "yellow"),
        Blank(),
        Field("Name", ""),
        Field("Description", ""),
        Field("Location", ""),
        Field("Owner", ""),
        Field("Author", ""),
        Field("Primary Key", ""),
        Field("Sources", ""),
        MultiLineField("Spark Query", ""),
        Blank(),
        Heading("keys │ ↑ move up │ ↓ move down │ CTRL-s save │ ESC quit │ press enter to edit", "black", "white")
    )

    private fun updateDisplay(input: String? = null) {
        val width = terminalSize.columns

        // By default hide the cursor until the user enters edit mode
        terminal.hideCursor()

        terminal.moveCursorToHome()

        val selectedElementIndex = selectableElementIndexes[selectedField]

        // TODO - review this and use index accessor
        pageElements = pageElements
            .withIndex()
            .map { item ->
                val selected = item.index == selectedElementIndex
                when(val element = item.value) {
                    is Blank -> element
                    is Heading -> element
                    is Field -> element.copy(
                        selected = selected,
                        margin = inputFieldMargin,
                        value = if (selected && input != null) input else element.value
                    )
                    is MultiLineField -> element.copy(
                        selected = selected,
                        margin = inputFieldMargin,
                        value = if (selected && input != null) input else element.value
                    )
                }
            }

        pageElements.forEach { e ->
            val content = e.render(width)
            if (content.isNotEmpty()) println(session.toAnsi(content))
            else println()
        }

        terminal.moveCursorToHome()

        val selectedElement = pageElements[selectedElementIndex]
        val inputLength = if (selectedElement is Field) selectedElement.value.length else 0

        terminal.moveCursorTo(selectedElementIndex, inputLength + 15)

        terminal.flush()
    }



    private fun handleSignal(signal: Terminal.Signal) {
        // Ensure that we only react to the expected signal.
        if (signal == Terminal.Signal.WINCH) {
            terminalSize.copy(terminal.size)
            // TODO - calling updateDisplay here in response to a resize will abort an open TextEditor session
            updateDisplay()
        }
    }

    // Allow ctrl-c to interrupt the command.
    private fun checkInterrupted() {
        Thread.yield()
        if (Thread.currentThread().isInterrupted) {
            throw InterruptedException()
        }
    }

    private fun resetState() {
        pageElements = emptyPageElements()
        selectedField = 0
    }

    private fun enableRawMode() {
        terminal.enterRawMode()
    }

    private fun disableRawMode() {
        terminal.attributes = originalAttributes
    }

    fun run() {
        resetState()
        terminal.clearDisplay()

        val keys = bindKeys()

        terminalSize.copy(terminal.size)
        terminal.handle(Terminal.Signal.WINCH, this::handleSignal)
        display.reset()

        enableRawMode()

        updateDisplay()

        while(true) {
            checkInterrupted()

            when (bindingReader.readBinding(keys, null, true)) {
                Operation.EXIT -> {
                    handleExit()
                    break
                }
                Operation.UP -> updatePositionAndRefreshDisplay(-1)
                Operation.DOWN -> updatePositionAndRefreshDisplay(1)
                Operation.EDIT -> {
                    val selectedElementIndex = selectableElementIndexes[selectedField]
                    val selectedElement = pageElements[selectedElementIndex]

                    if (selectedElement is MultiLineField) handleMultiLineFieldEdit(selectedElement)
                    else if (selectedElement is Field) handleFieldEdit(selectedElement)
                }
                Operation.SAVE -> {
                    // a result of true indicates success so quit the loop if true
                    if (handleSave()) break
                }
                else -> terminal.bell()
            }
        }

        terminal.clearDisplay()
        terminal.showCursor()
        terminal.flush()
        disableRawMode()
    }


    private fun updatePositionAndRefreshDisplay(i: Int) {
        // Ensure the updated value remains within the bounds minPosition < position < maxPosition
        val newPosition = Integer.min(maxPosition, max(minPosition, selectedField + i))
        // Only update the display if the position has changed
        if (newPosition != selectedField) {
            selectedField = newPosition
            updateDisplay()
        }
    }

    private fun handleMultiLineFieldEdit(selectedElement: MultiLineField) {
        val input = TextEditor(session, "Editing Spark SQL Query").run(selectedElement.value)
        updateDisplay(input)
    }

    private fun updateStatusLine(status: String, fgColor: String = "black", bgColor: String = "white") {
        terminal.saveCursorPosition()
        terminal.moveCursorTo(23, 0) // Start of status line

        val padding = " ".repeat(terminal.width - status.length - 2)

        print(session.toAnsi("@|fg($fgColor),bg($bgColor),bold  $status$padding|@"))

        terminal.restoreSavedCursorPosition()
        terminal.flush()
    }

    private fun handleFieldEdit(selectedElement: Field) {
        disableRawMode()

        updateStatusLine("Editing ${selectedElement.name.lowercase()} │ press enter to save changes")

        val inputLength = selectedElement.value.length

        // Move the cursor to the start of the value string before we accept input since it will be in the
        // wrong place if we already have a value (we default to placing the cursor at the end of the string).
        terminal.moveCursorLeft(inputLength)

        val lineReader = LineReaderBuilder.builder()
            .terminal(this.terminal)
            .build()

        val currentValue = selectedElement.value

        // Since we need to set the prompt to a single space we need to shift the cursor left on char before
        // we accept user input otherwise the cursor will not be properly aligned.
        terminal.moveCursorLeft(1)
        // Now make the cursor visible
        terminal.showCursor()

        val input = lineReader.readLine(" ", null, currentValue)

        // Hide the cursor again as soon as the user has hit enter.
        terminal.hideCursor()

        enableRawMode()
        updateDisplay(input)
    }

    private fun handleSave(): Boolean {

        updateStatusLine("Saving domain...", bgColor = "yellow")

        val newDomain = WriteableDomain(
            name = pageElements[5].fieldValue(),
            description = pageElements[6].fieldValue(),
            location = pageElements[7].fieldValue(),
            owner = pageElements[8].fieldValue(),
            author = pageElements[9].fieldValue(),
            version = "1.0.0",
            tags = emptyMap(), // TODO - support tags
            // TODO - for now we only support a single table
            tables = listOf(
               Table(
                   name = pageElements[13].fieldValue(),
                   description = pageElements[14].fieldValue(),
                   location = pageElements[15].fieldValue(),
                   owner = pageElements[16].fieldValue(),
                   author = pageElements[17].fieldValue(),
                   version = "1.0.0",
                   tags = emptyMap(), // TODO - support tags
                   primaryKey = pageElements[18].fieldValue(),
                   transform = Transform(
                       sources = pageElements[19].fieldValue().split(","), // We assume csv values
                       viewText = pageElements[20].multiLineFieldValue()
                   ),
                   mapping = Mapping("Should this be optional instead?")
               )
            )
        )

        disableRawMode()

        val lineReader = LineReaderBuilder.builder()
            .terminal(this.terminal)
            .build()

        try {
            service.createDomain(newDomain)
            updateStatusLine("Domain created successfully │ Press enter to continue", bgColor = "green")
            lineReader.readLine()
            return true
        }
        catch (brx: BadRequestException) {
            terminal.bell()
            updateStatusLine("Create failed - Check that all data has been provided │ Press enter to return to editor", bgColor = "red", fgColor = "white")
            lineReader.readLine()
            return false
        }
        catch (cx: ConflictException) {
            terminal.bell()
            updateStatusLine("Create failed - Check that all data has been provided │ Press enter to return to editor", bgColor = "red", fgColor = "white")
            updateStatusLine("This domain name is already in use │ Press enter to return to editor", bgColor = "red", fgColor = "white")
            lineReader.readLine()
            return false
        }
        catch (ex: Exception) {
            terminal.bell()
            updateStatusLine("Create failed - Check that all data has been provided │ Press enter to return to editor", bgColor = "red", fgColor = "white")
            updateStatusLine("Create failed due to unexpected error │ Press enter to return to continue", bgColor = "red", fgColor = "white")
            lineReader.readLine()
            return false
        }
        finally {
            enableRawMode()
            updateDisplay()
        }
    }

    private fun Element.fieldValue(): String = (this as Field).value
    private fun Element.multiLineFieldValue(): String = (this as MultiLineField).value

    private fun handleExit() {
        terminal.clearDisplay()
        terminal.showCursor()
        disableRawMode()
    }

    private enum class Operation {
        EXIT,
        UP,
        DOWN,
        EDIT,
        SAVE
    }

    private fun bindKeys(): KeyMap<Operation> {
        val map = KeyMap<Operation>()

        map.bind(Operation.EXIT, "\u001B")
        map.bind(Operation.UP, "\u001B[A", "k")
        map.bind(Operation.DOWN, "\u001B[B", "j")
        map.bind(Operation.EDIT, "\r")
        map.bind(Operation.SAVE, KeyMap.ctrl('S'))

        // Set a shorter timeout for ambiguous keys so hitting ESC to exit is more responsive
        map.ambiguousTimeout = keyReaderTimeout

        return map
    }

}
