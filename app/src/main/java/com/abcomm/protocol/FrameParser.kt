package com.abcomm.protocol

/**
 * Extracts complete '<...>' protocol frames from continuous incoming byte streams.
 */
interface FrameParser {
    fun process(chunk: String): List<String>
    fun reset()
}
