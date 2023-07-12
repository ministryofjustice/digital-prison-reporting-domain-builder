package uk.gov.justice.digital.headers

import java.util.*

interface Header {
    val name: String
    val value: String

    companion object {
        const val TRACE_ID_HEADER_NAME = "x-dpr-trace-id"
        const val SESSION_ID_HEADER_NAME = "x-dpr-session-id"
        const val API_KEY_HEADER_NAME = "x-dpr-api-key"
    }
}

// Trace ID header. Set on each outgoing request. Should be echoed back in server response.
class TraceIdHeader(override val value: String = UUID.randomUUID().toString()): Header {
    override val name = Header.TRACE_ID_HEADER_NAME
}

// Session ID header. Should be constant for the duration of a session. Should be echoed back in server response.
class SessionIdHeader(override val value: String = UUID.randomUUID().toString()): Header {
    override val name = Header.SESSION_ID_HEADER_NAME
    companion object {
        // A single instance available for the duration of a session to be used where a session ID header is required.
        val instance = SessionIdHeader()
    }
}

// API Key header. Set by frontend clients on all requests to the backend which validates the secret key.
// This is an interim measure until we integrate with auth.
class ApiKeyHeader(override val value: String): Header {
    override val name = Header.API_KEY_HEADER_NAME
}