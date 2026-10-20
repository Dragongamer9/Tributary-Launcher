package app.tributary

import android.util.Xml
import androidx.core.text.HtmlCompat
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Public social APIs (Twitter, Instagram, Facebook, Feedly) are closed or heavily restricted today.
 * RSS/Atom still works for Reddit, Mastodon, Tumblr, YouTube, blogs and news sites.
 */
object FeedRepository {
    suspend fun fetchAll(urls: List<String>): List<FeedItem> = coroutineScope {
        urls.map { u -> async { runCatching { fetch(u) }.getOrDefault(emptyList()) } }
            .awaitAll().flatten().distinctBy { it.id }.sortedByDescending { it.time }
    }

    private fun fetch(url: String): List<FeedItem> {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "Tributary/0.1 (Android)")
        return try { c.inputStream.use { parse(it, URL(url).host) } } finally { c.disconnect() }
    }

    private val datePatterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z", "EEE, dd MMM yyyy HH:mm:ss zzz",
        "yyyy-MM-dd'T'HH:mm:ssXXX", "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
    )

    // SimpleDateFormat is not thread-safe and feeds are parsed concurrently, so build one per call.
    private fun parseDate(s: String): Long {
        val t = s.trim()
        for (p in datePatterns) {
            try { return SimpleDateFormat(p, Locale.US).parse(t)!!.time } catch (_: Exception) {}
        }
        return 0L
    }

    private val imgRegex = Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private fun strip(s: String) = HtmlCompat.fromHtml(s, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()

    fun parse(stream: InputStream, fallbackSource: String): List<FeedItem> {
        val out = mutableListOf<FeedItem>()
        try {
            val p = Xml.newPullParser()
            p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            p.setInput(stream, null)
            var source = fallbackSource
            var sourceSet = false
            var inItem = false
            var title = ""
            var link = ""
            var desc = ""
            var img: String? = null
            var date = ""
            var guid = ""
            var ev = p.eventType
            while (ev != XmlPullParser.END_DOCUMENT) {
                if (ev == XmlPullParser.START_TAG) {
                    val n = p.name
                    if (n == "item" || n == "entry") {
                        inItem = true; title = ""; link = ""; desc = ""; img = null; date = ""; guid = ""
                    } else if (inItem && n == "title") {
                        title = p.nextText()
                    } else if (inItem && n == "link") {
                        val href = p.getAttributeValue(null, "href")
                        val rel = p.getAttributeValue(null, "rel")
                        val t = p.nextText()
                        if (href != null) {
                            if (rel == null || rel == "alternate") link = href
                        } else {
                            link = t.trim()
                        }
                    } else if (inItem && (n == "description" || n == "summary" || n == "content:encoded" || n == "content")) {
                        val t = p.nextText()
                        if (n == "content:encoded" || desc.isEmpty()) desc = t
                        if (img == null) img = imgRegex.find(t)?.groupValues?.get(1)
                    } else if (inItem && (n == "pubDate" || n == "updated" || n == "published" || n == "dc:date")) {
                        date = p.nextText()
                    } else if (inItem && (n == "guid" || n == "id")) {
                        guid = p.nextText()
                    } else if (inItem && (n == "media:thumbnail" || n == "media:content" || n == "enclosure")) {
                        val u = p.getAttributeValue(null, "url")
                        val type = p.getAttributeValue(null, "type") ?: ""
                        if (u != null && img == null && (n == "media:thumbnail" || type.isEmpty() || type.startsWith("image"))) img = u
                    } else if (!inItem && !sourceSet && n == "title") {
                        source = p.nextText().trim()
                        sourceSet = true
                    }
                } else if (ev == XmlPullParser.END_TAG && inItem && (p.name == "item" || p.name == "entry")) {
                    inItem = false
                    out.add(
                        FeedItem(
                            "$source|${guid.ifEmpty { link }}|$title", strip(title), strip(desc).take(240),
                            link.trim(), img, source, parseDate(date)
                        )
                    )
                }
                ev = p.next()
            }
        } catch (_: Exception) {
            // return whatever was parsed before the error
        }
        return out
    }
}
