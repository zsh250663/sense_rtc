package com.sensetime.lib_rtc.util

class TagParser {
    interface TagCallback {
        fun onTagDetected(tagName: String, content: String)
    }

    private var callback: TagCallback? = null
    private val buffer = StringBuilder()
    private var insideTag = false
    private var currentTagName: String? = null

    fun setTagCallback(callback: TagCallback) {
        this.callback = callback
    }

    fun processToken(token: String): String {
        buffer.append(token)

        if (!insideTag && buffer.contains("<")) {
            insideTag = true
        }

        if (insideTag) {
            val bufferString = buffer.toString()

            val selfClosingTagRegex = Regex("<(\\w+)/>")
            val selfClosingMatch = selfClosingTagRegex.find(bufferString)
            if (selfClosingMatch != null) {
                val tagName = selfClosingMatch.groupValues[1]
                callback?.onTagDetected(tagName, "")
                buffer.clear()
                insideTag = false
                return ""
            }

            val openingTagRegex = Regex("<(\\w+)>(.*?)</\\1>")
            val openingMatch = openingTagRegex.find(bufferString)
            if (openingMatch != null) {
                val tagName = openingMatch.groupValues[1]
                val content = openingMatch.groupValues[2]
                callback?.onTagDetected(tagName, content)
                buffer.clear()
                insideTag = false
                return ""
            }

            val incompleteTagRegex = Regex("<(\\w+)>")
            val incompleteMatch = incompleteTagRegex.find(bufferString)
            if (incompleteMatch != null) {
                currentTagName = incompleteMatch.groupValues[1]
            }

            if (currentTagName != null && bufferString.contains("</$currentTagName>")) {
                val contentStartIndex = bufferString.indexOf(">", bufferString.indexOf("<$currentTagName>")) + 1
                val contentEndIndex = bufferString.indexOf("</$currentTagName>")
                val content = bufferString.substring(contentStartIndex, contentEndIndex)
                callback?.onTagDetected(currentTagName!!, content)
                buffer.clear()
                insideTag = false
                currentTagName = null
                return ""
            }

            return ""
        }

        return token
    }
}
