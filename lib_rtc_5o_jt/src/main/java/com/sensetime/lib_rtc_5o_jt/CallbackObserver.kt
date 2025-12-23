package com.sensetime.lib_rtc_5o_jt

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class CallbackObserver(private val callback: ISenseEngineCallback) : DefaultLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        SenseEngine.getDefault.addCallback(callback)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        SenseEngine.getDefault.removeCallback(callback)
    }
}
