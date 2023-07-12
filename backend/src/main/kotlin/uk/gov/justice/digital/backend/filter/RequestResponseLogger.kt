package uk.gov.justice.digital.backend.filter

import io.micronaut.core.async.publisher.Publishers
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.MutableHttpResponse
import io.micronaut.http.annotation.Filter
import io.micronaut.http.annotation.Filter.MATCH_ALL_PATTERN
import io.micronaut.http.filter.HttpServerFilter
import io.micronaut.http.filter.ServerFilterChain
import io.micronaut.http.filter.ServerFilterPhase
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory

@Filter(MATCH_ALL_PATTERN)
class RequestResponseLogger : HttpServerFilter {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun getOrder(): Int = ServerFilterPhase.FIRST.before()

    override fun doFilter(request: HttpRequest<*>?, chain: ServerFilterChain?): Publisher<MutableHttpResponse<*>> {

        val startMs = System.currentTimeMillis()

        logger.info("Received request method:{} uri:{} headers:{} from:{}",
            request?.method,
            request?.uri,
            request?.headers?.joinToString { entry -> "${entry.key}=${entry.value}" },
            request?.remoteAddress
        )

        val response = chain?.proceed(request)

        return Publishers.map(response, logResponse(startMs))
    }

    private val logResponse: (Long) -> (MutableHttpResponse<*>) -> MutableHttpResponse<*> = { startMs ->
        { response ->
            logger.info("Returning response status:{} durationMs:{} contentLength:{} headers:{}",
                response.code(),
                System.currentTimeMillis() - startMs,
                response.getBody(Argument.ofTypeVariable(String::class.java, "body")).map { it.length }.orElse(0),
                response.headers.joinToString { entry -> "${entry.key}=${entry.value}" },
            )
            response
        }
    }

}