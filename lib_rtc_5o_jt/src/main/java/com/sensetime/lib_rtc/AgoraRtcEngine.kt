package com.sensetime.lib_rtc

import android.content.Context
import android.util.Log
import android.view.TextureView
import com.sensetime.lib_rtc.util.RtcVideoSizeUtil
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.Constants.AUDIO_SCENARIO_GAME_STREAMING
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.CameraCapturerConfiguration
import io.agora.rtc2.video.LowLightEnhanceOptions
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AgoraRtcEngine(
    private val context: Context
) : IRtcEngineEventHandler(), IRtcEngine {
    private val TAG = "AgoraRtcEngine"
    private var mRtcEngine: RtcEngine? = null
    private val userVolumeMap = mutableMapOf<Int, Int>() // 记录远端用户静音计数
    private val MAX_SILENT_COUNT = 5 // 连续静音次数的阈值
    private var callback: IRtcEngineCallback? = null

    var joinChannelSuccessCallback: (() -> Unit)? = null

    override fun onAudioVolumeIndication(speakers: Array<out AudioVolumeInfo>?, totalVolume: Int) {
        super.onAudioVolumeIndication(speakers, totalVolume)
        speakers?.forEach { speaker ->
            val uid = speaker.uid
            val volume = speaker.volume

            if (uid != 0) {
                // 0 表示本地用户
                handleRemoteUserVolume(uid, volume)
            }
        }
    }

    private fun handleRemoteUserVolume(uid: Int, volume: Int) {
        if (volume == 0) {
            // 增加静音计数
            userVolumeMap[uid] = (userVolumeMap[uid] ?: 0) + 1
        } else {
            // 如果音量不为 0，重置计数
            userVolumeMap[uid] = 0
//            LogUtil.d("正在讲话")
            callback?.onUserRemoteSpeaking()
        }

        // 检查静音计数是否超过阈值
        if ((userVolumeMap[uid] ?: 0) >= MAX_SILENT_COUNT) {
            onUserStoppedSpeaking(uid)
        }
    }

    private fun onUserStoppedSpeaking(uid: Int) {
//        LogUtil.d("停止讲话")
        callback?.onUserRemoteStoppedSpeaking()
        userVolumeMap[uid] = 0
    }

    // 成功加入频道回调
    override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
        super.onJoinChannelSuccess(channel, uid, elapsed)
        Log.d(TAG, "onJoinChannelSuccess:$uid")
        joinChannelSuccessCallback?.invoke()
    }

    override fun onLeaveChannel(stats: RtcStats?) {
        super.onLeaveChannel(stats)
//        LogUtil.d("onLeaveChannel")
    }

    // 远端用户或主播离开当前频道回调
    override fun onUserOffline(uid: Int, reason: Int) {
        super.onUserOffline(uid, reason)
//        LogUtil.d("onUserOffline:$uid")
    }

    override fun onError(err: Int) {
        super.onError(err)
//        LogUtil.e("onError:$err")
    }

    override fun onConnectionLost() {
        super.onConnectionLost()
//        LogUtil.d("onConnectionLost")
    }

    override fun onConnectionStateChanged(state: Int, reason: Int) {
        super.onConnectionStateChanged(state, reason)
//        LogUtil.d("onConnectionStateChanged:$state,reason:$reason")
    }

    override fun setCameraFocusPositionInPreview(positionX: Float, positionY: Float) {
        mRtcEngine?.setCameraFocusPositionInPreview(positionX, positionY)
    }

    override fun setCameraTorchOn(on: Boolean) {
        mRtcEngine?.setCameraTorchOn(on)
    }

    fun initialize(
        agoraAppId: String?
    ) {
        try {
            // 创建 RtcEngineConfig 对象，并进行配置
            val config = RtcEngineConfig()
            config.mContext = context
            config.mAppId = agoraAppId
            config.mEventHandler = this
            // 创建并初始化 RtcEngine
            mRtcEngine = RtcEngine.create(config)
        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }
        mRtcEngine?.enableAudioVolumeIndication(200, 3, true)
        mRtcEngine?.enableAudio()
        mRtcEngine?.setAINSMode(true, 0)
        mRtcEngine?.setParameters("{\"che.audio.aiaec.working_mode\": 2}")
        mRtcEngine?.setAudioScenario(AUDIO_SCENARIO_GAME_STREAMING)

        val videoEncoder = VideoEncoderConfiguration()
        videoEncoder.codecType = VideoEncoderConfiguration.VIDEO_CODEC_TYPE.VIDEO_CODEC_H264
        val model = RtcVideoSizeUtil.convert(context)
        videoEncoder.dimensions =
            VideoEncoderConfiguration.VideoDimensions(model.width, model.height)
        videoEncoder.orientationMode = VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_FIXED_PORTRAIT
        videoEncoder.frameRate = 30
        mRtcEngine?.setVideoEncoderConfiguration(videoEncoder)

        val cameraCaptureConfiguration =
            CameraCapturerConfiguration(CameraCapturerConfiguration.CAMERA_DIRECTION.CAMERA_REAR)
        mRtcEngine?.setCameraCapturerConfiguration(cameraCaptureConfiguration)
        // 增强暗光场景光线
        val lowLightOptions = LowLightEnhanceOptions()
        mRtcEngine?.setLowlightEnhanceOptions(true, lowLightOptions)
    }

    fun joinChannel(agoraToken: String?, agoraChannelId: String?, clientUid: String?) {
        // 创建 ChannelMediaOptions 对象，并进行配置
        val options = ChannelMediaOptions()
        // 根据场景将用户角色设置为 BROADCASTER (主播) 或 AUDIENCE (观众)
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
        // 直播场景下，设置频道场景为 BROADCASTING (直播场景)
        options.channelProfile = Constants.CHANNEL_PROFILE_LIVE_BROADCASTING

        options.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY

        // 发布麦克风采集的音频
        options.publishMicrophoneTrack = true
        // 发布摄像头采集的视频
        options.publishCameraTrack = true
        // 自动订阅所有音频流
        options.autoSubscribeAudio = true
        // 自动订阅所有视频流
        options.autoSubscribeVideo = true

        // 使用临时 Token 加入频道，自行指定用户 ID 并确保其在频道内的唯一性
        mRtcEngine?.joinChannel(agoraToken, agoraChannelId, clientUid?.toIntOrNull() ?: 1, options)
    }

    // 静音
    override fun muteLocalAudioStream(enable: Boolean) {
        // 不上传本地采集的音频数据
//        mRtcEngine?.muteLocalAudioStream(!enable)
        adjustVolume(enable)
    }

    private fun adjustVolume(mute: Boolean) {
        if (mute) {
            mRtcEngine?.adjustRecordingSignalVolume(0)
        } else {
            mRtcEngine?.adjustRecordingSignalVolume(20)
        }
    }

    fun adjustRecordingSignalVolume(volumn: Int) {
        mRtcEngine?.adjustRecordingSignalVolume(volumn)
    }

    // 翻转摄像头
    override fun flipCamera() {
        mRtcEngine?.switchCamera()
    }

