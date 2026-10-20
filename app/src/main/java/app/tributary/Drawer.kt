package app.tributary

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** All apps as a vertically scrolling list (opened by swiping left). Results filter as you type. */
@Composable
fun DrawerScreen(vm: FlowViewModel, theme: FlowTheme, autoFocus: Boolean, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val apps by vm.apps.collectAsState()
    val hidden by vm.hidden.collectAsState()
    val dock by vm.dock.collectAsState()
    var q by remember { mutableStateOf("") }
    val results = remember(apps, hidden, q) {
        val visible = apps.filter { it.pkg !in hidden }
        if (q.isBlank()) visible
        else visible.map { it to fuzzyScore(q, it.label) }
            .filter { it.second >= 0 }.sortedByDescending { it.second }.map { it.first }
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(autoFocus) { if (autoFocus) focus.requestFocus() }

    Column(
        Modifier.fillMaxSize().background(theme.bg.copy(alpha = 1f)).statusBarsPadding().imePadding().padding(horizontal = 12.dp)
    ) {
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(4.dp))
        LazyColumn(contentPadding = PaddingValues(bottom = 110.dp)) {
            items(results, key = { it.component.flattenToString() }) { a ->
                DrawerRow(a, theme, a.pkg in dock, vm) { ctx.launchApp(a); onClose() }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerRow(app: AppEntry, theme: FlowTheme, pinned: Boolean, vm: FlowViewModel, onLaunch: () -> Unit) {
    val ctx = LocalContext.current
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().combinedClickable(onClick = onLaunch, onLongClick = { menu = true })
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(app.icon, null, Modifier.size(44.dp))
            Spacer(Modifier.width(16.dp))
            Text(app.label, color = theme.text, fontSize = 17.sp)
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (pinned) "Remove from tray" else "Add to tray") },
                onClick = { menu = false; vm.toggleDock(app.pkg) }
            )
            DropdownMenuItem(text = { Text("Hide app") }, onClick = { menu = false; vm.toggleHidden(app.pkg) })
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
