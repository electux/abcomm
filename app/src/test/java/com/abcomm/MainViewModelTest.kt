package com.abcomm

import com.abcomm.communication.CommunicationProvider
import com.abcomm.communication.ConnectionMode
import com.abcomm.communication.ConnectionStatus
import com.abcomm.ui.MainViewModel
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val communicator = mockk<CommunicationProvider>(relaxed = true)
    private lateinit var viewModel: MainViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel(communicator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is disconnected`() {
        val state = viewModel.uiState.value
        assertEquals(ConnectionStatus.Disconnected, state.connectionStatus)
        assertFalse(state.isConnected)
        assertFalse(state.isConnecting)
        assertEquals(ConnectionMode.BLE, state.connectionMode)
        assertEquals(List(8) { false }, state.channelStates)
        assertEquals("", state.boardId)
        assertEquals("", state.firmwareVersion)
    }

    @Test
    fun `status update changes uiState and triggers queries on connect`() {
        val statusListenerSlot = slot<(ConnectionStatus) -> Unit>()
        verify { communicator.setStatusListener(capture(statusListenerSlot)) }

        // Update to Connecting
        statusListenerSlot.captured(ConnectionStatus.Connecting("microHIL"))
        assertTrue(viewModel.uiState.value.isConnecting)
        assertFalse(viewModel.uiState.value.isConnected)
        assertEquals(ConnectionStatus.Connecting("microHIL"), viewModel.uiState.value.connectionStatus)

        // Update to Connected
        statusListenerSlot.captured(ConnectionStatus.Connected("microHIL"))
        assertFalse(viewModel.uiState.value.isConnecting)
        assertTrue(viewModel.uiState.value.isConnected)
        assertEquals(ConnectionStatus.Connected("microHIL"), viewModel.uiState.value.connectionStatus)

        // Verifies auto-query commands sent upon connection
        verify { communicator.sendCommand("<mh#all#stat#end>") }
        verify { communicator.sendCommand("<mh#sys#id#end>") }
        verify { communicator.sendCommand("<mh#sys#version#end>") }
    }

    @Test
    fun `toggleChannel sends correct framed command when connected`() {
        // Setup connected state
        val statusListenerSlot = slot<(ConnectionStatus) -> Unit>()
        verify { communicator.setStatusListener(capture(statusListenerSlot)) }
        statusListenerSlot.captured(ConnectionStatus.Connected("microHIL"))

        // Toggle channel 0 (index 0 -> channel 1)
        viewModel.toggleChannel(0)

        verify { communicator.sendCommand("<mh#ch#1#on#end>") }
        assertTrue(viewModel.uiState.value.channelStates[0])

        // Toggle again to turn off
        viewModel.toggleChannel(0)
        verify { communicator.sendCommand("<mh#ch#1#off#end>") }
        assertFalse(viewModel.uiState.value.channelStates[0])
    }

    @Test
    fun `toggleChannel does nothing when disconnected`() {
        viewModel.toggleChannel(0)

        verify(exactly = 0) { communicator.sendCommand(any()) }
        assertFalse(viewModel.uiState.value.channelStates[0])
    }

    @Test
    fun `setAllChannels sends correct framed command when connected`() {
        val statusListenerSlot = slot<(ConnectionStatus) -> Unit>()
        verify { communicator.setStatusListener(capture(statusListenerSlot)) }
        statusListenerSlot.captured(ConnectionStatus.Connected("microHIL"))

        // Set all on
        viewModel.setAllChannels(true)
        verify { communicator.sendCommand("<mh#all#on#end>") }
        assertTrue(viewModel.uiState.value.channelStates.all { it })

        // Set all off
        viewModel.setAllChannels(false)
        verify { communicator.sendCommand("<mh#all#off#end>") }
        assertTrue(viewModel.uiState.value.channelStates.all { !it })
    }

    @Test
    fun `incoming response updates channel states from snapshot`() {
        val responseListenerSlot = slot<(String) -> Unit>()
        verify { communicator.setResponseListener(capture(responseListenerSlot)) }

        val snapshot = "mh#sys#channels: 1:ON 2:OFF 3:ON 4:OFF 5:OFF 6:OFF 7:OFF 8:ON #end"
        responseListenerSlot.captured(snapshot)

        val states = viewModel.uiState.value.channelStates
        assertTrue(states[0])
        assertFalse(states[1])
        assertTrue(states[2])
        assertTrue(states[7])
        assertEquals(snapshot, viewModel.uiState.value.lastResponse)
    }

    @Test
    fun `incoming response updates board ID and firmware version`() {
        val responseListenerSlot = slot<(String) -> Unit>()
        verify { communicator.setResponseListener(capture(responseListenerSlot)) }

        responseListenerSlot.captured("mh#sys#mh:333:2023:0#end")
        assertEquals("mh:333:2023:0", viewModel.uiState.value.boardId)

        responseListenerSlot.captured("mh#sys#microHIL v1.0.0#end")
        assertEquals("microHIL v1.0.0", viewModel.uiState.value.firmwareVersion)
    }

    @Test
    fun `disconnection resets channel states and clears device info`() {
        val statusListenerSlot = slot<(ConnectionStatus) -> Unit>()
        val responseListenerSlot = slot<(String) -> Unit>()
        verify { communicator.setStatusListener(capture(statusListenerSlot)) }
        verify { communicator.setResponseListener(capture(responseListenerSlot)) }

        // Connect and receive info + channels
        statusListenerSlot.captured(ConnectionStatus.Connected("microHIL"))
        responseListenerSlot.captured("mh#sys#mh:333:2023:0#end")
        responseListenerSlot.captured("mh#sys#channels: 1:ON 2:ON 3:ON 4:ON 5:ON 6:ON 7:ON 8:ON #end")

        assertTrue(viewModel.uiState.value.isConnected)
        assertEquals("mh:333:2023:0", viewModel.uiState.value.boardId)
        assertTrue(viewModel.uiState.value.channelStates[0])

        // Trigger Disconnect
        statusListenerSlot.captured(ConnectionStatus.Disconnected)

        assertFalse(viewModel.uiState.value.isConnected)
        assertEquals(ConnectionStatus.Disconnected, viewModel.uiState.value.connectionStatus)
        assertEquals("", viewModel.uiState.value.boardId)
        assertEquals("", viewModel.uiState.value.firmwareVersion)
        assertTrue(viewModel.uiState.value.channelStates.all { !it })
    }

    @Test
    fun `switching connection mode updates uiState`() {
        viewModel.setConnectionMode(ConnectionMode.WIFI)
        assertEquals(ConnectionMode.WIFI, viewModel.uiState.value.connectionMode)

        viewModel.setConnectionMode(ConnectionMode.BLE)
        assertEquals(ConnectionMode.BLE, viewModel.uiState.value.connectionMode)
    }
}
