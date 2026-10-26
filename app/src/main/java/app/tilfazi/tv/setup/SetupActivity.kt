package app.tilfazi.tv.setup

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusProperties
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.ADDON_GAME
import app.tilfazi.tv.feed.ADDON_NONE
import app.tilfazi.tv.feed.ADDON_PRAYER
import app.tilfazi.tv.feed.ADDON_SKY
import app.tilfazi.tv.feed.Choices
import app.tilfazi.tv.feed.Clubs
import app.tilfazi.tv.feed.Fixture
import app.tilfazi.tv.feed.Gallery
import app.tilfazi.tv.feed.Hour
import app.tilfazi.tv.feed.Picks
import app.tilfazi.tv.feed.Places
import app.tilfazi.tv.feed.Shelf
import app.tilfazi.tv.feed.Sky
import app.tilfazi.tv.home.MenuRow
import app.tilfazi.tv.home.okKeys
import app.tilfazi.tv.look.Addon
import app.tilfazi.tv.ambient.AmbientFrame
import app.tilfazi.tv.ambient.DIMS
import app.tilfazi.tv.ambient.SCENES
import app.tilfazi.tv.look.Blurs
import app.tilfazi.tv.look.BackdropMotion
import app.tilfazi.tv.look.DENSITIES
import app.tilfazi.tv.look.MOTIONS
import app.tilfazi.tv.look.ShrinkToFit
import app.tilfazi.tv.feed.ADDON_LEAGUE
import app.tilfazi.tv.feed.ADDON_VERSE
import app.tilfazi.tv.feed.ADDON_HADITH
import app.tilfazi.tv.feed.placesOf
import app.tilfazi.tv.look.clock12
import app.tilfazi.tv.look.GLOW_PLACES
import app.tilfazi.tv.look.GlassCard
import app.tilfazi.tv.look.Board
import app.tilfazi.tv.look.CLOCK_STYLES
import app.tilfazi.tv.look.ClockFace
import app.tilfazi.tv.look.GAME_STYLES
import app.tilfazi.tv.look.Hairline
import app.tilfazi.tv.look.Look
import app.tilfazi.tv.look.PRAYER_STYLES
import app.tilfazi.tv.look.SKY_STYLES
import app.tilfazi.tv.look.Tints
import app.tilfazi.tv.look.freshSky
import app.tilfazi.tv.look.nextGame
import app.tilfazi.tv.look.rememberBoard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.animation.core.animateFloat
import kotlinx.coroutines.withContext

/** The app's own settings: one list, and for a row picked a page of its own. */
class SetupActivity : ComponentActivity() {
    companion object {
        /** an extra: «favourites» opens the favourites editor alone */
        const val PAGE = "page"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Look.load(this)
        setContent {
            app.tilfazi.tv.look.TvScreen {
                val direct = intent.getStringExtra(PAGE) == "favourites"
                Box(Modifier.fillMaxSize().background(Look.NightLow)) {
                    if (direct) app.tilfazi.tv.home.FavouritesEditor(onDone = { finish() }) else Setup(onDone = { finish() })
                }
            }
        }
    }
}

private enum class Page(val title: String) {
    Main("إعدادات تلفازي"), Favourites("المفضلة"), Font("الخط"), Backdrop("الخلفية"), Addons("الإضافات"), Addon("الإضافة"), Clock("شكل الساعة"), Ambient("شاشة التوقف"),
    City("المدينة"), Club("الفريق المفضل"), Key("مفتاح Unsplash"), Topic("موضوع الصور"), Home("الواجهة الرئيسية"), Size("حجم الواجهة"), Edges("هوامش الشاشة"), Update("التحديثات"), Contact("تواصل معي"), Remove("إزالة تلفازي"),
}

val PHOTO_SOURCES = listOf("الافتراضية", "ملفات التلفزيون", "Unsplash")
private val SLOT_NAMES = listOf("الإضافة الأولى", "الإضافة الثانية", "الإضافة الثالثة")
private val ADDON_KINDS = listOf(ADDON_NONE, ADDON_PRAYER, ADDON_GAME, ADDON_SKY, ADDON_LEAGUE, ADDON_VERSE, ADDON_HADITH)
private val ADDON_NAMES = mapOf(ADDON_NONE to "بدون", ADDON_PRAYER to "الصلاة", ADDON_GAME to "المباراة", ADDON_SKY to "الطقس",
    ADDON_LEAGUE to "الدوري", ADDON_VERSE to "آية اليوم", ADDON_HADITH to "حديث اليوم")
private val LAYOUTS = listOf("ثلاث صغيرة", "كبيرة ثم صغيرة", "صغيرة ثم كبيرة", "كبيرة وحدها")

/** so a preview is never empty: a game and a sky to draw when there is none yet */
private fun sampleGame(now: Long) = Fixture("الهلال", "النصر", null, null, "دوري روشن السعودي", now + 2 * 86_400_000L + 5 * 3_600_000L)
private fun sampleSky(now: Long) = Sky(now, 27.0, 1, 31.0, 19.0, 30, 9.0, true,
    (1..5).map { Hour(now + it * 3_600_000L, 27.0 + it % 3 - 1, if (it == 3) 2 else 1, true) })

