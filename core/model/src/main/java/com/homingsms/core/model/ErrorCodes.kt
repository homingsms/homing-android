package com.homingsms.core.model

object ErrorCodes {
    const val GENERIC_FAILURE = "generic_failure"
    const val NO_SERVICE = "no_service"
    const val RADIO_OFF = "radio_off"
    const val NULL_PDU = "null_pdu"
    const val SMS_LIMIT_REACHED = "sms_limit_reached"
    const val SHORT_CODE_NOT_ALLOWED = "short_code_not_allowed"
    const val EXPIRED = "expired"
    const val DELIVERY_FAILED = "delivery_failed"
    const val MMS_FAILED = "mms_failed"
    const val SEND_ERROR = "send_error"
    const val INVALID_RECIPIENT = "invalid_recipient"
    const val SIM_NOT_FOUND = "sim_not_found"

    private val pattern = Regex("""^\[([a-z0-9_]{1,64})] ?(.*)$""", RegexOption.DOT_MATCHES_ALL)

    fun encode(code: String, message: String): String = "[$code] $message"

    fun decode(error: String?): MessageError? {
        if (error.isNullOrBlank()) return null
        val match = pattern.matchEntire(error) ?: return MessageError(GENERIC_FAILURE, error)
        return MessageError(match.groupValues[1], match.groupValues[2])
    }
}

data class MessageError(val code: String, val message: String)
