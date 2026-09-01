package com.abcomm

import com.abcomm.protocol.DeviceResponse
import com.abcomm.protocol.matchers.AllChannelsSnapshotMatcher
import com.abcomm.protocol.matchers.AllChannelsStateMatcher
import com.abcomm.protocol.matchers.BoardIdMatcher
import com.abcomm.protocol.matchers.ChannelStateMatcher
import com.abcomm.protocol.matchers.FirmwareVersionMatcher
import com.abcomm.protocol.matchers.MaskAppliedMatcher
import com.abcomm.protocol.matchers.SystemResettingMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseMatchersTest {

    @Test
    fun `ChannelStateMatcher parses valid frame and rejects invalid`() {
        val matcher = ChannelStateMatcher()
        val res = matcher.match("mh#sys#channel 4 on#end")
        assertTrue(res is DeviceResponse.ChannelState)
        assertEquals(4, (res as DeviceResponse.ChannelState).channel)
        assertTrue(res.isOn)

        assertNull(matcher.match("mh#sys#all channels on#end"))
    }

    @Test
    fun `AllChannelsStateMatcher parses valid frame and rejects invalid`() {
        val matcher = AllChannelsStateMatcher()
        val res = matcher.match("mh#sys#all channels off#end")
        assertTrue(res is DeviceResponse.AllChannelsState)
        assertFalse((res as DeviceResponse.AllChannelsState).isOn)

        assertNull(matcher.match("mh#sys#channel 1 on#end"))
    }

    @Test
    fun `AllChannelsSnapshotMatcher parses valid snapshot and rejects invalid`() {
        val matcher = AllChannelsSnapshotMatcher()
        val res = matcher.match("mh#sys#channels: 1:ON 2:OFF 3:OFF 4:OFF 5:OFF 6:OFF 7:OFF 8:ON #end")
        assertTrue(res is DeviceResponse.AllChannelsSnapshot)
        val snapshot = res as DeviceResponse.AllChannelsSnapshot
        assertTrue(snapshot.states[0])
        assertFalse(snapshot.states[1])
        assertTrue(snapshot.states[7])

        assertNull(matcher.match("mh#sys#something else#end"))
    }

    @Test
    fun `MaskAppliedMatcher parses valid mask and rejects invalid`() {
        val matcher = MaskAppliedMatcher()
        val res = matcher.match("mh#sys#channels mask applied: 10101010#end")
        assertTrue(res is DeviceResponse.AllChannelsSnapshot)
        val snapshot = res as DeviceResponse.AllChannelsSnapshot
        assertEquals(listOf(true, false, true, false, true, false, true, false), snapshot.states)

        assertNull(matcher.match("invalid frame"))
    }

    @Test
    fun `BoardIdMatcher parses valid board id and rejects invalid`() {
        val matcher = BoardIdMatcher()
        val res = matcher.match("mh#sys#mh:333:2023:0#end")
        assertTrue(res is DeviceResponse.BoardId)
        assertEquals("mh:333:2023:0", (res as DeviceResponse.BoardId).id)

        assertNull(matcher.match("mh#sys#microHIL v1.0.0#end"))
    }

    @Test
    fun `FirmwareVersionMatcher parses valid version and rejects invalid`() {
        val matcher = FirmwareVersionMatcher()
        val res = matcher.match("mh#sys#microHIL v1.0.0#end")
        assertTrue(res is DeviceResponse.FirmwareVersion)
        assertEquals("microHIL v1.0.0", (res as DeviceResponse.FirmwareVersion).version)

        assertNull(matcher.match("mh#sys#mh:333:2023:0#end"))
    }

    @Test
    fun `SystemResettingMatcher parses resetting keyword and rejects invalid`() {
        val matcher = SystemResettingMatcher()
        val res = matcher.match("mh#sys#system resetting...#end")
        assertTrue(res is DeviceResponse.SystemResetting)

        assertNull(matcher.match("mh#sys#ok#end"))
    }
}
