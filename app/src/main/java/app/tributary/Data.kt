package app.tributary

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.service.notification.NotificationListenerService
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.flow.MutableStateFlow

data class AppEntry(val label: String, val pkg: String, val component: ComponentName, val icon: ImageBitmap)
data class FeedItem(val id: String, val title: String, val summary: String, val link: String,
                    val image: String?, val source: String, val time: Long)
data class NotifItem(val key: String, val pkg: String, val title: String, val text: String,
                     val intent: PendingIntent?, val time: Long)

object NotificationHub {
    val items = MutableStateFlow<List<NotifItem>>(emptyList())
    @Volatile var service: NotificationListenerService? = null
    fun dismiss(key: String) { try { service?.cancelNotification(key) } catch (_: Exception) {} }
}

val DEFAULT_FEEDS = listOf(
    "https://www.theverge.com/rss/index.xml",
    "https://hnrss.org/frontpage",
    "https://www.reddit.com/r/Android/.rss"
)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("tributary", Context.MODE_PRIVATE)
    var feeds: List<String>
        get() = sp.getString("feeds", null)?.split("\n")?.filter { it.isNotBlank() } ?: DEFAULT_FEEDS
        set(v) { sp.edit().putString("feeds", v.joinToString("\n")).apply() }
    var dock: List<String>?
        get() = sp.getString("dock", null)?.split(",")?.filter { it.isNotBlank() }
        set(v) { sp.edit().putString("dock", v?.joinToString(",")).apply() }
    var theme: Int
        get() = sp.getInt("theme", 0)
        set(v) { sp.edit().putInt("theme", v).apply() }
    /** 0 = mosaic (2 columns), 1 = wide (1 column), 2 = narrow (compact rows) */
    var layout: Int
        get() = sp.getInt("layout", 0)
        set(v) { sp.edit().putInt("layout", v).apply() }
    var leftHanded: Boolean
        get() = sp.getBoolean("leftHanded", false)
        set(v) { sp.edit().putBoolean("leftHanded", v).apply() }
    var hidden: Set<String>
        get() = sp.getString("hidden", null)?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        set(v) { sp.edit().putString("hidden", v.joinToString(",")).apply() }
    var headerImage: String?
        get() = sp.getString("headerImage", null)
        set(v) { sp.edit().putString("headerImage", v).apply() }
}

class AppRepository(private val ctx: Context) {
    fun load(): List<AppEntry> {
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(i, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                val ai = ri.activityInfo
                AppEntry(ri.loadLabel(pm).toString(), ai.packageName,
                    ComponentName(ai.packageName, ai.name),
                    ri.loadIcon(pm).toBitmap(128, 128).asImageBitmap())
            }
            .sortedBy { it.label.lowercase() }
    }

    fun defaultDock(): List<String> {
        val pm = ctx.packageManager
        return listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")),
            Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        ).mapNotNull { pm.resolveActivity(it, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName }
            .filter { it != "android" }.distinct()
    }
}

/** Fast fuzzy match: prefix > substring > in-order subsequence. -1 = no match. */
fun fuzzyScore(query: String, target: String): Int {
    val q = query.trim().lowercase(); val t = target.lowercase()
    if (q.isEmpty()) return 0
    if (t.startsWith(q)) return 1000 - t.length
    val idx = t.indexOf(q)
    if (idx >= 0) return 800 - idx
    var qi = 0; var score = 0; var last = -2
    for ((ti, c) in t.withIndex()) {
        if (qi < q.length && c == q[qi]) {
            score += if (ti == last + 1) 5 else 1
            if (ti == 0 || t[ti - 1] == ' ') score += 3
            last = ti; qi++
        }
    }
    return if (qi == q.length) score else -1
}
