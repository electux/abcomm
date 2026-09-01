package com.abcomm.protocol

/**
 * Parser converting raw ASCII response frames into strongly-typed DeviceResponse domain objects.
 */
interface ResponseParser {
    fun parse(frame: String): DeviceResponse
}
