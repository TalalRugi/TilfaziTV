package app.tilfazi.tv.feed

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** How each part is drawn: one of three shapes per widget, the backdrop's mode, its colour and its blur. */
data class Looks(
    val clock: Int = 0,
    val prayer: Int = 2,
    val game: Int = 0,
    val sky: Int = 2,
    /** 0: frosted glass over a colour glowing from the top corner; 1: Unsplash photos */
    val backdrop: Int = 0,
    val tint: Int = 0,
    /** the photos' blur, 0 (none) to 4 (very soft); the glass is always at its softest */
    val blur: Int = 2,
    /** where the photos come from: 0 the ones built in, 1 the owner's on the TV, 2 Unsplash */
    val photos: Int = 0,
    /** what the four add-on places show: [ADDON_NONE], [ADDON_PRAYER], [ADDON_GAME], [ADDON_SKY], [ADDON_LEAGUE], [ADDON_VERSE], [ADDON_HADITH] */
    val addons: List<Int> = listOf(ADDON_PRAYER, ADDON_GAME, ADDON_SKY, ADDON_LEAGUE),
    /** 0: floating on the backdrop without card background; 1: bright frosted glass cards; 2: dark translucent cards */
    val cards: Int = 0,
    /** where the glass's colour glows from: 0 right, 1 left, 2 the middle of the screen, 3 the top middle */
    val glow: Int = 3,
    /** the glass backdrop's motion (see MOTIONS, 0 none) and how dense it is, 0–2 */
    val motion: Int = 0,
    val density: Int = 1,
    /** the screensaver's scene (see the ambient package) and how much it is dimmed: 0 none, 1 a little, 2 a lot */
    val scene: Int = 0,
    val dim: Int = 0,
    /** the font set (see Look.FONTS) */
    val font: Int = 0,
    /** see [placesOf] */
    val layout: Int = LAYOUT_THREE,
    /** 1: no animation at all on the home screen, for slower TVs */
    val still: Int = 0,
    /** the screensaver's galleries move on by themselves every ten minutes (1), or only by the remote's arrows (0) */
    val artAuto: Int = 1,
    /** 1: auto-cycle leagues and matches on the home screen; 0: stay still until browsed with remote */
    val autoScroll: Int = 1,
    /** how large everything is drawn, see SIZES; 2 is as designed */
    val size: Int = 2,
    /** room left round the edges for TVs that cut them off (overscan), see EDGES */
    val edge: Int = 0,
    /** top Dynamic Island match tracking: 0 off, 1 on events (toast), 2 always on during live match */
    val overlayMode: Int = 1,
    /** top Dynamic Island prayer alerts: 0 off, 1 at Adhan only, 2 countdown before/after Adhan, 3 continuous countdown */
    val overlayPrayer: Int = 2,
    /** minutes before Adhan to show countdown: 0: 5m, 1: 10m, 2: 15m, 3: 20m, 4: 30m */
    val overlayPre: Int = 1,
    /** minutes after Adhan to show elapsed time: 0: 5m, 1: 10m, 2: 15m, 3: 20m, 4: 25m, 5: 30m */
    val overlayPost: Int = 2,
    /** show overlay on home screen: 0 only over other apps, 1 everywhere including home screen */
    val overlayHome: Int = 0,
    /** legacy position field (the Dynamic Island is always docked to top-centre) */
    val overlayPos: Int = 0,
    /** background opacity of the top Dynamic Island: 0 light (60%), 1 medium (82%), 2 bezel-dark (96%) */
    val overlayAlpha: Int = 2,
)

