package app.tributary

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LayoutNames = listOf("Mosaic", "Wide", "Narrow")

@Composable
private fun Chip(text: String, selected: Boolean, theme: FlowTheme, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(theme.card)
            .border(2.dp, if (selected) theme.accent else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, color = theme.text, fontSize = 13.sp)
    }
}

/** Feed layout, left-handed mode, header photo, refresh and hidden apps. */
@Composable
fun FeedAndHomeSettings(vm: FlowViewModel, theme: FlowTheme) {
    val ctx = LocalContext.current
    val layout by vm.layout.collectAsState()
    val left by vm.leftHanded.collectAsState()
    val photo by vm.headerImage.collectAsState()
    val apps by vm.apps.collectAsState()
    val hidden by vm.hidden.collectAsState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            vm.setHeaderImage(uri.toString())
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Feed layout", color = theme.sub)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LayoutNames.forEachIndexed { i, n -> Chip(n, i == layout, theme) { vm.setLayout(i) } }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Left-handed layout", color = theme.text)
            Switch(checked = left, onCheckedChange = { vm.setLeftHanded(it) })
        }
        Text("Header photo", color = theme.sub)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch(arrayOf("image/*")) }) { Text("Choose photo") }
            if (photo != null) OutlinedButton(onClick = { vm.setHeaderImage(null) }) { Text("Remove") }
        }
        OutlinedButton(onClick = { vm.refreshFeed() }) { Text("Refresh feed now") }

        val hiddenApps = apps.filter { it.pkg in hidden }
        if (hiddenApps.isNotEmpty()) {
            Text("Hidden apps", color = theme.sub)
            hiddenApps.forEach { a ->
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(a.label, color = theme.text)
                    Text("Unhide", color = theme.accent, modifier = Modifier.clickable { vm.toggleHidden(a.pkg) }.padding(8.dp))
                }
            }
        }
    }
}
