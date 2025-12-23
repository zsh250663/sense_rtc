package com.sensetime.lib_rtc

interface IRtcEngine {
    fun close()
    fun startVideo()
    fun stopVideo()
    fun enableAudio()
    fun disableAudio()
    fun flipCamera()
    fun muteLocalAudioStream(enable: Boolean)
    fun setCameraFocusPositionInPreview(positionX: Float, positionY: Float)
    fun setCameraTorchOn(on: Boolean)
}