val SIZES = listOf("أصغر", "صغير", "عادي", "كبير", "أكبر")
val SIZE_SCALES = listOf(.84f, .92f, 1f, 1.08f, 1.16f)
val EDGES = listOf("بدون", "صغيرة", "متوسطة", "كبيرة")
val EDGE_PARTS = listOf(0f, .02f, .035f, .05f)
val OVERLAY_MODES = listOf("مطفأ", "عند الأحداث", "طوال المباراة")
val OVERLAY_PRAYER_MODES = listOf("مطفأ", "عند الأذان فقط", "العد التنازلي قبل وبعد الأذان", "عد تنازلي مستمر وثابت")
val OVERLAY_PRE_MINS = listOf(5, 10, 15, 20, 30)
val OVERLAY_PRE_LABELS = listOf("٥ دقائق", "١٠ دقائق", "١٥ دقيقة", "٢٠ دقيقة", "٣٠ دقيقة")
val OVERLAY_POST_MINS = listOf(5, 10, 15, 20, 25, 30)
val OVERLAY_POST_LABELS = listOf("٥ دقائق", "١٠ دقائق", "١٥ دقيقة", "٢٠ دقيقة", "٢٥ دقيقة", "٣٠ دقيقة")
val OVERLAY_HOME_MODES = listOf("في التطبيقات الأخرى فقط", "في الشاشة الرئيسية والتطبيقات")
val OVERLAY_PLACES = listOf("أعلى المنتصف", "أسفل اليسار", "أسفل اليمين", "أعلى اليسار")
val OVERLAY_ALPHAS = listOf("شفافة", "متوسطة", "داكنة كالإطار")
val OVERLAY_ALPHA_VALS = listOf(0.60f, 0.82f, 0.96f)

const val ADDON_NONE = 0
const val ADDON_PRAYER = 1
const val ADDON_GAME = 2
const val ADDON_SKY = 3
const val ADDON_LEAGUE = 4
const val ADDON_VERSE = 5
const val ADDON_HADITH = 6

/** how the add-ons share the Bento stage: 0 four cards (3 cols), 1 three cards (2 cols), 2 two large cards, 3 one large card */
const val LAYOUT_THREE = 0
const val LAYOUT_LARGE_TOP = 1
const val LAYOUT_LARGE_BOTTOM = 2
const val LAYOUT_ONE = 3

/** for each place the layout uses, whether it is a full-height large card */
fun placesOf(layout: Int): List<Boolean> = when (layout) {
    LAYOUT_LARGE_TOP -> listOf(false, false, true)
    LAYOUT_LARGE_BOTTOM -> listOf(true, true)
    LAYOUT_ONE -> listOf(true)
    else -> listOf(false, false, true, true)
}

/** Everything the owner picks, in one snapshot that the screens observe. */
data class Picks(
    val place: Place,
    val clubId: String?,
    val worldClubId: String? = null,
    val latinDigits: Boolean,
    val unsplashKey: String,
    val photoTopic: String,
    /** the favourites in order; null until the owner has picked any (the TV's first apps stand in), empty when all were taken out */
    val favourites: List<String>?,
    /** the all-apps grid's own order, apart from the favourites; apps not in it follow, alphabetically */
    val order: List<String> = emptyList(),
    val names: Map<String, String>,
    val looks: Looks = Looks(),
    /** the first-run screens are done (or were skipped) */
    val onboarded: Boolean = true,
    /** enabled domestic league slugs for the standings tables */
    val leagues: List<String> = listOf("ksa.1", "eng.1"),
    /** prayer calculation authority (see Salah.METHODS) */
    val prayerMethod: Int = 0,
    /** Asr madhhab: 0 Shafi'i/Hanbali/Maliki (factor 1), 1 Hanafi (factor 2) */
    val prayerMadhhab: Int = 0,
    /** manual minute offsets for Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha */
    val prayerAdjust: List<Int> = listOf(0, 0, 0, 0, 0, 0),
    /** custom selected clubs to follow in the match widget (if empty, defaults to clubId and worldClubId) */
    val gameClubs: List<String> = emptyList(),
) {
    /** all followed clubs to fetch and display matches for */
    val clubIds: List<String> get() = (listOfNotNull(clubId, worldClubId) + gameClubs).distinct()
}

object Choices {
    private lateinit var store: SharedPreferences
    private val flow = MutableStateFlow(
        Picks(Places.all.first(), null, null, false, "", DEFAULT_TOPIC, null, names = emptyMap())
    )
    val picks: StateFlow<Picks> get() = flow

    const val DEFAULT_TOPIC = "saudi arabia landscape"

    fun init(c: Context) {
        store = c.getSharedPreferences("choices", Context.MODE_PRIVATE)
        flow.value = read()
        app.tilfazi.tv.look.Look.still = flow.value.looks.still == 1
    }

