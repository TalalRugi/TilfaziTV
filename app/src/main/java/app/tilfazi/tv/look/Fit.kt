package app.tilfazi.tv.look

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import app.tilfazi.tv.feed.Choices
import app.tilfazi.tv.feed.EDGE_PARTS
import app.tilfazi.tv.feed.SIZE_SCALES

/**
 * Every screen is laid out for a 960 × 540 dp TV, whatever its pixels, its shape and the density its maker chose: the
 * density is set so that box fits the screen, and a screen of another shape (16:10, 21:9, 4:3) gets the room left over
 * along its longer side, never less. So the clock, the add-ons and the text are the same share of every screen, a
 * 32-inch TV, a 75-inch one or a PC monitor. On top of that the owner's size, and an edge kept clear for TVs that cut
 * theirs off. The text ignores the TV's own font scale, which would push it out of its boxes. Arabic, right to left.
 */
@Composable
fun TvScreen(content: @Composable () -> Unit) {
    val looks by Choices.picks.collectAsState()
    val scale = SIZE_SCALES[looks.looks.size.coerceIn(0, SIZE_SCALES.lastIndex)]
    val edge = EDGE_PARTS[looks.looks.edge.coerceIn(0, EDGE_PARTS.lastIndex)]
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val pad = minOf(w, h) * edge
        val k = minOf((w - 2 * pad) / 960f, (h - 2 * pad) / 540f).coerceAtLeast(.1f) * scale
        val outer = LocalDensity.current
        Box(Modifier.fillMaxSize().padding(with(outer) { pad.toDp() })) {
            CompositionLocalProvider(LocalDensity provides Density(k, 1f), LocalLayoutDirection provides LayoutDirection.Rtl) { content() }
        }
    }
}
