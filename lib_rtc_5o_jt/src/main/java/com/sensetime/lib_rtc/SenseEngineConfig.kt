package com.sensetime.lib_rtc

import android.content.Context
import com.sensetime.lib_rtc.util.RtcVideoSizeUtil

class SenseEngineConfig private constructor(
    val audioOnly: Boolean,
    val voiceType: String,
//    val systemPrompt: String,
    val vadNegThreshold: Float,
    val vadPosThreshold: Float,
    val vadMinSpeechDurationMs: Int,
    val vadMinSilenceDurationMs: Int,
    val videoWidth: Int,
    val videoHeight: Int,
    val modelId: String?,
    val url: String,
    val iss: String,
    val secretKey: String,
    val orientationMode: OrientationMode,
//    val initPrompt: String?
    val token: String?,
    val sceneType: SenseEngine.SCENE?,
    val senseChatSessionId: String? = null,
    val senseChatParentId: String? = null,
    val pipelinePreset: String? = null,
    val connectType: Int = ConnectionType.CALL_WITHOUT_SEARCH.value,
    val story_id: Int,
    val exposedFunction: IExposedFunction? = null
) {
    class Builder {
        private var audioOnly: Boolean = false
        private var voiceType: String = "zh_female_tianmeixiaoyuan_moon_bigtts"

        //        private var systemPrompt: String =
//            "你的名字叫商量，你是一个年轻的女生，你的性格和蔼，充满阳光与正能量。你说话清新自然，言语间透着细致和体贴，总能以舒缓而愉悦的方式与人交流，让人感到放松和愉快。"
        private var vadNegThreshold: Float = 0.5f
        private var vadPosThreshold: Float = 0.98f
        private var vadMinSpeechDurationMs: Int = 400
        private var vadMinSilenceDurationMs: Int = 800
        private var videoWidth: Int = 0
        private var videoHeight: Int = 0
        private var modelId: String? = ""
        private var url: String = "wss://api-gai.sensetime.com/agent-5o/duplex/ws2"
        private var iss: String = ""
        private var secretKey: String = ""
        private var orientationMode: OrientationMode = OrientationMode.ADAPTIVE
        private var token: String? = ""
        private var senseChatSessionId: String? = null
        private var senseChatParentId: String? = null
//        private var initPrompt: String? = ""
        private var pipelinePreset: String? = null
        private var connectType: Int = ConnectionType.CALL_WITHOUT_SEARCH.value
        private var story_id: Int = 0
        private var sceneType: SenseEngine.SCENE? = null
        private var exposedFunction: IExposedFunction? = null

        fun setExposedFunction(exposedFunction: IExposedFunction) = apply {
            this.exposedFunction = exposedFunction
        }

        fun setSceneType(sceneType: SenseEngine.SCENE) = apply {
            this.sceneType = sceneType
        }

        fun setStoryId(story_id: Int) = apply {
            this.story_id = story_id
        }

        fun setConnectType(connectType: Int) = apply {
            this.connectType = connectType
        }

        fun setPipelinePreset(pipelinePreset: String?) = apply { this.pipelinePreset = pipelinePreset }

        fun setAudioOnly(audioOnly: Boolean) = apply { this.audioOnly = audioOnly }
        fun setVoiceType(voiceType: String) = apply { this.voiceType = voiceType }

        //        fun setSystemPrompt(systemPrompt: String) = apply { this.systemPrompt = systemPrompt }
        fun setVadNegThreshold(vadNegThreshold: Float) =
            apply { this.vadNegThreshold = vadNegThreshold }

        fun setVadPosThreshold(vadPosThreshold: Float) =
            apply { this.vadPosThreshold = vadPosThreshold }

        fun setVadMinSpeechDurationMs(vadMinSpeechDurationMs: Int) =
            apply { this.vadMinSpeechDurationMs = vadMinSpeechDurationMs }

        fun setVadMinSilenceDurationMs(vadMinSilenceDurationMs: Int) =
            apply { this.vadMinSilenceDurationMs = vadMinSilenceDurationMs }

        //        fun setVideoWidth(videoWidth: Int) = apply { this.videoWidth = videoWidth }
//        fun setVideoHeight(videoHeight: Int) = apply { this.videoHeight = videoHeight }
        fun setModelId(modelId: String?) = apply { this.modelId = modelId }
        fun setUrl(url: String) = apply { this.url = url }
        fun setIss(iss: String) = apply { this.iss = iss }
        fun setSecretKey(secretKey: String) = apply { this.secretKey = secretKey }
        fun setOrientationMode(orientationMode: OrientationMode) = apply { this.orientationMode = orientationMode }
        fun setToken(token: String?) = apply { this.token = token }

        //        fun setInitPrompt(initPrompt: String?) = apply { this.initPrompt = initPrompt }
        fun setSenseChatSessionId(senseChatSessionId: String?) = apply { this.senseChatSessionId = senseChatSessionId }
        fun setSenseChatParentId(senseChatParentId: String?) = apply { this.senseChatParentId = senseChatParentId }

        fun build(context: Context): SenseEngineConfig {
            val model = RtcVideoSizeUtil.convert(context)
            videoWidth = model.width
            videoHeight = model.height
            return SenseEngineConfig(
                audioOnly,
                voiceType,
                vadNegThreshold,
                vadPosThreshold,
                vadMinSpeechDurationMs,
                vadMinSilenceDurationMs,
                videoWidth,
                videoHeight,
                modelId,
                url,
                iss,
                secretKey,
                orientationMode,
                token,
                sceneType,
                senseChatSessionId,
                senseChatParentId,
                pipelinePreset,
                connectType,
                story_id,
                exposedFunction
            )
        }
    }

    enum class OrientationMode {
        PORTRAIT, LANDSCAPE, ADAPTIVE
    }
}
