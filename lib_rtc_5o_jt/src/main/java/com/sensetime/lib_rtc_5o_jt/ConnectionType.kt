package com.sensetime.lib_rtc_5o_jt

enum class ConnectionType(val value: Int) {
    // 0: 普通音视频，带联网搜索  1: 讲故事  2:普通音视频
    OPEN_CALL_SEARCH(0), STORY(1), CALL_WITHOUT_SEARCH(2)
}