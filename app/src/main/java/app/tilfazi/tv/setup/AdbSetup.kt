package app.tilfazi.tv.setup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.tilfazi.tv.feed.Choices
import app.tilfazi.tv.feed.Clubs
import app.tilfazi.tv.feed.Places

/**
 * Setup from a computer, one or more extras per call:
 *   adb shell am broadcast -a app.tilfazi.tv.CONFIG -n app.tilfazi.tv/.setup.AdbSetup \
 *     --es place taif --es club 136013 --es favourites pkg1,pkg2 --es unsplash KEY --es topic "desert" \
 *     --es digits latin --es name "pkg=الاسم" --es clock 1 --es prayer 2 --es backdrop 0 --es tint 3 --es blur 2
 * Only the adb shell may send it (the receiver asks for DUMP). It answers with the settings now in force.
 */
class AdbSetup : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        i.getStringExtra("place")?.let { key -> if (Places.all.any { it.key == key }) Choices.setPlace(key) }
        i.getStringExtra("club")?.let { id -> Choices.setClub(if (id == "none") null else id) }
        i.getStringExtra("favourites")?.let {
            if (it == "auto") Choices.resetFavourites()   // as on a fresh install
            else Choices.setFavourites(it.split(',').map(String::trim).filter(String::isNotEmpty))
        }
        i.getStringExtra("unsplash")?.let { Choices.setUnsplash(it) }
        i.getStringExtra("topic")?.let { Choices.setTopic(it) }
        i.getStringExtra("digits")?.let { Choices.setLatinDigits(it == "latin") }
        for (part in listOf("clock", "prayer", "game", "sky", "backdrop", "tint", "blur", "photos", "addon1", "addon2", "addon3", "cards", "glow", "scene", "dim", "font", "layout", "motion", "density", "still", "artAuto", "size", "edge")) {
            i.getStringExtra(part)?.toIntOrNull()?.let { Choices.setLook(part, it) }
        }
        i.getStringExtra("onboard")?.let { Choices.setOnboarded(it != "again") }   // «again» shows the first-run screens once more
        i.getStringExtra("name")?.let { pair ->
            val eq = pair.indexOf('=')
            if (eq > 0) Choices.setName(pair.substring(0, eq), pair.substring(eq + 1))
        }
        val p = Choices.picks.value
        val report = "place=${p.place.key} club=${p.clubId ?: "none"}(${Clubs.find(p.clubId)?.name ?: ""}) " +
            "digits=${if (p.latinDigits) "latin" else "hindi"} unsplash=${if (p.unsplashKey.isBlank()) "off" else "on"} " +
            "topic=${p.photoTopic} favourites=${p.favourites?.joinToString(",") ?: "auto"} looks=${p.looks}"
        Log.i("Tilfazi", report)
        resultData = report
    }
}
