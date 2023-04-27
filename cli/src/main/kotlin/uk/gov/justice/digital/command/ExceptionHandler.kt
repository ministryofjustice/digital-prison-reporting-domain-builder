package uk.gov.justice.digital.command

import uk.gov.justice.digital.DomainBuilder

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
