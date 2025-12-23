package com.sensetime.lib_rtc_5o_jt.model

data class FnCallEnd<T>(
    val type: String?,
    val call_id: String?,
    val status: String?,
    val result_summary: T
)
