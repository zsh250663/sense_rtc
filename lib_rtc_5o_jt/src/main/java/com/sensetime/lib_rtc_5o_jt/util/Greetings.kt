package com.sensetime.lib_rtc_5o_jt.util

object Greetings {

    private val translatorGreetingList = listOf(
//        "<style>高兴</style>遇到陌生单词别慌！手指着问我：这啥意思呀？我来告诉你～",
//        "<style>高兴</style>不懂单词？手指着问：啥意思？",
//        "<style>高兴</style>同学，遇不认识单词别愁！手指着问：老师，啥意思？",
//        "<style>高兴</style>学英语遇陌生单词，别慌！手指着问：老师，单词啥意思？我来解释！",
//        "<style>高兴</style>解单词难题！遇不懂单词，手指着问：“老师，单词啥意思？”",
//        "<style>高兴</style>学英语遇陌生单词，很正常，手指着问：“老师，单词啥意思？” 马上回答！",
        "<style>高兴</style>不懂单词？手指着问：什么意思？",
    )

    private val touristGuideGreetingList = listOf(
//        "<style>高兴</style>举起手机让我看到景点或建筑，试着问：能介绍下这个建筑吗？",
//        "<style>高兴</style>风景等你发现！举起手机让我看到景点或建筑，然后问我：这是什么地方？",
//        "<style>高兴</style>hi，朋友，举起手机让我看到景点或建筑，试试问：“这地方有啥好玩的？” ",
//        "<style>高兴</style>一起领略美景！举起手机让我看到景点或建筑，试试问 ：能介绍下这个地方吗？",
//        "<style>高兴</style>你好，我是导游！举起手机让我看到景点或建筑，问我 “能介绍下这景点吗”。",
//        "<style>高兴</style>欢迎来到这美妙世界！举起手机让我看到景点或建筑，然后说：能介绍下这地方吗？",
        "<style>高兴</style>让我看到景点或建筑，然后说：能介绍一下吧",
    )

    private val storytellerGreetingList = listOf(
//        "<style>高兴</style>欢迎来到魔法绘本世界！摊开绘本让老师看到内容，说‘开始讲故事吧’~",
//        "<style>高兴</style>绘本小船要启航咯！摊开绘本给老师看，然后说‘准备好啦，讲故事吧’~",
//        "<style>高兴</style>准备好听故事没？摊开绘本让老师看到画面，说：老师讲故事吧～",
//        "<style>高兴</style>小朋友们，要进神奇绘本世界啦！摊平绘本让老师看到，准备好就大声说：“老师，快讲绘本故事！”",
//        "<style>高兴</style>要讲绘本故事啦！摊开绘本给老师看，说 “我准备好啦”。",
//        "<style>高兴</style>小朋友们好！摊开绘本让老师看到内容，说 “开始讲故事” 。",
        "<style>高兴</style>摊开绘本，然后说‘开始讲故事吧’~",
    )

    private val characterGreetingList = mapOf(
        // 阳光少女
        "F12" to listOf(
            "<style>高兴</style>嗨，你好呀！我叫5o，我呢，喜欢探索世界的美好。非常高兴认识你，今天有什么好事发生？",
        ),
        // 暖男学长
        "M20" to listOf(
            "<style>高兴</style>嗨，我是5o，商汤的陪伴助手！最近学了个超实用的小技巧，能让心情瞬间变好～ 你今天有没有遇到什么特别的事想聊聊呀？",
            "<style>高兴</style>嘿，我叫5o，今天发现窗台上的小花开了，感觉超治愈的！你呢，今天有什么好事发生？",
        ),
        // 邻家学姐
        "nvguo59" to listOf(
            "<style>期待</style>你好呀，我是5o，今天尝试了新的咖啡，感觉超好喝，你平时有没有什么喜欢的小爱好",
            "<style>期待</style>刚刚泡了一壶茉莉花茶，满屋子都是香气～ 今天，想和我聊些什么？",
        ),
        // 成熟男音
        "zhili" to listOf(
            "<style>高兴</style>你好，我是5o，今天晨跑时发现巷口的老墙钻出了一朵野蔷薇，还挺好看——最近在你生活里有发生什么开心的事情吗？",
            "<style>高兴</style>上班路过早点摊，油条滋啦滋啦响得跟交响乐似的~ 今天，想和我聊些什么？",
        ),
    )

    fun buildGreeting(
        characterId: String,
        translator: Boolean = false,
        touristGuide: Boolean = false,
        storyteller: Boolean = false,
    ): String {
        if (translator) {
            return translatorGreetingList.random()
        }

        if (touristGuide) {
            return touristGuideGreetingList.random()
        }

        if (storyteller) {
            return storytellerGreetingList.random()
        }

        return ""

//        return characterGreetingList[characterId]?.random() ?: ""
    }
}