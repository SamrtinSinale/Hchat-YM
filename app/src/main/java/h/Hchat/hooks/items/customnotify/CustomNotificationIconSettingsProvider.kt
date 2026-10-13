package h.Hchat.hooks.items.customnotify

import h.Hchat.hooks.core.BaseFeature
import h.Hchat.hooks.core.FeatureContext
import h.Hchat.ui.FeatureSettingsProvider
import h.Hchat.ui.SimpleFeatureSettingsProvider

class CustomNotificationIconFeature : BaseFeature() {
    override fun featureId(): String = ID
    override fun name(): String = "自定义通知图标"
    override fun isEnabled(context: FeatureContext): Boolean = true

    override fun onFeatureInit(context: FeatureContext) {
        registerSettingsProvider(CustomNotificationIconSettingsProvider())
    }

    override fun onFeatureInstall(context: FeatureContext) = Unit

    override fun onFeatureDestroy(context: FeatureContext) {
        CustomNotificationIconPicker.destroy()
    }

    companion object {
        const val ID = "custom_notification_icon"
        const val REQUEST_CODE = 0x2768
    }
}

class CustomNotificationIconSettingsProvider : SimpleFeatureSettingsProvider(
    CustomNotificationIconFeature.ID,
    "自定义通知图标",
    "选择并恢复微信通知栏的小图标",
    FeatureSettingsProvider.CATEGORY_ENHANCE
)
