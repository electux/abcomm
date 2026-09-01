package com.abcomm.communication

/**
 * Unified communication contract combining lifecycle control, command dispatch,
 * and asynchronous event observation for microHIL connections.
 */
interface CommunicationProvider : ConnectionController, CommandSender, ConnectionObservable
