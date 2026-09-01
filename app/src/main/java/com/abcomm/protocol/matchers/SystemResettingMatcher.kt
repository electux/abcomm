package com.abcomm.protocol.matchers

import com.abcomm.protocol.DeviceResponse
import com.abcomm.protocol.MicrohilProtocolConstants
import com.abcomm.protocol.ResponseMatcher

class SystemResettingMatcher : ResponseMatcher {
    override fun match(frame: String): DeviceResponse? {
        if (frame.contains(MicrohilProtocolConstants.RESP_RESETTING_KEYWORD, ignoreCase = true)) {
            return DeviceResponse.SystemResetting
        }
        return null
    }
}