/** the shapes of one kind of add-on, and which is in force */
private fun stylesOf(kind: Int, picks: Picks) = when (kind) {
    ADDON_PRAYER -> "prayer" to PRAYER_STYLES to picks.looks.prayer
    ADDON_GAME -> "game" to GAME_STYLES to picks.looks.game
    else -> "sky" to SKY_STYLES to picks.looks.sky
}

private fun addonLabel(kind: Int, picks: Picks, large: Boolean): String = when {
    kind == ADDON_NONE -> "متوقفة"
    large || kind == ADDON_LEAGUE || kind == ADDON_VERSE || kind == ADDON_HADITH -> ADDON_NAMES.getValue(kind)
    else -> ADDON_NAMES.getValue(kind) + "، " + stylesOf(kind, picks).let { (a, i) -> a.second[i.coerceIn(0, 2)] }
}

@Composable
private fun Setup(onDone: () -> Unit) {
    val picks by Choices.picks.collectAsState()
    val board = rememberBoard(picks)
    var page by remember { mutableStateOf(Page.Main) }
    var slot by remember { mutableIntStateOf(0) }
    // where Back from a text box goes: the page it was opened from
    var from by remember { mutableStateOf(Page.Main) }
    // the main page: the setting under the focus (its page shows on the left) and whether the focus went into it
    var at by remember { mutableIntStateOf(0) }
    var inside by remember { mutableStateOf(false) }
    val back = {
        if (page == Page.Main) inside = false
        page = if (page == Page.Addon || page == Page.Key || page == Page.Topic || page == Page.Favourites) Page.Main else Page.Main
    }
    BackHandler { if (page == Page.Main && !inside) onDone() else back() }
    val l = picks.looks
    val tint = Tints.of(l.tint).color
    val c = LocalContext.current

    if (page == Page.Favourites) { app.tilfazi.tv.home.FavouritesEditor(onDone = { back() }); return }
    val body: @Composable (Page) -> Unit = { shown -> when (shown) {
            Page.Main, Page.Favourites -> {}
            Page.Update -> UpdatePage(c)
            Page.Contact -> ContactPage()
            Page.Remove -> RemovePage(c)
            Page.Size -> Centred(
                choices = { Chips(app.tilfazi.tv.feed.SIZES, l.size.coerceIn(0, 4), focusCurrent = true) { Choices.setLook("size", it) } },
                note = "يتغير كل شيء معه فوراً: الساعة والإضافات والتطبيقات والنصوص",
            ) {
                Text(clock12(board.now, picks.place.zone, picks.latinDigits), style = Look.style(120.sp, Look.Ink, big = true))
                Text(app.tilfazi.tv.feed.SIZES[l.size.coerceIn(0, 4)], style = Look.style(26.sp, Look.Gold, FontWeight.Medium))
            }
            Page.Edges -> Centred(
                choices = { Chips(app.tilfazi.tv.feed.EDGES, l.edge.coerceIn(0, 3), focusCurrent = true) { Choices.setLook("edge", it) } },
                note = "بعض التلفزيونات تقص أطراف الصورة. زِد الهامش حتى ترى الإطار الذهبي حول الشاشة كاملاً",
            ) {
                Text("⌜ ⌝", style = Look.style(60.sp, Look.Gold))
                Text(app.tilfazi.tv.feed.EDGES[l.edge.coerceIn(0, 3)], style = Look.style(40.sp, Look.Ink, big = true))
                Text("⌞ ⌟", style = Look.style(60.sp, Look.Gold))
            }
            Page.Home -> {
                val homes = remember(page) { homes(c) }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("الواجهات المثبتة على التلفزيون. اختر واحدة لفتحها.", style = Look.style(15.sp, Look.Soft))
                    Choose(homes.map { (name, open, current) -> Triple(name, if (current) "الحالية" else null) { open() } }, selected = 0)
                }
            }
            Page.Backdrop -> BackdropPage(picks, tint, c, openKey = { from = Page.Backdrop; page = Page.Key }, openTopic = { from = Page.Backdrop; page = Page.Topic })
            Page.Addons -> Column(Modifier.verticalScroll(paneScroll())) {
                Text("التخطيط", style = Look.style(15.sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(bottom = 10.dp))
                Showcase(LAYOUTS, l.layout, tint, { Choices.setLook("layout", it) }, cardWidth = 196.dp, cardHeight = 130.dp) { i ->
                    // the column drawn as blocks: a large place twice the height of a small one
                    Column(Modifier.width(110.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        placesOf(i).forEach { large ->
                            Box(Modifier.fillMaxWidth().height(if (large) 52.dp else 22.dp)
                                .background(if (large) Look.Gold.copy(alpha = .55f) else Color(0x40FFFFFF), RoundedCornerShape(6.dp)))
                        }
                    }
                }
                Heading("الأماكن")
                Column(Modifier.width(620.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    placesOf(l.layout).forEachIndexed { i, large ->
                        MenuRow(SLOT_NAMES[i] + if (large) " (كبيرة)" else "", { slot = i; page = Page.Addon },
                            detail = addonLabel(l.addons[i], picks, large))
                    }
                }
                Heading("خلفية الإضافات")
                Showcase(listOf("بدون، تطفو على الشاشة", "بطاقات زجاجية"), l.cards, tint, { Choices.setLook("cards", it) },
                    cardWidth = 300.dp, cardHeight = 150.dp, autoFocus = false) { i ->
                    val p = picks.copy(looks = l.copy(cards = i))
                    Fit(250.dp) {
                        val sky = freshSky(board) ?: sampleSky(board.now)
                        if (i == 1) GlassCard(.5f) { Addon(ADDON_SKY, board, p, .5f, sky = sky) } else Addon(ADDON_SKY, board, p, .5f, sky = sky)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            Page.Addon -> AddonPage(slot, placesOf(l.layout).getOrElse(slot) { false }, board, picks, tint)
            Page.Ambient -> AmbientPage(picks, c)
            Page.Font -> {
                val sets = Look.available
                Centred(
                    choices = {
                        Chips(sets.map { Look.FONTS[it] }, sets.indexOf(l.font).coerceAtLeast(0), focusCurrent = true) { i ->
                            Choices.setLook("font", sets[i]); Look.load(c)
                        }
                    },
                    note = "تبي خط آخر؟ اطلب من Claude أو Codex أن يضيفه لتلفازي، وستجده هنا في هذه القائمة.\nالخطوط المضمّنة مرخّصة برخصة SIL Open Font License",
                ) {
                    // a sample in the faces now in force: the large figures and the text
                    Text(clock12(board.now, picks.place.zone, picks.latinDigits), style = Look.style(96.sp, Look.Ink, big = true))
                    Text("السبت ١٣ جمادى الأولى ١٤٤٨ هـ", style = Look.style(24.sp, Look.Ink, FontWeight.Medium))
                    Text("الصلاة القادمة: الظهر، دوري روشن السعودي", style = Look.style(18.sp, Look.Soft))
                }
            }
            Page.Clock -> Column(Modifier.verticalScroll(paneScroll())) {
                Showcase(CLOCK_STYLES, l.clock, tint, { Choices.setLook("clock", it); back() }, cardWidth = 272.dp, cardHeight = 176.dp, perRow = 3) { i ->
                    ClockFace(i, board.now, picks, when (i) { 2 -> .5f; 4 -> .36f; in 6..8 -> .44f; else -> .42f })
                }
                Spacer(Modifier.height(24.dp))
            }
            Page.City -> Choose(
                Places.all.map { p -> Triple(p.name, null as String?) { Choices.setPlace(p.key); back() } },
                selected = Places.all.indexOfFirst { it.key == picks.place.key }.coerceAtLeast(0),
            )
            Page.Club -> {
                val rows = listOf(Triple("بدون", null as String?) { Choices.setClub(null); back() }) +
                    Clubs.followable.map { club -> Triple(club.name, null as String?) { Choices.setClub(club.id); back() } }
                Choose(rows, selected = (Clubs.followable.indexOfFirst { it.id == picks.clubId } + 1).coerceAtLeast(0))
            }
            Page.Key -> Entry("الصق مفتاح Access Key من unsplash.com/developers، أو أرسله من الكمبيوتر.", picks.unsplashKey) {
                Choices.setUnsplash(it); back()
            }
            Page.Topic -> Entry("بالإنجليزية، مثل: desert night، mountains، saudi arabia landscape", picks.photoTopic) {
                Choices.setTopic(it); back()
            }
        } }
    if (page == Page.Main) {
        MainPage(mainGroups(picks, c) { page = it }, tint, at, { at = it }, inside, { inside = it }, body)
        return
    }
    // a page of its own, full screen: one opened from inside another (an add-on place, a text box)
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 36.dp)) {
        Text(if (page == Page.Addon) SLOT_NAMES[slot] else page.title, style = Look.style(28.sp, big = true))
        Spacer(Modifier.height(12.dp))
        Hairline(Modifier.width(260.dp))
        Spacer(Modifier.height(18.dp))
        body(page)
    }
}

/**
 * Everything about the backdrop on one page: the two modes as cards, then what belongs to the one in force —
 * the colours for the glass; the source, the blur and, for Unsplash, its key and topic for the photos.
 */
@Composable
private fun BackdropPage(picks: Picks, tint: Color, c: Context, openKey: () -> Unit, openTopic: () -> Unit) {
    val l = picks.looks
    var owned by remember { mutableIntStateOf(0) }
    var readTick by remember { mutableIntStateOf(0) }
    val askToRead = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { readTick++ }
    LaunchedEffect(l.photos, readTick) { owned = withContext(Dispatchers.IO) { Gallery.owned(c).size } }

    Column(Modifier.verticalScroll(paneScroll())) {
        Showcase(listOf("زجاج ملوّن", "صور"), l.backdrop, tint, { Choices.setLook("backdrop", it) }, cardWidth = 300.dp, cardHeight = 150.dp) { i ->
            if (i == 0) Text("لون يتوهج بنعومة من الزاوية العليا", style = Look.style(14.sp, Look.Ink, FontWeight.Medium))
            else ScenePreview(c, "صورة جديدة كل نصف ساعة")
        }
        if (l.backdrop == 0) {
            Heading("اللون")
            Swatches(Tints.all.map { it.name }, Tints.all.map { it.color }, l.tint, { Choices.setLook("tint", it) }, autoFocus = false)
            Heading("موقع اللون")
            Showcase(GLOW_PLACES, l.glow, tint, { Choices.setLook("glow", it) }, cardWidth = 196.dp, cardHeight = 110.dp, autoFocus = false,
                glowOf = { it }) { }
            Heading("الحركة")
            Showcase(MOTIONS, l.motion, tint, { Choices.setLook("motion", it) }, cardWidth = 196.dp, cardHeight = 110.dp, autoFocus = false, perRow = 4) { i ->
                BackdropMotion(i, 1, tint)
                if (i == 0) Text("بدون حركة", style = Look.style(13.sp, Look.Soft, FontWeight.Medium))
            }
            if (l.motion > 0) {
                Heading("الكثافة")
                Chips(DENSITIES, l.density.coerceIn(0, 2)) { Choices.setLook("density", it) }
            }
        } else {
            Heading("مصدر الصور")
            Showcase(PHOTO_SOURCES, l.photos, tint, { pick ->
                Choices.setLook("photos", pick)
                if (pick == 1 && !Gallery.mayRead(c)) askToRead.launch(Gallery.readPermission())
                if (pick == 2 && picks.unsplashKey.isBlank()) openKey()
            }, cardWidth = 250.dp, cardHeight = 120.dp, autoFocus = false) { i ->
                Text(
                    when (i) {
                        0 -> "٣ صور من السعودية\nملكية عامة"
                        1 -> (if (owned > 0) "وُجدت ${Look.digits(owned.toString(), picks.latinDigits)} صورة\n" else "") + "في مجلد «${Gallery.FOLDER}»\nعلى التلفزيون أو فلاشة USB"
                        else -> if (picks.unsplashKey.isBlank()) "يحتاج مفتاحاً" else "عن: ${picks.photoTopic}"
                    },
                    style = Look.style(13.sp, Look.Ink, FontWeight.Medium),
                )
            }
            Heading("التمويه")
            Chips(Blurs.names, l.blur.coerceIn(0, 4)) { Choices.setLook("blur", it) }
            if (l.photos == 2) {
                Heading("Unsplash")
                Column(Modifier.width(520.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuRow("المفتاح", openKey, detail = if (picks.unsplashKey.isBlank()) "غير مضاف" else "مضاف")
                    MenuRow("موضوع الصور", openTopic, detail = picks.photoTopic)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * The screensaver, apart from the home screen: the scene picked, drawn live in a large preview, the scenes to pick
 * from, the dimming, and a way to see it now.
 */
@Composable
private fun AmbientPage(picks: Picks, c: Context) {
    val l = picks.looks
    val shown = if (l.scene == 2) picks.copy(looks = l.copy(backdrop = 1)) else picks   // the gallery always has photos
    val board = rememberBoard(shown)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (Pane.live) runCatching { focus.requestFocus() } }
    // the scene large in the middle, as the TV will show it, and under it what to choose
    Column(Modifier.fillMaxWidth().verticalScroll(paneScroll()), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(560.dp, 315.dp).clip(RoundedCornerShape(18.dp)).border(2.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // the scene at full size, shrunk whole into the frame: what is cut off here is cut off on the TV
            Box(Modifier.requiredSize(960.dp, 540.dp).graphicsLayer { scaleX = 560f / 960f; scaleY = 560f / 960f }) {
                AmbientFrame(l.scene, l.dim, board, shown)
            }
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.width(260.dp)) {
            MenuRow("شاهدها الآن", { c.startActivity(android.content.Intent(c, app.tilfazi.tv.ambient.AmbientActivity::class.java)) })
        }
        Spacer(Modifier.height(22.dp))
        Text("المشهد", style = Look.style(18.sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(bottom = 10.dp))
        Box(Modifier.focusRequester(focus)) { Chips(SCENES, l.scene, perRow = 5) { Choices.setLook("scene", it) } }
        if (l.scene == 2) {
            Spacer(Modifier.height(20.dp))
            Text("الصور", style = Look.style(18.sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(bottom = 10.dp))
            Chips(listOf("صور ثابتة", "تتحرك كل ١٠ دقائق"), l.artAuto.coerceIn(0, 1)) { Choices.setLook("artAuto", it) }
            Text("وفي الحالتين تتنقل بينها بالأسهم ◀ ▶", style = Look.style(14.sp, Look.Faint), modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("التعتيم", style = Look.style(18.sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(bottom = 10.dp))
        Chips(DIMS, l.dim.coerceIn(0, 2)) { Choices.setLook("dim", it) }
        Spacer(Modifier.height(30.dp))
    }
}

/** one add-on place: what it shows (or nothing), then, for a widget, its three shapes; both drawn live */
@Composable
private fun AddonPage(slot: Int, large: Boolean, board: Board, picks: Picks, tint: Color) {
    val kind = picks.looks.addons[slot]
    val game = nextGame(board) ?: sampleGame(board.now)
    val sky = freshSky(board) ?: sampleSky(board.now)
    Column(Modifier.verticalScroll(paneScroll())) {
        Heading("ماذا تعرض")
        // a large place shows its add-ons large, in taller cards
        Showcase(ADDON_KINDS.map { ADDON_NAMES.getValue(it) }, ADDON_KINDS.indexOf(kind), tint,
            { Choices.setLook("addon${slot + 1}", ADDON_KINDS[it]) },
            cardWidth = if (large) 200.dp else 164.dp, cardHeight = if (large) 220.dp else 140.dp, perRow = 5) { i ->
            val k = ADDON_KINDS[i]
            if (k == ADDON_NONE) Text("لا شيء في هذا المكان", style = Look.style(13.sp, Look.Soft, FontWeight.Medium))
            else if (k == ADDON_LEAGUE && board.table == null) Text("يظهر عند اختيار فريق", style = Look.style(13.sp, Look.Soft, FontWeight.Medium))
            else ShrinkToFit { Fit(if (large) 180.dp else 150.dp) { Addon(k, board, picks, if (large) .42f else .34f, game, sky, large = large) } }
        }
        if (kind in listOf(ADDON_PRAYER, ADDON_GAME, ADDON_SKY) && !large) {
            val (named, current) = stylesOf(kind, picks)
            val (part, names) = named
            Heading("الشكل")
            Showcase(names, current, tint, { Choices.setLook(part, it) }, cardWidth = 272.dp, cardHeight = 170.dp, autoFocus = false) { i ->
                val p = picks.copy(looks = when (kind) {
                    ADDON_PRAYER -> picks.looks.copy(prayer = i)
                    ADDON_GAME -> picks.looks.copy(game = i)
                    else -> picks.looks.copy(sky = i)
                })
                Fit(244.dp) { Addon(kind, board, p, .55f, game, sky) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** one of the built-in photos, small, with a line over it */
@Composable
private fun ScenePreview(c: Context, line: String) {
    val bmp by produceState<Bitmap?>(null) {
        value = withContext(Dispatchers.IO) { Gallery.scenes(c).lastOrNull()?.let { Gallery.decode(c, it, 480) } }
    }
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.BottomCenter) {
        bmp?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Text(line, style = Look.style(14.sp, Look.Ink, FontWeight.Medium), modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun Choose(rows: List<Triple<String, String?, () -> Unit>>, selected: Int) {
    val focus = remember(rows.size) { FocusRequester() }
    LaunchedEffect(rows.size, selected) { if (Pane.live) runCatching { focus.requestFocus() } }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(rows) { i, (name, detail, run) ->
            MenuRow(name, run, if (i == selected) Modifier.focusRequester(focus) else Modifier, detail)
        }
    }
}

@Composable
private fun Entry(hint: String, start: String, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(start) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (Pane.live) runCatching { focus.requestFocus() } }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(hint, style = Look.style(15.sp, Look.Soft))
        BasicTextField(
            value = value,
            onValueChange = { value = it },
            singleLine = true,
            textStyle = Look.style(20.sp, Look.Ink, FontWeight.Medium).copy(shadow = null),
            cursorBrush = SolidColor(Look.Gold),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave(value) }),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus)
                .background(Color(0x14FFFFFF), RoundedCornerShape(12.dp))
                .border(1.dp, Look.Gold.copy(alpha = .5f), RoundedCornerShape(12.dp))
                .padding(16.dp),
        )
        MenuRow("حفظ", { onSave(value) })
    }
}

private fun homeAlias(c: Context) = android.content.ComponentName(c, "app.tilfazi.tv.home.AsHome")

/** the name of the app the Home key opens now */
private fun homeName(c: Context): String {
    val pm = c.packageManager
    val r = pm.resolveActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME), 0)
    val pkg = r?.activityInfo?.packageName
    return if (pkg == null || pkg == "android" || pkg == "com.android.settings") "تسأل كل مرة" else r.loadLabel(pm).toString()
}

/**
 * The homes on the TV: name, what opening it does, and whether the Home key goes there now. tilfazi is always one,
 * even when the TV no longer lists it, so picking it gives it back the Home key.
 */
private fun homes(c: Context): List<Triple<String, () -> Unit, Boolean>> {
    val pm = c.packageManager
    val now = pm.resolveActivity(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
    val others = pm.queryIntentActivities(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME), 0)
        .filter { it.activityInfo.packageName != c.packageName && !it.activityInfo.packageName.contains("settings") }
        .map { r ->
            val i = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME)
                .setClassName(r.activityInfo.packageName, r.activityInfo.name).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            Triple(r.loadLabel(pm).toString(), { runCatching { c.startActivity(i) }; Unit }, r.activityInfo.packageName == now)
        }
    val mine = Triple("تلفازي", {
        pm.setComponentEnabledSetting(homeAlias(c), android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
        runCatching { c.startActivity(android.content.Intent(c, app.tilfazi.tv.home.HomeActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
        Unit
    }, now == c.packageName)
    return listOf(mine) + others
}

/** a setting on the main page: its page shows on the left; one with none (a switch) runs [run] on OK instead */
private class Item(val name: String, val value: String?, val about: String, val page: Page? = null, val run: () -> Unit = {})

private fun mainGroups(picks: Picks, c: Context, go: (Page) -> Unit): List<Pair<String, List<Item>>> {
    val l = picks.looks
    return listOf(
        "الشاشة الرئيسية" to listOf(
            Item("الخلفية", if (l.backdrop == 1) "صور، ${PHOTO_SOURCES[l.photos.coerceIn(0, 2)]}" else "زجاج، ${Tints.of(l.tint).name}",
                "لون زجاجي هادئ أو صور تتبدل خلف الواجهة", Page.Backdrop),
            Item("الإضافات", LAYOUTS[l.layout.coerceIn(0, 3)], "الصلاة والطقس والمباريات وغيرها بجانب الساعة", Page.Addons),
            Item("شكل الساعة", CLOCK_STYLES[l.clock.coerceIn(0, CLOCK_STYLES.lastIndex)], "طريقة عرض الوقت في أعلى الواجهة", Page.Clock),
        ),
        "الشكل" to listOf(
            Item("شاشة التوقف", SCENES[l.scene.coerceIn(0, SCENES.lastIndex)], "ما يظهر حين يُترك التلفزيون دون استخدام", Page.Ambient),
            Item("الخط", Look.FONTS.getOrElse(l.font) { Look.FONTS[0] }, "خط الكتابة في كل الشاشات", Page.Font),
            Item("الحركة والأنيميشن", if (l.still == 1) "متوقفة" else "تعمل", "حركات الواجهة وانتقالاتها؛ أوقفها لتكون أخف على التلفزيونات الضعيفة", run = { Choices.setLook("still", 1 - l.still) }),
            Item("الأرقام", if (picks.latinDigits) "123" else "١٢٣", "الأرقام العربية أو اللاتينية", run = { Choices.setLatinDigits(!picks.latinDigits) }),
        ),
        "لك" to listOf(
            Item("المدينة", picks.place.name, "لمواقيت الصلاة والطقس", Page.City),
            Item("الفريق المفضل", Clubs.find(picks.clubId)?.name ?: "بدون", "تظهر مباراته القادمة في الإضافات", Page.Club),
        ),
        "التلفزيون" to listOf(
            Item("حجم الواجهة", app.tilfazi.tv.feed.SIZES[l.size.coerceIn(0, 4)], "كبّرها إن كنت تجلس بعيداً أو كانت الشاشة صغيرة", Page.Size),
            Item("هوامش الشاشة", app.tilfazi.tv.feed.EDGES[l.edge.coerceIn(0, 3)], "إن كانت أطراف الواجهة مقصوصة على تلفزيونك، فاترك لها هامشاً", Page.Edges),
            Item("الواجهة الرئيسية", homeName(c), "الواجهات المثبتة على التلفزيون، وفتح أي منها", Page.Home),
        ),
        "تلفازي" to listOf(
            Item("التحديثات", Look.digits(app.tilfazi.tv.feed.Updater.current(c), picks.latinDigits), "ابحث عن نسخة أحدث من تلفازي وثبّتها", Page.Update),
            Item("تواصل معي", "@TalalRugi", "مشكلة في الواجهة أو تحتاج مساعدة؟ راسلني على X", Page.Contact),
            Item("إزالة تلفازي", null, "حذف تلفازي وإرجاع واجهة التلفزيون الأصلية", Page.Remove),
        ),
    )
}

/**
 * The main page in two panes: the settings in groups down the right, and on the left the page of the one under the
 * focus, drawn as it is. ◀ or OK goes into it, Back comes out to the list again.
 */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
private fun MainPage(
    groups: List<Pair<String, List<Item>>>, tint: Color, at: Int, onAt: (Int) -> Unit, inside: Boolean, onInside: (Boolean) -> Unit,
    body: @Composable (Page) -> Unit,
) {
    Pane.live = inside
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { Pane.live = true } }
    val all = groups.flatMap { it.second }
    val rows = remember(all.size) { List(all.size) { FocusRequester() } }
    val shown = all[at.coerceIn(0, all.lastIndex)]
    var held by remember { mutableStateOf(false) }   // the focus is in the page now
    var paneHas by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(inside) { if (!inside) runCatching { rows[at].requestFocus() } }
    val open = { i: Int ->
        val item = all[i]
        when {
            item.page == Page.Favourites -> item.run().also { onAt(i) }.let { }
            item.page != null -> onInside(true)
            else -> item.run()
        }
    }
    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.linearGradient(
        listOf(tint.copy(alpha = .20f), Look.NightLow, Look.NightLow)))) {
        Row(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 30.dp), horizontalArrangement = Arrangement.spacedBy(26.dp)) {
            Column(Modifier.width(250.dp).fillMaxHeight()) {
                Text("إعدادات تلفازي", style = Look.style(28.sp, big = true), modifier = Modifier.padding(start = 12.dp))
                Spacer(Modifier.height(10.dp))
                // ▶ off the page's edge lands on the setting it belongs to, not on whatever row lies beside it
                Column(Modifier.focusProperties { enter = { rows[at] } }.focusGroup().verticalScroll(rememberScrollState()).padding(bottom = 30.dp)) {
                    var n = 0
                    groups.forEach { (title, items) ->
                        Text(title, style = Look.style(12.sp, Look.Gold, FontWeight.Medium),
                            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp))
                        items.forEach { item ->
                            val i = n++
                            NavRow(item, i == at, inside, Modifier.focusRequester(rows[i]), { if (!inside) onAt(i) }, { open(i) })
                        }
                    }
                }
            }
            // the page itself, drawn for a wider screen and shrunk into the pane, so it looks as it does full size
            androidx.compose.foundation.layout.BoxWithConstraints(
                Modifier.weight(1f).fillMaxHeight()
                    .background(Color(0x0DFFFFFF), RoundedCornerShape(24.dp))
                    .border(1.dp, if (inside) Look.Gold.copy(alpha = .45f) else Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .onFocusChanged {
                        paneHas = it.hasFocus
                        when {
                            it.hasFocus && !inside -> scope.launch { runCatching { rows[at].requestFocus() } }   // nothing in it takes the focus until ◀
                            it.hasFocus -> held = true
                            // ▶ off its edge went back to the list; a move between two of its parts lets go for a moment
                            // too, so it counts only if the focus is still outside a frame later
                            held -> scope.launch {
                                androidx.compose.runtime.withFrameNanos { }
                                if (!paneHas && held) { held = false; onInside(false) }
                            }
                        }
                    },
            ) {
                val wide = 720.dp
                val k = maxWidth / wide
                val d = androidx.compose.ui.platform.LocalDensity.current
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(d.density * k, d.fontScale),
                ) {
                    Column(Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 28.dp)) {
                        // the name is already lit in the list; a page with no parts of its own says what it is for
                        val page = shown.page
                        if (page != null && page != Page.Favourites) {
                            // drawn anew on the way in, so the page puts the focus where it always does
                            androidx.compose.runtime.key(page, inside) {
                                val scroll = rememberScrollState()
                                Box(Modifier.fillMaxSize()) {
                                    androidx.compose.runtime.CompositionLocalProvider(LocalPaneScroll provides scroll) { body(page) }
                                    MoreBelow(scroll, Modifier.align(Alignment.BottomCenter))
                                }
                            }
                        } else {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(shown.name, style = Look.style(26.sp, Look.Soft, FontWeight.Medium))
                                Spacer(Modifier.height(10.dp))
                                if (shown.value != null) Text(shown.value, style = Look.style(72.sp, Look.Gold, big = true))
                                Spacer(Modifier.height(18.dp))
                                Text(shown.about, style = Look.style(18.sp, Look.Soft), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                Spacer(Modifier.height(30.dp))
                                Text("اضغط OK للتبديل", style = Look.style(16.sp, Look.Faint))
                            }
                        }
                    }
                }
            }
        }
        if (shown.page == Page.Edges) Box(Modifier.fillMaxSize().border(3.dp, Look.Gold))   // where the screen ends, to set the edge by
    }
}

@Composable
private fun NavRow(item: Item, current: Boolean, inside: Boolean, modifier: Modifier, onFocus: () -> Unit, onOk: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val lit = focused || (current && inside)
    Row(
        modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }
            .onPreviewKeyEvent { e ->
                // ◀ goes into the page on the left (in Arabic, the page lies after the list)
                if (!inside && e.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT && item.page != null) {
                    if (e.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) onOk(); true
                } else false
            }
            .okKeys(onOk, onOk)
            .focusable()
            .background(when { focused -> Color(0xF2FFFFFF); lit -> Color(0x33FFFFFF); else -> Color.Transparent }, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.name, style = Look.style(15.sp, if (focused) Color(0xFF111111) else Look.Ink, FontWeight.Medium).copy(shadow = null),
            modifier = Modifier.weight(1f), maxLines = 1)
        if (item.value != null) Text(item.value, maxLines = 1,
            style = Look.style(12.sp, if (focused) Color(0xFF666666) else Look.Faint).copy(shadow = null),
            modifier = Modifier.widthIn(max = 110.dp))
    }
}

/** the scrolling of the page shown in the pane, so the pane can tell when there is more of it below */
private val LocalPaneScroll = androidx.compose.runtime.compositionLocalOf<androidx.compose.foundation.ScrollState?> { null }

@Composable
private fun paneScroll() = LocalPaneScroll.current ?: rememberScrollState()

/**
 * While a page goes on below the pane's edge: its last line fades out, and a gold «more» with an arrow shows under
 * it, bobbing gently; it goes once the bottom is reached.
 */
@Composable
private fun MoreBelow(scroll: androidx.compose.foundation.ScrollState, modifier: Modifier) {
    val more = scroll.maxValue in 1 until Int.MAX_VALUE && scroll.value < scroll.maxValue - 4   // MAX_VALUE: nothing scrolls by it
    androidx.compose.animation.AnimatedVisibility(more, modifier, enter = androidx.compose.animation.fadeIn(), exit = androidx.compose.animation.fadeOut()) {
        val bob by androidx.compose.animation.core.rememberInfiniteTransition(label = "more").animateFloat(
            0f, 6f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900),
                androidx.compose.animation.core.RepeatMode.Reverse), label = "bob")
        Box(
            Modifier.fillMaxWidth().height(110.dp)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3141416)))),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Row(
                Modifier.padding(bottom = 14.dp).graphicsLayer { translationY = if (Look.still) 0f else bob }
                    .background(Color(0x26D6AF6B), RoundedCornerShape(50)).border(1.dp, Look.Gold.copy(alpha = .6f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("المزيد بالأسفل", style = Look.style(15.sp, Look.Gold, FontWeight.Medium).copy(shadow = null))
                Spacer(Modifier.width(8.dp))
                Text("▼", style = Look.style(13.sp, Look.Gold).copy(shadow = null))
            }
        }
    }
}

/** a page of one choice: what it does, drawn large in the middle, and the choices under it, all centred */
@Composable
private fun Centred(choices: @Composable () -> Unit, note: String, sample: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            sample()
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { choices() }
        Spacer(Modifier.height(16.dp))
        Text(note, style = Look.style(15.sp, Look.Faint), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(8.dp))
    }
}

/** checks the releases on GitHub; when there is a newer one, downloads it and hands it to Android's installer */
@Composable
private fun UpdatePage(c: Context) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var state by remember { mutableStateOf("") }
    var found by remember { mutableStateOf<app.tilfazi.tv.feed.Updater.Release?>(null) }
    val now = app.tilfazi.tv.feed.Updater.current(c)
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("النسخة الحالية", style = Look.style(18.sp, Look.Soft))
        Text(now, style = Look.style(64.sp, Look.Gold, big = true))
        Spacer(Modifier.height(24.dp))
        if (state.isNotEmpty()) Text(state, style = Look.style(20.sp, Look.Ink), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.width(360.dp)) {
            val r = found
            if (r == null) MenuRow("ابحث عن تحديث", {
                state = "جارٍ البحث…"
                scope.launch {
                    val r = runCatching { app.tilfazi.tv.feed.Updater.newer(c) }
                    found = r.getOrNull()
                    state = when {
                        r.isFailure -> "تعذّر الاتصال بصفحة التحديثات، تأكد من الإنترنت"
                        found != null -> "نسخة جديدة: ${found!!.version}"
                        else -> "عندك أحدث نسخة"
                    }
                }
            }, Modifier.focusRequester(remember { FocusRequester() }.also { f -> LaunchedEffect(Unit) { if (Pane.live) runCatching { f.requestFocus() } } }))
            else MenuRow("نزّل وثبّت ${r.version}", {
                state = "جارٍ التنزيل…"
                scope.launch {
                    state = if (app.tilfazi.tv.feed.Updater.install(c, r)) "وافق على التثبيت في الرسالة التي تظهر" else "تعذّر التنزيل، حاول مرة أخرى"
                }
            })
        }
    }
}

/** my account on X, with a code to scan from a phone */
@Composable
private fun ContactPage() {
    val qr = remember { qrBitmap("https://x.com/TalalRugi", 360) }
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        Column(Modifier.weight(1f)) {
            Text("تواجه مشكلة في الواجهة أو تحتاج مساعدة؟", style = Look.style(22.sp, Look.Ink, FontWeight.Medium))
            Spacer(Modifier.height(14.dp))
            Text("راسلني على X", style = Look.style(18.sp, Look.Soft))
            Spacer(Modifier.height(6.dp))
            Text("@TalalRugi", style = Look.style(44.sp, Look.Gold, big = true))
            Spacer(Modifier.height(14.dp))
            Text("امسح الرمز بكاميرا الجوال", style = Look.style(16.sp, Look.Faint))
        }
        Image(qr.asImageBitmap(), "x.com/TalalRugi", Modifier.size(220.dp).clip(RoundedCornerShape(16.dp)).background(Color.White).padding(12.dp))
    }
}

