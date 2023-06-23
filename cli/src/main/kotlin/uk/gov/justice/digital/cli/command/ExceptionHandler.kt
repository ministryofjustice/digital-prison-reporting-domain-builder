package uk.gov.justice.digital.cli.command

import uk.gov.justice.digital.cli.DomainBuilder
import uk.gov.justice.digital.cli.client.BadRequestException
import uk.gov.justice.digital.cli.client.ConflictException
import uk.gov.justice.digital.cli.service.JsonParsingFailedException

object ExceptionHandler {

    fun <T> runAndHandleExceptions(d: DomainBuilder, block: () -> T) {
        try { block() }
        catch (cx: ConflictException) {
            d.print("""
                
                @|red,bold Could not create new domain|@
                
                @|white,bold ${cx.localizedMessage}|@
                
                @|blue,bold Possible fixes|@
                
                1. Use a different domain name
                2. Use a different domain status
                
                Note: If the DRAFT status is causing a conflict consider amending 
                      the existing domain draft instead
                 
            """.trimIndent())
        }
        catch (jpx: JsonParsingFailedException) {
            d.print("""
                
                @|red,bold Error: Could not create new domain|@
                
                @|white,bold Cause: ${jpx.localizedMessage}|@
                
                @|blue,bold Possible fixes|@
                
                1. Read the cause above since it will usually describe the
                   problem with the JSON and what needs to be done to fix it
                2. Ensure that your JSON is syntactically valid
                3. Ensure that all mandatory fields have been given a value
                4. Ensure that the status value is fully capitalised
                
                
            """.trimIndent())
        }
        catch (brx: BadRequestException) {
            d.print("""
                
                @|red,bold Could not create new domain|@
                
                @|white,bold ${brx.localizedMessage}|@
                 
            """.trimIndent())
        }
        catch (ex: Exception) {
            println("Caught exception: $ex")
            d.print("""
                
                @|red,bold There was a problem with your request|@
                
                Please try again later.
                
                Cause: @|red ${ex.localizedMessage}|@
                 
            """.trimIndent())
        }
    }

}
