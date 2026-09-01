package com.abcomm.protocol.matchers

import com.abcomm.protocol.DeviceResponse
import com.abcomm.protocol.MicrohilProtocolConstants
import com.abcomm.protocol.ResponseMatcher

class ChannelStateMatcher : ResponseMatcher {
    override fun match(frame: String): DeviceResponse? {
        val match = CHANNEL_REGEX.matchEntire(frame.trim()) ?: return null
        val ch = match.groupValues[1].toInt()
        val isOn = match.groupValues[2].equals(MicrohilProtocolConstants.STATE_ON, ignoreCase = true)
        return DeviceResponse.ChannelState(ch, isOn)
    }

    companion object {
        private val CHANNEL_REGEX =
            Regex("""mh#sys#channel\s+([1-8])\s+(on|off)#end""", RegexOption.IGNORE_CASE)
    }
}
