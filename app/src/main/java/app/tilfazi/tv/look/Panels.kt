package app.tilfazi.tv.look

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Fixture
import app.tilfazi.tv.feed.Picks
import app.tilfazi.tv.feed.Salah
import app.tilfazi.tv.feed.Salat
import app.tilfazi.tv.feed.Sky
import java.util.Calendar
import kotlin.math.roundToInt

/** each widget's three shapes, as the settings name them */
val PRAYER_STYLES = listOf("خط اليوم", "حلقة", "جدول")
val GAME_STYLES = listOf("الشعارات", "لوحة النتيجة", "العد التنازلي")
val SKY_STYLES = listOf("أيقونة", "كبير", "الساعات القادمة")

@Composable
private fun Block(scale: Float, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp * scale)) { content() }
}

// ================================================================ the prayer

/** where the day stands: its five prayers (tomorrow's after Isha), the next one, and how far along the way to it */
internal class DayState(val line: List<Salat>, val next: Int, val progress: Float)

internal fun dayState(now: Long, picks: Picks): DayState {
    val today = Salah.day(now, picks)
    val line = if (today.any { it.at > now }) today else Salah.day(now + 86_400_000L, picks)
    val k = line.indexOfFirst { it.at > now }.coerceAtLeast(0)
    val from = if (k == 0) Salah.day(line[0].at - 86_400_000L, picks).last().at else line[k - 1].at
    val progress = ((now - from).toFloat() / (line[k].at - from).coerceAtLeast(1)).coerceIn(0f, 1f)
    return DayState(line, k, progress)
}

@Composable
fun PrayerWidget(style: Int, now: Long, picks: Picks, scale: Float) {
    val d = dayState(now, picks)
    when (style) {
        1 -> PrayerRing(d, now, picks, scale)
        2 -> PrayerTable(d, now, picks, scale)
        else -> PrayerLine(d, now, picks, scale)
    }
}

/** the next prayer and its countdown over a gold progress line + the five times below */
@Composable
private fun PrayerLine(d: DayState, now: Long, picks: Picks, scale: Float) = Block(scale) {
    val next = d.line[d.next]
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(next.name, style = Look.style((24 * scale).sp, Look.Ink, big = true), modifier = Modifier.weight(1f))
        Text(countdown(next.at - now, picks.latinDigits), style = Look.style((28 * scale).sp, Look.Gold, big = true))
    }
    Spacer(Modifier.height(8.dp * scale))
    DayLine(d.next, d.progress, scale)
    Spacer(Modifier.height(6.dp * scale))
    Row(Modifier.fillMaxWidth()) {
        d.line.forEachIndexed { i, s ->
            val on = i == d.next
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    s.name,
                    style = Look.style((12 * scale).sp, if (on) Look.Gold else if (i < d.next) Look.Faint else Look.Soft, if (on) FontWeight.Medium else FontWeight.Normal),
                )
                Text(
                    clock12(s.at, picks.place.zone, picks.latinDigits).substringBefore(' '),
                    style = Look.style((13 * scale).sp, if (on) Look.Ink else Look.Faint, big = true),
                )
            }
        }
    }
}

@Composable
internal fun DayLine(next: Int, progress: Float, scale: Float) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pulse = if (Look.still) 1f else rememberInfiniteTransition(label = "pulse")
        .animateFloat(.55f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse").value
    Canvas(Modifier.fillMaxWidth().height(12.dp * scale)) {
        val y = size.height / 2
        fun x(slot: Float): Float {
            val pos = (slot + .5f) / 5f * size.width
            return if (rtl) size.width - pos else pos
        }
        val dot = 3.5.dp.toPx() * scale
        drawLine(Color.White.copy(alpha = .18f), Offset(x(-.5f), y), Offset(x(4.5f), y), 1.5.dp.toPx(), StrokeCap.Round)
        val here = x((next - 1 + progress).coerceAtLeast(-.5f))
        drawLine(Look.Gold.copy(alpha = .85f), Offset(x(-.5f), y), Offset(here, y), 2.dp.toPx(), StrokeCap.Round)
        for (i in 0 until 5) {
            val c = Offset(x(i.toFloat()), y)
            when {
                i < next -> drawCircle(Look.Gold, dot, c)
                i == next -> {
                    drawCircle(Look.Gold.copy(alpha = .28f * pulse), dot * 2.5f, c)
                    drawCircle(Look.Gold, dot * 1.15f, c, style = Stroke(1.5.dp.toPx()))
                }
                else -> drawCircle(Color.White.copy(alpha = .35f), dot * .8f, c)
            }
        }
        drawCircle(Color.White, dot * .7f, Offset(here, y))
    }
}

