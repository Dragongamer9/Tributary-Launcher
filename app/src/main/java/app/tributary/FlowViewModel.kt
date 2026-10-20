package app.tributary

import android.app.Application
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FlowViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    val apps = MutableStateFlow<List<AppEntry>>(emptyList())
    val feed = MutableStateFlow<List<FeedItem>>(emptyList())
    val refreshing = MutableStateFlow(false)
    val feeds = MutableStateFlow(prefs.feeds)
    val dock = MutableStateFlow(prefs.dock ?: emptyList())
    val themeIndex = MutableStateFlow(prefs.theme)
    val listenerOn = MutableStateFlow(false)
    val homeTick = MutableStateFlow(0)
    val layout = MutableStateFlow(prefs.layout)
    val leftHanded = MutableStateFlow(prefs.leftHanded)
    val hidden = MutableStateFlow(prefs.hidden)
    val headerImage = MutableStateFlow(prefs.headerImage)
    private var lastFetch = 0L

    init {
        reloadApps()
        refreshFeed()
    }

    fun reloadApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val repo = AppRepository(getApplication())
            apps.value = repo.load()
            if (prefs.dock == null) {
                val d = repo.defaultDock().take(4)
                dock.value = d
                prefs.dock = d
            }
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            refreshing.value = true
            feed.value = withContext(Dispatchers.IO) { FeedRepository.fetchAll(feeds.value) }
            lastFetch = System.currentTimeMillis()
            refreshing.value = false
        }
    }

    fun onResume() {
        val a = getApplication<Application>()
        listenerOn.value = NotificationManagerCompat.getEnabledListenerPackages(a).contains(a.packageName)
        if (System.currentTimeMillis() - lastFetch > 15 * 60_000) refreshFeed()
    }

    fun addFeed(u: String) {
        if (u.startsWith("http") && u !in feeds.value) {
            feeds.value = feeds.value + u
            prefs.feeds = feeds.value
            refreshFeed()
        }
    }

    fun removeFeed(u: String) {
        feeds.value = feeds.value - u
        prefs.feeds = feeds.value
        refreshFeed()
    }

    fun toggleDock(pkg: String) {
        val cur = dock.value
        dock.value = if (pkg in cur) cur - pkg else (cur + pkg).takeLast(5)
        prefs.dock = dock.value
    }

    fun setTheme(i: Int) {
        themeIndex.value = i
        prefs.theme = i
    }

    fun setLayout(i: Int) {
        layout.value = i
        prefs.layout = i
    }

    fun setLeftHanded(v: Boolean) {
        leftHanded.value = v
        prefs.leftHanded = v
    }

    fun toggleHidden(pkg: String) {
        hidden.value = if (pkg in hidden.value) hidden.value - pkg else hidden.value + pkg
        prefs.hidden = hidden.value
    }

    fun setHeaderImage(uri: String?) {
        headerImage.value = uri
        prefs.headerImage = uri
    }
}
