package h.Hchat.hooks.items.customnotify

import android.app.Activity
import android.content.Context
import android.content.Intent
import h.Hchat.hooks.items.conversationgroup.ConversationGroupFileRequest
import h.Hchat.hooks.items.conversationgroup.ConversationGroupFileResultHooks
import h.Hchat.utils.HLog
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** 复用现有有界文件请求，取消/销毁后不会覆盖图标或保留页面回调。 */
object CustomNotificationIconPicker {
    private val requests = ConcurrentHashMap<Int, ConversationGroupFileRequest<String?>>()
    private val sequence = AtomicInteger(0x7810)
    private val hooks = ConversationGroupFileResultHooks(::onResult)

    fun launch(activity: Activity, callback: (Boolean) -> Unit) {
        if (!hooks.ensure(activity.javaClass)) { callback(false); return }
        val code = (0..239).asSequence().map {
            sequence.updateAndGet { if (it >= 0x78ff) 0x7810 else it + 1 }
        }.firstOrNull { !requests.containsKey(it) } ?: run { callback(false); return }
        val appContext = activity.applicationContext
        val request = ConversationGroupFileRequest.create<String?>(
            activity = activity,
            callback = { path ->
                val saved = path != null && CustomNotificationIconStore.commit(appContext, path)
                callback(saved)
            },
            onDiscard = { path -> CustomNotificationIconStore.discard(appContext, path) },
            onReleased = { requests.remove(code) }
        ) ?: run { callback(false); return }
        requests[code] = request
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try { activity.startActivityForResult(intent, code) }
        catch (first: Throwable) {
            try {
                val fallback = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                activity.startActivityForResult(Intent.createChooser(fallback, "选择通知图标"), code)
            } catch (error: Throwable) {
                HLog.e("[Hchat:NotificationIcon] 打开图片选择器失败", error)
                request.deliver(null)
            }
        }
    }

    private fun onResult(owner: Activity, code: Int, result: Int, data: Intent?) {
        val request = requests[code] ?: return
        if (request.activity.get() !== owner) return
        val uri = data?.data
        if (result != Activity.RESULT_OK || uri == null) { request.deliver(null); return }
        val appContext = owner.applicationContext
        request.execute(
            task = { canceled -> if (canceled.get()) null else CustomNotificationIconStore.prepare(appContext, uri) },
            failure = { error -> HLog.e("[Hchat:NotificationIcon] 读取图片失败", error); null }
        )
    }

    fun reset(context: Context): Boolean {
        requests.values.toList().forEach { it.cancel() }
        return CustomNotificationIconStore.reset(context)
    }

    internal fun destroy() {
        requests.values.toList().forEach { it.cancel() }
        requests.clear()
        hooks.clear()
    }
}
