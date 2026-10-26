package app.tilfazi.tv.look

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import app.tilfazi.tv.feed.Feeds
import app.tilfazi.tv.feed.Fixture
import app.tilfazi.tv.feed.Gallery
import app.tilfazi.tv.feed.Picks
import app.tilfazi.tv.feed.Sky
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the clock, the panels and the backdrop show; kept fresh by [rememberBoard]. */
@Stable
class Board {
    var now by mutableLongStateOf(System.currentTimeMillis())
    var sky by mutableStateOf<Sky?>(null)
    var fixtures by mutableStateOf<List<Fixture>>(emptyList())
    var results by mutableStateOf<List<Fixture>>(emptyList())
    var table by mutableStateOf<app.tilfazi.tv.feed.Table?>(null)
    var photo by mutableStateOf<Bitmap?>(null)
    var photoBy by mutableStateOf("")
    /** bumped when the owner's photos may have changed (read permission just granted): they are looked for again */
    var galleryTick by mutableIntStateOf(0)
}

private const val HALF_HOUR = 30 * 60_000L

/** One board per screen: the second ticks, and the weather, the games and the photo every half hour. */
@Composable
fun rememberBoard(picks: Picks): Board {
    val c = LocalContext.current
    val feeds = remember { Feeds(c.applicationContext) }
    val board = remember { Board() }

    LaunchedEffect(Unit) {
        while (true) {
            board.now = System.currentTimeMillis()
            // once a second, or once a minute when animation is off
            delay(if (Look.still) 60_000 - board.now % 60_000 else 1000 - board.now % 1000)
        }
    }
    LaunchedEffect(picks.place) {
        board.sky = withContext(Dispatchers.IO) { feeds.keptSky(picks.place) }
        while (true) {
            feeds.fetchSky(picks.place)?.let { board.sky = it }
            delay(HALF_HOUR)
        }
    }
    LaunchedEffect(picks.clubId) {
        val club = picks.clubId
        if (club == null) { board.fixtures = emptyList(); board.results = emptyList(); board.table = null; return@LaunchedEffect }
        withContext(Dispatchers.IO) {
            board.fixtures = feeds.keptFixtures(club)
            board.results = feeds.keptResults(club)
            board.table = feeds.keptTable(club)
        }
        // TheSportsDB's free key refuses quick runs of requests (HTTP 429): a few seconds apart, and again in two
        // minutes when one was refused
        while (true) {
            var ok = feeds.fetchFixtures(club)?.also { board.fixtures = it } != null
            delay(4_000)
            ok = (feeds.fetchResults(club)?.also { board.results = it } != null) && ok
            delay(4_000)
            ok = (feeds.fetchTable(club)?.also { board.table = it } != null) && ok
            delay(if (ok) HALF_HOUR else 120_000L)
        }
    }
    // the photo, only in the photo mode: a new one every half hour from the chosen source. The owner's own or
    // Unsplash's fall back on the ones built in while there are none (no photos found, no key, no network).
    val looks = picks.looks
    LaunchedEffect(looks.backdrop, looks.photos, picks.unsplashKey, picks.photoTopic, board.galleryTick) {
        if (looks.backdrop != 1) { board.photo = null; return@LaunchedEffect }
        var turn = (System.currentTimeMillis() / HALF_HOUR).toInt()
        if (looks.photos == 2) withContext(Dispatchers.IO) { feeds.keptShot() }?.let { shot ->
            board.photo = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(shot.file.path) }
            board.photoBy = shot.by
        }
        while (true) {
            var shown = false
            if (looks.photos == 2) {
                feeds.fetchShot(picks.unsplashKey, picks.photoTopic)?.let { shot ->
                    board.photo = withContext(Dispatchers.IO) { BitmapFactory.decodeFile(shot.file.path) }
                    board.photoBy = shot.by
                    shown = true
                }
                if (!shown && board.photo != null) shown = true   // keep the last Unsplash photo while offline
            }
            if (!shown) {
                val list = withContext(Dispatchers.IO) {
                    (if (looks.photos == 1) Gallery.owned(c) else emptyList()).ifEmpty { Gallery.scenes(c) }
                }
                if (list.isNotEmpty()) {
                    withContext(Dispatchers.IO) { Gallery.decode(c, list[turn.mod(list.size)]) }?.let { board.photo = it }
                    board.photoBy = ""
                }
            }
            turn++
            delay(HALF_HOUR)
        }
    }
    return board
}

/** a club crest, loaded once per address */
@Composable
fun rememberBadge(url: String?): Bitmap? {
    val c = LocalContext.current
    val feeds = remember { Feeds(c.applicationContext) }
    var bmp by remember(url) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(url) {
        if (url != null) launch { bmp = feeds.badge(url) }
    }
    return bmp
}