    private fun read(): Picks {
        val names = store.all.filterKeys { it.startsWith("name:") }
            .mapNotNull { (k, v) -> (v as? String)?.let { k.removePrefix("name:") to it } }.toMap()
        val clubId = Clubs.current(store.getString("club", null)?.takeIf { it.isNotBlank() })
        val worldClubId = Clubs.current(store.getString("worldClub", null)?.takeIf { it.isNotBlank() })
        val savedLeagues = store.getString("leagues", null)
        val leagues = if (savedLeagues != null) {
            savedLeagues.split(',').filter { it.isNotBlank() }
        } else {
            val fromClubs = listOfNotNull("ksa.1", Clubs.find(worldClubId)?.league).distinct()
            if (fromClubs.size >= 2) fromClubs else (fromClubs + "eng.1").distinct()
        }
        val prayerMethod = store.getInt("prayer.method", 0)
        val prayerMadhhab = store.getInt("prayer.madhhab", 0)
        val prayerAdjustStr = store.getString("prayer.adjust", null)
        val prayerAdjust = if (prayerAdjustStr != null) {
            val parts = prayerAdjustStr.split(',').mapNotNull { it.toIntOrNull() }
            if (parts.size >= 6) parts.take(6) else (parts + List(6 - parts.size) { 0 })
        } else listOf(0, 0, 0, 0, 0, 0)
        val savedGameClubs = store.getString("gameClubs", null)
        val gameClubs = savedGameClubs?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
        return Picks(
            place = Places.find(store.getString("place", "riyadh")),
            clubId = clubId,
            worldClubId = worldClubId,
            gameClubs = gameClubs,
            leagues = if (leagues.isEmpty()) listOf("ksa.1") else leagues,
            prayerMethod = prayerMethod,
            prayerMadhhab = prayerMadhhab,
            prayerAdjust = prayerAdjust,
            latinDigits = store.getBoolean("latin", false),
            unsplashKey = store.getString("unsplash", "") ?: "",
            photoTopic = store.getString("topic", DEFAULT_TOPIC) ?: DEFAULT_TOPIC,
            favourites = store.getString("favourites", null)?.split(',')?.filter { it.isNotBlank() },
            order = (store.getString("order", "") ?: "").split(',').filter { it.isNotBlank() },
            names = names,
            onboarded = store.getBoolean("onboarded", false),
            looks = Looks(
                clock = store.getInt("look.clock", 0),
                prayer = store.getInt("look.prayer", 2),
                game = store.getInt("look.game", 0),
                sky = store.getInt("look.sky", 2),
                backdrop = store.getInt("look.backdrop", 0),
                tint = store.getInt("look.tint", 0),
                blur = store.getInt("look.blur", 2),
                photos = store.getInt("look.photos", 0),
                cards = store.getInt("look.cards", 0),
                glow = store.getInt("look.glow", 3),
                motion = store.getInt("look.motion", 0),
                density = store.getInt("look.density", 1),
                scene = store.getInt("look.scene", 0),
                dim = store.getInt("look.dim", 0),
                font = store.getInt("look.font", 0),
                layout = store.getInt("look.layout", LAYOUT_THREE),
                still = store.getInt("look.still", 0),
                artAuto = store.getInt("look.artAuto", 1),
                autoScroll = store.getInt("look.autoScroll", 1),
                size = store.getInt("look.size", 2),
                edge = store.getInt("look.edge", 0),
                overlayMode = store.getInt("look.overlayMode", 1),
                overlayPrayer = store.getInt("look.overlayPrayer", 2),
                overlayPre = store.getInt("look.overlayPre", 1),
                overlayPost = store.getInt("look.overlayPost", 2),
                overlayHome = store.getInt("look.overlayHome", 0),
                overlayPos = store.getInt("look.overlayPos", 0),
                overlayAlpha = store.getInt("look.overlayAlpha", 2),
                addons = listOf(
                    store.getInt("look.addon1", ADDON_PRAYER),
                    store.getInt("look.addon2", ADDON_GAME),
                    store.getInt("look.addon3", ADDON_SKY),
                    store.getInt("look.addon4", ADDON_LEAGUE),
                ),
            ),
        )
    }

