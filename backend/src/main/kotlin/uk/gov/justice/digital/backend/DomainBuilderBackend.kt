package uk.gov.justice.digital.backend

import io.micronaut.runtime.Micronaut

object DomainBuilderBackend {

    @JvmStatic
    fun main(args: Array<String>) {
        Micronaut.build(*args)
            .mainClass(DomainBuilderBackend::class.java)
            .environmentPropertySource(true)
            .environmentVariableIncludes(
                "POSTGRES_HOST",
                "POSTGRES_PORT",
                "POSTGRES_DB_NAME",
                "POSTGRES_USERNAME",
                "POSTGRES_PASSWORD"
            )
            .start()
    }

}