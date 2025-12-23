package com.sensetime.lib_rtc_5o_jt

interface MessageCallback {
    fun onStartSpeaking()
    fun onStopSpeaking()
    fun onResponseStartTextStream(index: Int)
    fun onResponseTextSegment(text: String?)
    fun audioAccepting()
    fun videoAccepting()
    fun onResponseFullText(text: String?)
    fun onOpen()
    fun onWebRtcError(failReason: String?)
    fun onWsClosed()
    fun asrTraceStart()
    fun asrTraceEnd(text: String?)
    fun sayItScheduled(turn_id: Int)
    fun agentStartPlay(turn_id: Int)
    fun agentEndPlay(turn_id: Int, complete: Boolean)
    fun fnCallStart(text: String?)
    fun fnCallEnd(text: String?)
    fun onRagSearchedInfo(text: String?)
}