/** a gold ring that fills on the way to the next prayer, the countdown inside; the prayer and the five times beside it */
@Composable
private fun PrayerRing(d: DayState, now: Long, picks: Picks, scale: Float) = Block(scale) {
    val next = d.line[d.next]
    val after = d.line.getOrNull(d.next + 1)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(104.dp * scale), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(104.dp * scale)) {
                val w = 4.5.dp.toPx() * scale
                val inset = w / 2
                val box = Size(size.width - w, size.height - w)
                drawArc(Color.White.copy(alpha = .12f), 0f, 360f, false, Offset(inset, inset), box, style = Stroke(w))
                drawArc(
                    Brush.sweepGradient(listOf(Look.Gold.copy(alpha = .45f), Look.Gold)), -90f, 360f * d.progress, false,
                    Offset(inset, inset), box, style = Stroke(w, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(countdown(next.at - now, picks.latinDigits), style = Look.style((17 * scale).sp, big = true))
                Text("متبقي", style = Look.style((10 * scale).sp, Look.Soft))
            }
        }
        Spacer(Modifier.width(20.dp * scale))
        Column {
            Text("الصلاة القادمة", style = Look.style((12 * scale).sp, Look.Gold, FontWeight.Medium))
            Text(next.name, style = Look.style((28 * scale).sp, Look.Ink, big = true))
            Text("الأذان " + clock12(next.at, picks.place.zone, picks.latinDigits), style = Look.style((14 * scale).sp, Look.Ink))
            if (after != null) {
                Spacer(Modifier.height(4.dp * scale))
                Text("بعدها ${after.name} " + clock12(after.at, picks.place.zone, picks.latinDigits), style = Look.style((12 * scale).sp, Look.Faint))
            }
        }
    }
}

/** the five times side by side without any box; the next prayer is distinguished by Gold on its name, and its countdown sits centred at the bottom */
@Composable
private fun PrayerTable(d: DayState, now: Long, picks: Picks, scale: Float) = Block(scale) {
    val next = d.line[d.next]
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp * scale)) {
        d.line.forEachIndexed { i, s ->
            val on = i == d.next
            Column(
                Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp * scale),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    s.name,
                    style = Look.style(
                        (if (on) 14f * scale else 13f * scale).sp,
                        if (on) Look.Gold else if (i < d.next) Look.Faint else Look.Soft,
                        if (on) FontWeight.Bold else FontWeight.Medium,
                    ),
                )
                Spacer(Modifier.height(5.dp * scale))
                val t = clock12(s.at, picks.place.zone, picks.latinDigits)
                Text(t.substringBefore(' '), style = Look.style((20 * scale).sp, if (i < d.next && !on) Look.Faint else Look.Ink, big = true))
                Text(t.substringAfter(' '), style = Look.style((10 * scale).sp, if (on) Look.Gold else Look.Faint))
            }
        }
    }
    Spacer(Modifier.height(10.dp * scale))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("متبقي على ${next.name}", style = Look.style((12 * scale).sp, Look.Soft, FontWeight.Medium))
        Spacer(Modifier.width(8.dp * scale))
        Text(countdown(next.at - now, picks.latinDigits), style = Look.style((15 * scale).sp, Look.Gold, big = true))
    }
}

// ================================================================ the game

@Composable
fun GameWidget(
    style: Int,
    game: Fixture,
    now: Long,
    picks: Picks,
    scale: Float,
    gamesCount: Int = 1,
    gameIndex: Int = 0,
) = when (style) {
    1 -> GameBoard(game, now, picks, scale, gamesCount, gameIndex)
    2 -> GameCountdown(game, now, picks, scale, gamesCount, gameIndex)
    else -> GameCrests(game, now, picks, scale, gamesCount, gameIndex)
}

