package app.tilfazi.tv.setup

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.home.okKeys
import app.tilfazi.tv.look.Glass
import app.tilfazi.tv.look.Look

/**
 * A row of choices drawn as they will look: each card shows [render] for its index, live, on the chosen backdrop
 * colour. The focused card grows a little; the one in force carries a gold mark. OK picks.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Showcase(
    names: List<String>,
    current: Int,
    tint: Color,
    onPick: (Int) -> Unit,
    cardWidth: Dp = 272.dp,
    cardHeight: Dp = 230.dp,
    autoFocus: Boolean = true,
    /** for the glow's own choice: each card glows from the place it stands for */
    glowOf: ((Int) -> Int)? = null,
    perRow: Int = names.size,
    render: @Composable (Int) -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) if (Pane.live) runCatching { focus.requestFocus() } }
    // a line holds [perRow] at most, and wraps sooner where the screen is narrower
    FlowRow(maxItemsInEachRow = perRow, verticalArrangement = Arrangement.spacedBy(14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) { listOf(names.indices.toList()).forEach { line ->
    run {
        line.forEach { i -> val name = names[i]
            var focused by remember { mutableStateOf(false) }
            val grow by animateFloatAsState(if (focused) 1.04f else 1f, label = "grow")
            val shape = RoundedCornerShape(18.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(cardWidth, cardHeight)
                        .scale(grow)
                        .then(if (i == current) Modifier.focusRequester(focus) else Modifier)
                        .onFocusChanged { focused = it.isFocused }
                        .okKeys({ onPick(i) }, { onPick(i) })
                        .focusable()
                        .clip(shape)
                        .background(Look.NightLow)
                        .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color(0x26FFFFFF), shape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (glowOf != null) Glass(tint, 40.dp, Modifier.fillMaxSize(), place = glowOf(i)) else Glass(tint, 60.dp, Modifier.fillMaxSize())
                    Box(Modifier.padding(14.dp), contentAlignment = Alignment.Center) { render(i) }
                    if (i == current) {
                        Box(
                            Modifier.align(Alignment.TopStart).padding(10.dp).size(22.dp).background(Look.Gold, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text("✓", style = Look.style(12.sp, Color(0xFF15120C), FontWeight.Bold).copy(shadow = null)) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(name, style = Look.style(16.sp, if (focused) Look.Ink else Look.Soft, FontWeight.Medium))
            }
        }
    }
    } }
}

/** a swatch per colour: a small glass card glowing in it */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Swatches(names: List<String>, colors: List<Color>, current: Int, onPick: (Int) -> Unit, autoFocus: Boolean = true) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (autoFocus) if (Pane.live) runCatching { focus.requestFocus() } }
    FlowRow(maxItemsInEachRow = 4, verticalArrangement = Arrangement.spacedBy(18.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        listOf(colors.indices.toList()).forEach { row ->
            run {
                row.forEach { i ->
                    var focused by remember { mutableStateOf(false) }
                    val shape = RoundedCornerShape(16.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(196.dp, 96.dp)
                                .then(if (i == current) Modifier.focusRequester(focus) else Modifier)
                                .onFocusChanged { focused = it.isFocused }
                                .okKeys({ onPick(i) }, { onPick(i) })
                                .focusable()
                                .clip(shape)
                                .background(Look.NightLow)
                                .border(if (focused) 3.dp else 1.dp, if (focused) Color.White else Color(0x26FFFFFF), shape),
                        ) {
                            Glass(colors[i], 40.dp, Modifier.fillMaxSize())
                            if (i == current) {
                                Box(Modifier.align(Alignment.BottomEnd).padding(10.dp).size(20.dp).background(Look.Gold, CircleShape), contentAlignment = Alignment.Center) {
                                    Text("✓", style = Look.style(11.sp, Color(0xFF15120C), FontWeight.Bold).copy(shadow = null))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(names[i], style = Look.style(14.sp, if (focused) Look.Ink else Look.Soft, FontWeight.Medium))
                    }
                }
            }
        }
    }
}

/** sizes the previews: widgets are drawn for a 440 dp column, the cards are narrower */
@Composable
fun Fit(width: Dp, content: @Composable () -> Unit) {
    Box(Modifier.width(width)) { content() }
}

/** a short row of choices as pills; the one in force is gold */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Chips(names: List<String>, current: Int, perRow: Int = names.size, focusCurrent: Boolean = false, onPick: (Int) -> Unit) {
    val here = remember { FocusRequester() }
    if (focusCurrent) LaunchedEffect(Unit) { kotlinx.coroutines.delay(400); if (Pane.live) runCatching { here.requestFocus() } }
    // a line holds [perRow] at most, and wraps sooner where the screen is narrower
    FlowRow(maxItemsInEachRow = perRow, verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) { listOf(names.indices.toList()).forEach { line ->
    run {
        line.forEach { i -> val name = names[i]
            var focused by remember { mutableStateOf(false) }
            val on = i == current
            val shape = RoundedCornerShape(50)
            Box(
                Modifier
                    .then(if (focusCurrent && on) Modifier.focusRequester(here) else Modifier)
                    .onFocusChanged { focused = it.isFocused }
                    .okKeys({ onPick(i) }, { onPick(i) })
                    .focusable()
                    .clip(shape)
                    .background(if (focused) Color(0xF2FFFFFF) else if (on) Look.Gold.copy(alpha = .28f) else Color(0x14FFFFFF))
                    .border(1.dp, if (on) Look.Gold.copy(alpha = .8f) else Color(0x1FFFFFFF), shape)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(name, style = Look.style(15.sp, if (focused) Color(0xFF111111) else if (on) Look.Gold else Look.Ink, FontWeight.Medium).copy(shadow = null))
            }
        }
    }
    } }
}

/** a small gold heading over a group of choices */
@Composable
fun Heading(text: String) {
    Text(text, style = Look.style(15.sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
}

/**
 * false while a settings page is only shown beside the list, not yet entered: its parts then leave the focus (and the
 * scrolling that comes with it) where it is.
 */
object Pane {
    var live = true
}