//    fun pause() {
//        disableAudio()
//        if (!config.audioOnly) {
//            stopVideo()
//        }
//    }
//
//    fun resume() {
//        enableAudio()
//        if (!config.audioOnly) {
//            startVideo()
//        }
//    }

    override fun startVideo() {
        mRtcEngine?.enableVideo()
        setupLocalVideo()
        mRtcEngine?.enableLocalVideo(true)
        mRtcEngine?.startPreview()
    }

    override fun stopVideo() {
        mRtcEngine?.disableVideo()
        mRtcEngine?.enableLocalVideo(false)
        mRtcEngine?.stopPreview()
    }

    fun enableVideoPreview() {
        mRtcEngine?.enableLocalVideo(true)
        mRtcEngine?.startPreview()
    }

    override fun enableAudio() {
        mRtcEngine?.enableAudio()
        mRtcEngine?.enableLocalAudio(true)
    }

    override fun disableAudio() {
        mRtcEngine?.disableAudio()
        mRtcEngine?.enableLocalAudio(false)
    }

    private fun setupLocalVideo() {
        GlobalScope.launch(Dispatchers.Main) {
            // 创建一个 SurfaceView 对象，并将其作为 FrameLayout 的子对象
            val surfaceView = TextureView(context)
            callback?.addLocalVideoSurface(surfaceView)
            // 将 SurfaceView 对象传入声网实时互动 SDK，设置本地视图
            mRtcEngine?.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 1))
        }
    }

    // 关闭rtc
    override fun close() {
        if (mRtcEngine == null) {
            return
        }
        // 停止本地视频预览
        mRtcEngine?.stopPreview()
        leaveChannel()
        mRtcEngine = null
        RtcEngine.destroy()
    }

    // 离开频道
    fun leaveChannel() {
        mRtcEngine?.leaveChannel()
    }

    fun setRtcEngineCallback(callback: IRtcEngineCallback) {
        this.callback = callback
    }
}