private fun meta(game: Fixture, picks: Picks): String {
    val zone = picks.place.zone
    return game.competition + "  ·  " + weekday(game.kickoff, zone) + " " + gregorianDate(game.kickoff, zone, picks.latinDigits)
}

/** the crest's own colour: the average of its strongly coloured pixels, or none for a grey or white crest */
private fun tone(bmp: Bitmap): Color? {
    val small = Bitmap.createScaledBitmap(bmp, 24, 24, true)
    var r = 0L; var g = 0L; var b = 0L; var n = 0
    val hsv = FloatArray(3)
    for (yy in 0 until 24) for (xx in 0 until 24) {
        val p = small.getPixel(xx, yy)
        if (android.graphics.Color.alpha(p) < 200) continue
        android.graphics.Color.colorToHSV(p, hsv)
        if (hsv[1] < .35f || hsv[2] < .25f) continue
        r += android.graphics.Color.red(p); g += android.graphics.Color.green(p); b += android.graphics.Color.blue(p); n++
    }
    return if (n < 12) null else Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
}

/** a crest, with a halo of its own colour behind it when [halo] is set */
@Composable
internal fun Crest(url: String?, size: Dp, halo: Dp? = null) {
    val bmp = rememberBadge(url)
    val glow = remember(bmp) { bmp?.let(::tone) }
    Box(Modifier.size(halo ?: size), contentAlignment = Alignment.Center) {
        if (halo != null && glow != null) {
            Box(Modifier.size(halo).background(Brush.radialGradient(listOf(glow.copy(alpha = .40f), Color.Transparent))))
        }
        if (bmp != null) Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(size))
    }
}

internal fun isGameLive(game: Fixture, now: Long): Boolean =
    game.isLive || (!game.isDone && game.kickoff <= now)

internal fun liveScoreText(game: Fixture, picks: Picks): String? =
    if (game.homeScore != null && game.awayScore != null)
        Look.ltr("${Look.digits("${game.homeScore}", picks.latinDigits)} - ${Look.digits("${game.awayScore}", picks.latinDigits)}")
    else null

internal fun liveClockLabel(game: Fixture, picks: Picks): String {
    val c = game.clock.takeIf { it.isNotBlank() && it != "مباشر" }
    return if (c != null) "● مباشر · ${Look.digits(c, picks.latinDigits)}" else "● مباشر الآن"
}

/** Modern stadium card with ambient glow crests, top competition badge and live pill */
@Composable
private fun GameCrests(
    game: Fixture,
    now: Long,
    picks: Picks,
    scale: Float,
    gamesCount: Int = 1,
    gameIndex: Int = 0,
) = Block(scale) {
    val live = isGameLive(game, now)
    val score = if (live) liveScoreText(game, picks) else null
    val zone = picks.place.zone

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .background(Color(0x24D4AF37), RoundedCornerShape(999.dp))
                .border(0.5.dp, Look.Gold.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp * scale, vertical = 2.dp * scale)
        ) {
            Text(
                game.competition,
                style = Look.style((11 * scale).sp, Look.Gold, FontWeight.Medium),
                maxLines = 1,
            )
        }
        if (live) {
            Box(
                Modifier
                    .background(Color(0x33E53935), RoundedCornerShape(999.dp))
                    .border(0.5.dp, Look.Live.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp * scale, vertical = 2.dp * scale)
            ) {
                Text(
                    liveClockLabel(game, picks),
                    style = Look.style((11 * scale).sp, Look.Live, FontWeight.Bold),
                    maxLines = 1,
                )
            }
        } else {
            val kickoffDay = weekday(game.kickoff, zone)
            val kickoffDate = gregorianDate(game.kickoff, zone, picks.latinDigits)
            Text(
                "$kickoffDay $kickoffDate",
                style = Look.style((11 * scale).sp, Look.Soft, FontWeight.Normal),
                maxLines = 1,
            )
        }
    }

    Spacer(Modifier.height(8.dp * scale))

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Crest(game.homeBadge, 48.dp * scale, 68.dp * scale)
            Spacer(Modifier.height(4.dp * scale))
            Text(
                game.home,
                style = Look.style((13.5f * scale).sp, Look.Ink, FontWeight.SemiBold),
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
        Column(
            Modifier.padding(horizontal = 4.dp * scale),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (live) (score ?: "مباشر") else clock12(game.kickoff, zone, picks.latinDigits),
                style = Look.style(
                    ((if (live && score != null) 28 else 24) * scale).sp,
                    if (live && score == null) Look.Live else Look.Ink,
                    big = true,
                ),
            )
            Spacer(Modifier.height(2.dp * scale))
            Text(
                if (live) game.statusArabic
                else until(game.kickoff - now, picks.latinDigits),
                style = Look.style(
                    (11.5f * scale).sp,
                    if (live) Look.Live else Look.Gold,
                    FontWeight.Medium,
                ),
            )
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Crest(game.awayBadge, 48.dp * scale, 68.dp * scale)
            Spacer(Modifier.height(4.dp * scale))
            Text(
                game.away,
                style = Look.style((13.5f * scale).sp, Look.Ink, FontWeight.SemiBold),
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (gamesCount > 1) {
        Spacer(Modifier.height(6.dp * scale))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(gamesCount.coerceAtMost(8)) { idx ->
                val active = idx == gameIndex
                Box(
                    Modifier
                        .padding(horizontal = 2.5.dp)
                        .size(if (active) 5.dp * scale else 3.5.dp * scale)
                        .background(if (active) Look.Gold else Look.Gold.copy(alpha = 0.25f), CircleShape)
                )
            }
        }
    }
}