    private fun edit(block: SharedPreferences.Editor.() -> Unit) {
        store.edit().apply(block).apply()
        flow.value = read()
        app.tilfazi.tv.look.Look.still = flow.value.looks.still == 1
    }

    fun setOnboarded(done: Boolean) = edit { putBoolean("onboarded", done) }
    fun setPlace(key: String) = edit { putString("place", key) }
    fun setClub(id: String?) = edit {
        val oldClub = flow.value.clubId
        putString("club", id ?: "")
        if (id != null && "ksa.1" !in flow.value.leagues) {
            putString("leagues", (flow.value.leagues + "ksa.1").distinct().joinToString(","))
        }
        if (flow.value.gameClubs.isNotEmpty()) {
            val updated = flow.value.gameClubs.toMutableList()
            if (oldClub != null) updated.remove(oldClub)
            if (id != null && id !in updated) updated.add(0, id)
            putString("gameClubs", updated.distinct().joinToString(","))
        }
    }
    fun setWorldClub(id: String?) = edit {
        val oldClub = flow.value.worldClubId
        putString("worldClub", id ?: "")
        val league = Clubs.find(id)?.league
        if (league != null && league !in flow.value.leagues) {
            putString("leagues", (flow.value.leagues + league).distinct().joinToString(","))
        }
        if (flow.value.gameClubs.isNotEmpty()) {
            val updated = flow.value.gameClubs.toMutableList()
            if (oldClub != null) updated.remove(oldClub)
            if (id != null && id !in updated) updated.add(id)
            putString("gameClubs", updated.distinct().joinToString(","))
        }
    }
    fun setLeagues(slugs: List<String>) = edit {
        putString("leagues", slugs.distinct().joinToString(","))
    }
    fun setGameClubs(clubs: List<String>) = edit {
        putString("gameClubs", clubs.distinct().joinToString(","))
    }
    fun toggleGameClub(id: String) = edit {
        val current = if (flow.value.gameClubs.isNotEmpty()) flow.value.gameClubs else flow.value.clubIds
        val updated = if (id in current) current - id else current + id
        putString("gameClubs", updated.distinct().joinToString(","))
    }
    fun resetGameClubs() = edit {
        remove("gameClubs")
    }
    fun setPrayerMethod(method: Int) = edit { putInt("prayer.method", method) }
    fun setPrayerMadhhab(madhhab: Int) = edit { putInt("prayer.madhhab", madhhab) }
    fun setPrayerAdjust(adjust: List<Int>) = edit { putString("prayer.adjust", adjust.joinToString(",")) }
    fun adjustPrayerMinute(index: Int, delta: Int) = edit {
        val list = flow.value.prayerAdjust.toMutableList()
        while (list.size < 6) list.add(0)
        list[index] = (list[index] + delta).coerceIn(-60, 60)
        putString("prayer.adjust", list.joinToString(","))
    }
    fun resetPrayerAdjust() = edit { remove("prayer.adjust") }
    fun setLatinDigits(on: Boolean) = edit { putBoolean("latin", on) }
    fun setUnsplash(key: String) = edit { putString("unsplash", key.trim()) }
    fun setTopic(topic: String) = edit { putString("topic", topic.trim().ifEmpty { DEFAULT_TOPIC }) }
    fun setOrder(pkgs: List<String>) = edit { putString("order", pkgs.distinct().joinToString(",")) }

    /** back to the fresh-install row: the first six apps */
    fun resetFavourites() = edit { remove("favourites") }
    fun setFavourites(pkgs: List<String>) = edit { putString("favourites", pkgs.distinct().take(Shelf.MAX_FAVOURITES).joinToString(",")) }
    /** one of the look settings: clock, prayer, game, sky, backdrop, tint, blur, photos, addon1, addon2, addon3, cards, glow, scene or dim */
    fun setLook(part: String, value: Int) = edit { putInt("look.$part", value) }
    fun setName(pkg: String, name: String?) = edit {
        if (name.isNullOrBlank()) remove("name:$pkg") else putString("name:$pkg", name.trim())
    }
}
