package app.tributary

import androidx.compose.ui.graphics.Color

data class FlowTheme(val name: String, val bg: Color, val card: Color, val text: Color, val sub: Color, val accent: Color)

val Themes = listOf(
    FlowTheme("Midnight", Color(0xF2101218), Color(0xFF1B1F2A), Color(0xFFF2F4F8), Color(0xFF9AA3B5), Color(0xFF6C9BFF)),
    FlowTheme("Paper",    Color(0xF2F6F4EF), Color(0xFFFFFFFF), Color(0xFF1B1B1F), Color(0xFF6B6B73), Color(0xFFE2572B)),
    FlowTheme("Sunset",   Color(0xF22A1322), Color(0xFF3D1D33), Color(0xFFFFEEE6), Color(0xFFD9A8B8), Color(0xFFFF8A5B)),
    FlowTheme("Forest",   Color(0xF20E1A15), Color(0xFF16291F), Color(0xFFE8F3EC), Color(0xFF8DB09C), Color(0xFF4FD08A)),
)