/** Broadcast scoreboard bar: crest and name on each side, score or time in gold badge */
@Composable
private fun GameBoard(
    game: Fixture,
    now: Long,
    picks: Picks,
    scale: Float,
    gamesCount: Int = 1,
    gameIndex: Int = 0,
) = Block(scale) {
    val live = isGameLive(game, now)
    val score = if (live) liveScoreText(game, picks) else null
    val zone = picks.place.zone

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .background(Color(0x24D4AF37), RoundedCornerShape(999.dp))
                .border(0.5.dp, Look.Gold.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp * scale, vertical = 2.dp * scale)
        ) {
            Text(game.competition, style = Look.style((11 * scale).sp, Look.Gold, FontWeight.Medium), maxLines = 1)
        }
        Text(
            if (live) liveClockLabel(game, picks) else until(game.kickoff - now, picks.latinDigits),
            style = Look.style((11 * scale).sp, if (live) Look.Live else Look.Soft, if (live) FontWeight.Bold else FontWeight.Normal),
        )
    }

    Spacer(Modifier.height(8.dp * scale))

    val bar = RoundedCornerShape(14.dp * scale)
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0x540A0C12), Color(0x380A0C12))), bar)
            .border(1.dp, Look.Gold.copy(alpha = 0.26f), bar)
            .padding(8.dp * scale),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).padding(horizontal = 6.dp * scale), verticalAlignment = Alignment.CenterVertically) {
            Crest(game.homeBadge, 34.dp * scale)
            Spacer(Modifier.width(8.dp * scale))
            Text(game.home, style = Look.style((15 * scale).sp, Look.Ink, FontWeight.SemiBold), maxLines = 1)
        }
        Box(
            Modifier
                .background(if (live) Look.Live else Look.Gold, RoundedCornerShape(10.dp * scale))
                .padding(horizontal = 14.dp * scale, vertical = 6.dp * scale)
        ) {
            Text(
                if (live) (score ?: "مباشر") else clock12(game.kickoff, zone, picks.latinDigits),
                style = Look.style((15 * scale).sp, if (live) Color.White else Color(0xFF15120C), big = true).copy(shadow = null),
            )
        }
        Row(Modifier.weight(1f).padding(horizontal = 6.dp * scale), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            Text(game.away, style = Look.style((15 * scale).sp, Look.Ink, FontWeight.SemiBold), maxLines = 1, modifier = Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(8.dp * scale))
            Crest(game.awayBadge, 34.dp * scale)
        }
    }

    if (gamesCount > 1) {
        Spacer(Modifier.height(6.dp * scale))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(gamesCount.coerceAtMost(8)) { idx ->
                val active = idx == gameIndex
                Box(
                    Modifier
                        .padding(horizontal = 2.5.dp)
                        .size(if (active) 5.dp * scale else 3.5.dp * scale)
                        .background(if (active) Look.Gold else Look.Gold.copy(alpha = 0.25f), CircleShape)
                )
            }
        }
    }
}

