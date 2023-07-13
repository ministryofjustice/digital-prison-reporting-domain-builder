package uk.gov.justice.digital.cli.editor

import org.jline.keymap.BindingReader
import org.jline.keymap.KeyMap
import org.jline.utils.InfoCmp.Capability
import uk.gov.justice.digital.cli.editor.TextEditor.Operation.*
import uk.gov.justice.digital.cli.session.InteractiveSession
import kotlin.math.min

/**
 * A simple text editor that provides enough functionality to allow a user to enter sql queries, tags etc..
 *
 * There are some limitations with regard to how text wrapping is handled as follows
 *   o pressing enter does not insert a new line or break the current line at the cursor location
 *   o deletion will only move any trailing text up to the next line if there is space for the entire string
 *   o maximum number of lines hardcoded to 20 lines (this can easily be made dynamic if required)
 */
class TextEditor(private val session: InteractiveSession, private val heading: String) {

    private val keyReaderTimeout = 250L

    private val terminal = session.terminal()

    private val lineOffset = 4 // Top three lines of the screen are not editable
    // Editor bounds. Can be made dynamic by taking width/height from the terminal.
    private val minLine = 0
    private val maxLine = 20
    private val minColumn = 0
    private val maxColumn = terminal.width

    private val tabLength = 4 // Tabs will be converted into this number of spaces

    // Initial empty text area to be edited.
    private val lines = Array(maxLine) { _ -> "" }.toMutableList()

    // Current cursor position where currentLine is relative to lineOffset.
    private var currentLine = 0
    private var currentColumn = 0

    fun run(text: String? = null): String {

        val originalAttributes = terminal.enterRawMode()

        terminal.showCursor()
        terminal.clearDisplay()

        terminal.flush()

        val padding = " ".repeat(terminal.width - heading.length - 1)

        println()
        println(session.toAnsi("@|fg(black),bg(cyan),bold  $heading$padding|@"))
        println()

        val editReader = BindingReader(terminal.reader())

        val map = bindKeys()

        // Populate lines with existing data where defined
        text?.let {
            text.split("\n")
                .withIndex()
                .forEach { lines[it.index] = it.value }
        }

        lines.forEach { println(it) }

        terminal.moveCursorUp(lines.size)

        terminal.flush()

        terminal.saveCursorPosition()
        terminal.moveCursorTo(23, 0)

        val statusLine = "keys │ ↑ move up │ ↓ move down │ ← move left │ → move right │ CTRL-S save │ ESC exit"
            .take(terminal.width - 2)
        val statusPadding = " ".repeat(terminal.width - min(statusLine.length, terminal.width - 1))
        print(session.toAnsi("@|fg(black),bg(white),bold  $statusLine$statusPadding|@"))

        terminal.restoreSavedCursorPosition()
        terminal.flush()

        while(true) {
            when (editReader.readBinding(map, null, true)) {
                UP      -> handleUp()
                DOWN    -> handleDown()
                LEFT    -> handleLeft()
                RIGHT   -> handleRight()
                INSERT  -> handleInsert(editReader.lastBinding)
                ENTER   -> handleEnter()
                DELETE  -> handleDelete()
                EXIT    -> return text ?: ""
                ACCEPT  -> break
                else    -> terminal.bell()
            }
            terminal.flush()
        }

        // Restore original terminal settings which will disable raw mode.
        terminal.attributes = originalAttributes

        return lines.filter { it.isNotEmpty() }.joinToString("\n")
    }

    private fun handleUp() {
        if (currentLine > minLine) {
            terminal.moveCursorUp(1)
            currentLine--
            val line = lines[currentLine]

            if (currentColumn > line.length) {
                print("\u001B[0G")
                print("\u001B[${line.length + 1}G")
                currentColumn = line.length
            }
        }
    }

    private fun handleDown() {
        if ((currentLine < lines.filter { it.isNotEmpty() }.size - 1) && lines[currentLine].isNotEmpty()) {
            currentLine++
            terminal.moveCursorDown(1)
            terminal.flush()
            // When moving from a longer line to a shorter line ensure the cursor is at the end of the line.
            currentColumn = Integer.min(currentColumn, lines[currentLine].length)
            terminal.moveCursorTo(currentLine + lineOffset, currentColumn)
            terminal.flush()
        }
    }

