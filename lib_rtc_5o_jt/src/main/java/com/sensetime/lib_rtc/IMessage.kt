package com.sensetime.lib_rtc

import com.sensetime.lib_rtc.model.MessageContent

interface IMessage {
    fun sendMsg(
        text: String? = null,
        images: List<MessageContent>? = null,
        audio: MessageContent? = null
    )
    fun setSystemPrompt(system_prompt: String?)
    fun setSystemPrompt(system_prompt: String?, prompt_type: String?)
    fun setSystemPrompt(story_id: Int, prompt_type: String?)
    fun setVoiceTTsType(voice_type: String?)
    fun configPipeline(story_id: Int)

    fun postSayIt(message: String)
}