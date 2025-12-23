package com.sensetime.lib_rtc_5o_jt

interface IExposedFunction {
    fun onCreateSessionResult(sessionId: String?)
    fun getLatestToken(): String?
    fun getRagId(): String?
    suspend fun checkAndRefreshTokenIfNeeded(): Boolean
}