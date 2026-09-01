package com.abcomm.protocol

/**
 * Protocol constants defining frame boundaries, channel numbers,
 * command keywords, and system strings for microHIL firmware.
 */
object MicrohilProtocolConstants {
    const val FRAME_START = '<'
    const val FRAME_END = '>'

    const val MIN_CHANNEL = 1
    const val MAX_CHANNEL = 8
    const val CHANNEL_COUNT = 8
    const val MASK_LENGTH = 8

    const val CMD_PREFIX_CH = "mh#ch#"
    const val CMD_PREFIX_ALL = "mh#all#"
    const val CMD_PREFIX_SYS = "mh#sys#"
    const val CMD_SUFFIX_END = "#end"

    const val STATE_ON = "on"
    const val STATE_OFF = "off"

    const val CMD_STAT = "stat"
    const val CMD_ID = "id"
    const val CMD_VERSION = "version"
    const val CMD_RESET = "reset"
    const val CMD_MASK = "mask"
    const val CMD_TMR = "tmr"
    const val CMD_PULSE = "pulse"
    const val CMD_BLINK = "blink"

    const val RESP_CHANNELS_PREFIX = "mh#sys#channels:"
    const val RESP_MASK_APPLIED_PREFIX = "mh#sys#channels mask applied:"
    const val RESP_RESETTING_KEYWORD = "system resetting"
}
