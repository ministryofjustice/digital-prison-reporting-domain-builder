package uk.gov.justice.digital.headers

import java.util.*

interface Header {
    val name: String
    val value: String
}

// Trace ID header. Set on each outgoing request. Should be echoed back in server response.
class TraceIdHeader(override val value: String = UUID.randomUUID().toString()): Header {
    override val name = "x-dpr-trace-id"
}

// Session ID header. Should be constant for the duration of a session. Should be echoed back in server response.
class SessionIdHeader(override val value: String = UUID.randomUUID().toString()): Header {
    override val name = "x-dpr-session-id"
}