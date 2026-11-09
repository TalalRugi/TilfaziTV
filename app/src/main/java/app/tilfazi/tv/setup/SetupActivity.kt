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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import app.tilfazi.tv.feed.Salah
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
import app.tilfazi.tv.look.BentoCard
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
import app.tilfazi.tv.look.PrayerWidget
import app.tilfazi.tv.look.GameWidget
import app.tilfazi.tv.look.SkyWidget
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
    Main("إعدادات تلفازي"), Favourites("المفضلة"), Backdrop("الخلفية"), Addons("الإضافات"), Ambient("شاشة التوقف والساعة"),
    Prayer("مواقيت الصلاة والمدينة"), City("المدينة"), PrayerMethod("طريقة حساب الصلاة"), PrayerMadhhab("المذهب الفقهي"), PrayerAdjust("تعديل أوقات الصلاة"),
    Sports("الأندية والدوريات"), Club("الفريق المحلي"), WorldClub("الفريق العالمي"), Leagues("الدوريات المفعلة"),
    Overlay("الجزيرة العلوية"), Display("إعدادات العرض والخط"), About("حول تلفازي"),
    Font("الخط"), Clock("شكل الساعة"), Key("مفتاح Unsplash"), Topic("موضوع الصور"), Home("الواجهة الرئيسية"),
    Size("حجم الواجهة"), Edges("هوامش الشاشة"), Update("التحديثات"), Contact("تواصل معي"), Remove("إزالة تلفازي"),
}

val PHOTO_SOURCES = listOf("الافتراضية", "ملفات التلفزيون", "Unsplash")
private val SLOT_NAMES = listOf("الإضافة الأولى", "الإضافة الثانية", "الإضافة الثالثة", "الإضافة الرابعة")
private val ADDON_KINDS = listOf(ADDON_NONE, ADDON_PRAYER, ADDON_GAME, ADDON_SKY, ADDON_LEAGUE, ADDON_VERSE, ADDON_HADITH)
private val ADDON_NAMES = mapOf(ADDON_NONE to "بدون", ADDON_PRAYER to "الصلاة", ADDON_GAME to "المباراة", ADDON_SKY to "الطقس",
    ADDON_LEAGUE to "الدوري", ADDON_VERSE to "آية اليوم", ADDON_HADITH to "حديث اليوم")
