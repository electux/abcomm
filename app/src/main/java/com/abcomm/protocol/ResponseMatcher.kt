package com.abcomm.protocol

/**
 * Pattern matcher evaluating raw frame text and returning the corresponding DeviceResponse model if matched.
 */
interface ResponseMatcher {
    fun match(frame: String): DeviceResponse?
}
