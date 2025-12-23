package com.sensetime.lib_rtc_5o_jt

import android.content.Context
import android.util.Log
import android.view.TextureView
import com.sensetime.lib_rtc_5o_jt.model.MessageContent
import com.sensetime.lib_rtc_5o_jt.util.TagParser
import java.lang.ref.WeakReference

class SenseEngine private constructor() : IRtcEngine, IMessage, ISenseEngineCallback {
    private var rtcEngine: AgoraRtcEngine? = null
    private var webSocketManager: WebSocketManager? = null
    private var engineConfig: SenseEngineConfig? = null
    private val tagParser = TagParser().apply {
        setTagCallback(this@SenseEngine)
    }
    var hasConnected = false
    @Volatile
    private var isAgoraInit = false

    // 音视频 故事 一对一场景
    enum class SCENE {
        AUDIO, STORY, ONE_BY_ONE
    }

    private val sceneRouteMap = mutableMapOf<SCENE, DefaultSenseEngine>()

    fun registerScene(type: SCENE, scene: DefaultSenseEngine) {
        sceneRouteMap[type] = scene
    }

    // 用于初始化声网引擎， 加速后面的start
    fun initialStart(context: Context) {
        if (rtcEngine == null) {
            rtcEngine = AgoraRtcEngine(context)
            rtcEngine?.setRtcEngineCallback(this)
        }
        initialize("742ce1a98af6401095c55c48057f4af1")
    }

    fun start(context: Context, config: SenseEngineConfig) {
        closeKeepEngine()
        enableAudio()

        this.engineConfig = config
        if (rtcEngine == null) {
            rtcEngine = AgoraRtcEngine(context)
            rtcEngine?.setRtcEngineCallback(this)
        }

        rtcEngine?.joinChannelSuccessCallback = {
            webSocketManager?.configAsr()
            if (config.connectType == ConnectionType.STORY.value) {
                // 讲故事场景
                configPipeline(config.story_id)
            }
            webSocketManager?.startServing()
            webSocketManager?.enableRag()
        }

        webSocketManager = WebSocketManager()
        webSocketManager?.rtcInit = { appId, token, channelId, clientUid ->
            // 这里是在子线程
            if (isAgoraInit) {
                Log.d("WebSocketManager", "声网引擎已经初始化，直接加入房间")
                rtcEngine?.joinChannel(token, channelId, clientUid)
            } else {
                Log.d("WebSocketManager", "初始化声网引擎")
                initialize(appId)
                // 如果提前初始化加入房间的话， 跟他对话会有声音！！！
                rtcEngine?.joinChannel(token, channelId, clientUid)
            }
        }
        webSocketManager?.setWebSocketCallback(this)
        webSocketManager?.startSocket(config)
    }

    @Deprecated("不建议使用, 会导致下一次SenseEngine start变慢")
    override fun close() {
        rtcEngine?.close()
        webSocketManager?.close()
        rtcEngine = null
        webSocketManager = null
    }

    fun closeKeepEngine() {
        leaveChannel()
        closeWebSocket()
    }

    fun closeWithStopAudioAndVideo() {
        disableAudio()
        stopVideo()
        closeKeepEngine()
    }

    private fun closeWebSocket() {
        webSocketManager?.close()
    }

    private fun leaveChannel() {
        rtcEngine?.leaveChannel()
    }

    override fun startVideo() {
        rtcEngine?.startVideo()
    }

    override fun stopVideo() {
        rtcEngine?.stopVideo()
    }

    fun enableVideoPreview() {
        rtcEngine?.enableVideoPreview()
    }

    override fun enableAudio() {
        rtcEngine?.enableAudio()
    }

    override fun disableAudio() {
        rtcEngine?.disableAudio()
    }

    override fun flipCamera() {
        rtcEngine?.flipCamera()
    }

    override fun muteLocalAudioStream(enable: Boolean) {
        rtcEngine?.muteLocalAudioStream(enable)
    }

    override fun setCameraFocusPositionInPreview(positionX: Float, positionY: Float) {
        rtcEngine?.setCameraFocusPositionInPreview(positionX, positionY)
    }

    override fun setCameraTorchOn(on: Boolean) {
        rtcEngine?.setCameraTorchOn(on)
    }

    private fun initialize(
        agoraAppId: String?
    ) {
        rtcEngine?.initialize(agoraAppId)
        isAgoraInit = true
    }

    fun getSessionId(): String? {
        return webSocketManager?.getSessionId()
    }

    fun getSenseChatSessionId(): String? {
        return webSocketManager?.getSenseChatSessionId()
    }

    override fun sendMsg(text: String?, images: List<MessageContent>?, audio: MessageContent?) {
        webSocketManager?.postMultiModalGenerate(text, images, audio)
    }

    override fun setSystemPrompt(system_prompt: String?) {
        webSocketManager?.setSystemPrompt(system_prompt)
    }

    override fun setSystemPrompt(system_prompt: String?, prompt_type: String?) {
        webSocketManager?.setSystemPrompt(system_prompt, prompt_type)
    }

    override fun setSystemPrompt(story_id: Int, prompt_type: String?) {
        webSocketManager?.setSystemPrompt(story_id, prompt_type)
    }

    override fun setVoiceTTsType(voice_type: String?) {
        webSocketManager?.setVoiceTTsType(voice_type)
    }

    override fun configPipeline(story_id: Int) {
        webSocketManager?.configPipeline(story_id)
    }

    override fun postSayIt(message: String) {
        webSocketManager?.postSayIt(message)
    }

