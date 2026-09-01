package com.abcomm.protocol

/**
 * Formatter constructing framed outbound ASCII command strings formatted for microHIL firmware.
 */
interface CommandFormatter {
    fun formatChannel(channel: Int, on: Boolean): String
    fun formatAllChannels(on: Boolean): String
    fun formatQueryAllStatus(): String
    fun formatQueryChannelStatus(channel: Int): String
    fun formatQueryBoardId(): String
    fun formatQueryVersion(): String
    fun formatReset(): String
    fun formatMask(mask: String): String
    fun formatTimer(channel: Int, seconds: Int): String
    fun formatPulse(channel: Int, durationMs: Int): String
    fun formatBlink(channel: Int, onMs: Int, offMs: Int, count: Int): String
}
