package com.abcomm.protocol

/**
 * Default stream frame parser for microHIL '<...>' framing.
 */
class MicrohilFrameParser(
    private val frameStart: Char = MicrohilProtocolConstants.FRAME_START,
    private val frameEnd: Char = MicrohilProtocolConstants.FRAME_END
) : FrameParser {
    private val currentFrame = StringBuilder()
    private var isReceiving = false

    @Synchronized
    override fun process(chunk: String): List<String> {
        val completedFrames = mutableListOf<String>()
        for (c in chunk) {
            when {
                c == frameStart -> {
                    isReceiving = true
                    currentFrame.clear()
                }
                c == frameEnd -> {
                    if (isReceiving) {
                        completedFrames.add(currentFrame.toString())
                        currentFrame.clear()
                        isReceiving = false
                    }
                }
                isReceiving -> {
                    if (c != '\r' && c != '\n') {
                        currentFrame.append(c)
                    }
                }
            }
        }
        return completedFrames
    }

    @Synchronized
    override fun reset() {
        currentFrame.clear()
        isReceiving = false
    }
}
