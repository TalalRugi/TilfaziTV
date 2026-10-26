package app.tilfazi.tv.home

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.key.onPreviewKeyEvent
import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.R
import app.tilfazi.tv.feed.Choices
import app.tilfazi.tv.feed.Shelf
import app.tilfazi.tv.feed.Title
import app.tilfazi.tv.look.Look
import app.tilfazi.tv.look.Scenery
import app.tilfazi.tv.look.Stage
import app.tilfazi.tv.look.rememberBoard
import app.tilfazi.tv.setup.SetupActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val COLUMNS = 5

@Composable
fun HomeScreen(homeAgain: Int, appsChanged: Int, sourceKey: Int = 0) {
    val c = LocalContext.current
    val picks by Choices.picks.collectAsState()
    // the first run: the welcome screens alone, the home only once they are done
    if (!picks.onboarded) { app.tilfazi.tv.start.Welcome(onDone = {}); return }
    val board = rememberBoard(picks)
    val firstFavourite = remember { FocusRequester() }
    // the first frame already has the favourites: their six cards read from disk at once (the rest follow)
    var all by remember { mutableStateOf(Shelf.last.ifEmpty { picks.favourites?.let { Shelf.fromDisk(c, it) } ?: emptyList() }) }
    var onFirst by remember { mutableStateOf(false) }   // the focus is on the first favourite
    var row by remember { mutableIntStateOf(-1) }   // the grid row with the focus; -1: the focus is up on the home screen
    val browsing = row >= 0

    // the list kept in memory shows at once; Android is asked again only when apps changed
    LaunchedEffect(appsChanged, picks.names) {
        if (Shelf.last.isEmpty()) all = withContext(Dispatchers.IO) { Shelf.fromDisk(c) }.ifEmpty { all }   // after a restart: the last list, at once
        all = withContext(Dispatchers.IO) { Shelf.cached(c, picks.names) }
    }
    LaunchedEffect(appsChanged) { if (appsChanged > 1) board.galleryTick++ }   // back from elsewhere: new photos may be on the TV
    val favourites = Shelf.favourites(all, picks.favourites)

    fun backToTop() {
        row = -1
        runCatching { firstFavourite.requestFocus() }
    }
    LaunchedEffect(homeAgain, all.isNotEmpty(), favourites.isEmpty()) { if (all.isNotEmpty()) backToTop() }
    // Back: from anywhere to the first favourite; from the first favourite, the screensaver
    // the grid of all apps, in its own order; one app at a time can be carried to a new place
    val grid = Shelf.ordered(all, picks.order)
    var moving by remember { mutableStateOf<String?>(null) }
    var menuFor by remember { mutableStateOf<Title?>(null) }
    var sources by remember { mutableStateOf<List<app.tilfazi.tv.feed.Source>?>(null) }
    LaunchedEffect(sourceKey) { if (sourceKey > 0) sources = app.tilfazi.tv.feed.Inputs.list(c) }
    val gridFocus = remember { mutableMapOf<String, FocusRequester>() }
    /** while an app is carried: the arrows move it a place (◀ ▶) or a row (▲ ▼); Back puts it down */
    fun moveKey(k: KeyEvent, pkg: String): Boolean {
        if (k.action != KeyEvent.ACTION_DOWN) return k.keyCode in listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN)
        val by = when (k.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> 1   // Arabic runs right to left
            KeyEvent.KEYCODE_DPAD_RIGHT -> -1
            KeyEvent.KEYCODE_DPAD_DOWN -> COLUMNS
            KeyEvent.KEYCODE_DPAD_UP -> -COLUMNS
            else -> return false
        }
        val list = grid.map { it.pkg }.toMutableList()
        val i = list.indexOf(pkg)
        val j = (i + by).coerceIn(0, list.lastIndex)
        if (j != i) { list.add(j, list.removeAt(i)); Choices.setOrder(list) }
        return true
    }
    // a favourite carried along the row: ◀ ▶ move it a place, OK or Back puts it down
    var movingFav by remember { mutableStateOf<String?>(null) }
    val favFocus = remember { mutableMapOf<String, FocusRequester>() }
    fun moveFavKey(k: KeyEvent, pkg: String): Boolean {
        val arrows = listOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN)
        if (k.action != KeyEvent.ACTION_DOWN) return k.keyCode in arrows
        val by = when (k.keyCode) { KeyEvent.KEYCODE_DPAD_LEFT -> 1; KeyEvent.KEYCODE_DPAD_RIGHT -> -1; else -> return k.keyCode in arrows }
        val list = favourites.map { it.pkg }.toMutableList()
        val i = list.indexOf(pkg)
        val j = (i + by).coerceIn(0, list.lastIndex)
        if (j != i) { list.add(j, list.removeAt(i)); Choices.setFavourites(list) }
        return true
    }
    BackHandler(enabled = movingFav != null) { movingFav = null }
    BackHandler(enabled = moving != null) { moving = null }
    BackHandler(enabled = moving == null && movingFav == null && menuFor == null && sources == null) { if (onFirst) c.startAmbient() else backToTop() }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screen = maxHeight
        val wide = maxWidth
        val cell = (wide - 136.dp - 24.dp * (COLUMNS - 1)) / COLUMNS
        val rowStep = cell * 9f / 16f + 28.dp + 28.dp   // a card, its name, the gap
        // No scrolling container, so nothing scrolls by itself when a card takes the focus: the page slides by
        // this much, to the grid with the focused row second from the top, or back to the top.
        val shift by animateDpAsState(
            if (row < 0) 0.dp else screen - 96.dp + rowStep * (row - 1).coerceAtLeast(0), tween(450), label = "shift",
        )
        Scenery(board, picks.looks, picks.place, dimmer = if (browsing) 0.55f else 0f) {
            Column(Modifier.fillMaxWidth().wrapContentHeight(Alignment.Top, unbounded = true).offset(y = -shift)) {
                Box(Modifier.fillMaxWidth().height(screen)) {
                    // the buttons: top left (End is the left in Arabic)
                    Row(
                        Modifier.align(Alignment.TopEnd).padding(top = 32.dp, end = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val onTop = { row = -1; onFirst = false }
                        Pill(painterResource(R.drawable.inputs), "المصادر", { sources = app.tilfazi.tv.feed.Inputs.list(c) }, onTop)
                        Pill(painterResource(R.drawable.sliders), "الإعدادات", { c.open(Intent(Settings.ACTION_SETTINGS)) }, onTop)
                        Pill(painterResource(R.drawable.crescent), "إعدادات تلفازي", { c.open(Intent(c, SetupActivity::class.java)) }, onTop)
                    }
                    // the app's name, top right, level with the buttons
                    Row(
                        Modifier.align(Alignment.TopStart).padding(top = 36.dp, start = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(painterResource(R.drawable.crescent), null, tint = Look.Gold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("تلفازي", style = Look.style(20.sp, Look.Ink, big = true))
                    }

                    Stage(
                        board, picks,
                        Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top = 82.dp, start = 56.dp, end = 48.dp).height(300.dp),
                    )

                    // the favourites, in a soft band at the bottom
                    val gap = 16.dp
                    // the cards are always sized for a full row of six, so the band keeps its height with fewer
                    val tile = (wide - 96.dp - 40.dp - gap * (Shelf.MAX_FAVOURITES - 1)) / Shelf.MAX_FAVOURITES
                    // with no favourites the band itself takes the focus, so a held OK still reaches the editor
                    var bandFocused by remember { mutableStateOf(false) }
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 48.dp, end = 48.dp, bottom = 32.dp)
                            .fillMaxWidth()
                            .background(Color(0x1FFFFFFF), RoundedCornerShape(26.dp))
                            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(26.dp))
                            .then(if (favourites.isEmpty()) Modifier.height(tile * 9f / 16f + 44.dp).focusRequester(firstFavourite)
                                .onFocusChanged { bandFocused = it.isFocused; if (it.isFocused) { row = -1; onFirst = true } }
                                .okKeys({}, { c.editFavourites() }).focusable()
                                .border(1.dp, if (bandFocused) Color(0x66FFFFFF) else Color.Transparent, RoundedCornerShape(26.dp)) else Modifier)
                            .padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (favourites.isEmpty() && bandFocused) Text("اضغط OK مطوّلاً لاختيار تطبيقاتك", style = Look.style(14.sp, Look.Faint))
                        favourites.forEachIndexed { i, app ->
                            val fr = favFocus.getOrPut(app.pkg) { FocusRequester() }
                            val up = movingFav == app.pkg
                            AppTile(
                                app, tile,
                                onFocus = { row = -1; onFirst = i == 0 },
                                onOk = { if (up) movingFav = null else c.open(app.open) },
                                onHold = { if (!up) menuFor = app },
                                showName = false,
                                lifted = up,
                                modifier = (if (i == 0) Modifier.focusRequester(firstFavourite) else Modifier)
                                    .focusRequester(fr).onPreviewKeyEvent { e -> up && moveFavKey(e.nativeKeyEvent, app.pkg) },
                            )
                            LaunchedEffect(i, up) { if (up) runCatching { fr.requestFocus() } }   // the focus follows the moved app
                        }
                    }
                }

                Text(
                    "كل التطبيقات", style = Look.style(13.sp, Look.Gold, FontWeight.Medium),
                    modifier = Modifier.padding(start = 68.dp, top = 8.dp, bottom = 18.dp),
                )
                Column(Modifier.padding(start = 68.dp, end = 68.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                    grid.chunked(COLUMNS).forEachIndexed { r, line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            line.forEach { app ->
                                val fr = gridFocus.getOrPut(app.pkg) { FocusRequester() }
                                val up = moving == app.pkg
                                AppTile(
                                    app, cell,
                                    onFocus = { row = r; onFirst = false },
                                    onOk = { if (up) moving = null else c.open(app.open) },
                                    onHold = { if (!up) menuFor = app },
                                    showName = true,
                                    lifted = up,
                                    modifier = Modifier.focusRequester(fr).onPreviewKeyEvent { e -> up && moveKey(e.nativeKeyEvent, app.pkg) },
                                )
                                LaunchedEffect(r, up, grid.indexOf(app)) { if (up) runCatching { fr.requestFocus() } }   // the focus follows the moved app
                            }
                        }
                    }
                }
            }
        }

        sources?.let { list ->
            AppMenu(if (list.isEmpty()) "لم يُعثر على مصادر" else "المصادر", onClose = { sources = null },
                actions = if (list.isEmpty()) listOf("إعدادات المصادر" to { c.open(Intent("android.settings.INPUT_SETTINGS")) })
                else list.map { s -> s.name to { app.tilfazi.tv.feed.Inputs.open(c, s); Unit } })
        }
        menuFor?.let { app ->
            val pkgs = favourites.map { it.pkg }
            val isFav = app.pkg in pkgs
            AppMenu(app.label, onClose = { menuFor = null }, actions = listOfNotNull(
                "نقل" to { if (isFav && movingFav == null && app in favourites && row < 0) movingFav = app.pkg else moving = app.pkg },
                if (isFav) "إزالة من المفضلة" to { Choices.setFavourites(pkgs - app.pkg) }
                else if (pkgs.size < Shelf.MAX_FAVOURITES) "إضافة للمفضلة" to { Choices.setFavourites(pkgs + app.pkg) } else null,
                "تعديل المفضلة" to { c.editFavourites() },
                "إلغاء التثبيت" to { c.uninstall(app.pkg) },
            ))
        }
    }
}

