package h.Hchat.hooks.items.customnotify

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.util.AtomicFile
import h.Hchat.preferences.HchatStorage
import h.Hchat.utils.HLog
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/** 兼容旧定制包的图标路径；后台准备、页面存活时提交，取消不改已有图标。 */
object CustomNotificationIconStore {
    private const val TAG = "[Hchat:NotificationIcon]"
    private const val MAX_EDGE = 256
    private const val MAX_BYTES = 1024 * 1024L
    private data class Cache(val path: String, val modified: Long, val size: Long, val bitmap: Bitmap)
    private var cache: Cache? = null

    @JvmStatic
    @Synchronized
    fun hasCustomIcon(context: Context): Boolean = sources(context).any { it.isFile && it.length() in 1..MAX_BYTES }

    @JvmStatic
    fun apply(builder: Notification.Builder, defaultIcon: Int, context: Context): Notification.Builder {
        val bitmap = load(context)
        if (bitmap != null) {
            try { return builder.setSmallIcon(Icon.createWithBitmap(bitmap)) }
            catch (error: Throwable) { HLog.e("$TAG 设置小图标失败，回退原图标", error) }
        }
        return builder.setSmallIcon(defaultIcon)
    }

    internal fun prepare(context: Context, uri: Uri): String? {
        var bitmap: Bitmap? = null
        var target: File? = null
        return try {
            bitmap = decode(context, uri) ?: return null
            target = File(HchatStorage.storageDir(context), ".notify-icon-${UUID.randomUUID()}.png")
            FileOutputStream(target).use {
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                it.fd.sync()
            }
            check(target.length() in 1..MAX_BYTES)
            target.absolutePath
        } catch (error: Throwable) {
            target?.delete()
            HLog.e("$TAG 准备通知图片失败", error)
            null
        } finally { bitmap?.recycle() }
    }

    @Synchronized
    internal fun commit(context: Context, path: String): Boolean {
        val source = preparedFile(context, path) ?: return false
        val target = AtomicFile(iconFile(context))
        var stream: FileOutputStream? = null
        return try {
            check(source.length() in 1..MAX_BYTES)
            stream = target.startWrite()
            source.inputStream().use { it.copyTo(stream) }
            stream.fd.sync()
            target.finishWrite(stream)
            stream = null
            File(context.cacheDir, "eta_notify_icon.png").delete()
            cache = null
            true
        } catch (error: Throwable) {
            stream?.let { target.failWrite(it) }
            HLog.e("$TAG 提交通知图标失败，保留旧图标", error)
            false
        } finally { source.delete() }
    }

    internal fun discard(context: Context, path: String?) {
        path?.let { preparedFile(context, it)?.delete() }
    }

    @JvmStatic
    @Synchronized
    fun reset(context: Context): Boolean {
        return try {
            AtomicFile(iconFile(context)).delete()
            File(context.cacheDir, "eta_notify_icon.png").delete()
            File(HchatStorage.storageDir(context), "custom_notification_small_icon.png").delete()
            cache = null
            !hasCustomIcon(context)
        } catch (error: Throwable) { HLog.e("$TAG 恢复图标失败", error); false }
    }

    @Synchronized
    private fun load(context: Context): Bitmap? {
        for (file in sources(context)) {
            if (!file.isFile || file.length() !in 1..MAX_BYTES) continue
            val modified = file.lastModified()
            val size = file.length()
            cache?.takeIf { it.path == file.absolutePath && it.modified == modified && it.size == size && !it.bitmap.isRecycled }
                ?.let { return it.bitmap }
            val bitmap = runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
                var sample = 1
                while (max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_EDGE * 2) sample *= 2
                BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            }.getOrNull() ?: continue
            // 不 recycle 已缓存的位图：Notification.Builder 可能仍持有它。
            cache = Cache(file.absolutePath, modified, size, bitmap)
            return bitmap
        }
        cache = null
        return null
    }

    private fun sources(context: Context) = listOf(
        iconFile(context), File(context.cacheDir, "eta_notify_icon.png"),
        File(HchatStorage.storageDir(context), "custom_notification_small_icon.png")
    )
    private fun iconFile(context: Context) = File(HchatStorage.storageDir(context), "notify_icon.png")
    private fun preparedFile(context: Context, path: String): File? {
        val file = File(path).canonicalFile
        return file.takeIf {
            it.parentFile == HchatStorage.storageDir(context).canonicalFile &&
                it.name.startsWith(".notify-icon-") && it.name.endsWith(".png")
        }
    }

    private fun decode(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_EDGE * 2) sample *= 2
        val source = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val edge = max(source.width, source.height)
        if (edge <= MAX_EDGE) return source
        val scale = MAX_EDGE.toFloat() / edge
        val result = Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true)
        if (result !== source) source.recycle()
        return result
    }
}
