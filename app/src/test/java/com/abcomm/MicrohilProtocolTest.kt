package com.abcomm

import com.abcomm.protocol.CommandFormatter
import com.abcomm.protocol.DeviceResponse
import com.abcomm.protocol.MicrohilCommandFormatter
import com.abcomm.protocol.MicrohilFrameParser
import com.abcomm.protocol.MicrohilResponseParser
import com.abcomm.protocol.ResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicrohilProtocolTest {

    private val formatter: CommandFormatter = MicrohilCommandFormatter()
    private val responseParser: ResponseParser = MicrohilResponseParser()

    @Test
    fun `formatChannel creates valid framed command`() {
        assertEquals("<mh#ch#1#on#end>", formatter.formatChannel(1, true))
        assertEquals("<mh#ch#8#off#end>", formatter.formatChannel(8, false))
    }

    @Test
    fun `formatAllChannels creates valid framed command`() {
        assertEquals("<mh#all#on#end>", formatter.formatAllChannels(true))
        assertEquals("<mh#all#off#end>", formatter.formatAllChannels(false))
    }

    @Test
    fun `formatQuery commands create valid framed commands`() {
        assertEquals("<mh#all#stat#end>", formatter.formatQueryAllStatus())
        assertEquals("<mh#ch#3#stat#end>", formatter.formatQueryChannelStatus(3))
        assertEquals("<mh#sys#id#end>", formatter.formatQueryBoardId())
        assertEquals("<mh#sys#version#end>", formatter.formatQueryVersion())
        assertEquals("<mh#sys#reset#end>", formatter.formatReset())
        assertEquals("<mh#all#mask#10101010#end>", formatter.formatMask("10101010"))
    }

    @Test
    fun `FrameParser handles complete frame`() {
        val parser = MicrohilFrameParser()
        val frames = parser.process("<mh#sys#all channels on#end>")
        assertEquals(1, frames.size)
        assertEquals("mh#sys#all channels on#end", frames[0])
    }

    @Test
    fun `FrameParser handles split chunks`() {
        val parser = MicrohilFrameParser()
        val frames1 = parser.process("<mh#sys#channel")
        assertEquals(0, frames1.size)

        val frames2 = parser.process(" 2 on#end>")
        assertEquals(1, frames2.size)
        assertEquals("mh#sys#channel 2 on#end", frames2[0])
    }

    @Test
    fun `FrameParser handles multiple frames in single chunk`() {
        val parser = MicrohilFrameParser()
        val frames = parser.process("<mh#sys#mh:333:2023:0#end><mh#sys#microHIL v1.0.0#end>")
        assertEquals(2, frames.size)
        assertEquals("mh#sys#mh:333:2023:0#end", frames[0])
        assertEquals("mh#sys#microHIL v1.0.0#end", frames[1])
    }

    @Test
    fun `parseResponse parses channel single state`() {
        val resp1 = responseParser.parse("mh#sys#channel 3 on#end")
        assertTrue(resp1 is DeviceResponse.ChannelState)
        assertEquals(3, (resp1 as DeviceResponse.ChannelState).channel)
        assertTrue(resp1.isOn)

        val resp2 = responseParser.parse("mh#sys#channel 5 off#end")
        assertTrue(resp2 is DeviceResponse.ChannelState)
        assertEquals(5, (resp2 as DeviceResponse.ChannelState).channel)
        assertFalse(resp2.isOn)
    }

    @Test
    fun `parseResponse parses all channels status snapshot`() {
        val raw = "mh#sys#channels: 1:ON 2:OFF 3:ON 4:OFF 5:OFF 6:OFF 7:OFF 8:ON #end"
        val resp = responseParser.parse(raw)
        assertTrue(resp is DeviceResponse.AllChannelsSnapshot)
        val snapshot = resp as DeviceResponse.AllChannelsSnapshot
        assertTrue(snapshot.states[0])
        assertFalse(snapshot.states[1])
        assertTrue(snapshot.states[2])
        assertFalse(snapshot.states[3])
        assertFalse(snapshot.states[4])
        assertFalse(snapshot.states[5])
        assertFalse(snapshot.states[6])
        assertTrue(snapshot.states[7])
    }

    @Test
    fun `parseResponse parses board ID and firmware version`() {
        val idResp = responseParser.parse("mh#sys#mh:333:2023:0#end")
        assertTrue(idResp is DeviceResponse.BoardId)
        assertEquals("mh:333:2023:0", (idResp as DeviceResponse.BoardId).id)

        val verResp = responseParser.parse("mh#sys#microHIL v1.0.0#end")
        assertTrue(verResp is DeviceResponse.FirmwareVersion)
        assertEquals("microHIL v1.0.0", (verResp as DeviceResponse.FirmwareVersion).version)
    }

    @Test
    fun `parseResponse parses system reset`() {
        val resetResp = responseParser.parse("mh#sys#system resetting...#end")
        assertTrue(resetResp is DeviceResponse.SystemResetting)
    }
}
