package com.abcomm

import io.mockk.confirmVerified
import io.mockk.every
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
        assertEquals("Disconnected", state.status)
        assertFalse(state.isConnected)
        assertFalse(state.isConnecting)
        assertEquals(List(8) { false }, state.channelStates)
    }

    @Test
    fun `status update changes uiState`() {
        val statusListenerSlot = slot<(String) -> Unit>()
        verify { communicator.setStatusListener(capture(statusListenerSlot)) }

        // Update to Connecting
        statusListenerSlot.captured("Connecting...")
        assertTrue(viewModel.uiState.value.isConnecting)
        assertFalse(viewModel.uiState.value.isConnected)
        assertEquals("Connecting...", viewModel.uiState.value.status)

        // Update to Connected
        statusListenerSlot.captured("Connected to Device")
        assertFalse(viewModel.uiState.value.isConnecting)
        assertTrue(viewModel.uiState.value.isConnected)
        assertEquals("Connected to Device", viewModel.uiState.value.status)
    }

    @Test
    fun `toggleChannel sends correct command when connected`() {
        // Setup connected state
        val statusListenerSlot = slot<(String) -> Unit>()
        verify { communicator.setStatusListener(capture(statusListenerSlot)) }
        statusListenerSlot.captured("Connected")

        // Toggle channel 0 (index 0 -> channel 1)
        viewModel.toggleChannel(0)

        verify { communicator.sendCommand("mh#ch#1#on#end") }
        assertTrue(viewModel.uiState.value.channelStates[0])

        // Toggle again to turn off
        viewModel.toggleChannel(0)
        verify { communicator.sendCommand("mh#ch#1#off#end") }
        assertFalse(viewModel.uiState.value.channelStates[0])
    }

    @Test
    fun `toggleChannel does nothing when disconnected`() {
        // Initially disconnected
        viewModel.toggleChannel(0)

        verify(exactly = 0) { communicator.sendCommand(any()) }
        assertFalse(viewModel.uiState.value.channelStates[0])
    }

    @Test
    fun `setAllChannels sends correct command when connected`() {
        // Setup connected state
        val statusListenerSlot = slot<(String) -> Unit>()
        verify { communicator.setStatusListener(capture(statusListenerSlot)) }
        statusListenerSlot.captured("Connected")

        // Set all on
        viewModel.setAllChannels(true)
        verify { communicator.sendCommand("mh#ch#all#on#end") }
        assertTrue(viewModel.uiState.value.channelStates.all { it })

        // Set all off
        viewModel.setAllChannels(false)
        verify { communicator.sendCommand("mh#ch#all#off#end") }
        assertTrue(viewModel.uiState.value.channelStates.all { !it })
    }

    @Test
    fun `setAllChannels does nothing when disconnected`() {
        // Initially disconnected
        viewModel.setAllChannels(true)

        verify(exactly = 0) { communicator.sendCommand(any()) }
        assertTrue(viewModel.uiState.value.channelStates.all { !it })
    }
}
