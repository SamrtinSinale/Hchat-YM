package h.Hchat.hooks.items.customnotify

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import h.Hchat.ui.FeatureSettingsProvider
import h.Hchat.ui.miuix.ActionRow
import h.Hchat.ui.miuix.BottomActionBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import h.Hchat.ui.miuix.InsetDivider
import h.Hchat.ui.miuix.PageScaffold
import h.Hchat.ui.miuix.SettingsCard
import top.yukonga.miuix.kmp.basic.SmallTitle

@Composable
internal fun CustomNotificationIconMiuixPage(
    context: Context,
    provider: FeatureSettingsProvider,
    onBack: () -> Unit
) {
    val activity = remember(context) { findActivity(context) }
    val scrollBehavior = MiuixScrollBehavior()
    val listState = rememberLazyListState()
    PageScaffold(
        title = provider.title(),
        largeTitle = provider.title(),
        scrollBehavior = scrollBehavior,
        bottomBar = { BottomActionBar("返回", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 84.dp
            )
        ) {
            item { SmallTitle(text = "通知小图标") }
            item {
                SettingsCard {
                    ActionRow(
                        "选择自定义图标",
                        if (CustomNotificationIconStore.hasCustomIcon(context)) "已设置自定义通知图标" else "使用微信默认图标"
                    ) {
                        val owner = activity
                        if (owner == null || owner.isFinishing || owner.isDestroyed) {
                            Toast.makeText(context, "当前页面无法打开图片选择器", Toast.LENGTH_SHORT).show()
                        } else {
                            CustomNotificationIconPicker.launch(owner) { saved ->
                                Toast.makeText(owner, if (saved) "通知图标已更新" else "通知图标保存失败或已取消", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    InsetDivider()
                    ActionRow("恢复默认图标", "清除自定义图片，恢复微信原始通知图标") {
                        val restored = CustomNotificationIconPicker.reset(context)
                        Toast.makeText(
                            context,
                            if (restored) "已恢复默认通知图标" else "恢复默认图标失败",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }
}

private tailrec fun findActivity(context: Context?): Activity? = when (context) {
    null -> null
    is Activity -> context
    is android.content.ContextWrapper -> findActivity(context.baseContext)
    else -> null
}
