package com.abcomm.protocol.matchers

import com.abcomm.protocol.DeviceResponse
import com.abcomm.protocol.ResponseMatcher

class BoardIdMatcher : ResponseMatcher {
    override fun match(frame: String): DeviceResponse? {
        val match = BOARD_ID_REGEX.matchEntire(frame.trim()) ?: return null
        return DeviceResponse.BoardId(match.groupValues[1])
    }

    companion object {
        private val BOARD_ID_REGEX =
            Regex("""mh#sys#(mh:[^#]+)#end""")
    }
}
