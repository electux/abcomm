package com.abcomm.protocol.matchers

import com.abcomm.protocol.DeviceResponse
import com.abcomm.protocol.MicrohilProtocolConstants
import com.abcomm.protocol.ResponseMatcher

class AllChannelsStateMatcher : ResponseMatcher {
    override fun match(frame: String): DeviceResponse? {
        val match = ALL_CHANNELS_REGEX.matchEntire(frame.trim()) ?: return null
        val isOn = match.groupValues[1].equals(MicrohilProtocolConstants.STATE_ON, ignoreCase = true)
        return DeviceResponse.AllChannelsState(isOn)
    }

    companion object {
        private val ALL_CHANNELS_REGEX =
            Regex("""mh#sys#all channels\s+(on|off)#end""", RegexOption.IGNORE_CASE)
    }
}
