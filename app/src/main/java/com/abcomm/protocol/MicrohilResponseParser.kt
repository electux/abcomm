package com.abcomm.protocol

import com.abcomm.protocol.matchers.AllChannelsSnapshotMatcher
import com.abcomm.protocol.matchers.AllChannelsStateMatcher
import com.abcomm.protocol.matchers.BoardIdMatcher
import com.abcomm.protocol.matchers.ChannelStateMatcher
import com.abcomm.protocol.matchers.FirmwareVersionMatcher
import com.abcomm.protocol.matchers.MaskAppliedMatcher
import com.abcomm.protocol.matchers.SystemResettingMatcher

/**
 * Evaluates incoming frames against an ordered collection of ResponseMatchers,
 * returning the first matched DeviceResponse or DeviceResponse.Unknown if unmatched.
 */
class MicrohilResponseParser(
    private val matchers: List<ResponseMatcher> = defaultMatchers()
) : ResponseParser {

    override fun parse(frame: String): DeviceResponse {
        val trimmed = frame.trim()
        for (matcher in matchers) {
            val response = matcher.match(trimmed)
            if (response != null) {
                return response
            }
        }
        return DeviceResponse.Unknown(trimmed)
    }

    companion object {
        fun defaultMatchers(): List<ResponseMatcher> = listOf(
            ChannelStateMatcher(),
            AllChannelsStateMatcher(),
            AllChannelsSnapshotMatcher(),
            MaskAppliedMatcher(),
            BoardIdMatcher(),
            FirmwareVersionMatcher(),
            SystemResettingMatcher()
        )
    }
}
