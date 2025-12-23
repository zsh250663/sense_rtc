package com.sensetime.lib_rtc

interface IExposedFunction {
    fun onCreateSessionResult(sessionId: String?)
    fun getLatestToken(): String?
    fun getRagId(): String?
    suspend fun checkAndRefreshTokenIfNeeded(): Boolean
}