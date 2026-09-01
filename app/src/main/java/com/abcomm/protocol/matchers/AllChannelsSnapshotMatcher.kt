package com.abcomm.protocol.matchers

import com.abcomm.protocol.DeviceResponse
import com.abcomm.protocol.MicrohilProtocolConstants
import com.abcomm.protocol.ResponseMatcher

class AllChannelsSnapshotMatcher : ResponseMatcher {
    override fun match(frame: String): DeviceResponse? {
        val trimmed = frame.trim()
        if (trimmed.startsWith(MicrohilProtocolConstants.RESP_CHANNELS_PREFIX) &&
            trimmed.endsWith(MicrohilProtocolConstants.CMD_SUFFIX_END)
        ) {
            val content = trimmed
                .removePrefix(MicrohilProtocolConstants.RESP_CHANNELS_PREFIX)
                .removeSuffix(MicrohilProtocolConstants.CMD_SUFFIX_END)
                .trim()
            val states = BooleanArray(MicrohilProtocolConstants.CHANNEL_COUNT) { false }
            CHANNEL_ITEM_REGEX.findAll(content).forEach { matchResult ->
                val chIndex = matchResult.groupValues[1].toInt() - 1
                val isOn = matchResult.groupValues[2].equals("ON", ignoreCase = true)
                if (chIndex in 0 until MicrohilProtocolConstants.CHANNEL_COUNT) {
                    states[chIndex] = isOn
                }
            }
            return DeviceResponse.AllChannelsSnapshot(states.toList())
        }
        return null
    }

    companion object {
        private val CHANNEL_ITEM_REGEX =
            Regex("""([1-8]):(ON|OFF)""", RegexOption.IGNORE_CASE)
    }
}
