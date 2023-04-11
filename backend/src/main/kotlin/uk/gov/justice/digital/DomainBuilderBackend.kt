package uk.gov.justice.digital

import io.micronaut.runtime.Micronaut

object DomainBuilderBackend {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.run(DomainBuilderBackend::class.java)
    }

}