private val LAYOUTS = listOf("شبكة رباعية", "ثلاث بطاقات", "بطاقتان كبيرتان", "بطاقة واحدة")

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
        page = Page.Main
    }
    BackHandler { if (page == Page.Main && !inside) onDone() else back() }
    val l = picks.looks
    val tint = Tints.of(l.tint).color
    val c = LocalContext.current

    if (page == Page.Favourites) { app.tilfazi.tv.home.FavouritesEditor(onDone = { back() }); return }
    val body: @Composable (Page) -> Unit = { shown -> when (shown) {
            Page.Main, Page.Favourites -> {}
            Page.Addons -> AddonsPage(picks, tint, board)
            Page.Backdrop -> BackdropPage(picks, tint, c, openKey = { from = Page.Backdrop; page = Page.Key }, openTopic = { from = Page.Backdrop; page = Page.Topic })
            Page.Ambient -> AmbientPage(picks, c)
            Page.Prayer -> PrayerPage(picks)
            Page.Sports, Page.Club, Page.WorldClub, Page.Leagues -> SportsPage(picks)
            Page.Overlay -> OverlayPage(picks, c)
            Page.Display -> DisplayPage(picks, c)
            Page.About -> AboutPage(picks, c)
            Page.City -> CityPage(picks, onPicked = { back() })
            Page.PrayerAdjust -> PrayerAdjustPage(picks, onBack = { back() })
            Page.Update -> UpdatePage(c)
            Page.Contact -> ContactPage()
            Page.Remove -> RemovePage(c)
            Page.Key -> Entry("الصق مفتاح Access Key من unsplash.com/developers، أو أرسله من الكمبيوتر.", picks.unsplashKey) {
                Choices.setUnsplash(it); back()
            }
            Page.Topic -> Entry("بالإنجليزية، مثل: desert night، mountains، saudi arabia landscape", picks.photoTopic) {
                Choices.setTopic(it); back()
            }
            else -> {}
        } }
    if (page == Page.Main) {
        MainPage(mainGroups(picks, c) { page = it }, tint, at, { at = it }, inside, { inside = it }, body)
        return
    }
    // a page of its own, full screen: one opened from inside another (e.g. text entry)
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 36.dp)) {
        Text(page.title, style = Look.style(28.sp, big = true))
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
                        0 -> "١٤ صورة من السعودية\nملكية عامة"
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
        Text("شكل الساعة الفنية", style = Look.style(18.sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(bottom = 10.dp))
        Chips(CLOCK_STYLES, l.clock, perRow = 3) { Choices.setLook("clock", it) }
        Spacer(Modifier.height(20.dp))
        Text("التعتيم", style = Look.style(18.sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(bottom = 10.dp))
        Chips(DIMS, l.dim.coerceIn(0, 2)) { Choices.setLook("dim", it) }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SlotBlock(
    slot: Int,
    kind: Int,
    isLarge: Boolean,
    picks: Picks,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val l = picks.looks
    val title = ADDON_NAMES.getOrElse(kind) { "بدون" }
    val subtitle = when {
        kind == ADDON_NONE -> "فارغ"
        isLarge -> "بطاقة طولية"
        kind == ADDON_PRAYER -> PRAYER_STYLES.getOrElse(l.prayer) { "" }
        kind == ADDON_GAME -> GAME_STYLES.getOrElse(l.game) { "" }
        kind == ADDON_SKY -> SKY_STYLES.getOrElse(l.sky) { "" }
        else -> "افتراضي"
    }

    val isFilled = kind != ADDON_NONE

    Box(
        modifier
            .graphicsLayer {
                val sc = if (focused) 1.04f else 1f
                scaleX = sc
                scaleY = sc
            }
            .clip(shape)
            .border(
                if (focused) 2.5.dp else 1.dp,
                when {
                    focused -> Color.White
                    isFilled && isLarge -> Look.Gold.copy(alpha = 0.50f)
                    isFilled -> Color(0x35FFFFFF)
                    else -> Color(0x18FFFFFF)
                },
                shape
            )
            .background(
                when {
                    focused -> Color(0xF2FFFFFF)
                    isFilled && isLarge -> Look.Gold.copy(alpha = 0.38f)
                    isFilled -> Color(0x30FFFFFF)
                    else -> Color(0x10FFFFFF)
                },
                shape
            )
            .onFocusChanged { focused = it.isFocused }
            .okKeys(onSelect, onSelect)
            .focusable()
            .padding(horizontal = 8.dp, vertical = if (isLarge) 10.dp else 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                title,
                style = Look.style(
                    if (isLarge) 16.sp else 13.5.sp,
                    if (focused) Color(0xFF15120C) else Look.Ink,
                    FontWeight.Bold
                ).copy(shadow = null),
                maxLines = 1,
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(1.dp))
                Text(
                    subtitle,
                    style = Look.style(
                        if (isLarge) 11.sp else 9.5.sp,
                        if (focused) Color(0xFF555555) else if (isLarge) Look.Gold else Look.Soft,
                        FontWeight.Medium
                    ).copy(shadow = null),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BentoSlotGrid(
    layout: Int,
    picks: Picks,
    onSelectSlot: (Int) -> Unit,
) {
    val l = picks.looks

    Box(
        Modifier
            .fillMaxWidth()
            .background(Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(16.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (layout) {
            0 -> {
                Row(Modifier.fillMaxWidth().height(115.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SlotBlock(0, l.addons.getOrElse(0) { ADDON_NONE }, isLarge = false, picks, Modifier.fillMaxWidth().weight(1f)) { onSelectSlot(0) }
                        SlotBlock(1, l.addons.getOrElse(1) { ADDON_NONE }, isLarge = false, picks, Modifier.fillMaxWidth().weight(1f)) { onSelectSlot(1) }
                    }
                    SlotBlock(2, l.addons.getOrElse(2) { ADDON_NONE }, isLarge = true, picks, Modifier.weight(1f).fillMaxHeight()) { onSelectSlot(2) }
                    SlotBlock(3, l.addons.getOrElse(3) { ADDON_NONE }, isLarge = true, picks, Modifier.weight(1f).fillMaxHeight()) { onSelectSlot(3) }
                }
            }
            1 -> {
                Row(Modifier.fillMaxWidth().height(115.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SlotBlock(0, l.addons.getOrElse(0) { ADDON_NONE }, isLarge = false, picks, Modifier.fillMaxWidth().weight(1f)) { onSelectSlot(0) }
                        SlotBlock(1, l.addons.getOrElse(1) { ADDON_NONE }, isLarge = false, picks, Modifier.fillMaxWidth().weight(1f)) { onSelectSlot(1) }
                    }
                    SlotBlock(2, l.addons.getOrElse(2) { ADDON_NONE }, isLarge = true, picks, Modifier.weight(1f).fillMaxHeight()) { onSelectSlot(2) }
                }
            }
            2 -> {
                Row(Modifier.fillMaxWidth().height(115.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SlotBlock(0, l.addons.getOrElse(0) { ADDON_NONE }, isLarge = true, picks, Modifier.weight(1f).fillMaxHeight()) { onSelectSlot(0) }
                    SlotBlock(1, l.addons.getOrElse(1) { ADDON_NONE }, isLarge = true, picks, Modifier.weight(1f).fillMaxHeight()) { onSelectSlot(1) }
                }
            }
            else -> {
                Row(Modifier.fillMaxWidth().height(105.dp)) {
                    SlotBlock(0, l.addons.getOrElse(0) { ADDON_NONE }, isLarge = true, picks, Modifier.fillMaxWidth().fillMaxHeight()) { onSelectSlot(0) }
                }
            }
        }
    }
}

@Composable
private fun PrayerStyleShowcase(current: Int, onPick: (Int) -> Unit, tint: Color, board: Board, picks: Picks) {
    Showcase(
        names = listOf("العد التنازلي وخط الصلوات", "حلقة الأذان الدائرية", "جدول مواقيت اليوم كاملة"),
        current = current.coerceIn(0, 2),
        tint = tint,
        onPick = onPick,
        cardWidth = 215.dp,
        cardHeight = 135.dp,
        autoFocus = false,
        perRow = 3,
    ) { idx ->
        Box(
            Modifier
                .requiredSize(320.dp, 160.dp)
                .graphicsLayer {
                    scaleX = 0.58f
                    scaleY = 0.58f
                },
            contentAlignment = Alignment.Center,
        ) {
            PrayerWidget(style = idx, now = board.now, picks = picks, scale = 1f)
        }
    }
}

@Composable
private fun GameStyleShowcase(current: Int, onPick: (Int) -> Unit, tint: Color, board: Board, picks: Picks) {
    val fixture = remember(board.now) { nextGame(board, picks) ?: sampleGame(board.now) }
    Showcase(
        names = listOf("شعار الفريقين والموعد", "شريط النتيجة ولوحة البث", "العد التنازلي البارز"),
        current = current.coerceIn(0, 2),
        tint = tint,
        onPick = onPick,
        cardWidth = 215.dp,
        cardHeight = 135.dp,
        autoFocus = false,
        perRow = 3,
    ) { idx ->
        Box(
            Modifier
                .requiredSize(320.dp, 160.dp)
                .graphicsLayer {
                    scaleX = 0.58f
                    scaleY = 0.58f
                },
            contentAlignment = Alignment.Center,
        ) {
            GameWidget(style = idx, game = fixture, now = board.now, picks = picks, scale = 1f)
        }
    }
}

@Composable
private fun SkyStyleShowcase(current: Int, onPick: (Int) -> Unit, tint: Color, board: Board, picks: Picks) {
    val weather = remember(board.now) { freshSky(board) ?: sampleSky(board.now) }
    Showcase(
        names = listOf("الحرارة وأيقونة الطقس", "الطقس المفصل والرياح", "توقعات الساعات القادمة"),
        current = current.coerceIn(0, 2),
        tint = tint,
        onPick = onPick,
        cardWidth = 215.dp,
        cardHeight = 135.dp,
        autoFocus = false,
        perRow = 3,
    ) { idx ->
        Box(
            Modifier
                .requiredSize(320.dp, 160.dp)
                .graphicsLayer {
                    scaleX = 0.58f
                    scaleY = 0.58f
                },
            contentAlignment = Alignment.Center,
        ) {
            SkyWidget(style = idx, sky = weather, picks = picks, scale = 1f)
        }
    }
}

@Composable
private fun AddonsPage(picks: Picks, tint: Color, board: Board) {
    val l = picks.looks
    var editingSlot by remember { mutableStateOf<Int?>(null) }
    var subPage by remember { mutableStateOf<String?>(null) } // "gameClubs"

    when {
        subPage == "gameClubs" -> {
            GameClubsPage(picks, onBack = { subPage = null })
        }
        editingSlot != null -> {
            val slot = editingSlot!!
            val isLarge = placesOf(l.layout).getOrElse(slot) { false }
            val currentKind = l.addons.getOrElse(slot) { ADDON_NONE }
            BackHandler(enabled = true) { editingSlot = null }

            val kinds = listOf(ADDON_PRAYER, ADDON_GAME, ADDON_SKY, ADDON_LEAGUE, ADDON_VERSE, ADDON_HADITH, ADDON_NONE)
            val kindNames = listOf("الصلاة", "المباراة", "الطقس", "ترتيب الدوري", "آية اليوم", "حديث اليوم", "بدون إضافة")
            val currentKindIdx = kinds.indexOf(currentKind).coerceAtLeast(0)

            Column(Modifier.fillMaxSize().verticalScroll(paneScroll()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${SLOT_NAMES.getOrElse(slot) { "الإضافة ${slot + 1}" }} · ${if (isLarge) "بطاقة طولية كاملة" else "بطاقة مكدسة"}",
                        style = Look.style(18.sp, Look.Gold, FontWeight.Medium),
                    )
                    Text("اختر نوع وشكل الإضافة", style = Look.style(13.sp, Look.Soft))
                }

                Text("اختر نوع الإضافة لهذا الموضع:", style = Look.style(15.sp, Look.Gold, FontWeight.Medium))
                Showcase(
                    names = kindNames,
                    current = currentKindIdx,
                    tint = tint,
                    onPick = { idx ->
                        val k = kinds[idx]
                        val currentAddons = l.addons.toMutableList()
                        while (currentAddons.size < 4) currentAddons.add(ADDON_NONE)
                        if (k != ADDON_NONE) {
                            val existing = currentAddons.indexOf(k)
                            if (existing != -1 && existing != slot) {
                                Choices.setLook("addon${existing + 1}", currentAddons[slot])
                            }
                        }
                        Choices.setLook("addon${slot + 1}", k)
                    },
                    cardWidth = 145.dp,
                    cardHeight = 100.dp,
                    perRow = 4,
                    autoFocus = false,
                ) { idx ->
                    val k = kinds[idx]
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            text = ADDON_NAMES.getValue(k),
                            style = Look.style(17.sp, Look.Ink, FontWeight.Bold)
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = when (k) {
                                ADDON_PRAYER -> "مواقيت الأذان"
                                ADDON_GAME -> "مباريات الأندية"
                                ADDON_SKY -> "حالة الطقس"
                                ADDON_LEAGUE -> "جدول الترتيب"
                                ADDON_VERSE -> "آية وتدبر"
                                ADDON_HADITH -> "حديث نبوي"
                                else -> "إفراغ الموضع"
                            },
                            style = Look.style(11.5.sp, Look.Gold, FontWeight.Medium)
                        )
                    }
                }

                if (currentKind in listOf(ADDON_PRAYER, ADDON_GAME, ADDON_SKY) && !isLarge) {
                    Spacer(Modifier.height(14.dp))
                    Text("شكل هذه البطاقة (${ADDON_NAMES[currentKind]})", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                    Text("اختر المظهر المفضل من بين الأشكال التالية:", style = Look.style(13.sp, Look.Soft))
                    Spacer(Modifier.height(4.dp))
                    when (currentKind) {
                        ADDON_PRAYER -> PrayerStyleShowcase(l.prayer, { Choices.setLook("prayer", it) }, tint, board, picks)
                        ADDON_GAME -> GameStyleShowcase(l.game, { Choices.setLook("game", it) }, tint, board, picks)
                        ADDON_SKY -> SkyStyleShowcase(l.sky, { Choices.setLook("sky", it) }, tint, board, picks)
                    }
                } else if (isLarge) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0x14FFFFFF), RoundedCornerShape(14.dp))
                            .border(1.dp, Look.Gold.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(16.dp)
                    ) {
                        Text(
                            "هذا الموضع عبارة عن بطاقة طولية كاملة ويتم عرضها تلقائياً بتصميم طولي مخصص ومفصل في الواجهة الرئيسية.",
                            style = Look.style(13.sp, Look.Ink)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Hairline()
                Spacer(Modifier.height(6.dp))
                MenuRow("◀ حفظ والعودة لشبكة الإضافات", { editingSlot = null })
                Spacer(Modifier.height(24.dp))
            }
        }
        else -> {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { if (Pane.live) runCatching { focus.requestFocus() } }
            val places = placesOf(l.layout)

            Column(Modifier.fillMaxSize().verticalScroll(paneScroll()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("تخطيط شبكة الإضافات", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                Box(Modifier.focusRequester(focus)) {
                    Showcase(LAYOUTS, l.layout, tint, { Choices.setLook("layout", it) }, cardWidth = 196.dp, cardHeight = 130.dp) { i ->
                        Row(Modifier.width(136.dp).height(64.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            when (i) {
                                0 -> {
                                    Column(Modifier.weight(1.1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0x40FFFFFF), RoundedCornerShape(6.dp)))
                                        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0x40FFFFFF), RoundedCornerShape(6.dp)))
                                    }
                                    Box(Modifier.weight(1f).fillMaxHeight().background(Look.Gold.copy(alpha = .55f), RoundedCornerShape(6.dp)))
                                    Box(Modifier.weight(1f).fillMaxHeight().background(Look.Gold.copy(alpha = .40f), RoundedCornerShape(6.dp)))
                                }
                                1 -> {
                                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0x40FFFFFF), RoundedCornerShape(6.dp)))
                                        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0x40FFFFFF), RoundedCornerShape(6.dp)))
                                    }
                                    Box(Modifier.weight(1f).fillMaxHeight().background(Look.Gold.copy(alpha = .55f), RoundedCornerShape(6.dp)))
                                }
                                2 -> {
                                    Box(Modifier.weight(1f).fillMaxHeight().background(Look.Gold.copy(alpha = .55f), RoundedCornerShape(6.dp)))
                                    Box(Modifier.weight(1f).fillMaxHeight().background(Look.Gold.copy(alpha = .45f), RoundedCornerShape(6.dp)))
                                }
                                else -> {
                                    Box(Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 16.dp).background(Look.Gold.copy(alpha = .55f), RoundedCornerShape(6.dp)))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("أماكن وتوزيع الإضافات في الشبكة", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                Text("اختر أي موضع في الشبكة لتعديل نوع الإضافة أو شكلها:", style = Look.style(13.sp, Look.Soft))
                Spacer(Modifier.height(2.dp))
                BentoSlotGrid(l.layout, picks, onSelectSlot = { editingSlot = it })

                val activeKinds = places.indices.map { l.addons.getOrElse(it) { ADDON_NONE } }

                if (ADDON_PRAYER in activeKinds) {
                    Spacer(Modifier.height(8.dp))
                    Text("شكل بطاقة الصلاة", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                    Text("اختر المظهر المفضل لمواقيت الصلاة في الواجهة:", style = Look.style(13.sp, Look.Soft))
                    PrayerStyleShowcase(l.prayer, { Choices.setLook("prayer", it) }, tint, board, picks)
                }

                if (ADDON_GAME in activeKinds) {
                    Spacer(Modifier.height(8.dp))
                    Text("شكل بطاقة المباراة", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                    Text("اختر المظهر المفضل للمباريات القادمة ونتائج الأندية:", style = Look.style(13.sp, Look.Soft))
                    GameStyleShowcase(l.game, { Choices.setLook("game", it) }, tint, board, picks)
                    Spacer(Modifier.height(4.dp))
                    MenuRow(
                        name = "الفرق المعروضة في بطاقة المباراة",
                        detail = if (picks.gameClubs.isEmpty()) "${picks.clubIds.size} فرق (الافتراضي)" else "${picks.gameClubs.size} فرق محددة",
                        onOk = { subPage = "gameClubs" },
                    )
                    MenuRow(
                        name = "التمرير التلقائي بين المباريات",
                        detail = if (l.autoScroll == 1) "مفعّل" else "متوقف",
                        onOk = { Choices.setLook("autoScroll", 1 - l.autoScroll) },
                    )
                }

                if (ADDON_SKY in activeKinds) {
                    Spacer(Modifier.height(8.dp))
                    Text("شكل بطاقة الطقس", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                    Text("اختر المظهر المفضل لحالة الجو ودرجات الحرارة:", style = Look.style(13.sp, Look.Soft))
                    SkyStyleShowcase(l.sky, { Choices.setLook("sky", it) }, tint, board, picks)
                }

                Spacer(Modifier.height(6.dp))
                Hairline()
                Spacer(Modifier.height(4.dp))
                Text("المظهر العام للبطاقات", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                MenuRow(
                    name = "نمط خلفية البطاقات في الواجهة",
                    detail = if (l.cards == 0) "عائمة بدون خلفية" else "بطاقات داكنة كالإطار",
                    onOk = { Choices.setLook("cards", 1 - l.cards) },
                )
                Spacer(Modifier.height(28.dp))
            }
        }
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
private fun WorldClubPage(picks: Picks, onPicked: () -> Unit) {
    val leagues = Clubs.worldLeagues
    val currentClub = Clubs.find(picks.worldClubId)
    var leagueIdx by remember(picks.worldClubId) {
        mutableIntStateOf(leagues.indexOfFirst { it.slug == currentClub?.league }.coerceAtLeast(0))
    }
    val league = leagues[leagueIdx.coerceIn(0, leagues.lastIndex)]
    val clubs = league.clubs
    val rows = listOf(Triple("بدون", if (picks.worldClubId == null) "الحالي" else null) { Choices.setWorldClub(null); onPicked() }) +
        clubs.map { club ->
            Triple(club.name, if (club.id == picks.worldClubId) "الحالي" else null) { Choices.setWorldClub(club.id); onPicked() }
        }
    val selected = (clubs.indexOfFirst { it.id == picks.worldClubId } + 1).coerceAtLeast(0)
    Column(Modifier.fillMaxSize()) {
        Text("الدوري", style = Look.style(15.sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(bottom = 10.dp))
        Chips(leagues.map { it.shortName }, leagueIdx, perRow = 4) { leagueIdx = it }
        Spacer(Modifier.height(16.dp))
        Text(league.name, style = Look.style(15.sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(bottom = 8.dp))
        Box(Modifier.weight(1f)) {
            Choose(rows, selected = selected)
        }
    }
}

@Composable
private fun CityPage(picks: Picks, onPicked: () -> Unit) {
    val countries = Places.countries
    var pickedCountry by remember { mutableStateOf<String?>(null) }

    if (pickedCountry != null) {
        val country = pickedCountry!!
        val cities = Places.forCountry(country)
        val selectedCityIdx = cities.indexOfFirst { it.key == picks.place.key }

        BackHandler(enabled = true) { pickedCountry = null }

        val rows = listOf(
            Triple("◀ العودة لقائمة الدول", country) { pickedCountry = null }
        ) + cities.map { p ->
            Triple(p.name, if (p.key == picks.place.key) "الحالي" else null) {
                Choices.setPlace(p.key)
                onPicked()
            }
        }
        val selected = if (selectedCityIdx >= 0) selectedCityIdx + 1 else 1

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("مدن $country", style = Look.style(18.sp, Look.Gold, FontWeight.Medium))
                Text("${cities.size} مدينة", style = Look.style(13.sp, Look.Soft))
            }
            Box(Modifier.weight(1f)) {
                androidx.compose.runtime.key(country) {
                    Choose(rows, selected = selected.coerceIn(0, rows.lastIndex))
                }
            }
        }
    } else {
        val currentCountryIdx = countries.indexOf(picks.place.country).coerceAtLeast(0)
        val rows = countries.map { country ->
            val isCurrent = country == picks.place.country
            val count = Places.forCountry(country).size
            val detail = if (isCurrent) "الحالي (${picks.place.name})" else "$count مدينة"
            Triple(country, detail) {
                pickedCountry = country
            }
        }

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("اختر الدولة", style = Look.style(18.sp, Look.Gold, FontWeight.Medium))
                Text("${countries.size} دولة حول العالم", style = Look.style(13.sp, Look.Soft))
            }
            Box(Modifier.weight(1f)) {
                androidx.compose.runtime.key("countries_list") {
                    Choose(rows, selected = currentCountryIdx)
                }
            }
        }
    }
}

@Composable
private fun PrayerPage(picks: Picks) {
    var subPage by remember { mutableStateOf<String?>(null) } // null, "city", "method", "madhhab", "adjust"

    when (subPage) {
        "city" -> {
            BackHandler(enabled = true) { subPage = null }
            CityPage(picks, onPicked = { subPage = null })
        }
        "method" -> {
            BackHandler(enabled = true) { subPage = null }
            val rows = Salah.METHODS.mapIndexed { idx, m ->
                Triple(m.name, if (idx == picks.prayerMethod) "الحالي" else null) {
                    Choices.setPrayerMethod(idx)
                    subPage = null
                }
            }
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("طريقة حساب مواقيت الصلاة", style = Look.style(18.sp, Look.Gold, FontWeight.Medium))
                    Text("الهيئات والوزارات الإسلامية", style = Look.style(13.sp, Look.Soft))
                }
                Box(Modifier.weight(1f)) {
                    Choose(rows, selected = picks.prayerMethod)
                }
            }
        }
        "madhhab" -> {
            BackHandler(enabled = true) { subPage = null }
            val rows = Salah.MADHHABS.mapIndexed { idx, m ->
                Triple(m, if (idx == picks.prayerMadhhab) "الحالي" else null) {
                    Choices.setPrayerMadhhab(idx)
                    subPage = null
                }
            }
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("المذهب الفقهي في صلاة العصر", style = Look.style(18.sp, Look.Gold, FontWeight.Medium))
                    Text("تحديد وقت دخول صلاة العصر", style = Look.style(13.sp, Look.Soft))
                }
                Box(Modifier.weight(1f)) {
                    Choose(rows, selected = picks.prayerMadhhab)
                }
            }
        }
        "adjust" -> {
            PrayerAdjustPage(picks, onBack = { subPage = null })
        }
        else -> {
            val now = System.currentTimeMillis()
            val dayPrayers = Salah.day(now, picks)
            val nextSalat = Salah.next(now, picks)
            val (sunriseAt, _) = Salah.sun(now, picks)
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { if (Pane.live) runCatching { focus.requestFocus() } }

            val allTimes = listOf(
                "الفجر" to (dayPrayers.getOrNull(0)?.at ?: 0L),
                "الشروق" to sunriseAt,
                "الظهر" to (dayPrayers.getOrNull(1)?.at ?: 0L),
                "العصر" to (dayPrayers.getOrNull(2)?.at ?: 0L),
                "المغرب" to (dayPrayers.getOrNull(3)?.at ?: 0L),
                "العشاء" to (dayPrayers.getOrNull(4)?.at ?: 0L),
            )

            Column(Modifier.fillMaxSize().verticalScroll(paneScroll()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Live preview card showing all 5 prayers + sunrise
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Look.Gold.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("مواقيت اليوم في ${picks.place.name}", style = Look.style(15.sp, Look.Gold, FontWeight.Medium))
                            if (nextSalat != null) {
                                Text("القادمة: ${nextSalat.name}", style = Look.style(13.sp, Look.Live, FontWeight.Bold))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            allTimes.forEach { (name, at) ->
                                val isNext = nextSalat?.name == name
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(name, style = Look.style(12.sp, if (isNext) Look.Gold else Look.Soft, FontWeight.Medium))
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        clock12(at, picks.place.zone, picks.latinDigits),
                                        style = Look.style(14.sp, if (isNext) Look.Ink else Look.Soft.copy(alpha = 0.85f), FontWeight.SemiBold)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("الموقع والحساب الفلكي", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                MenuRow(
                    name = "المدينة الحالية",
                    detail = "${picks.place.name} (${picks.place.country})",
                    onOk = { subPage = "city" },
                    modifier = Modifier.focusRequester(focus),
                )
                MenuRow(
                    name = "طريقة الحساب الفلكي",
                    detail = Salah.METHODS.getOrElse(picks.prayerMethod) { Salah.METHODS[0] }.name,
                    onOk = { subPage = "method" },
                )
                MenuRow(
                    name = "المذهب في صلاة العصر",
                    detail = Salah.MADHHABS.getOrElse(picks.prayerMadhhab) { Salah.MADHHABS[0] },
                    onOk = { subPage = "madhhab" },
                )
                MenuRow(
                    name = "تعديل أوقات الصلاة يدوياً",
                    detail = if (picks.prayerAdjust.all { it == 0 }) "مضبوطة فلكياً (بدون تعديل)" else "تعديل بالدقائق",
                    onOk = { subPage = "adjust" },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PrayerAdjustPage(picks: Picks, onBack: () -> Unit = {}) {
    var activeIdx by remember { mutableStateOf<Int?>(null) }
    val now = System.currentTimeMillis()
    val dayPrayers = Salah.day(now, picks)
    val (sunriseAt, _) = Salah.sun(now, picks)

    val prayersList = listOf(
        Triple(0, "الفجر", dayPrayers.getOrNull(0)?.at ?: 0L),
        Triple(1, "الشروق", sunriseAt),
        Triple(2, "الظهر", dayPrayers.getOrNull(1)?.at ?: 0L),
        Triple(3, "العصر", dayPrayers.getOrNull(2)?.at ?: 0L),
        Triple(4, "المغرب", dayPrayers.getOrNull(3)?.at ?: 0L),
        Triple(5, "العشاء", dayPrayers.getOrNull(4)?.at ?: 0L),
    )

    if (activeIdx != null) {
        val idx = activeIdx!!
        val (_, name, at) = prayersList[idx]
        val currentAdjust = picks.prayerAdjust.getOrElse(idx) { 0 }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { if (Pane.live) runCatching { focus.requestFocus() } }
        BackHandler(enabled = true) { activeIdx = null }

        val timeStr = clock12(at, picks.place.zone, picks.latinDigits)

        Column(Modifier.fillMaxSize().verticalScroll(paneScroll()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("تعديل وقت $name", style = Look.style(18.sp, Look.Gold, FontWeight.Medium))
                Text(
                    when {
                        currentAdjust == 0 -> "مضبوط فلكياً (٠)"
                        currentAdjust > 0 -> "+${Look.digits(currentAdjust.toString(), picks.latinDigits)} دقيقة"
                        else -> "${Look.digits(currentAdjust.toString(), picks.latinDigits)} دقيقة"
                    },
                    style = Look.style(15.sp, if (currentAdjust == 0) Look.Soft else Look.Gold, FontWeight.Bold),
                )
            }

            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0x14FFFFFF)).padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(timeStr, style = Look.style(36.sp, Look.Ink, big = true))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (currentAdjust == 0) "الوقت مطابق للحساب الفلكي المعتمد" else "تم تقديم/تأخير الوقت بـ $currentAdjust دقيقة",
                        style = Look.style(13.sp, Look.Soft),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { MenuRow("- دقيقة واحدة", { Choices.adjustPrayerMinute(idx, -1) }) }
                Box(Modifier.weight(1f)) { MenuRow("+ دقيقة واحدة", { Choices.adjustPrayerMinute(idx, +1) }, Modifier.focusRequester(focus)) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { MenuRow("- ٥ دقائق", { Choices.adjustPrayerMinute(idx, -5) }) }
                Box(Modifier.weight(1f)) { MenuRow("+ ٥ دقائق", { Choices.adjustPrayerMinute(idx, +5) }) }
            }

            Spacer(Modifier.height(6.dp))
            Hairline()
            Spacer(Modifier.height(4.dp))
            if (currentAdjust != 0) {
                MenuRow("إعادة وقت $name للحساب الفلكي (٠)", { Choices.adjustPrayerMinute(idx, -currentAdjust) })
            }
            MenuRow("◀ حفظ والعودة لقائمة الصلوات", { activeIdx = null })
            Spacer(Modifier.height(16.dp))
        }
    } else {
        BackHandler(enabled = true) { onBack() }
        val anyAdjusted = picks.prayerAdjust.any { it != 0 }
        val focus = remember { FocusRequester() }
        LaunchedEffect(Unit) { if (Pane.live) runCatching { focus.requestFocus() } }

        Column(Modifier.fillMaxSize().verticalScroll(paneScroll()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "اختر الصلاة لتعديل موعدها بالتقديم أو التأخير:",
                style = Look.style(14.sp, Look.Soft),
                modifier = Modifier.padding(bottom = 4.dp),
            )
            prayersList.forEachIndexed { i, (idx, name, at) ->
                val adj = picks.prayerAdjust.getOrElse(idx) { 0 }
                val timeStr = clock12(at, picks.place.zone, picks.latinDigits)
                val statusText = when {
                    adj == 0 -> "مضبوط (٠)"
                    adj > 0 -> "+${Look.digits(adj.toString(), picks.latinDigits)} د"
                    else -> "${Look.digits(adj.toString(), picks.latinDigits)} د"
                }
                MenuRow(
                    name = "$name · $timeStr",
                    detail = statusText,
                    onOk = { activeIdx = idx },
                    modifier = if (i == 0) Modifier.focusRequester(focus) else Modifier,
                )
            }

            if (anyAdjusted) {
                Spacer(Modifier.height(8.dp))
                Hairline()
                Spacer(Modifier.height(4.dp))
                MenuRow("إعادة ضبط جميع الصلوات للحساب الفلكي (٠ دقيقة)", { Choices.resetPrayerAdjust() })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GameClubsPage(picks: Picks, onBack: () -> Unit) {
    BackHandler(enabled = true) { onBack() }
    val allLeagues = Clubs.leagues
    var leagueIdx by remember { mutableIntStateOf(0) }
    val leagueTabs = listOf("الكل") + allLeagues.map { it.shortName }

    val currentClubs = if (leagueIdx == 0) {
        allLeagues.flatMap { it.clubs }.distinctBy { it.id }
    } else {
        allLeagues[leagueIdx - 1].clubs
    }

    val followed = picks.clubIds.toSet()
    val isCustom = picks.gameClubs.isNotEmpty()

    val rows = mutableListOf<Triple<String, String?, () -> Unit>>()

    if (isCustom) {
        rows.add(Triple("استعادة الافتراضي (فريقك المحلي والعالمي)", "إعادة ضبط") {
            Choices.resetGameClubs()
        })
    }

    currentClubs.forEach { club ->
        val isPrimary = club.id == picks.clubId || club.id == picks.worldClubId
        val isFollowed = club.id in followed
        val status = when {
            isPrimary -> "فريق أساسي ⭐"
            isFollowed -> "متابع ✓"
            else -> null
        }
        val leagueName = if (leagueIdx == 0) {
            allLeagues.firstOrNull { it.slug == club.league }?.shortName ?: ""
        } else ""
        val displayName = if (leagueName.isNotEmpty()) "${club.name}  ($leagueName)" else club.name
        rows.add(Triple(displayName, status) {
            Choices.toggleGameClub(club.id)
        })
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("الفرق المعروضة في إضافة المباراة", style = Look.style(18.sp, Look.Gold, FontWeight.Medium))
            Text("${followed.size} فرق متابعة", style = Look.style(13.sp, Look.Soft))
        }
        Chips(leagueTabs, leagueIdx, perRow = 4) { leagueIdx = it }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.weight(1f)) {
            Choose(rows, selected = 0)
        }
    }
}

@Composable
private fun SportsPage(picks: Picks) {
    var subPage by remember { mutableStateOf<String?>(null) } // null, "localClub", "worldClub"

    when (subPage) {
        "localClub" -> {
            BackHandler(enabled = true) { subPage = null }
            val rows = listOf(Triple("بدون (إلغاء الفريق المحلي)", null as String?) { Choices.setClub(null); subPage = null }) +
                Clubs.localClubs.map { club ->
                    Triple(club.name, if (club.id == picks.clubId) "الحالي" else null) {
                        Choices.setClub(club.id)
                        subPage = null
                    }
                }
            val selected = (Clubs.localClubs.indexOfFirst { it.id == picks.clubId } + 1).coerceAtLeast(0)
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("اختر فريقك المحلي", style = Look.style(18.sp, Look.Gold, FontWeight.Medium))
                    Text("دوري روشن والأندية السعودية", style = Look.style(13.sp, Look.Soft))
                }
                Box(Modifier.weight(1f)) {
                    Choose(rows, selected = selected)
                }
            }
        }
        "worldClub" -> {
            BackHandler(enabled = true) { subPage = null }
            WorldClubPage(picks, onPicked = { subPage = null })
        }
        else -> {
            val allLeagues = Clubs.leagues
            val enabledLeagues = picks.leagues.toSet()
            val localClubName = Clubs.find(picks.clubId)?.name ?: "بدون فريق"
            val worldClubName = Clubs.find(picks.worldClubId)?.name ?: "بدون فريق"
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { if (Pane.live) runCatching { focus.requestFocus() } }

            Column(Modifier.fillMaxSize().verticalScroll(paneScroll()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("فرقك المفضلة", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                Text("تُعرض في جدول الترتيب والجزيرة العلوية للمباريات:", style = Look.style(13.sp, Look.Soft))
                MenuRow(
                    name = "الفريق المحلي (السعودي)",
                    detail = localClubName,
                    onOk = { subPage = "localClub" },
                    modifier = Modifier.focusRequester(focus),
                )
                MenuRow(
                    name = "الفريق العالمي (الأوروبي)",
                    detail = worldClubName,
                    onOk = { subPage = "worldClub" },
                )

                Spacer(Modifier.height(6.dp))
                Hairline()
                Spacer(Modifier.height(4.dp))

                Text("الدوريات المعروضة في جدول الترتيب", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                allLeagues.forEach { league ->
                    val on = league.slug in enabledLeagues
                    MenuRow(
                        name = league.name,
                        detail = if (on) "مفعّل" else "معطّل",
                        onOk = {
                            val updated = if (on) {
                                if (enabledLeagues.size > 1) enabledLeagues - league.slug else enabledLeagues
                            } else {
                                enabledLeagues + league.slug
                            }
                            Choices.setLeagues(updated.toList())
                        },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun OverlayPage(picks: Picks, c: Context) {
    val l = picks.looks
    val mode = l.overlayMode.coerceIn(0, 2)
    val prayerMode = l.overlayPrayer.coerceIn(0, 3)
    val preIdx = l.overlayPre.coerceIn(0, app.tilfazi.tv.feed.OVERLAY_PRE_LABELS.lastIndex)
    val postIdx = l.overlayPost.coerceIn(0, app.tilfazi.tv.feed.OVERLAY_POST_LABELS.lastIndex)
    val homeIdx = l.overlayHome.coerceIn(0, 1)
    val alphaIdx = l.overlayAlpha.coerceIn(0, 2)
    val pillAlpha = app.tilfazi.tv.feed.OVERLAY_ALPHA_VALS[alphaIdx]
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (Pane.live) runCatching { focus.requestFocus() } }
    androidx.compose.runtime.DisposableEffect(Unit) {
        app.tilfazi.tv.overlay.MatchOverlay.setSettingsOpen(true)
        onDispose { app.tilfazi.tv.overlay.MatchOverlay.setSettingsOpen(false) }
    }

    val homeClub = Clubs.find(picks.clubId)?.name ?: "الهلال"
    val awayClub = if (homeClub == "الهلال") "النصر" else "الهلال"
    val scoreText = Look.ltr("${Look.digits("1", picks.latinDigits)} - ${Look.digits("1", picks.latinDigits)}")
    val minText = Look.digits("74'", picks.latinDigits)
    val nextPrayerName = app.tilfazi.tv.feed.Salah.next(System.currentTimeMillis(), picks)?.name ?: "العشاء"
    val preMins = app.tilfazi.tv.feed.OVERLAY_PRE_MINS[preIdx]
    val postMins = app.tilfazi.tv.feed.OVERLAY_POST_MINS[postIdx]

    fun runDemo(kind: String) {
        if (!app.tilfazi.tv.overlay.MatchOverlay.canDraw(c)) {
            app.tilfazi.tv.overlay.MatchOverlay.requestPermission(c)
        } else {
            app.tilfazi.tv.overlay.MatchOverlay.triggerDemo(kind)
        }
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Standalone Dynamic Island shape at the top, updating live according to what is enabled/disabled
        val notchShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp)
        Box(
            Modifier
                .padding(top = 4.dp, bottom = 14.dp)
                .background(Color(0xFF0A0B0E).copy(alpha = pillAlpha), notchShape)
                .border(1.2.dp, if (mode > 0 || prayerMode > 0) Look.Gold.copy(alpha = 0.7f) else Color(0x33FFFFFF), notchShape)
                .padding(horizontal = 22.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (mode > 0 || prayerMode > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (prayerMode > 0) {
                        val prayerTitle = when (prayerMode) {
                            1 -> "☪ أذان $nextPrayerName"
                            2 -> "☪ اقترب أذان $nextPrayerName"
                            else -> "☪ أذان $nextPrayerName"
                        }
                        val prayerDetail = when (prayerMode) {
                            1 -> "· مضى على الأذان ${Look.digits(postMins.toString(), picks.latinDigits)} دقيقة"
                            2 -> "· بعد ${Look.ltr(Look.digits("${preMins.toString().padStart(2, '0')}:00", picks.latinDigits))}"
                            else -> "· بعد ${Look.ltr(Look.digits("02:15:30", picks.latinDigits))}"
                        }
                        Text(prayerTitle, style = Look.style(14.sp, Look.Gold, FontWeight.Bold).copy(shadow = null))
                        Text(prayerDetail, style = Look.style(13.sp, Look.Soft).copy(shadow = null))
                    }
                    if (prayerMode > 0 && mode > 0) {
                        Text("│", style = Look.style(14.sp, Look.Gold.copy(alpha = 0.4f)).copy(shadow = null))
                    }
                    if (mode > 0) {
                        Text(homeClub, style = Look.style(14.sp, Look.Ink, FontWeight.Bold).copy(shadow = null))
                        Text(scoreText, style = Look.style(16.sp, Look.Gold, FontWeight.Bold).copy(shadow = null))
                        Text(awayClub, style = Look.style(14.sp, Look.Ink, FontWeight.Bold).copy(shadow = null))
                        Text("│", style = Look.style(13.sp, Color(0x33FFFFFF)).copy(shadow = null))
                        Text("● $minText", style = Look.style(13.sp, Color(0xFF4CD964), FontWeight.Medium).copy(shadow = null))
                        if (mode == 1) {
                            Text("│", style = Look.style(13.sp, Look.Gold.copy(alpha = 0.4f)).copy(shadow = null))
                            Text("● هدف التعادل · $minText", style = Look.style(13.sp, Look.Gold, FontWeight.Bold).copy(shadow = null))
                        }
                    }
                }
            } else {
                Text("الجزيرة العلوية متوقفة حالياً", style = Look.style(14.sp, Look.Soft).copy(shadow = null))
            }
        }

        Hairline(Modifier.width(340.dp))
        Spacer(Modifier.height(14.dp))

        // Scrollable clean controls
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(paneScroll()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val hasPerm = app.tilfazi.tv.overlay.MatchOverlay.canDraw(c)
            if (!hasPerm) {
                MenuRow("تفعيل صلاحية الظهور فوق التطبيقات", { app.tilfazi.tv.overlay.MatchOverlay.requestPermission(c) }, detail = "مطلوب")
                Spacer(Modifier.height(4.dp))
            }

            // Section 1: Prayer alerts & countdown
            Text("نظام الصلاة والعد التنازلي", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
            Box(Modifier.focusRequester(focus)) {
                Chips(app.tilfazi.tv.feed.OVERLAY_PRAYER_MODES, prayerMode, perRow = 2) { Choices.setLook("overlayPrayer", it) }
            }

            if (prayerMode in 2..3) {
                Spacer(Modifier.height(2.dp))
                Text("بدء العد التنازلي قبل الأذان:", style = Look.style(13.sp, Look.Soft))
                Chips(app.tilfazi.tv.feed.OVERLAY_PRE_LABELS, preIdx, perRow = 5) { Choices.setLook("overlayPre", it) }
            }

            if (prayerMode in 1..3) {
                Spacer(Modifier.height(2.dp))
                Text("مدة البقاء بعد الأذان (مضى على الأذان):", style = Look.style(13.sp, Look.Soft))
                Chips(app.tilfazi.tv.feed.OVERLAY_POST_LABELS, postIdx, perRow = 6) { Choices.setLook("overlayPost", it) }
            }

            Spacer(Modifier.height(6.dp))
            Hairline()
            Spacer(Modifier.height(6.dp))

            // Section 2: Matches
            Text("متابعة مباريات كرة القدم", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
            Chips(app.tilfazi.tv.feed.OVERLAY_MODES, mode) { Choices.setLook("overlayMode", it) }

            Spacer(Modifier.height(6.dp))
            Hairline()
            Spacer(Modifier.height(6.dp))

            // Section 3: Appearance & Display Locations
            Text("المظهر ومكان الظهور", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
            Chips(app.tilfazi.tv.feed.OVERLAY_HOME_MODES, homeIdx) { Choices.setLook("overlayHome", it) }
            Spacer(Modifier.height(2.dp))
            Text("شفافية الجزيرة:", style = Look.style(13.sp, Look.Soft))
            Chips(app.tilfazi.tv.feed.OVERLAY_ALPHAS, alphaIdx) { Choices.setLook("overlayAlpha", it) }

            Spacer(Modifier.height(6.dp))
            Hairline()
            Spacer(Modifier.height(6.dp))

            // Section 4: Live Testing Pills
            Text("تجربة الجزيرة العلوية الآن", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) { MenuRow("صلاة + مباراة", { runDemo("both") }) }
                Box(Modifier.weight(1f)) { MenuRow("عد تنازلي", { runDemo("countdown") }) }
                Box(Modifier.weight(1f)) { MenuRow("بعد الأذان", { runDemo("prayer") }) }
                Box(Modifier.weight(1f)) { MenuRow("هدف", { runDemo("goal") }) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DisplayPage(picks: Picks, c: Context) {
    val l = picks.looks
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (Pane.live) runCatching { focus.requestFocus() } }

    var fontSubpage by remember { mutableStateOf(false) }

    if (fontSubpage) {
        BackHandler(enabled = true) { fontSubpage = false }
        val sets = Look.available
        val rows = sets.map { idx ->
            Triple(Look.FONTS[idx], if (idx == l.font) "الحالي ✓" else null) {
                Choices.setLook("font", idx)
                Look.load(c)
                fontSubpage = false
            }
        }
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("اختر خط الواجهة", style = Look.style(18.sp, Look.Gold, FontWeight.Medium))
                Text("مرخصة برخصة الخط المفتوح", style = Look.style(13.sp, Look.Soft))
            }
            Box(Modifier.weight(1f)) {
                Choose(rows, selected = sets.indexOf(l.font).coerceAtLeast(0))
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(paneScroll()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("الخطوط ولغة الأرقام", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
        MenuRow(
            name = "خط الكتابة في الواجهة",
            detail = Look.FONTS.getOrElse(l.font) { Look.FONTS[0] },
            onOk = { fontSubpage = true },
            modifier = Modifier.focusRequester(focus),
        )
        MenuRow(
            name = "رسم الأرقام",
            detail = if (picks.latinDigits) "أرقام إنجليزية (123)" else "أرقام عربية (١٢٣)",
            onOk = { Choices.setLatinDigits(!picks.latinDigits) },
        )

        Spacer(Modifier.height(6.dp))
        Hairline()
        Spacer(Modifier.height(4.dp))

        Text("أبعاد الشاشة وهوامش العرض", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
        MenuRow(
            name = "حجم عناصر الواجهة",
            detail = app.tilfazi.tv.feed.SIZES[l.size.coerceIn(0, 4)],
            onOk = { Choices.setLook("size", (l.size + 1) % 5) },
        )
        MenuRow(
            name = "هوامش أطراف الشاشة (قص الحواف)",
            detail = app.tilfazi.tv.feed.EDGES[l.edge.coerceIn(0, 3)],
            onOk = { Choices.setLook("edge", (l.edge + 1) % 4) },
        )

        Spacer(Modifier.height(6.dp))
        Hairline()
        Spacer(Modifier.height(4.dp))

        Text("الأداء والتأثيرات الحركية", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
        MenuRow(
            name = "الحركة والانتقالات الناعمة",
            detail = if (l.still == 1) "متوقفة (أخف على الجهاز)" else "تعمل بنعومة",
            onOk = { Choices.setLook("still", 1 - l.still) },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AboutPage(picks: Picks, c: Context) {
    var subPage by remember { mutableStateOf<String?>(null) } // null, "homes", "update", "contact", "remove"

    when (subPage) {
        "homes" -> {
            BackHandler(enabled = true) { subPage = null }
            val homes = homes(c)
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("اختر الواجهة الرئيسية", style = Look.style(18.sp, Look.Gold, FontWeight.Medium))
                    Text("الواجهات المثبتة على التلفزيون", style = Look.style(13.sp, Look.Soft))
                }
                Box(Modifier.weight(1f)) {
                    Choose(homes.map { (name, open, current) -> Triple(name, if (current) "الحالية ✓" else null) { open(); subPage = null } }, selected = 0)
                }
            }
        }
        "update" -> {
            BackHandler(enabled = true) { subPage = null }
            UpdatePage(c)
        }
        "contact" -> {
            BackHandler(enabled = true) { subPage = null }
            ContactPage()
        }
        "remove" -> {
            BackHandler(enabled = true) { subPage = null }
            RemovePage(c)
        }
        else -> {
            val focus = remember { FocusRequester() }
            LaunchedEffect(Unit) { if (Pane.live) runCatching { focus.requestFocus() } }

            Column(Modifier.fillMaxSize().verticalScroll(paneScroll()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x18FFFFFF))
                        .border(1.dp, Look.Gold.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("تلفازي TV", style = Look.style(24.sp, Look.Gold, FontWeight.Bold))
                        Spacer(Modifier.height(4.dp))
                        Text("الإصدار ${Look.digits("2.0.0", picks.latinDigits)}", style = Look.style(14.sp, Look.Ink, FontWeight.Medium))
                        Spacer(Modifier.height(4.dp))
                        Text("واجهة تلفزيون عربية هادئة وسريعة بدون إعلانات", style = Look.style(12.sp, Look.Soft))
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text("إدارة التطبيق والنظام", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
                MenuRow(
                    name = "الواجهة الرئيسية للتلفزيون (Launcher)",
                    detail = homeName(c),
                    onOk = { subPage = "homes" },
                    modifier = Modifier.focusRequester(focus),
                )
                MenuRow(
                    name = "البحث عن تحديثات",
                    detail = "النسخة 2.0.0",
                    onOk = { subPage = "update" },
                )
                MenuRow(
                    name = "تواصل ومساعدة",
                    detail = "@TalalRugi",
                    onOk = { subPage = "contact" },
                )

                Spacer(Modifier.height(6.dp))
                Hairline()
                Spacer(Modifier.height(4.dp))

                MenuRow(
                    name = "إزالة تلفازي واستعادة الواجهة السابقة",
                    detail = "حذف التطبيق",
                    onOk = { subPage = "remove" },
                )
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Choose(rows: List<Triple<String, String?, () -> Unit>>, selected: Int) {
    val safeSelected = selected.coerceIn(0, (rows.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (safeSelected - 1).coerceAtLeast(0))
    val focus = remember(rows.size, safeSelected) { FocusRequester() }
    LaunchedEffect(rows.size, safeSelected) {
        if (Pane.live) {
            runCatching {
                listState.scrollToItem((safeSelected - 1).coerceAtLeast(0))
                focus.requestFocus()
            }
        }
    }
    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        itemsIndexed(rows) { i, (name, detail, run) ->
            MenuRow(name, run, if (i == safeSelected) Modifier.focusRequester(focus) else Modifier, detail)
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
        "المظهر والواجهة" to listOf(
            Item(
                name = "الإضافات",
                value = LAYOUTS[l.layout.coerceIn(0, 3)],
                about = "تخصيص شبكة البطاقات وأماكنها وأشكال بطاقات الصلاة والمباريات",
                page = Page.Addons,
            ),
            Item(
                name = "الخلفية",
                value = if (l.backdrop == 1) "صور، ${PHOTO_SOURCES[l.photos.coerceIn(0, 2)]}" else "زجاج، ${Tints.of(l.tint).name}",
                about = "لون زجاجي هادئ أو صور طبيعية تتبدل خلف الواجهة",
                page = Page.Backdrop,
            ),
            Item(
                name = "شاشة التوقف والساعة",
                value = SCENES[l.scene.coerceIn(0, SCENES.lastIndex)],
                about = "المشاهد الطبيعية، أشكال الساعة الفنية، والتعتيم التلقائي",
                page = Page.Ambient,
            ),
        ),
        "المحتوى والتفضيلات" to listOf(
            Item(
                name = "مواقيت الصلاة والمدينة",
                value = "${picks.place.name} · ${Salah.METHODS.getOrElse(picks.prayerMethod) { Salah.METHODS[0] }.name}",
                about = "المدينة، الهيئة الفلكية المعتمدة، مذهب العصر، والتعديل اليدوي بالدقائق",
                page = Page.Prayer,
            ),
            Item(
                name = "الأندية والدوريات",
                value = listOfNotNull(Clubs.find(picks.clubId)?.name, Clubs.find(picks.worldClubId)?.name).joinToString(" · ").ifEmpty { "حدد أنديتك" },
                about = "فريقك المحلي والعالمي المفضل، وتفعيل دوريات جدول الترتيب",
                page = Page.Sports,
            ),
            Item(
                name = "الجزيرة العلوية",
                value = when {
                    l.overlayMode > 0 && l.overlayPrayer > 0 -> "المباريات والصلاة"
                    l.overlayMode > 0 -> "المباريات"
                    l.overlayPrayer > 0 -> "الصلاة"
                    else -> "مطفأة"
                },
                about = "شريط علوي ذكي لمتابعة الصلاة والمباريات في الواجهة وفوق التطبيقات",
                page = Page.Overlay,
            ),
        ),
        "الجهاز والتطبيق" to listOf(
            Item(
                name = "إعدادات العرض والخط",
                value = Look.FONTS.getOrElse(l.font) { Look.FONTS[0] },
                about = "نوع الخط، حجم عناصر الواجهة، هوامش الشاشة، والأرقام",
                page = Page.Display,
            ),
            Item(
                name = "حول تلفازي",
                value = "الإصدار 2.0",
                about = "معلومات الإصدار، الواجهة الافتراضية، التحديثات، والمساعدة",
                page = Page.About,
            ),
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
            Column(Modifier.width(280.dp).fillMaxHeight()) {
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
                    .border(if (inside) 1.5.dp else 1.dp, if (inside) Look.Gold.copy(alpha = .6f) else Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
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
                    Column(Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 26.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(shown.name, style = Look.style(22.sp, Look.Gold, FontWeight.Bold))
                                Spacer(Modifier.height(3.dp))
                                Text(shown.about, style = Look.style(13.sp, Look.Soft), maxLines = 1)
                            }
                            if (inside) {
                                Box(
                                    Modifier
                                        .background(Color(0x26D6AF6B), RoundedCornerShape(20.dp))
                                        .border(1.dp, Look.Gold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text("تحكم نشط · اضغط Back للرجوع للقائمة", style = Look.style(11.5.sp, Look.Gold, FontWeight.Medium))
                                }
                            } else {
                                Box(
                                    Modifier
                                        .background(Color(0x14FFFFFF), RoundedCornerShape(20.dp))
                                        .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text("اضغط OK للدخول والتعديل", style = Look.style(11.5.sp, Look.Faint))
                                }
                            }
                        }
                        Hairline(Modifier.fillMaxWidth().padding(bottom = 16.dp))

                        // the name is already lit in the list; a page with no parts of its own says what it is for
                        val page = shown.page
                        if (page != null && page != Page.Favourites) {
                            // drawn anew on the way in, so the page puts the focus where it always does
                            androidx.compose.runtime.key(page, inside) {
                                val scroll = rememberScrollState()
                                Box(Modifier.weight(1f).fillMaxWidth()) {
                                    androidx.compose.runtime.CompositionLocalProvider(LocalPaneScroll provides scroll) { body(page) }
                                    MoreBelow(scroll, Modifier.align(Alignment.BottomCenter))
                                }
                            }
                        } else {
                            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
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
    val isCurrentInside = current && inside
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
            .border(
                1.dp,
                if (isCurrentInside) Look.Gold.copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .background(
                when {
                    focused -> Color(0xF2FFFFFF)
                    isCurrentInside -> Color(0x28D6AF6B)
                    current -> Color(0x18FFFFFF)
                    else -> Color.Transparent
                },
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(item.name, style = Look.style(15.sp, if (focused) Color(0xFF111111) else if (isCurrentInside) Look.Gold else Look.Ink, FontWeight.Medium).copy(shadow = null),
            modifier = Modifier.weight(1f), maxLines = 1)
        if (item.value != null) Text(item.value, maxLines = 1,
            style = Look.style(12.sp, if (focused) Color(0xFF666666) else if (isCurrentInside) Look.Gold.copy(alpha = 0.8f) else Look.Faint).copy(shadow = null),
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
