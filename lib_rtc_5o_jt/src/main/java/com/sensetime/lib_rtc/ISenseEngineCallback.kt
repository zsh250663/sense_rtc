package com.sensetime.lib_rtc

import com.sensetime.lib_rtc.util.TagParser

interface ISenseEngineCallback : IRtcEngineCallback, MessageCallback, TagParser.TagCallback {
}