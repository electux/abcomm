package com.abcomm.protocol.matchers

import com.abcomm.protocol.DeviceResponse
import com.abcomm.protocol.ResponseMatcher

class FirmwareVersionMatcher : ResponseMatcher {
    override fun match(frame: String): DeviceResponse? {
        val match = VERSION_REGEX.matchEntire(frame.trim()) ?: return null
        return DeviceResponse.FirmwareVersion(match.groupValues[1])
    }

    companion object {
        private val VERSION_REGEX =
            Regex("""mh#sys#(microHIL[^#]*)#end""", RegexOption.IGNORE_CASE)
    }
}
