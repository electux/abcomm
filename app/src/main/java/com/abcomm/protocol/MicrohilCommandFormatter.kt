package com.abcomm.protocol

/**
 * Default implementation of CommandFormatter for microHIL protocol.
 */
class MicrohilCommandFormatter : CommandFormatter {

    override fun formatChannel(channel: Int, on: Boolean): String {
        require(channel in MicrohilProtocolConstants.MIN_CHANNEL..MicrohilProtocolConstants.MAX_CHANNEL) {
            "Channel must be between ${MicrohilProtocolConstants.MIN_CHANNEL} and ${MicrohilProtocolConstants.MAX_CHANNEL}"
        }
        val state = if (on) MicrohilProtocolConstants.STATE_ON else MicrohilProtocolConstants.STATE_OFF
        return "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_CH}$channel#$state${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"
    }

    override fun formatAllChannels(on: Boolean): String {
        val state = if (on) MicrohilProtocolConstants.STATE_ON else MicrohilProtocolConstants.STATE_OFF
        return "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_ALL}$state${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"
    }

    override fun formatQueryAllStatus(): String =
        "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_ALL}${MicrohilProtocolConstants.CMD_STAT}${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"

    override fun formatQueryChannelStatus(channel: Int): String {
        require(channel in MicrohilProtocolConstants.MIN_CHANNEL..MicrohilProtocolConstants.MAX_CHANNEL) {
            "Channel must be between ${MicrohilProtocolConstants.MIN_CHANNEL} and ${MicrohilProtocolConstants.MAX_CHANNEL}"
        }
        return "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_CH}$channel#${MicrohilProtocolConstants.CMD_STAT}${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"
    }

    override fun formatQueryBoardId(): String =
        "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_SYS}${MicrohilProtocolConstants.CMD_ID}${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"

    override fun formatQueryVersion(): String =
        "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_SYS}${MicrohilProtocolConstants.CMD_VERSION}${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"

    override fun formatReset(): String =
        "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_SYS}${MicrohilProtocolConstants.CMD_RESET}${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"

    override fun formatMask(mask: String): String {
        require(mask.length == MicrohilProtocolConstants.MASK_LENGTH && mask.all { it == '0' || it == '1' }) {
            "Mask must be exactly ${MicrohilProtocolConstants.MASK_LENGTH} characters of 0s and 1s"
        }
        return "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_ALL}${MicrohilProtocolConstants.CMD_MASK}#$mask${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"
    }

    override fun formatTimer(channel: Int, seconds: Int): String {
        require(channel in MicrohilProtocolConstants.MIN_CHANNEL..MicrohilProtocolConstants.MAX_CHANNEL) {
            "Channel must be between ${MicrohilProtocolConstants.MIN_CHANNEL} and ${MicrohilProtocolConstants.MAX_CHANNEL}"
        }
        return "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_CH}$channel#${MicrohilProtocolConstants.CMD_TMR}#$seconds${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"
    }

    override fun formatPulse(channel: Int, durationMs: Int): String {
        require(channel in MicrohilProtocolConstants.MIN_CHANNEL..MicrohilProtocolConstants.MAX_CHANNEL) {
            "Channel must be between ${MicrohilProtocolConstants.MIN_CHANNEL} and ${MicrohilProtocolConstants.MAX_CHANNEL}"
        }
        return "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_CH}$channel#${MicrohilProtocolConstants.CMD_PULSE}#$durationMs${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"
    }

    override fun formatBlink(channel: Int, onMs: Int, offMs: Int, count: Int): String {
        require(channel in MicrohilProtocolConstants.MIN_CHANNEL..MicrohilProtocolConstants.MAX_CHANNEL) {
            "Channel must be between ${MicrohilProtocolConstants.MIN_CHANNEL} and ${MicrohilProtocolConstants.MAX_CHANNEL}"
        }
        return "${MicrohilProtocolConstants.FRAME_START}${MicrohilProtocolConstants.CMD_PREFIX_CH}$channel#${MicrohilProtocolConstants.CMD_BLINK}#$onMs#$offMs#$count${MicrohilProtocolConstants.CMD_SUFFIX_END}${MicrohilProtocolConstants.FRAME_END}"
    }
}
