package com.sensetime.lib_rtc.model

data class FnCallEnd<T>(
    val type: String?,
    val call_id: String?,
    val status: String?,
    val result_summary: T
)
