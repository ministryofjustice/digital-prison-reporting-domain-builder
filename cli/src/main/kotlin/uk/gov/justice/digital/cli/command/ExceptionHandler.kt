package uk.gov.justice.digital.cli.command

import uk.gov.justice.digital.cli.DomainBuilder

// TODO - make this a base class and define the domain builder property too
object ExceptionHandler {

    fun <T> runAndHandleExceptions(d: DomainBuilder, block: () -> T) {
        try { block() }
        catch (ex: Exception) {
            d.print("""
                
                @|red,bold There was a problem with your request.|@
                
                Please try again later.
                
                Cause: @|red ${ex.localizedMessage}|@
                 
            """.trimIndent())
        }
    }

}
