package app.tilfazi.tv.look

import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.tilfazi.tv.feed.ADDON_GAME
import app.tilfazi.tv.feed.ADDON_LEAGUE
import app.tilfazi.tv.feed.ADDON_VERSE
import app.tilfazi.tv.feed.ADDON_HADITH
import app.tilfazi.tv.feed.ADDON_NONE
import app.tilfazi.tv.feed.placesOf
import app.tilfazi.tv.feed.ADDON_PRAYER
import app.tilfazi.tv.feed.ADDON_SKY
import app.tilfazi.tv.feed.Fixture
import app.tilfazi.tv.feed.Picks
import app.tilfazi.tv.feed.Sky
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * The two columns that both the home screen and the screensaver show, centred on one line: the clock at the right,
 * and at the left the three floating panels (prayer, game, weather) with gold hairlines between them, each drawn in
 * the shape picked in the settings. [scale] grows everything for the screensaver, which has the whole screen.
 */
@Composable
fun Stage(board: Board, picks: Picks, modifier: Modifier = Modifier, scale: Float = 1f, panelWidth: Dp = 440.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        // the first child of a Row is at the right in Arabic
        ShrinkToFit { ClockFace(picks.looks.clock, board.now, picks, scale) }
        ShrinkToFit(Modifier.width(panelWidth * scale)) {
            Column(Modifier.width(panelWidth * scale)) {
                // the three add-on places in order: on glass cards with room between them, or floating with a
                // hairline between those that have something to show
                // the layout decides how many places there are and which are large
                val cards = picks.looks.cards == 1
                var first = true
                placesOf(picks.looks.layout).forEachIndexed { i, large ->
                    val kind = picks.looks.addons.getOrElse(i) { ADDON_NONE }
                    if (!hasAddon(kind, board, large)) return@forEachIndexed
                    if (!first) { if (cards) Spacer(Modifier.height(12.dp * scale)) else Hairline() }
                    first = false
                    if (cards) GlassCard(scale) { Addon(kind, board, picks, scale, large = large) } else Addon(kind, board, picks, scale, large = large)
                }
            }
        }
    }
}

/**
 * Lays its content out at its natural height and, when that is taller than the room it has, draws it smaller (from
 * the middle) so that every combination of shapes fits without being cut off.
 */
@Composable
fun ShrinkToFit(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val p = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val room = constraints.maxHeight
        val s = if (room != Constraints.Infinity && p.height > room) room.toFloat() / p.height else 1f
        val h = if (s < 1f) room else p.height
        layout(p.width, h) {
            p.placeWithLayer(0, (h - p.height) / 2) {
                scaleX = s; scaleY = s
                transformOrigin = TransformOrigin(.5f, .5f)
            }
        }
    }
}

/** whether an add-on has something to show now: off has nothing, a game only when one is coming, the weather only when fresh */
fun hasAddon(kind: Int, board: Board, large: Boolean = false) = when (kind) {
    ADDON_PRAYER -> true
    ADDON_GAME -> nextGame(board) != null || (large && board.results.isNotEmpty())
    ADDON_SKY -> freshSky(board) != null
    ADDON_LEAGUE -> board.table?.rows?.isNotEmpty() == true
    ADDON_VERSE, ADDON_HADITH -> true
    else -> false
}

/** one add-on: small in the shape picked for it, or large */
@Composable
fun Addon(kind: Int, board: Board, picks: Picks, scale: Float, game: Fixture? = nextGame(board), sky: Sky? = freshSky(board), large: Boolean = false) {
    if (large) when (kind) {
        ADDON_PRAYER -> BigPrayer(board.now, picks, scale)
        ADDON_GAME -> BigGames(game, board, picks, scale)
        ADDON_SKY -> sky?.let { BigSky(it, picks, scale) }
        ADDON_LEAGUE -> board.table?.let { BigLeague(it, picks, scale) }
        ADDON_VERSE -> VerseWidget(board.now, picks, scale, large = true)
        ADDON_HADITH -> HadithWidget(board.now, picks, scale, large = true)
    } else when (kind) {
        ADDON_PRAYER -> PrayerWidget(picks.looks.prayer, board.now, picks, scale)
        ADDON_GAME -> game?.let { GameWidget(picks.looks.game, it, board.now, picks, scale) }
        ADDON_SKY -> sky?.let { SkyWidget(picks.looks.sky, it, picks, scale) }
        ADDON_LEAGUE -> board.table?.let { SmallLeague(it, picks, scale) }
        ADDON_VERSE -> VerseWidget(board.now, picks, scale, large = false)
        ADDON_HADITH -> HadithWidget(board.now, picks, scale, large = false)
    }
}

