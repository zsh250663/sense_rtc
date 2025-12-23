package com.sensetime.lib_rtc.model

data class MessageContent(
    val type: String?,
    val url: String? = null,
    val base64: String? = null
)

data class MultiTypeMessage(
    val type: String?,
    val text: String? = null,
    val images: List<MessageContent>? = null,
    val audio: MessageContent?
)
