package h.Hchat.hooks.items.customnotify

/** 6.5.0 定制通知格式的纯文本规则；不读取微信数据库或推测发送人。 */
internal object CustomNotificationText {
    private val legacyCount = Regex("^\\[\\d+条]")
    private val currentCount = Regex("^\\[\\d+]")

    fun removeCount(text: String): String = text.replace(legacyCount, "").trim()

    fun clean(text: String, title: String): String {
        val result = removeCount(text)
        val prefix = title.takeIf { it.isNotEmpty() }?.plus(":") ?: return result
        return if (result.startsWith(prefix)) result.removePrefix(prefix).trimStart() else result
    }

    fun unread(text: String, count: Int): String {
        val result = removeCount(text)
        return if (count <= 1 || currentCount.containsMatchIn(result)) result else "[$count]$result"
    }

    fun expandedLines(lines: List<String>, count: Int): List<String> =
        if (count > 3 && lines.size > 1) listOf("[$count]") + lines else lines

    // 原补丁：表情正文为空时显示占位，正文非空时保留正文。
    fun emoji(body: String): String = if (body.isBlank()) "[表情]" else body
}