/** Hero countdown card with overlapping crests and prominent countdown digits */
@Composable
private fun GameCountdown(
    game: Fixture,
    now: Long,
    picks: Picks,
    scale: Float,
    gamesCount: Int = 1,
    gameIndex: Int = 0,
) = Block(scale) {
    val left = game.kickoff - now
    val live = isGameLive(game, now)
    val score = if (live) liveScoreText(game, picks) else null
    val days = left / 86_400_000L
    val zone = picks.place.zone

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .background(Color(0x24D4AF37), RoundedCornerShape(999.dp))
                .border(0.5.dp, Look.Gold.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp * scale, vertical = 2.dp * scale)
        ) {
            Text(game.competition, style = Look.style((11 * scale).sp, Look.Gold, FontWeight.Medium), maxLines = 1)
        }
        if (live) {
            Text(liveClockLabel(game, picks), style = Look.style((11 * scale).sp, Look.Live, FontWeight.Bold))
        } else {
            Text(clock12(game.kickoff, zone, picks.latinDigits), style = Look.style((11.5f * scale).sp, Look.Soft))
        }
    }

    Spacer(Modifier.height(6.dp * scale))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(88.dp * scale)) {
            Crest(game.homeBadge, 46.dp * scale, 58.dp * scale)
            Box(Modifier.offset(x = 34.dp * scale, y = 6.dp * scale)) {
                Crest(game.awayBadge, 46.dp * scale, 58.dp * scale)
            }
        }
        Spacer(Modifier.width(14.dp * scale))
        Column(Modifier.weight(1f)) {
            Text("${game.home} × ${game.away}", style = Look.style((15 * scale).sp, Look.Ink, FontWeight.SemiBold), maxLines = 1)
            Spacer(Modifier.height(2.dp * scale))
            Text(
                if (live) game.statusArabic
                else until(game.kickoff - now, picks.latinDigits),
                style = Look.style((11.5f * scale).sp, if (live) Look.Live else Look.Gold, FontWeight.Medium),
                maxLines = 1,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            val big = when {
                live -> score ?: "مباشر"
                days >= 1 -> Look.digits(days.toString(), picks.latinDigits)
                else -> countdown(left, picks.latinDigits)
            }
            Text(big, style = Look.style(((if (days >= 1 && !live) 36 else 24) * scale).sp, if (live) Look.Live else Look.Ink, big = true))
            Text(
                when {
                    live -> "النتيجة"
                    days >= 2 -> "أيام"
                    days == 1L -> "يوم"
                    else -> "على البداية"
                },
                style = Look.style((11 * scale).sp, if (live) Look.Live else Look.Soft),
            )
        }
    }

    if (gamesCount > 1) {
        Spacer(Modifier.height(6.dp * scale))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            repeat(gamesCount.coerceAtMost(8)) { idx ->
                val active = idx == gameIndex
                Box(
                    Modifier
                        .padding(horizontal = 2.5.dp)
                        .size(if (active) 5.dp * scale else 3.5.dp * scale)
                        .background(if (active) Look.Gold else Look.Gold.copy(alpha = 0.25f), CircleShape)
                )
            }
        }
    }
}

// ================================================================ the weather

@Composable
fun SkyWidget(style: Int, sky: Sky, picks: Picks, scale: Float) = when (style) {
    1 -> SkyBig(sky, picks, scale)
    2 -> SkyHours(sky, picks, scale)
    else -> SkyIcon(sky, picks, scale)
}

internal fun deg(t: Double, picks: Picks) = Look.ltr(Look.digits("${t.roundToInt()}°", picks.latinDigits))

internal fun skyWord(code: Int) = when (code) {
    0 -> "صافٍ"
    1 -> "صافٍ غالبًا"
    2 -> "غائم جزئيًا"
    3 -> "غائم"
    45, 48 -> "ضباب"
    51, 53, 55, 56, 57 -> "رذاذ"
    61, 63, 80, 81 -> "أمطار"
    65, 82 -> "أمطار غزيرة"
    66, 67 -> "مطر متجمد"
    71, 73, 75, 77, 85, 86 -> "ثلوج"
    95 -> "عواصف رعدية"
    96, 99 -> "عواصف وبرد"
    else -> ""
}