    private val callbacks = mutableSetOf<WeakReference<ISenseEngineCallback>>()

    fun addCallback(callback: ISenseEngineCallback) {
        callbacks.add(WeakReference(callback))
    }

    fun removeCallback(callback: ISenseEngineCallback) {
        callbacks.removeAll { it.get() == callback || it.get() == null }
    }

    private fun topCallback(): ISenseEngineCallback? {
        return callbacks.lastOrNull()?.get()
    }

    override fun onUserRemoteSpeaking() {
        sceneRouteMap[engineConfig?.sceneType]?.onUserRemoteSpeaking()
        topCallback()?.onUserRemoteSpeaking()
    }

    override fun onUserRemoteStoppedSpeaking() {
        sceneRouteMap[engineConfig?.sceneType]?.onUserRemoteStoppedSpeaking()
        topCallback()?.onUserRemoteStoppedSpeaking()
    }

    override fun addLocalVideoSurface(surface: TextureView) {
        sceneRouteMap[engineConfig?.sceneType]?.addLocalVideoSurface(surface)
        topCallback()?.addLocalVideoSurface(surface)
    }

    override fun onStartSpeaking() {
        sceneRouteMap[engineConfig?.sceneType]?.onStartSpeaking()
        topCallback()?.onStartSpeaking()
    }

    override fun onStopSpeaking() {
        sceneRouteMap[engineConfig?.sceneType]?.onStopSpeaking()
        topCallback()?.onStopSpeaking()
    }

    override fun onResponseStartTextStream(index: Int) {
        sceneRouteMap[engineConfig?.sceneType]?.onResponseStartTextStream(index)
        topCallback()?.onResponseStartTextStream(index)
    }

    private fun removeTag(content: String): String {
        return content.replace("<style>.*?</style>".toRegex(), "")
            .replace("<obj>.*?</obj>".toRegex(), "")
            .replace("<role>.*?</role>".toRegex(), "")
            .replace("<speed>.*?</speed>".toRegex(), "")
            .replace("<dialect>.*?</dialect>".toRegex(), "")
            .replace("<whisper/>".toRegex(), "")
    }

    override fun onResponseTextSegment(text: String?) {
        text?.let {
            var processedToken = tagParser.processToken(it)

            processedToken = removeTag(processedToken)

            if (processedToken.isNotEmpty()) {
                sceneRouteMap[engineConfig?.sceneType]?.onResponseTextSegment(processedToken)
                topCallback()?.onResponseTextSegment(processedToken)
            }
        }
    }

    override fun audioAccepting() {
        hasConnected = true
        rtcEngine?.adjustRecordingSignalVolume(20)
        sceneRouteMap[engineConfig?.sceneType]?.audioAccepting()
        topCallback()?.audioAccepting()
    }

    override fun videoAccepting() {
        sceneRouteMap[engineConfig?.sceneType]?.videoAccepting()
        topCallback()?.videoAccepting()
    }

    override fun onResponseFullText(text: String?) {
        text?.let {
            val processedToken = removeTag(it)
            sceneRouteMap[engineConfig?.sceneType]?.onResponseFullText(processedToken)
            topCallback()?.onResponseFullText(processedToken)
        }
    }

    override fun onOpen() {
        sceneRouteMap[engineConfig?.sceneType]?.onOpen()
        topCallback()?.onOpen()
    }

    override fun onWebRtcError(failReason: String?) {
        hasConnected = false
        sceneRouteMap[engineConfig?.sceneType]?.onWebRtcError(failReason)
        topCallback()?.onWebRtcError(failReason)
    }

    override fun onWsClosed() {
        hasConnected = false
    }

    override fun asrTraceStart() {
        sceneRouteMap[engineConfig?.sceneType]?.asrTraceStart()
        topCallback()?.asrTraceStart()
    }

    override fun asrTraceEnd(text: String?) {
        sceneRouteMap[engineConfig?.sceneType]?.asrTraceEnd(text)
        topCallback()?.asrTraceEnd(text)
    }

    override fun sayItScheduled(turn_id: Int) {
        sceneRouteMap[engineConfig?.sceneType]?.sayItScheduled(turn_id)
        topCallback()?.sayItScheduled(turn_id)
    }

    override fun agentStartPlay(turn_id: Int) {
        sceneRouteMap[engineConfig?.sceneType]?.agentStartPlay(turn_id)
        topCallback()?.agentStartPlay(turn_id)
    }

    override fun agentEndPlay(turn_id: Int, complete: Boolean) {
        sceneRouteMap[engineConfig?.sceneType]?.agentEndPlay(turn_id, complete)
        topCallback()?.agentEndPlay(turn_id, complete)
    }

    override fun fnCallStart(text: String?) {
        sceneRouteMap[engineConfig?.sceneType]?.fnCallStart(text)
        topCallback()?.fnCallStart(text)
    }

    override fun fnCallEnd(text: String?) {
        sceneRouteMap[engineConfig?.sceneType]?.fnCallEnd(text)
        topCallback()?.fnCallEnd(text)
    }

    override fun onRagSearchedInfo(text: String?) {
        sceneRouteMap[engineConfig?.sceneType]?.onRagSearchedInfo(text)
        topCallback()?.onRagSearchedInfo(text)
    }

    override fun onTagDetected(tagName: String, content: String) {
        sceneRouteMap[engineConfig?.sceneType]?.onTagDetected(tagName, content)
        topCallback()?.onTagDetected(tagName, content)
    }

    companion object {
        val getDefault by lazy { SenseEngine() }
    }
}