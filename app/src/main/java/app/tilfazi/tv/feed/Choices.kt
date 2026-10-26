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
    /** what the three add-on places show, from the top: [ADDON_NONE], [ADDON_PRAYER], [ADDON_GAME] or [ADDON_SKY] */
    val addons: List<Int> = listOf(ADDON_PRAYER, ADDON_GAME, ADDON_SKY),
    /** the add-ons on frosted glass cards (1) or floating on the backdrop with hairlines between them (0) */
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
    /** how large everything is drawn, see SIZES; 2 is as designed */
    val size: Int = 2,
    /** room left round the edges for TVs that cut them off (overscan), see EDGES */
    val edge: Int = 0,
)

val SIZES = listOf("أصغر", "صغير", "عادي", "كبير", "أكبر")
val SIZE_SCALES = listOf(.84f, .92f, 1f, 1.08f, 1.16f)
val EDGES = listOf("بدون", "صغيرة", "متوسطة", "كبيرة")
val EDGE_PARTS = listOf(0f, .02f, .035f, .05f)

const val ADDON_NONE = 0
const val ADDON_PRAYER = 1
const val ADDON_GAME = 2
const val ADDON_SKY = 3
const val ADDON_LEAGUE = 4
const val ADDON_VERSE = 5
const val ADDON_HADITH = 6

/** how the add-ons share the column: 0 three small, 1 large then small, 2 small then large, 3 one large */
const val LAYOUT_THREE = 0
const val LAYOUT_LARGE_TOP = 1
const val LAYOUT_LARGE_BOTTOM = 2
const val LAYOUT_ONE = 3

/** for each place the layout uses, whether it is large */
fun placesOf(layout: Int): List<Boolean> = when (layout) {
    LAYOUT_LARGE_TOP -> listOf(true, false)
    LAYOUT_LARGE_BOTTOM -> listOf(false, true)
    LAYOUT_ONE -> listOf(true)
    else -> listOf(false, false, false)
}

/** Everything the owner picks, in one snapshot that the screens observe. */
data class Picks(
    val place: Place,
    val clubId: String?,
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
)

object Choices {
    private lateinit var store: SharedPreferences
    private val flow = MutableStateFlow(
        Picks(Places.all.first(), null, false, "", DEFAULT_TOPIC, null, names = emptyMap())
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
        return Picks(
            place = Places.find(store.getString("place", "riyadh")),
            clubId = Clubs.current(store.getString("club", null)?.takeIf { it.isNotBlank() }),
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
                size = store.getInt("look.size", 2),
                edge = store.getInt("look.edge", 0),
                addons = listOf(store.getInt("look.addon1", ADDON_PRAYER), store.getInt("look.addon2", ADDON_GAME), store.getInt("look.addon3", ADDON_SKY)),
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
    fun setClub(id: String?) = edit { putString("club", id ?: "") }
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
