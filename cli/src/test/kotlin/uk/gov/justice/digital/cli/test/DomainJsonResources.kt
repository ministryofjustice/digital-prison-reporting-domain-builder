package uk.gov.justice.digital.cli.test

import java.lang.IllegalStateException

object DomainJsonResources {

    val validDomain: String by lazy {
        resourceAsString("/domains/valid-domain.json")
    }

    val invalidDomain: String by lazy {
        resourceAsString("/domains/invalid-domain.json")
    }

    private fun resourceAsString(path: String): String =
        this::class.java.getResourceAsStream(path)?.readBytes()?.decodeToString()
            ?: throw IllegalStateException("Failed to read resource: $path")

}