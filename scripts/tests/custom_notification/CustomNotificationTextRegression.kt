package h.Hchat.hooks.items.customnotify

fun main() {
    check(CustomNotificationText.clean("[4条]张三: 你好", "张三") == "你好")
    check(CustomNotificationText.clean("李四: 你好", "群聊") == "李四: 你好")
    check(CustomNotificationText.clean("[金额]100", "") == "[金额]100")
    check(CustomNotificationText.unread("[2条]你好", 4) == "[4]你好")
    check(CustomNotificationText.unread("[4]你好", 4) == "[4]你好")
    check(CustomNotificationText.unread("你好", 1) == "你好")
    check(CustomNotificationText.expandedLines(listOf("甲", "乙"), 4) == listOf("[4]", "甲", "乙"))
    check(CustomNotificationText.expandedLines(listOf("甲", "乙"), 3) == listOf("甲", "乙"))
    check(CustomNotificationText.emoji("") == "[表情]")
    check(CustomNotificationText.emoji("   ") == "[表情]")
    check(CustomNotificationText.emoji("[微笑]") == "[微笑]")
    println("PASS: 12 production notification text regressions")
}
