package app.tributary

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Round photo in the centre, time on one side and the date on the other (like FlowHome's header). */
@Composable
fun PhotoHeader(vm: FlowViewModel, theme: FlowTheme, refreshing: Boolean, onPhotoTap: () -> Unit) {
    val ctx = LocalContext.current
    val photo by vm.headerImage.collectAsState()
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(30_000) }
    }
    val minute = now / 60_000
    val day = remember(minute) { SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(now)) }
    val date = remember(minute) { SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(now)) }
    val time = remember(minute) { android.text.format.DateFormat.getTimeFormat(ctx).format(Date(now)) }

    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, color = theme.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(if (refreshing) "Updating…" else "Today", color = theme.sub, fontSize = 13.sp)
        }
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(theme.accent).clickable(onClick = onPhotoTap),
            contentAlignment = Alignment.Center
        ) {
            val p = photo
            if (p != null) {
                AsyncImage(
                    model = p, contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("T", color = theme.bg.copy(alpha = 1f), fontSize = 38.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day, color = theme.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(date, color = theme.sub, fontSize = 13.sp)
        }
    }
}

/**
 * Quick launcher in the bottom corner (mirrored for left-handed use).
 * Tap to expand your favourite apps, long-press to jump to search.
 * In search mode the button turns into a magnifier.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CornerTray(vm: FlowViewModel, theme: FlowTheme, searchMode: Boolean, onSearch: () -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val apps by vm.apps.collectAsState()
    val dockPkgs by vm.dock.collectAsState()
    val left by vm.leftHanded.collectAsState()
    var open by remember { mutableStateOf(false) }
    val dockApps = remember(apps, dockPkgs) { dockPkgs.mapNotNull { p -> apps.firstOrNull { it.pkg == p } } }
    BackHandler(enabled = open) { open = false }

    Column(
        modifier.navigationBarsPadding().padding(16.dp),
        horizontalAlignment = if (left) Alignment.Start else Alignment.End,
        verticalArrangement = Arrangement.Bottom
    ) {
        AnimatedVisibility(open && !searchMode, enter = fadeIn(), exit = fadeOut()) {
            Surface(shape = RoundedCornerShape(28.dp), color = theme.card, shadowElevation = 8.dp) {
                Column(Modifier.padding(8.dp)) {
                    dockApps.forEach { a ->
                        AppIcon(a, theme, false, true, { vm.toggleDock(a.pkg) }) { open = false; ctx.launchApp(a) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.size(60.dp).clip(CircleShape).background(theme.accent).combinedClickable(
                onClick = { if (searchMode) onSearch() else open = !open },
                onLongClick = { open = false; onSearch() }
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (searchMode) "🔍" else if (open) "✕" else "◉",
                color = theme.bg.copy(alpha = 1f), fontSize = 24.sp
            )
        }
    }
}
