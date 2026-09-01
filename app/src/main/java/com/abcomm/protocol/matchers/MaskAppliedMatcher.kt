package com.abcomm.protocol.matchers

import com.abcomm.protocol.DeviceResponse
import com.abcomm.protocol.ResponseMatcher

class MaskAppliedMatcher : ResponseMatcher {
    override fun match(frame: String): DeviceResponse? {
        val match = MASK_REGEX.matchEntire(frame.trim()) ?: return null
        val maskStr = match.groupValues[1]
        val states = maskStr.map { it == '1' }
        return DeviceResponse.AllChannelsSnapshot(states)
    }

    companion object {
        private val MASK_REGEX =
            Regex("""mh#sys#channels mask applied:\s*([01]{8})#end""", RegexOption.IGNORE_CASE)
    }
}
