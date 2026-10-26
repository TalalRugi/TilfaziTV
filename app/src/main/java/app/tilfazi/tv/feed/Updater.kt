package app.tilfazi.tv.feed

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * tilfazi's updates, from its releases on GitHub: the latest release's version against this one, and its APK
 * downloaded and handed to Android's installer (which asks the owner to confirm, and refuses an APK not signed as
 * this one is).
 */
object Updater {
    private const val LATEST = "https://api.github.com/repos/TalalRugi/TilfaziTV/releases/latest"

    class Release(val version: String, val apk: String)

    fun current(c: Context): String =
        runCatching { c.packageManager.getPackageInfo(c.packageName, 0).versionName }.getOrNull() ?: "0"

    /** the latest release when it is newer than this build, null when this is the latest; throws when it cannot be read */
    suspend fun newer(c: Context): Release? = run {
        val o = Net.json(LATEST, mapOf("Accept" to "application/vnd.github+json"))
        val version = o.getString("tag_name").removePrefix("v")
        val assets = o.getJSONArray("assets")
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith(".apk") }?.getString("browser_download_url") ?: return null
        Release(version, apk).takeIf { isNewer(version, current(c)) }
    }

    private fun isNewer(a: String, b: String): Boolean {
        val x = a.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d > 0
        }
        return false
    }

    /** downloads the release and starts Android's installer on it; false when the download failed */
    suspend fun install(c: Context, r: Release): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = Net.bytes(r.apk)
            val pi = c.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val id = pi.createSession(params)
            pi.openSession(id).use { s ->
                s.openWrite("tilfazi.apk", 0, bytes.size.toLong()).use { it.write(bytes); s.fsync(it) }
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0)
                val done = PendingIntent.getBroadcast(c, id, Intent(c, Done::class.java), flags)
                s.commit(done.intentSender)
            }
            true
        }.getOrDefault(false)
    }

    /** Android's answer to the install: when it needs the owner's yes, its confirm screen is shown */
    class Done : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            if (i.getIntExtra(PackageInstaller.EXTRA_STATUS, -1) != PackageInstaller.STATUS_PENDING_USER_ACTION) return
            @Suppress("DEPRECATION")
            val confirm = (if (Build.VERSION.SDK_INT >= 33) i.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                else i.getParcelableExtra(Intent.EXTRA_INTENT)) ?: return
            c.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
