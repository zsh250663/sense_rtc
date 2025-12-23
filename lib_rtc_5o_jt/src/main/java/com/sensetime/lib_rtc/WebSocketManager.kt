package com.sensetime.lib_rtc

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import com.google.gson.Gson
import com.sensetime.lib_rtc.model.MessageContent
import com.sensetime.lib_rtc.model.MultiTypeMessage
import com.sensetime.lib_rtc.util.JwtTokenUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class WebSocketManager {
    private val TAG = "WebSocketManager"
    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null

    private var agoraAppId: String? = null
    private var agoraToken: String? = null
    private var agoraChannelId: String? = null
    private var sessionId: String? = null
    private var serverUId: String? = null
    private var clientUid: String? = null
    private var senseChatSessionId: String? = null

    private var config: SenseEngineConfig? = null
    private var callback: MessageCallback? = null
    private val gson by lazy { Gson() }
    private val requestId = "100"

    // 添加重连相关变量
    private var maxRetryCount = 3
    private var currentRetryCount = 0

    private val mainHandler = Handler(Looper.getMainLooper())

    var rtcInit: ((String?, String?, String?, String?) -> Unit)? = null

    private suspend fun checkAndRefreshTokenIfNeeded(): Boolean {
       return config?.exposedFunction?.checkAndRefreshTokenIfNeeded() == true
    }

    private fun start(config: SenseEngineConfig) {
        this.config = config

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 在连接前检查并刷新token
                val tokenRefreshSuccess = checkAndRefreshTokenIfNeeded()
                if (!tokenRefreshSuccess) {
                    Log.e(TAG, "Token refresh failed, cannot establish WebSocket connection")
                    callWebRtcError("Token验证失败，请重新登录")
                    return@launch
                }

                connectWebSocket(config)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start WebSocket connection: ${e.message}")
                callWebRtcError("连接失败: ${e.message}")
            }
        }
    }

    private fun connectWebSocket(config: SenseEngineConfig) {
        val url = config.url
        try {
            val originalUri = Uri.parse(url)
            val builder = originalUri.buildUpon()
                .appendQueryParameter("audio_only", config.audioOnly.toString())
                .appendQueryParameter("voice_type", config.voiceType)
                .appendQueryParameter(
                    "jwt",
                    JwtTokenUtil.generateToken(config.iss, config.secretKey)
                )
//                .appendQueryParameter("system_prompt", config.systemPrompt)
                .appendQueryParameter("vad_neg_threshold", config.vadNegThreshold.toString())
                .appendQueryParameter("vad_pos_threshold", config.vadPosThreshold.toString())
                .appendQueryParameter(
                    "vad_min_speech_duration_ms",
                    config.vadMinSpeechDurationMs.toString()
                )
                .appendQueryParameter(
                    "vad_min_silence_duration_ms",
                    config.vadMinSilenceDurationMs.toString()
                )
                .appendQueryParameter("video_width", config.videoWidth.toString())
                .appendQueryParameter("video_height", config.videoHeight.toString())
                .appendQueryParameter("type", config.connectType.toString())


//            config.pipelinePreset?.takeIf { it.isNotBlank() }?.let {
//                builder.appendQueryParameter("pipeline_preset", it)
//            }

            config.senseChatSessionId?.takeIf { it.isNotBlank() && it != "0" }?.let {
                builder.appendQueryParameter("session_id", it)
            }

            config.senseChatParentId?.takeIf { it.isNotBlank() }?.let {
                builder.appendQueryParameter("parent_id", it)
            } ?: run {
                builder.appendQueryParameter("parent_id", "0")
            }

            if (!config.modelId.isNullOrEmpty()) {
                builder.appendQueryParameter("model_name", config.modelId)
            }
//            if (!config.initPrompt.isNullOrEmpty()) {
//                builder.appendQueryParameter("init_prompt", config.initPrompt)
//            }

            val newUri = builder.build()
            Log.d(TAG, "websocket url query: $newUri")
            client = OkHttpClient.Builder()
                .pingInterval(10, TimeUnit.SECONDS)
                .build()
            val requestBuilder = Request.Builder().url(newUri.toString())

            // 获取最新的token
            val latestToken = config.exposedFunction?.getLatestToken()
            if (!latestToken.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $latestToken")
            }

            requestBuilder.addHeader("System-Type", "android")
            requestBuilder.addHeader("Version-Code", "2.4.0")
            val request = requestBuilder.build()
            webSocket = client?.newWebSocket(request, MyWebSocketListener())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleTokenExpiredAndRetry() {
        if (currentRetryCount >= maxRetryCount) {
            Log.e(TAG, "Max retry count reached, giving up")
            callWebRtcError("连接失败，请检查网络或重新登录")
            return
        }

        currentRetryCount++
        Log.d(
            TAG,
            "Token expired, attempting to refresh and reconnect (attempt $currentRetryCount/$maxRetryCount)"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val refreshSuccess = checkAndRefreshTokenIfNeeded()
                if (refreshSuccess && config != null) {
                    Log.d(TAG, "Token refreshed, reconnecting WebSocket...")
                    Thread.sleep(1000) // 等待1秒后重连
                    connectWebSocket(config!!)
                } else {
                    Log.e(TAG, "Failed to refresh token for retry")
                    callWebRtcError("Token刷新失败，请重新登录")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during token refresh and retry: ${e.message}")
                callWebRtcError("重连失败: ${e.message}")
            }
        }
    }

    fun getSessionId(): String? = sessionId

    fun getSenseChatSessionId(): String? = senseChatSessionId

    fun close() {
        thread {
            Log.d(TAG, "ws close")
            currentRetryCount = 0
            if (webSocket != null) {
                webSocket?.close(1000, "Closing the WebSocket connection")
                client?.dispatcher?.executorService?.shutdown()
                client?.connectionPool?.evictAll()
            }
            agoraAppId = ""
            agoraToken = ""
            agoraChannelId = ""
            clientUid = ""
            serverUId = ""
        }
    }

    private fun sendMsg(text: String) {
        Log.d(TAG, "sendMsg: $text")
        webSocket?.send(text)
    }

    private fun createSession() {
        sendMsg(gson.toJson(mapOf("type" to "CreateSession", "request_id" to requestId)))
    }

    private fun requestAgoraChannelInfo() {
        sendMsg(gson.toJson(mapOf("type" to "RequestAgoraChannelInfo", "request_id" to requestId)))
    }

    private fun requestAgoraToken() {
        sendMsg(
            gson.toJson(
                mapOf(
                    "type" to "RequestAgoraToken",
                    "duration" to 24 * 60 * 60,
                    "request_id" to requestId
                )
            )
        )
    }

    fun startServing() {
        sendMsg(gson.toJson(mapOf("type" to "StartServing")))
    }

    fun enableRag() {
        sendMsg(
            gson.toJson(
                mapOf(
                    "type" to "EnableRag",
                    "dataset_id" to config?.exposedFunction?.getRagId(),
                    "top_k" to 5,
                    "confidence" to 0.1,
                    "weight" to 0.5,
                    "structure_weight" to 0.5
                )
            )
        )
    }

    fun configAsr() {
        sendMsg(gson.toJson(mapOf("type" to "ConfigASR", "enable" to true)))
    }

    fun configPipeline(story_id: Int) {
        sendMsg(
            gson.toJson(
                mapOf(
                    "type" to "ConfigPipeline",
                    "story_id" to story_id
                )
            )
        )
    }

    fun setSystemPrompt(system_prompt: String?) {
        sendMsg(gson.toJson(mapOf("type" to "SetSystemPrompt", "system_prompt" to system_prompt)))
    }

    fun setSystemPrompt(system_prompt: String?, prompt_type: String?) {
        sendMsg(
            gson.toJson(
                mapOf(
                    "type" to "SetSystemPrompt",
                    "system_prompt" to system_prompt,
                    "prompt_type" to prompt_type
                )
            )
        )
    }

    fun setSystemPrompt(story_id: Int, prompt_type: String?) {
        sendMsg(
            gson.toJson(
                mapOf(
                    "type" to "SetSystemPrompt",
                    "story_id" to story_id,
                    "prompt_type" to prompt_type
                )
            )
        )
    }

    fun setVoiceTTsType(voice_type: String?) {
        sendMsg(
            gson.toJson(
                mapOf(
                    "type" to "SetVoiceType",
                    "voice_type" to voice_type,
                    "provider" to "nova_stream_tts_v4"
                )
            )
        )
    }

    fun postSayIt(message: String) = message.takeIf { it.isNotBlank() }?.let {
        sendMsg(gson.toJson(mapOf("type" to "PostSayIt", "text" to message)))
    }

    fun postMultiModalGenerate(
        text: String? = null,
        images: List<MessageContent>? = null,
        audio: MessageContent? = null
    ) {
        sendMsg(
            gson.toJson(
                MultiTypeMessage(
                    type = "PostMultimodalGenerate",
                    text = text,
                    images = images,
                    audio = audio
                )
            )
        )
    }

    private fun initRtc() {
        if (!TextUtils.isEmpty(agoraAppId)
            && !TextUtils.isEmpty(agoraToken)
            && !TextUtils.isEmpty(agoraChannelId)
        ) {
            rtcInit?.invoke(
                agoraAppId,
                agoraToken,
                agoraChannelId,
                clientUid
            )
        }
    }

    private fun postToMain(event: (() -> Unit)) {
        mainHandler.post {
            event()
        }
    }

    private fun callWebRtcError(failReason: String?) {
        postToMain {
            callback?.onWebRtcError(failReason)
        }
    }

    private fun onWsClosed() {
        postToMain {
            callback?.onWsClosed()
        }
    }

    private inner class MyWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            Log.d(TAG, "onOpen")
            currentRetryCount = 0 // 连接成功，重置重试计数
            postToMain {
                callback?.onOpen()
            }

            createSession()
            requestAgoraChannelInfo()
            requestAgoraToken()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            super.onMessage(webSocket, text)
            try {
                val msg = JSONObject(text)
                Log.d(TAG, "Message from server: $msg")
                when (msg.optString("type")) {
                    "CreateSessionID" -> {
                        senseChatSessionId = msg.optString("session_id")
                        Log.d(TAG, "got sense chat session_id: $senseChatSessionId")
                    }

                    "CreateSessionResult" -> {
                        sessionId = msg.optString("session_id")
                        Log.d(TAG, "got agora session_id: $sessionId")
                        config?.exposedFunction?.onCreateSessionResult(sessionId)
                    }

                    "AgoraChannelInfo" -> {
                        agoraAppId = msg.optString("appid")
                        Log.d(TAG, "got agora appid: $agoraAppId")

                        agoraChannelId = msg.optString("channel_id")
                        Log.d(TAG, "got agora channel: $agoraChannelId")

                        serverUId = msg.optString("server_uid")
                        Log.d(TAG, "got server_uid: $serverUId")

                        initRtc()
                    }

                    "AgoraToken" -> {
                        agoraToken = msg.optString("token")
                        Log.d(TAG, "got agora token: $agoraToken")

                        clientUid = msg.optString("client_uid")
                        Log.d(TAG, "got client_uid: $clientUid")

                        initRtc()
                    }

                    "StartSpeaking" -> {
                        postToMain {
                            callback?.onStartSpeaking()
                        }
                    }

                    "StopSpeaking" -> {
                        postToMain {
                            callback?.onStopSpeaking()
                        }
                    }

                    "ResponseStartTextStream" -> {
                        postToMain {
                            callback?.onResponseStartTextStream(msg.optInt("index"))
                        }
                    }

                    "ResponseTextSegment" -> {
                        postToMain {
                            callback?.onResponseTextSegment(msg.optString("text"))
                        }
                    }

                    "ResponseEndTextStream" -> {
                        postToMain {
                            callback?.onResponseFullText(msg.optString("text"))
                        }
                    }

                    "AudioAccepting" -> {
                        postToMain {
                            callback?.audioAccepting()
                        }
                    }

                    "VideoAccepting" -> {
                        postToMain {
                            callback?.videoAccepting()
                        }
                    }

                    "AsrTraceStart" -> {
                        postToMain {
                            callback?.asrTraceStart()
                        }
                    }

                    "AsrTraceUpdate" -> {

                    }

                    "AsrTraceEnd" -> {
                        postToMain {
                            callback?.asrTraceEnd(msg.optString("result"))
                        }
                    }

                    "SayItScheduled" -> {
                        postToMain {
                            callback?.sayItScheduled(msg.optInt("turn_id"))
                        }
                    }

                    "AgentStartPlay" -> {
                        postToMain {
                            callback?.agentStartPlay(msg.optInt("turn_id"))
                        }
                    }

                    "AgentEndPlay" -> {
                        postToMain {
                            callback?.agentEndPlay(
                                msg.optInt("turn_id"),
                                msg.optBoolean("complete")
                            )
                        }
                    }

                    "FnCallStart" -> {
                        postToMain {
                            callback?.fnCallStart(text)
                        }
                    }

                    "FnCallEnd" -> {
                        postToMain {
                            callback?.fnCallEnd(text)
                        }
                    }

                    "RagSearchedInfo" -> {
                        postToMain {
                            callback?.onRagSearchedInfo(text)
                        }
                    }

                }
            } catch (e: JSONException) {
                e.printStackTrace()
                Log.e(TAG, "Json parse error")
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)

            Log.e(TAG, "WebSocket error: ${t}, response code: ${response?.code}")

            // 检查是否是认证相关错误（401, 403等）
            if (response?.code == 401 || response?.code == 403) {
                Log.e(TAG, "Authentication error, attempting token refresh and retry")
                handleTokenExpiredAndRetry()
            } else {
                // 其他错误，使用原有的错误处理逻辑
                callWebRtcError(mapThrowableToMessage(t))
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            Log.e(TAG, "WebSocket connection closed, code: $code, reason: $reason")
            onWsClosed()
        }

        fun mapThrowableToMessage(t: Throwable?): String {
            return when (t) {
                is java.net.SocketTimeoutException -> "当前网络较弱，请检查网络"
                is java.net.UnknownHostException -> "网络连接已断开，请检查网络后重试～"
                is java.net.ConnectException -> "服务暂时不可用，工程师正在全力修复中"
                is javax.net.ssl.SSLHandshakeException -> "服务暂时不可用，工程师正在全力修复中"
                is java.io.EOFException -> "网络连接已断开，请检查网络后重试～"
                is java.net.NoRouteToHostException -> "网络连接已断开，请检查网络后重试～"
                is java.net.ProtocolException -> "服务暂时不可用，工程师正在全力修复中"
                is java.net.PortUnreachableException -> "服务暂时不可用，工程师正在全力修复中"
                is java.net.SocketException -> {
                    // 进一步细分 message
                    if (t.message?.contains("Connection reset") == true) {
                        "网络连接已断开，请检查网络后重试～"
                    } else {
                        "服务暂时不可用，工程师正在全力修复中"
                    }
                }

                else -> ""
            }
        }
    }

    fun startSocket(config: SenseEngineConfig) {
        start(config)
    }

    fun setWebSocketCallback(callback: MessageCallback?) {
        this.callback = callback
    }
}