/** the drawing and the temperature; the city and the day's range at the other end */
@Composable
private fun SkyIcon(sky: Sky, picks: Picks, scale: Float) = Block(scale) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SkyGlyph(sky.code, sky.day, 46.dp * scale)
        Spacer(Modifier.width(14.dp * scale))
        Text(deg(sky.temp, picks), style = Look.style((36 * scale).sp, big = true))
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(picks.place.name, style = Look.style((14 * scale).sp, Look.Gold, FontWeight.Medium))
            Spacer(Modifier.height(2.dp))
            Text(Look.ltr("↑ ${deg(sky.high, picks)}   ↓ ${deg(sky.low, picks)}"), style = Look.style((14 * scale).sp, Look.Soft))
        }
    }
}

/** the temperature large with the sky's word beside it, and a slim bar for the day's range */
@Composable
private fun SkyBig(sky: Sky, picks: Picks, scale: Float) = Block(scale) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(deg(sky.temp, picks), style = Look.style((46 * scale).sp, big = true).copy(lineHeight = (48 * scale).sp))
            Text(
                listOf(skyWord(sky.code), picks.place.name).filter { it.isNotEmpty() }.joinToString(" في "),
                style = Look.style((14.5f * scale).sp, Look.Ink, FontWeight.Medium),
            )
        }
        SkyGlyph(sky.code, sky.day, 56.dp * scale)
    }
    Spacer(Modifier.height(8.dp * scale))
    val span = (sky.high - sky.low).coerceAtLeast(1.0)
    val at = ((sky.temp - sky.low) / span).toFloat().coerceIn(0f, 1f)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(deg(sky.low, picks), style = Look.style((12 * scale).sp, Look.Soft))
        Spacer(Modifier.width(8.dp * scale))
        Canvas(Modifier.weight(1f).height(8.dp * scale)) {
            val y = size.height / 2
            val start = if (rtl) size.width else 0f
            val end = size.width - start
            drawLine(
                Brush.linearGradient(listOf(Color(0xFF8EC5FF), Color(0xFFFFC857)), Offset(start, y), Offset(end, y)),
                Offset(start, y), Offset(end, y), 3.dp.toPx(), StrokeCap.Round,
            )
            drawCircle(Color.White, 4.5.dp.toPx(), Offset(start + (end - start) * at, y))
        }
        Spacer(Modifier.width(8.dp * scale))
        Text(deg(sky.high, picks), style = Look.style((12 * scale).sp, Look.Soft))
    }
}

/** now, then the next five hours floating cleanly side by side */
@Composable
private fun SkyHours(sky: Sky, picks: Picks, scale: Float) = Block(scale) {
    val soon = sky.hours.filter { it.at > System.currentTimeMillis() }.take(5)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SkyGlyph(sky.code, sky.day, 38.dp * scale)
            Text(deg(sky.temp, picks), style = Look.style((24 * scale).sp, big = true))
            Text("الآن", style = Look.style((11 * scale).sp, Look.Gold, FontWeight.Medium))
        }
        Spacer(Modifier.width(12.dp * scale))
        Box(Modifier.width(1.dp / 2).height(64.dp * scale).background(Look.Gold.copy(alpha = .4f)))
        Spacer(Modifier.width(6.dp * scale))
        if (soon.isEmpty()) {
            Text(picks.place.name, style = Look.style((14 * scale).sp, Look.Gold))
        } else soon.forEach { h ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                SkyGlyph(h.code, h.day, 24.dp * scale)
                Spacer(Modifier.height(4.dp * scale))
                Text(deg(h.temp, picks), style = Look.style((14 * scale).sp, Look.Ink, FontWeight.Medium))
                val hour = cityTime(h.at, picks.place.zone).get(Calendar.HOUR_OF_DAY)
                Text(
                    Look.digits("${if (hour % 12 == 0) 12 else hour % 12}", picks.latinDigits) + if (hour < 12) " ص" else " م",
                    style = Look.style((10 * scale).sp, Look.Faint),
                )
            }
        }
    }
}
