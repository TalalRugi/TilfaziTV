package app.tilfazi.tv.home

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Choices
import app.tilfazi.tv.feed.Shelf
import app.tilfazi.tv.feed.Title
import app.tilfazi.tv.look.Blurs
import app.tilfazi.tv.look.Glass
import app.tilfazi.tv.look.Look
import app.tilfazi.tv.look.Tints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Picking and ordering the favourites in one place:
 * - the six places along the top, as they will be on the home screen; OK on one picks it up, ◀ ▶ carry it, OK puts
 *   it down;
 * - every app under them; OK adds it to the next free place, or takes it out, a gold number showing its place.
 * Each change is saved at once.
 */
@Composable
fun FavouritesEditor(onDone: () -> Unit, doneLabel: String = "تم") {
    val c = LocalContext.current
    val picks by Choices.picks.collectAsState()
    var all by remember { mutableStateOf(Shelf.last) }
    LaunchedEffect(picks.names) { all = withContext(Dispatchers.IO) { Shelf.cached(c, picks.names) } }
    val chosen = Shelf.favourites(all, picks.favourites).map { it.pkg }
    var lifted by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf<String?>(null) }
    val slotFocus = remember { mutableMapOf<String, FocusRequester>() }
    val first = remember { FocusRequester() }
    LaunchedEffect(all.isNotEmpty()) { if (all.isNotEmpty()) { delay(300); runCatching { first.requestFocus() } } }
    LaunchedEffect(note) { if (note != null) { delay(2500); note = null } }

    fun save(list: List<String>) = Choices.setFavourites(list)
    fun toggle(pkg: String) {
        if (pkg in chosen) save(chosen - pkg)
        else if (chosen.size >= Shelf.MAX_FAVOURITES) note = "المفضلة ممتلئة: أزل تطبيقاً أولاً"
        else save(chosen + pkg)
    }

    Box(Modifier.fillMaxSize()) {
        Glass(Tints.of(picks.looks.tint).color, Blurs.GLASS, place = picks.looks.glow)
        Column(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 32.dp)) {
            // the heading, how many are picked, and the way out
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("المفضلة", style = Look.style(32.sp, big = true))
                    Text(
                        if (lifted != null) "حرّك بالأسهم ◀ ▶ ثم OK لتثبيته" else "اختر حتى ستة تطبيقات تظهر في الشاشة الرئيسية",
                        style = Look.style(15.sp, if (lifted != null) Look.Gold else Look.Soft),
                    )
                }
                Count(chosen.size)
                Spacer(Modifier.width(14.dp))
                if (chosen.isNotEmpty()) { Box(Modifier.width(150.dp)) { MenuRow("مسح الكل", { lifted = null; save(emptyList()) }) }; Spacer(Modifier.width(10.dp)) }
                Box(Modifier.width(150.dp)) { MenuRow(doneLabel, onDone) }
            }
            Spacer(Modifier.height(18.dp))

            // the six places, in a band like the home screen's
            Row(
                Modifier.fillMaxWidth().background(Color(0x1FFFFFFF), RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(26.dp)).padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // only the apps picked, centred, as on the home screen; an empty band keeps its height
                val byPkg = all.associateBy { it.pkg }
                if (chosen.isEmpty()) Box(Modifier.height(82.dp))
                chosen.forEachIndexed { i, pkg ->
                    val app = byPkg[pkg] ?: return@forEachIndexed
                    val fr = slotFocus.getOrPut(app.pkg) { FocusRequester() }
                    val up = lifted == app.pkg
                    Slot(
                        app, i, 124.dp, up,
                        Modifier.focusRequester(fr).onPreviewKeyEvent { e ->
                            val k = e.nativeKeyEvent
                            if (lifted != app.pkg || k.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false
                            // Arabic runs right to left: ▶ goes toward the start of the row
                            val by = when (k.keyCode) { KeyEvent.KEYCODE_DPAD_LEFT -> 1; KeyEvent.KEYCODE_DPAD_RIGHT -> -1; else -> 0 }
                            if (by == 0) return@onPreviewKeyEvent k.keyCode == KeyEvent.KEYCODE_DPAD_UP || k.keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                            val j = i + by
                            if (j in chosen.indices) save(chosen.toMutableList().apply { add(j, removeAt(i)) })
                            true
                        },
                        onOk = { lifted = if (up) null else app.pkg },
                    )
                    LaunchedEffect(i, up) { if (up) runCatching { fr.requestFocus() } }   // the focus follows the moved app
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("كل التطبيقات", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                Spacer(Modifier.width(12.dp))
                Text("OK للإضافة أو الإزالة", style = Look.style(13.sp, Look.Faint))
                Spacer(Modifier.weight(1f))
                note?.let { Text(it, style = Look.style(14.sp, Look.Live, FontWeight.Medium)) }
            }
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(GridCells.Fixed(6), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp), modifier = Modifier.fillMaxWidth()) {
                items(all, key = { it.pkg }) { app ->
                    val at = chosen.indexOf(app.pkg)
                    Pickable(app, at, if (app == all.first()) Modifier.focusRequester(first) else Modifier) { lifted = null; toggle(app.pkg) }
                }
            }
        }
    }
}

