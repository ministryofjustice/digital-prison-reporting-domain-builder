package uk.gov.justice.digital.backend.filter

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.annotation.Filter.MATCH_ALL_PATTERN
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.filter.ServerFilterPhase
import org.reactivestreams.Publisher
import org.slf4j.MDC
import uk.gov.justice.digital.headers.Header
import java.util.*

@Filter(MATCH_ALL_PATTERN)
class TraceIdFilter : HttpServerFilter {

    override fun getOrder(): Int = ServerFilterPhase.TRACING.after()

    override fun doFilter(request: HttpRequest<*>?, chain: ServerFilterChain?): Publisher<MutableHttpResponse<*>> {
        val traceId = request?.headers?.get(Header.TRACE_ID_HEADER_NAME) ?: UUID.randomUUID().toString()
        val sessionId = request?.headers?.get(Header.SESSION_ID_HEADER_NAME)
        MDC.put(TRACE_ID, traceId)
        // If we have a session ID then stash this on the MDC too
        request?.headers?.get(Header.SESSION_ID_HEADER_NAME)?.let { MDC.put(SESSION_ID, it) }

        return Publishers.map(chain?.proceed(request), setResponseHeadersAndClearMdc(traceId, sessionId))
    }

    private val setResponseHeadersAndClearMdc: (String, String?) -> (MutableHttpResponse<*>) -> MutableHttpResponse<*> =
        { traceId: String, sessionId: String? ->
            { response ->
                // Set the trace and session id headers on the response.
                response.header(Header.TRACE_ID_HEADER_NAME, traceId)
                sessionId?.let { response.header(Header.SESSION_ID_HEADER_NAME, it) }

                // Clear the MDC
                MDC.remove(TRACE_ID)
                MDC.remove(SESSION_ID)

                response
            }
        }

    companion object {
        const val TRACE_ID = "traceId"
        const val SESSION_ID = "sessionId"
    }

}