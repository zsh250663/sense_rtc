package com.sensetime.lib_rtc_5o_jt

import android.view.TextureView

interface IRtcEngineCallback {
    fun onUserRemoteSpeaking()
    fun onUserRemoteStoppedSpeaking()
    fun addLocalVideoSurface(surface: TextureView)
}