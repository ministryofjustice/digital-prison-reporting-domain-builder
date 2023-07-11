package uk.gov.justice.digital.backend.filter

import io.micronaut.context.annotation.Value
import io.micronaut.core.async.publisher.Publishers
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.annotation.Filter.MATCH_ALL_PATTERN
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.headers.Header.Companion.API_KEY_HEADER_NAME
import java.util.function.Function

@Filter(MATCH_ALL_PATTERN)
class ApiKeyFilter : HttpServerFilter {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Value("\${auth.apiKey}")
    private lateinit var apiKey: String

    override fun doFilter(request: HttpRequest<*>?, chain: ServerFilterChain?): Publisher<MutableHttpResponse<*>> {
        val apiKeyIsValid = request?.headers?.get(API_KEY_HEADER_NAME)?.let {
            logger.info("Checking key: $it")
            it == apiKey
        } ?: false

        return if (apiKeyIsValid) {
            logger.info("Incoming request has a valid {} header", API_KEY_HEADER_NAME)
            Publishers.map(chain?.proceed(request), Function.identity())
        }
        else {
            logger.warn("Incoming request has an invalid {} header with value '{}'", API_KEY_HEADER_NAME, apiKey)
            Publishers.just(HttpResponse.unauthorized<Unit>())
        }
    }

}