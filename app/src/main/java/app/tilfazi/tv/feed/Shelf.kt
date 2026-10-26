package app.tilfazi.tv.feed

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** An app the home screen can open: its banner when it has one (16:9), else its icon, already drawn to a bitmap. */
data class Title(val pkg: String, val label: String, val picture: ImageBitmap, val wide: Boolean, val open: Intent)

/** Lists the TV's apps, with Arabic names for the common ones. */
object Shelf {
    private val arabic = mapOf(
        "com.google.android.youtube.tv" to "يوتيوب",
        "com.netflix.ninja" to "نتفليكس",
        "net.mbc.shahidTV" to "شاهد",
        "com.disney.disneyplus" to "ديزني +",
        "com.amazon.amazonvideo.livingroom" to "برايم فيديو",
        "com.apple.atve.androidtv.appletv" to "آبل تي في",
        "com.osn.go" to "OSN+",
        "com.intigral.jawwytv" to "stc tv",
        "com.thmanyah.shasha.tv" to "ثمانية",
        "com.anghami" to "أنغامي",
        "com.spotify.tv.android" to "سبوتيفاي",
        "com.android.vending" to "متجر Play",
        "com.google.android.youtube.tvmusic" to "يوتيوب ميوزك",
        "com.google.android.youtube.tvkids" to "يوتيوب كيدز",
        "com.android.tv.settings" to "الإعدادات",
        "com.dazn" to "دازن",
        "tv.twitch.android.app" to "تويتش",
    )

    /**
     * The last list, kept for the life of the app: the home screen shows it at once when it comes back, and asks
     * Android again only when apps were installed, removed or updated since ([changed]).
     */
    @Volatile var last: List<Title> = emptyList()
        private set
    private var lastNames: Map<String, String>? = null
    private var lastSeq = -1

    /** whether apps changed since the last list (Android counts every install, removal and update) */
    fun changed(c: Context): Boolean = c.packageManager.getChangedPackages(lastSeq.coerceAtLeast(0)).let { it != null || lastSeq < 0 }

    /**
     * After a restart, the last list as it was written to disk: the names, how each opens, and the art already kept in
     * the cache. It shows at once while Android is asked for the real list.
     */
    fun fromDisk(c: Context, only: Collection<String>? = null): List<Title> = runCatching {
        val a = org.json.JSONArray(java.io.File(c.filesDir, "shelf.json").readText())
        (0 until a.length()).mapNotNull { i ->
            val o = a.getJSONObject(i)
            if (only != null && o.getString("pkg") !in only) return@mapNotNull null
            val bmp = BitmapFactory.decodeFile(o.getString("art")) ?: return@mapNotNull null
            val open = Intent(Intent.ACTION_MAIN).addCategory(o.getString("category"))
                .setClassName(o.getString("pkg"), o.getString("activity"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            Title(o.getString("pkg"), o.getString("label"), bmp.asImageBitmap(), o.getBoolean("wide"), open)
        }
    }.getOrDefault(emptyList())

    private fun remember(c: Context, list: List<Title>) = runCatching {
        val a = org.json.JSONArray()
        list.forEach { t ->
            val art = java.io.File(c.cacheDir, "art").listFiles { f -> f.name.startsWith(t.pkg + "-") }?.maxByOrNull { it.lastModified() } ?: return@forEach
            a.put(org.json.JSONObject().put("pkg", t.pkg).put("label", t.label).put("wide", t.wide).put("art", art.path)
                .put("activity", t.open.component?.className).put("category", t.open.categories?.firstOrNull() ?: Intent.CATEGORY_LAUNCHER))
        }
        java.io.File(c.filesDir, "shelf.json").writeText(a.toString())
    }

    /** the list, from memory when nothing changed */
    fun cached(c: Context, given: Map<String, String>): List<Title> =
        if (last.isNotEmpty() && given == lastNames && !changed(c)) last else load(c, given)

    fun load(c: Context, given: Map<String, String>): List<Title> {
        val pm = c.packageManager
        val seq = pm.getChangedPackages(0)?.sequenceNumber ?: 0
        val seen = LinkedHashMap<String, Title>()
        // On a TV only the apps made for it, as the TV's own home lists them; elsewhere every launchable app
        val tv = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val categories = if (tv) listOf(Intent.CATEGORY_LEANBACK_LAUNCHER) else listOf(Intent.CATEGORY_LAUNCHER)
        for (category in categories) {
            val query = Intent(Intent.ACTION_MAIN).addCategory(category)
            for (r in pm.queryIntentActivities(query, PackageManager.MATCH_ALL)) {
                val info = r.activityInfo
                if (info.packageName == c.packageName || info.packageName in seen) continue
                // the art drawn once and kept on disk by package and version: no drawable to decode next time
                val stamp = runCatching { pm.getPackageInfo(info.packageName, 0).lastUpdateTime }.getOrDefault(0L)
                val file = java.io.File(java.io.File(c.cacheDir, "art").apply { mkdirs() }, "${info.packageName}-$stamp.png")
                val keptArt = if (file.isFile) BitmapFactory.decodeFile(file.path) else null
                val banner = if (keptArt != null) null else info.loadBanner(pm) ?: info.applicationInfo.loadBanner(pm)
                val wide = if (keptArt != null) keptArt.width > keptArt.height else banner != null
                val open = Intent(Intent.ACTION_MAIN).addCategory(category)
                    .setClassName(info.packageName, info.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                val own = r.loadLabel(pm).toString()
                val label = given[info.packageName] ?: arabic[info.packageName] ?: own
                val bmp = keptArt ?: (banner ?: r.loadIcon(pm)).toBitmap(if (wide) 320 else 144, if (wide) 180 else 144).also { b ->
                    runCatching { file.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) } }
                }
                seen[info.packageName] = Title(info.packageName, label, bmp.asImageBitmap(), wide, open)
            }
        }
        return seen.values.sortedBy { it.label }.also { last = it; lastNames = given; lastSeq = seq; remember(c, it) }
    }

    /** the favourites row holds at most this many */
    const val MAX_FAVOURITES = 6

    /** the grid of all apps: those the owner has placed, in their order, then the rest alphabetically */
    fun ordered(all: List<Title>, order: List<String>): List<Title> {
        val byPkg = all.associateBy { it.pkg }
        val placed = order.mapNotNull { byPkg[it] }
        return placed + all.filter { it.pkg !in order }
    }

    /**
     * The favourites in their order. Before the owner has picked any (a fresh install), the first six apps in
     * alphabetical order, those with a TV banner first so the row looks full, then any others to make up six.
     */
    fun favourites(all: List<Title>, picked: List<String>?): List<Title> {
        if (picked == null) return (all.filter { it.wide } + all.filterNot { it.wide }).take(MAX_FAVOURITES)
        val byPkg = all.associateBy { it.pkg }
        return picked.mapNotNull { byPkg[it] }.take(MAX_FAVOURITES)
    }
}

/** the drawable at the size a card shows it (never enlarged) */
private fun Drawable.toBitmap(width: Int, height: Int): Bitmap {
    val w = if (intrinsicWidth > 0) minOf(intrinsicWidth, width) else width
    val h = if (intrinsicHeight > 0) (w.toFloat() * intrinsicHeight / intrinsicWidth).toInt().coerceAtLeast(1) else height
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    setBounds(0, 0, w, h)
    draw(Canvas(bmp))
    return bmp
}
