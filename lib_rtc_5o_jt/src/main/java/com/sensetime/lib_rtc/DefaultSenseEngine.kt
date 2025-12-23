package com.sensetime.lib_rtc

import android.view.TextureView

abstract class DefaultSenseEngine: ISenseEngineCallback {
    override fun onUserRemoteSpeaking() {
    }

    override fun onUserRemoteStoppedSpeaking() {
    }

    override fun addLocalVideoSurface(surface: TextureView) {
    }

    override fun onStartSpeaking() {
    }

    override fun onStopSpeaking() {
    }

    override fun onResponseStartTextStream(index: Int) {
    }

    override fun onResponseTextSegment(text: String?) {
    }

    override fun audioAccepting() {
    }

    override fun videoAccepting() {
    }

    override fun onResponseFullText(text: String?) {
    }

    override fun onOpen() {
    }

    override fun onWebRtcError(failReason: String?) {
    }

    override fun asrTraceStart() {
    }

    override fun onTagDetected(tagName: String, content: String) {
    }

    override fun asrTraceEnd(text: String?) {
    }

    override fun sayItScheduled(turn_id: Int) {
    }

    override fun agentStartPlay(turn_id: Int) {
    }

    override fun agentEndPlay(turn_id: Int, complete: Boolean) {
    }

    override fun fnCallStart(text: String?) {
    }

    override fun fnCallEnd(text: String?) {
    }

    override fun onRagSearchedInfo(text: String?) {
    }

    override fun onWsClosed() {
    }
}