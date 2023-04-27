package uk.gov.justice.digital.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TemporaryTest {

    @Test
    fun `docker should be on the path`() {
        println("Running 'which docker' to determine if docker is on the path")
        val result = ProcessBuilder("which", "docker")
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor()
        assertEquals(0, result)
    }
}