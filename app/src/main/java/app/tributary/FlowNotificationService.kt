package app.tributary

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class FlowNotificationService : NotificationListenerService() {
    override fun onListenerConnected() { NotificationHub.service = this; refresh() }
    override fun onListenerDisconnected() { NotificationHub.service = null }
    override fun onNotificationPosted(sbn: StatusBarNotification?) { refresh() }
    override fun onNotificationRemoved(sbn: StatusBarNotification?) { refresh() }

    private fun refresh() {
        val list: List<StatusBarNotification> =
            try { activeNotifications?.toList() ?: emptyList() } catch (e: Exception) { emptyList() }
        NotificationHub.items.value = list
            .filter {
                it.isClearable && it.packageName != packageName &&
                    (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
            }
            .sortedByDescending { it.postTime }
            .map {
                val e = it.notification.extras
                NotifItem(
                    it.key, it.packageName,
                    e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: "",
                    e.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: "",
                    it.notification.contentIntent, it.postTime
                )
            }
    }
}
