package app.tilfazi.tv.feed

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri

/** A source on the TV: HDMI, the tuner, AV. */
class Source(val id: String, val name: String, val kind: Int)

/** The TV's inputs, as Android lists them, and how to switch to one. */
object Inputs {
    fun list(c: Context): List<Source> = runCatching {
        val m = c.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        // the passthrough inputs (HDMI, AV) and the tuner; not the maker's umbrella service with no picture of its own
        m.tvInputList.filter { it.parentId == null }.map { info ->
            val label = info.loadCustomLabel(c)?.toString()?.takeIf { it.isNotBlank() } ?: info.loadLabel(c).toString()
            Source(info.id, label.ifBlank { name(info.type) }, info.type)
        }.filterNot { s -> s.name.contains("TvInput", ignoreCase = true) || s.id.contains("DroidLogicTvInputService") && s.kind != TvInputInfo.TYPE_HDMI }.sortedWith(compareBy({ if (it.kind == TvInputInfo.TYPE_HDMI) 0 else 1 }, { it.name }))
    }.getOrDefault(emptyList())

    /** the TV's own home, when there is one besides tilfazi */
    fun stockHome(c: Context): Intent? =
        c.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .firstOrNull { it.activityInfo.packageName != c.packageName && it.activityInfo.packageName != "com.android.settings" }
            ?.let { Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setClassName(it.activityInfo.packageName, it.activityInfo.name) }

    fun name(kind: Int) = when (kind) {
        TvInputInfo.TYPE_HDMI -> "HDMI"
        TvInputInfo.TYPE_TUNER -> "البث التلفزيوني"
        TvInputInfo.TYPE_COMPOSITE -> "AV"
        TvInputInfo.TYPE_COMPONENT -> "Component"
        TvInputInfo.TYPE_VGA -> "VGA"
        else -> "مصدر"
    }

    /**
     * Switches to the source: its pass-through channel opened by the TV's own viewer. When nothing takes that, the
     * maker's input picker or the system's inputs page.
     */
    fun open(c: Context, s: Source): Boolean {
        // tilfazi's own player first: it needs no other app
        if (runCatching {
                c.startActivity(Intent(c, app.tilfazi.tv.home.InputActivity::class.java)
                    .putExtra(app.tilfazi.tv.home.InputActivity.EXTRA_ID, s.id)
                    .putExtra(app.tilfazi.tv.home.InputActivity.EXTRA_TYPE, s.kind)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
            }.getOrDefault(false)) return true
        return openElsewhere(c, s)
    }

    /**
     * A tuner that receives channels (an aerial, cable, satellite), not a source that only calls itself one: AirPlay,
     * screen mirroring and the like are «tuners» to Android, but open, as HDMI does, at their pass-through channel.
     */
    fun broadcast(s: Source) = s.kind == TvInputInfo.TYPE_TUNER &&
        listOf("dtv", "atv", "dvb", "isdb", "atsc", "tuner", "broadcast", "channel", "livetv").any { s.id.contains(it, true) } &&
        listOf("airplay", "cast", "mirror", "screen", "dlna").none { s.id.contains(it, true) }

    /** the TV's own viewer, when tilfazi's could not play it */
    fun openElsewhere(c: Context, s: Source): Boolean {
        val uri: Uri = if (broadcast(s)) TvContract.buildChannelsUriForInput(s.id) else TvContract.buildChannelUriForPassthroughInput(s.id)
        // every app on this TV that says it plays a channel, and the makers' TV apps we know, each given the source
        val pm = c.packageManager
        val homes = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }.filter { it != c.packageName }
        val viewers = pm.queryIntentActivities(Intent(Intent.ACTION_VIEW, uri), 0).map { it.activityInfo.packageName } + homes +
            listOf("com.droidlogic.tvsource", "com.droidlogic.android.tv", "com.droidlogic.tv", "com.android.tv", "com.google.android.tv",
                "com.mediatek.wwtv.tvcenter", "com.mitv.livetv")
        // each package with the source as a plain VIEW, then each of its activities named: some makers' viewers
        // answer only when one is named
        val installed = viewers.distinct().filter { runCatching { pm.getPackageInfo(it, 0); true }.getOrDefault(false) }
        val named = installed.flatMap { p ->
            runCatching { pm.getPackageInfo(p, android.content.pm.PackageManager.GET_ACTIVITIES).activities?.toList() }.getOrNull().orEmpty()
                .filter { it.exported && (it.name.contains("Tv", true) || it.name.contains("Source", true) || it.name.contains("Input", true)) }
                .map { Intent(Intent.ACTION_VIEW, uri).setClassName(p, it.name) }
        }
        val tries = installed.map { Intent(Intent.ACTION_VIEW, uri).setPackage(it) } + named + listOf(
            Intent(Intent.ACTION_VIEW, uri),
            Intent("android.intent.action.VIEW").setData(uri).setPackage("com.droidlogic.tvsource"),
            Intent("com.android.tv.action.VIEW_INPUTS"),
            Intent("android.settings.INPUT_SETTINGS"),
        )
        for (i in tries) if (runCatching { c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)) return true
        return false
    }
}