private fun qrBitmap(text: String, size: Int): Bitmap {
    val m = com.google.zxing.qrcode.QRCodeWriter().encode(text, com.google.zxing.BarcodeFormat.QR_CODE, size, size, mapOf(com.google.zxing.EncodeHintType.MARGIN to 0))
    val px = IntArray(size * size) { if (m[it % size, it / size]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
}

/**
 * Removing tilfazi: first the TV's own home switched back on (its page in the settings, «تفعيل»), so the TV is never
 * left without one; then tilfazi uninstalled. An app may not switch another back on itself.
 */
@Composable
private fun RemovePage(c: Context) {
    val stock = remember { listOf("com.google.android.apps.tv.launcherx", "com.google.android.tvlauncher").firstOrNull {
        runCatching { c.packageManager.getApplicationInfo(it, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS); true }.getOrDefault(false)
    } }
    fun off(p: String) = runCatching { !c.packageManager.getApplicationInfo(p, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS).enabled }.getOrDefault(false)
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("خطوتان، بالترتيب:", style = Look.style(20.sp, Look.Ink, FontWeight.Medium))
        Box(Modifier.width(520.dp)) {
            MenuRow("١. أرجع واجهة التلفزيون الأصلية", {
                runCatching { c.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    android.net.Uri.parse("package:${stock ?: ""}")).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }, Modifier.focusRequester(remember { FocusRequester() }.also { f -> LaunchedEffect(Unit) { if (Pane.live) runCatching { f.requestFocus() } } }),
                detail = if (stock == null) "لا توجد" else if (off(stock)) "اضغط «تفعيل»" else "تعمل")
        }
        Box(Modifier.width(520.dp)) {
            MenuRow("٢. احذف تلفازي", {
                runCatching { c.startActivity(android.content.Intent(android.content.Intent.ACTION_DELETE, android.net.Uri.parse("package:${c.packageName}"))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            })
        }
        Text("إن لم يظهر زر «تفعيل» في الخطوة الأولى، اطلب من Claude أو Codex: «ارجع تلفزيوني كما كان».", style = Look.style(15.sp, Look.Faint))
    }
}