    private fun handleLeft() {
        if (currentColumn > minColumn) {
            terminal.moveCursorLeft(1)
            currentColumn--
        }
    }

    private fun handleRight() {
        if (currentColumn < maxColumn && currentColumn < lines[currentLine].length) {
            terminal.moveCursorRight(1)
            currentColumn++
        }
    }

    private fun handleInsert(lastBinding: String) {
        // Remap tab to spaces
        val input = if (lastBinding == "\t") " ".repeat(tabLength) else lastBinding
        input.forEach { c ->
            if (currentColumn < maxColumn - 1) {
                val line = lines[currentLine]
                if (currentColumn == line.length) {
                    lines[currentLine] = line + c
                    print(c)
                    currentColumn++
                } else {
                    val charsRemaining = line.substring(currentColumn).length
                    lines[currentLine] = line.substring(0, currentColumn) +
                        c +
                        line.substring(currentColumn)
                    print(c)
                    print(line.substring(currentColumn))
                    currentColumn++

                    terminal.moveCursorLeft(charsRemaining)

                    terminal.flush()
                }
            }
            else if (currentLine < maxLine) {
                terminal.moveCursorDown(1)
                print("\r")
                currentLine++
                lines[currentLine] = c + lines[currentLine]
                print(lines[currentLine])
                terminal.moveCursorLeft(lines[currentLine].length - 1)
                terminal.flush()
                currentColumn = 1
            }
        }
    }

    private fun handleEnter() {
        if (currentLine < maxLine) {
            terminal.moveCursorDown(1)
            print("\r")
            currentColumn = 0
            currentLine++
        }
    }

    private fun handleDelete() {
        val line = lines[currentLine]

        if (line.isNotEmpty() && currentColumn > 0) {
            lines[currentLine] = line.removeRange(currentColumn - 1, currentColumn)
            terminal.moveCursorLeft(1)
            terminal.puts(Capability.delete_character)
            currentColumn--
        } else if (currentLine > minLine) {
            // Only allow the text on the current line to be moved to the line above if there is space.
            if (line.length + lines[currentLine-1].length < terminal.width) {
                // Clear the current line since the contents will be moved up to the end of line above.
                lines[currentLine] = ""
                terminal.clearLine()

                currentLine--

                // Move the cursor up to the end of the line above.
                terminal.moveCursorUp(1)
                terminal.moveCursorRight(lines[currentLine].length)
                currentColumn = lines[currentLine].length

                // Print the contents of the line we started on and move the cursor back to where it was...
                print(line)
                terminal.moveCursorLeft(line.length)
                // ...and append the original line to the end of the current line.
                lines[currentLine] = lines[currentLine] + line
            }
            else terminal.bell() // Cannot much current line content to line above - not enough space.
        }
    }

    enum class Operation {
        UP,
        DOWN,
        RIGHT,
        LEFT,
        INSERT,
        ENTER,
        DELETE,
        EXIT,
        ACCEPT,
    }

    private fun bindKeys(): KeyMap<Operation> {
        val map = KeyMap<Operation>()
        map.bind(UP, "\u001B[A")
        map.bind(DOWN, "\u001B[B")
        map.bind(RIGHT, "\u001B[C")
        map.bind(LEFT, "\u001B[D")
        map.bind(ENTER, "\r")
        map.bind(DELETE, "\b")
        map.bind(EXIT, "\u001B")
        map.bind(ACCEPT, KeyMap.ctrl('S'))

        // Allow user to enter characters which are passed to the insert handler.
        for (i in 32..255) {
            // Only bind to insert if the char is not DEL (ASCII 127)
            if (i != 127) map.bind(INSERT, Character.toString(i))
            else map.bind(DELETE, Character.toString(i))
        }

        // Also bind the tab character to insert
        map.bind(INSERT, Character.toString(9))

        // Set a shorter timeout for ambiguous keys so hitting ESC to exit is more responsive
        map.ambiguousTimeout = keyReaderTimeout

        return map
    }

}