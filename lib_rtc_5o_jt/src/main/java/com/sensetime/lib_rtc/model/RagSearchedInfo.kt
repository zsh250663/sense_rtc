package com.sensetime.lib_rtc.model

data class RagSearchedInfo(
    val type: String?,
    val Info: List<RagSearchItem>?
)

data class RagSearchItem(
    val Filename: String?,
    val text: String?
)
