package uk.gov.justice.digital.headers

import java.util.*

interface Header {
    val name: String
    val value: String

    companion object {
        const val TRACE_ID_HEADER_NAME = "x-dpr-trace-id"
        const val SESSION_ID_HEADER_NAME = "x-dpr-session-id"
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