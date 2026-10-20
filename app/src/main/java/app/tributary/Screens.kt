package app.tributary

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Overlay { None, Search, Settings }

fun Context.launchApp(a: AppEntry) {
    try {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(a.component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {}
}

private fun Context.open(i: Intent) {
    try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
}

@Composable
fun FlowRoot(vm: FlowViewModel) {
    val themeIdx by vm.themeIndex.collectAsState()
    val theme = Themes[themeIdx.coerceIn(0, Themes.lastIndex)]
    var overlay by remember { mutableStateOf(Overlay.None) }
    val homeTick by vm.homeTick.collectAsState()
    LaunchedEffect(homeTick) { overlay = Overlay.None }
    BackHandler(enabled = overlay != Overlay.None) { overlay = Overlay.None }

    Box(Modifier.fillMaxSize().background(theme.bg)) {
        HomeFeed(vm, theme) { overlay = Overlay.Settings }
        Dock(vm, theme, { overlay = Overlay.Search }, Modifier.align(Alignment.BottomCenter))
        AnimatedVisibility(overlay == Overlay.Search, enter = fadeIn(), exit = fadeOut()) {
            SearchScreen(vm, theme) { overlay = Overlay.None }
        }
        AnimatedVisibility(overlay == Overlay.Settings, enter = fadeIn(), exit = fadeOut()) {
            SettingsScreen(vm, theme) { overlay = Overlay.None }
        }
    }
}

@Composable
fun HomeFeed(vm: FlowViewModel, theme: FlowTheme, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    val feed by vm.feed.collectAsState()
    val notifs by NotificationHub.items.collectAsState()
    val apps by vm.apps.collectAsState()
    val refreshing by vm.refreshing.collectAsState()
    val listenerOn by vm.listenerOn.collectAsState()
    val byPkg = remember(apps) { apps.associateBy { it.pkg } }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = top + 8.dp, bottom = bottom + 130.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalItemSpacing = 10.dp
    ) {
        item(key = "header", span = StaggeredGridItemSpan.FullLine) {
            Header(theme, refreshing, { vm.refreshFeed() }, onSettings)
        }
        if (!listenerOn) {
            item(key = "perm", span = StaggeredGridItemSpan.FullLine) {
                Surface(
                    shape = RoundedCornerShape(16.dp), color = theme.card,
                    modifier = Modifier.fillMaxWidth().clickable {
                        ctx.open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                ) {
                    Text(
                        "Tap to show your notifications at the top of the feed",
                        color = theme.accent, modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
        items(notifs, key = { "n:" + it.key }, span = { StaggeredGridItemSpan.FullLine }) { n ->
            Surface(
                shape = RoundedCornerShape(16.dp), color = theme.card,
                modifier = Modifier.fillMaxWidth().clickable {
                    try { n.intent?.send() } catch (_: Exception) {}
                }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val appIcon = byPkg[n.pkg]
                    if (appIcon != null) {
                        Image(appIcon.icon, null, Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(n.title, color = theme.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(n.text, color = theme.sub, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Text("✕", color = theme.sub, modifier = Modifier.clickable { NotificationHub.dismiss(n.key) }.padding(8.dp))
                }
            }
        }
        items(feed, key = { "f:" + it.id }) { item ->
            Surface(
                shape = RoundedCornerShape(16.dp), color = theme.card,
                modifier = Modifier.fillMaxWidth().clickable {
                    if (item.link.isNotBlank()) ctx.open(Intent(Intent.ACTION_VIEW, Uri.parse(item.link)))
                }
            ) {
                Column {
                    val image = item.image
                    if (image != null) {
                        AsyncImage(
                            model = image, contentDescription = null, contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp, max = 170.dp)
                        )
                    }
                    Column(Modifier.padding(12.dp)) {
                        val age = if (item.time > 0)
                            DateUtils.getRelativeTimeSpanString(item.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
                        else ""
                        Text("${item.source} $age".trim(), color = theme.accent, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(item.title, color = theme.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        if (item.summary.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(item.summary, color = theme.sub, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Header(theme: FlowTheme, refreshing: Boolean, onRefresh: () -> Unit, onSettings: () -> Unit) {
    val ctx = LocalContext.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(30_000) }
    }
    val day = remember(now / 60_000) { SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(now)) }
    val date = remember(now / 60_000) { SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(now)) }
    val time = android.text.format.DateFormat.getTimeFormat(ctx).format(Date(now))
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), Arrangement.SpaceBetween, Alignment.Bottom) {
        Column {
            Text(day, color = theme.text, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("$date · $time", color = theme.sub, fontSize = 15.sp)
        }
        Row {
            Text(if (refreshing) "…" else "↻", color = theme.text, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onRefresh).padding(10.dp))
            Text("⚙", color = theme.text, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onSettings).padding(10.dp))
        }
    }
}

@Composable
fun Dock(vm: FlowViewModel, theme: FlowTheme, onOpenSearch: () -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val apps by vm.apps.collectAsState()
    val dockPkgs by vm.dock.collectAsState()
    val dockApps = remember(apps, dockPkgs) { dockPkgs.mapNotNull { p -> apps.firstOrNull { it.pkg == p } } }
    Surface(
        modifier = modifier.fillMaxWidth().pointerInput(Unit) {
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = { if (total < -40f) onOpenSearch() }
            ) { _, d -> total += d }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = theme.card, shadowElevation = 8.dp
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
                dockApps.forEach { a ->
                    AppIcon(a, theme, false, true, { vm.toggleDock(a.pkg) }) { ctx.launchApp(a) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(50), color = theme.bg.copy(alpha = 1f),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSearch)
            ) {
                Text("Search apps…", color = theme.sub, modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIcon(app: AppEntry, theme: FlowTheme, showLabel: Boolean, pinned: Boolean, onPinToggle: () -> Unit, onLaunch: () -> Unit) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Column(
            Modifier.width(72.dp).combinedClickable(onClick = onLaunch, onLongClick = { menu = true }).padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(app.icon, app.label, Modifier.size(52.dp))
            if (showLabel) Text(app.label, color = theme.text, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text(if (pinned) "Remove from dock" else "Add to dock") }, onClick = { menu = false; onPinToggle() })
            DropdownMenuItem(text = { Text("App info") }, onClick = {
                menu = false
                ctx.open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}")))
            })
            DropdownMenuItem(text = { Text("Uninstall") }, onClick = {
                menu = false
                ctx.open(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")))
            })
        }
    }
}

@Composable
fun SearchScreen(vm: FlowViewModel, theme: FlowTheme, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val apps by vm.apps.collectAsState()
    val dock by vm.dock.collectAsState()
    var q by remember { mutableStateOf("") }
    val results = remember(apps, q) {
        if (q.isBlank()) apps
        else apps.map { it to fuzzyScore(q, it.label) }.filter { it.second >= 0 }.sortedByDescending { it.second }.map { it.first }
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.fillMaxSize().background(theme.bg.copy(alpha = 1f)).statusBarsPadding().imePadding().padding(12.dp)) {
        TextField(
            value = q, onValueChange = { q = it }, singleLine = true,
            placeholder = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            shape = RoundedCornerShape(50),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = theme.card, unfocusedContainerColor = theme.card,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = theme.text, unfocusedTextColor = theme.text
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { results.firstOrNull()?.let { ctx.launchApp(it); onClose() } })
        )
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(GridCells.Adaptive(76.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(results, key = { it.component.flattenToString() }) { a ->
                AppIcon(a, theme, true, a.pkg in dock, { vm.toggleDock(a.pkg) }) { ctx.launchApp(a); onClose() }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: FlowViewModel, theme: FlowTheme, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val feeds by vm.feeds.collectAsState()
    val themeIdx by vm.themeIndex.collectAsState()
    var newUrl by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(theme.bg.copy(alpha = 1f)).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Settings", color = theme.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Done", color = theme.accent, modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
        }
        Text("Theme", color = theme.sub)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Themes.forEachIndexed { i, t ->
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(t.card)
                        .border(2.dp, if (i == themeIdx) t.accent else Color.Transparent, RoundedCornerShape(50))
                        .clickable { vm.setTheme(i) }.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(t.name, color = t.text, fontSize = 13.sp)
                }
            }
        }
        Button(onClick = { ctx.open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }) { Text("Notification access") }
        OutlinedButton(onClick = { ctx.open(Intent(Settings.ACTION_HOME_SETTINGS)) }) { Text("Set as default home app") }
        Text("Feeds (RSS / Atom URLs)", color = theme.sub)
        feeds.forEach { url ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(url, color = theme.text, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text("✕", color = theme.sub, modifier = Modifier.clickable { vm.removeFeed(url) }.padding(8.dp))
            }
        }
        OutlinedTextField(
            value = newUrl, onValueChange = { newUrl = it }, singleLine = true, label = { Text("https://…") },
            textStyle = TextStyle(color = theme.text), modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { vm.addFeed(newUrl.trim()); newUrl = "" }) { Text("Add feed") }
    }
}