/** Android's own uninstall screen for the app: it asks the owner to confirm */
private fun Context.uninstall(pkg: String) {
    runCatching { startActivity(Intent(Intent.ACTION_DELETE, android.net.Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** a small menu over the screen for a held OK: the app's name, then its actions */
@Composable
private fun AppMenu(title: String, actions: List<Pair<String, () -> Unit>>, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    // a window of its own over the home screen, so the focus cannot slip to the apps behind it
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Box(Modifier.fillMaxSize().background(Color(0xB3000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(360.dp).background(Color(0xF216171A), RoundedCornerShape(22.dp))
                .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(22.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = Look.style(20.sp, Look.Ink, FontWeight.Medium))
            Spacer(Modifier.height(6.dp))
            actions.forEachIndexed { i, (name, run) ->
                MenuRow(name, { onClose(); run() }, if (i == 0) Modifier.focusRequester(first) else Modifier)
            }
        }
    }
    }
}

private fun Context.open(i: Intent) {
    runCatching { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .recoverCatching { startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** the screensaver now, drawn by our own activity: this works whatever screensaver the system has picked (Back twice) */
/** a held OK on any app: straight to the favourites editor */
private fun Context.editFavourites() {
    startActivity(Intent(this, SetupActivity::class.java).putExtra(SetupActivity.PAGE, "favourites").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

private fun Context.startAmbient() {
    startActivity(Intent(this, app.tilfazi.tv.ambient.AmbientActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