fun nextGame(board: Board): Fixture? = board.fixtures.firstOrNull { it.kickoff + 2 * 3_600_000L > board.now }
fun freshSky(board: Board): Sky? = board.sky?.takeIf { board.now - it.at < 3 * 3_600_000L }

/**
 * A frosted card behind one add-on: a pale wash that is a little brighter at the top, a hairline edge that catches
 * the light along its top, and soft round corners.
 */
@Composable
fun GlassCard(scale: Float, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp * scale)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x24FFFFFF), Color(0x10FFFFFF))))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x0DFFFFFF))), shape)
            .padding(horizontal = 16.dp * scale, vertical = 2.dp * scale),
    ) { content() }
}

/** a 1-pixel gold line that fades out at both ends */
@Composable
fun Hairline(modifier: Modifier = Modifier.fillMaxWidth()) {
    Box(
        modifier
            .height(1.dp / 2)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Look.Gold.copy(alpha = .45f), Look.Gold.copy(alpha = .45f), Color.Transparent)))
    )
}

// ---------------------------------------------------------------- dates and times in the city

internal val DAYS = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
internal val MONTHS = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

internal fun cityTime(millis: Long, zone: String): Calendar =
    Calendar.getInstance(TimeZone.getTimeZone(zone), Locale.US).apply { timeInMillis = millis }

/** two digits, always 0-9: String.format would follow the TV's language and give ٠٧ even when 123 is picked */
internal fun two(n: Number) = n.toString().padStart(2, '0')

/** «٥:٠١ ص» */
fun clock12(millis: Long, zone: String, latin: Boolean): String {
    val k = cityTime(millis, zone)
    val h = k.get(Calendar.HOUR_OF_DAY)
    val text = "${if (h % 12 == 0) 12 else h % 12}:${two(k.get(Calendar.MINUTE))}"
    return Look.digits(text, latin) + if (h < 12) " ص" else " م"
}

/** «١٣ جمادى الأولى ١٤٤٨ هـ», Umm al-Qura */
internal fun hijriDate(millis: Long, zone: String, latin: Boolean, year: Boolean = true): String = runCatching {
    val f = SimpleDateFormat(if (year) "d MMMM y" else "d MMMM", ULocale("ar_SA@calendar=islamic-umalqura;numbers=latn"))
    f.timeZone = android.icu.util.TimeZone.getTimeZone(zone)
    val s = f.format(java.util.Date(millis)).replace(Regex("\\s*هـ\\s*$"), "")
    Look.digits(if (year) "$s هـ" else s, latin)
}.getOrDefault("")

/** «٢٤ أكتوبر» */
internal fun gregorianDate(millis: Long, zone: String, latin: Boolean, year: Boolean = false): String {
    val k = cityTime(millis, zone)
    val s = "${k.get(Calendar.DAY_OF_MONTH)} ${MONTHS[k.get(Calendar.MONTH)]}" + if (year) " ${k.get(Calendar.YEAR)}" else ""
    return Look.digits(s, latin)
}

internal fun weekday(millis: Long, zone: String) = DAYS[cityTime(millis, zone).get(Calendar.DAY_OF_WEEK) - 1]

/** «بعد ٣ أيام», «بكرة», «بعد ٢ س و١٠ د» */
internal fun until(ms: Long, latin: Boolean): String {
    val minutes = ms / 60_000
    val hours = minutes / 60
    val days = hours / 24
    fun n(v: Long) = Look.digits(v.toString(), latin)
    return when {
        days >= 2 -> "بعد ${n(days)} " + if (days <= 10) "أيام" else "يومًا"
        days == 1L -> "بكرة"
        hours >= 1 -> "بعد ${n(hours)} س و${n(minutes % 60)} د"
        else -> "بعد ${n(maxOf(1, minutes))} د"
    }
}

/** «٥:٤١:٥٦», or «٤١:٥٦» under an hour */
internal fun countdown(ms: Long, latin: Boolean): String {
    val left = (ms / 1000).coerceAtLeast(0)
    if (Look.still) {   // minutes only: the screen is redrawn once a minute
        val m = (left + 59) / 60
        return Look.ltr(Look.digits("${m / 60}:${two(m % 60)}", latin))
    }
    val s = if (left >= 3600) "${left / 3600}:${two(left / 60 % 60)}:${two(left % 60)}" else "${two(left / 60)}:${two(left % 60)}"
    return Look.ltr(Look.digits(s, latin))
}