/** «٣ من ٦» in a gold-edged capsule, filling as places are taken */
@Composable
private fun Count(n: Int) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.background(Look.Gold.copy(alpha = .14f), shape).border(1.dp, Look.Gold.copy(alpha = .5f), shape).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until Shelf.MAX_FAVOURITES) {
            Box(Modifier.padding(horizontal = 2.dp).size(8.dp).background(if (i < n) Look.Gold else Color(0x33FFFFFF), CircleShape))
        }
        Spacer(Modifier.width(10.dp))
        Text(Look.digits("$n من ${Shelf.MAX_FAVOURITES}", false) + " ", style = Look.style(14.sp, Look.Gold, FontWeight.Medium))
    }
}

@Composable
private fun Art(app: Title, modifier: Modifier) {
    val art = app.picture
    Box(modifier.background(Color(0xFF1C1C1E)), contentAlignment = Alignment.Center) {
        if (app.wide) Image(art, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Image(art, app.label, Modifier.size(44.dp))
    }
}

@Composable
private fun Number(n: Int, modifier: Modifier) {
    Box(modifier.size(24.dp).background(Look.Gold, CircleShape), contentAlignment = Alignment.Center) {
        Text(Look.digits("${n + 1}", false), style = Look.style(12.sp, Color(0xFF15120C), FontWeight.Bold).copy(shadow = null))
    }
}

/** one of the six places with its app; lifted, it rises in gold with arrows either side */
@Composable
private fun Slot(app: Title, i: Int, width: Dp, up: Boolean, modifier: Modifier, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val grow by animateFloatAsState(if (up) 1.14f else if (focused) 1.06f else 1f, label = "grow")
    val shape = RoundedCornerShape(12.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier.width(width).aspectRatio(16f / 9f).scale(grow)
                .onFocusChanged { focused = it.isFocused }.okKeys(onOk, onOk).focusable()
                .clip(shape)
                .border(if (up) 3.dp else if (focused) 2.dp else 1.dp, if (up) Look.Gold else if (focused) Color.White else Color(0x33FFFFFF), shape),
        ) {
            Art(app, Modifier.fillMaxSize())
            Number(i, Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(if (up) "◀  حرّك  ▶" else app.label, style = Look.style(12.sp, if (up) Look.Gold else Look.Soft, FontWeight.Medium),
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.width(width))
    }
}

/** an app in the grid: its number in gold when it is a favourite */
@Composable
private fun Pickable(app: Title, at: Int, modifier: Modifier, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier.fillMaxWidth().aspectRatio(16f / 9f).scale(if (focused) 1.06f else 1f)
                .onFocusChanged { focused = it.isFocused }.okKeys(onOk, onOk).focusable()
                .clip(shape)
                .then(if (focused || at >= 0) Modifier.border(2.dp, if (focused) Color.White else Look.Gold, shape) else Modifier),
        ) {
            Art(app, Modifier.fillMaxSize())
            if (at >= 0) {
                Box(Modifier.fillMaxSize().background(Color(0x40000000)))
                Number(at, Modifier.align(Alignment.TopStart).padding(6.dp))
            } else if (focused) {
                Box(Modifier.align(Alignment.TopStart).padding(6.dp).size(24.dp).background(Color(0xE6FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                    Text("+", style = Look.style(16.sp, Color(0xFF111111), FontWeight.Bold).copy(shadow = null))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(app.label, style = Look.style(12.sp, if (at >= 0) Look.Gold else Look.Soft, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
