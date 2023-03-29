package uk.gov.justice.digital

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.Example.generateMessage

class ExampleTest {

    @Test
    fun shouldReturnMessageWithName() {
        assertEquals("Hello Foo!", generateMessage("Foo"))
    }

}