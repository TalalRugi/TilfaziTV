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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp * scale)) { content() }
}

// ================================================================ the prayer

/** where the day stands: its five prayers (tomorrow's after Isha), the next one, and how far along the way to it */
private class DayState(val line: List<Salat>, val next: Int, val progress: Float)

private fun dayState(now: Long, picks: Picks): DayState {
    val place = picks.place
    val today = Salah.day(now, place)
    val line = if (today.any { it.at > now }) today else Salah.day(now + 86_400_000L, place)
    val k = line.indexOfFirst { it.at > now }.coerceAtLeast(0)
    val from = if (k == 0) Salah.day(line[0].at - 86_400_000L, place).last().at else line[k - 1].at
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

/** the next prayer and its countdown over a gold line through the day's five dots */
@Composable
private fun PrayerLine(d: DayState, now: Long, picks: Picks, scale: Float) = Block(scale) {
    val next = d.line[d.next]
    Row(verticalAlignment = Alignment.Bottom) {
        Text(next.name, style = Look.style((24 * scale).sp, big = true), modifier = Modifier.weight(1f))
        Text(countdown(next.at - now, picks.latinDigits), style = Look.style((30 * scale).sp, big = true))
    }
    Spacer(Modifier.height(10.dp * scale))
    DayLine(d.next, d.progress, scale)
    Spacer(Modifier.height(6.dp * scale))
    Row(Modifier.fillMaxWidth()) {
        d.line.forEachIndexed { i, s ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.name, style = Look.style((12 * scale).sp, if (i == d.next) Look.Gold else if (i < d.next) Look.Soft else Look.Faint,
                    if (i == d.next) FontWeight.Medium else FontWeight.Normal))
                if (i == d.next) Text(clock12(s.at, picks.place.zone, picks.latinDigits), style = Look.style((11 * scale).sp, Look.Soft))
            }
        }
    }
}

@Composable
private fun DayLine(next: Int, progress: Float, scale: Float) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val pulse = if (Look.still) 1f else rememberInfiniteTransition(label = "pulse")
        .animateFloat(.55f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "pulse").value
    Canvas(Modifier.fillMaxWidth().height(14.dp * scale)) {
        val y = size.height / 2
        fun x(slot: Float): Float {   // slot 0 is Fajr's dot; -.5 and 4.5 are the line's ends
            val pos = (slot + .5f) / 5f * size.width
            return if (rtl) size.width - pos else pos
        }
        val dot = 4.dp.toPx() * scale
        drawLine(Color.White.copy(alpha = .18f), Offset(x(-.5f), y), Offset(x(4.5f), y), 1.5.dp.toPx(), StrokeCap.Round)
        val here = x((next - 1 + progress).coerceAtLeast(-.5f))
        drawLine(Look.Gold.copy(alpha = .8f), Offset(x(-.5f), y), Offset(here, y), 1.5.dp.toPx(), StrokeCap.Round)
        for (i in 0 until 5) {
            val c = Offset(x(i.toFloat()), y)
            when {
                i < next -> drawCircle(Look.Gold, dot, c)
                i == next -> {
                    drawCircle(Look.Gold.copy(alpha = .25f * pulse), dot * 2.6f, c)
                    drawCircle(Look.Gold, dot * 1.15f, c, style = Stroke(1.5.dp.toPx()))
                }
                else -> drawCircle(Color.White.copy(alpha = .35f), dot * .8f, c)
            }
        }
        drawCircle(Color.White, dot * .7f, Offset(here, y))
    }
}

/** a gold ring that fills on the way to the next prayer, the countdown inside; the prayer and the one after it beside */
@Composable
private fun PrayerRing(d: DayState, now: Long, picks: Picks, scale: Float) = Block(scale) {
    val next = d.line[d.next]
    val after = d.line.getOrNull(d.next + 1)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(118.dp * scale), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(118.dp * scale)) {
                val w = 5.dp.toPx() * scale
                val inset = w / 2
                val box = Size(size.width - w, size.height - w)
                drawArc(Color.White.copy(alpha = .12f), 0f, 360f, false, Offset(inset, inset), box, style = Stroke(w))
                drawArc(
                    Brush.sweepGradient(listOf(Look.Gold.copy(alpha = .5f), Look.Gold)), -90f, 360f * d.progress, false,
                    Offset(inset, inset), box, style = Stroke(w, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(countdown(next.at - now, picks.latinDigits), style = Look.style((19 * scale).sp, big = true))
                Text("متبقي", style = Look.style((10 * scale).sp, Look.Soft))
            }
        }
        Spacer(Modifier.width(22.dp * scale))
        Column {
            Text("الصلاة القادمة", style = Look.style((12 * scale).sp, Look.Gold, FontWeight.Medium))
            Text(next.name, style = Look.style((32 * scale).sp, big = true))
            Text("الأذان " + clock12(next.at, picks.place.zone, picks.latinDigits), style = Look.style((14 * scale).sp, Look.Ink))
            if (after != null) {
                Spacer(Modifier.height(6.dp * scale))
                Text("بعدها ${after.name} " + clock12(after.at, picks.place.zone, picks.latinDigits), style = Look.style((12 * scale).sp, Look.Faint))
            }
        }
    }
}

/** the five times side by side; the next one sits in a soft gold capsule with its countdown */
@Composable
private fun PrayerTable(d: DayState, now: Long, picks: Picks, scale: Float) = Block(scale) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp * scale)) {
        d.line.forEachIndexed { i, s ->
            val on = i == d.next
            val shape = RoundedCornerShape(16.dp * scale)
            Column(
                Modifier
                    .weight(1f)
                    .then(
                        if (on) Modifier.background(Brush.verticalGradient(listOf(Look.Gold.copy(alpha = .30f), Look.Gold.copy(alpha = .08f))), shape)
                            .border(1.dp, Look.Gold.copy(alpha = .55f), shape) else Modifier
                    )
                    .padding(vertical = 12.dp * scale),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(s.name, style = Look.style((13 * scale).sp, if (on) Look.Gold else if (i < d.next) Look.Faint else Look.Soft, FontWeight.Medium))
                Spacer(Modifier.height(6.dp * scale))
                val t = clock12(s.at, picks.place.zone, picks.latinDigits)
                Text(t.substringBefore(' '), style = Look.style((20 * scale).sp, if (i < d.next) Look.Faint else Look.Ink, big = true))
                Text(t.substringAfter(' '), style = Look.style((10 * scale).sp, Look.Faint))
                if (on) {
                    Spacer(Modifier.height(4.dp * scale))
                    Text(countdown(s.at - now, picks.latinDigits), style = Look.style((11 * scale).sp, Look.Gold, FontWeight.Medium))
                }
            }
        }
    }
}

// ================================================================ the game

@Composable
fun GameWidget(style: Int, game: Fixture, now: Long, picks: Picks, scale: Float) = when (style) {
    1 -> GameBoard(game, now, picks, scale)
    2 -> GameCountdown(game, now, picks, scale)
    else -> GameCrests(game, now, picks, scale)
}

private fun meta(game: Fixture, picks: Picks): String {
    val zone = picks.place.zone
    return game.competition + "  " + weekday(game.kickoff, zone) + " " + gregorianDate(game.kickoff, zone, picks.latinDigits)
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
            Box(Modifier.size(halo).background(Brush.radialGradient(listOf(glow.copy(alpha = .38f), Color.Transparent))))
        }
        if (bmp != null) Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(size))
    }
}

@Composable
private fun KickOff(game: Fixture, now: Long, picks: Picks, size: Float, scale: Float) {
    val live = game.kickoff <= now
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (live) "مباشر" else clock12(game.kickoff, picks.place.zone, picks.latinDigits),
            style = Look.style((size * scale).sp, if (live) Look.Live else Look.Ink, big = true))
        Text(if (live) "جارية الآن" else until(game.kickoff - now, picks.latinDigits), style = Look.style((12 * scale).sp, Look.Gold))
    }
}

/** the crests large, each on a halo of its colour, the names small under them, the time between */
@Composable
private fun GameCrests(game: Fixture, now: Long, picks: Picks, scale: Float) = Block(scale) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Crest(game.homeBadge, 46.dp * scale, 64.dp * scale)
            Text(game.home, style = Look.style((13 * scale).sp, Look.Soft, FontWeight.Medium), maxLines = 1)
        }
        KickOff(game, now, picks, 26f, scale)
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Crest(game.awayBadge, 46.dp * scale, 64.dp * scale)
            Text(game.away, style = Look.style((13 * scale).sp, Look.Soft, FontWeight.Medium), maxLines = 1)
        }
    }
    Spacer(Modifier.height(6.dp * scale))
    Text(meta(game, picks), style = Look.style((11 * scale).sp, Look.Faint), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

/** one bar like a broadcast's score strip: crest and name on each side, the time in a gold box in the middle */
@Composable
private fun GameBoard(game: Fixture, now: Long, picks: Picks, scale: Float) = Block(scale) {
    Text(meta(game, picks), style = Look.style((12 * scale).sp, Look.Gold, FontWeight.Medium))
    Spacer(Modifier.height(10.dp * scale))
    val bar = RoundedCornerShape(14.dp * scale)
    Row(
        Modifier.fillMaxWidth().background(Color(0x1AFFFFFF), bar).border(1.dp, Color(0x1FFFFFFF), bar).padding(6.dp * scale),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f).padding(horizontal = 8.dp * scale), verticalAlignment = Alignment.CenterVertically) {
            Crest(game.homeBadge, 30.dp * scale)
            Spacer(Modifier.width(10.dp * scale))
            Text(game.home, style = Look.style((17 * scale).sp, big = true), maxLines = 1)
        }
        val live = game.kickoff <= now
        Box(Modifier.background(if (live) Look.Live else Look.Gold, RoundedCornerShape(10.dp * scale)).padding(horizontal = 14.dp * scale, vertical = 8.dp * scale)) {
            Text(if (live) "مباشر" else clock12(game.kickoff, picks.place.zone, picks.latinDigits),
                style = Look.style((16 * scale).sp, Color(0xFF15120C), big = true).copy(shadow = null))
        }
        Row(Modifier.weight(1f).padding(horizontal = 8.dp * scale), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            Text(game.away, style = Look.style((17 * scale).sp, big = true), maxLines = 1)
            Spacer(Modifier.width(10.dp * scale))
            Crest(game.awayBadge, 30.dp * scale)
        }
    }
    Spacer(Modifier.height(8.dp * scale))
    Text(if (game.kickoff <= now) "جارية الآن" else until(game.kickoff - now, picks.latinDigits), style = Look.style((12 * scale).sp, Look.Soft),
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

/** the time left as the hero: days, or hours and minutes, in large figures, the two crests overlapping beside it */
@Composable
private fun GameCountdown(game: Fixture, now: Long, picks: Picks, scale: Float) = Block(scale) {
    val left = game.kickoff - now
    val live = left <= 0
    val days = left / 86_400_000L
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(92.dp * scale)) {
            Crest(game.homeBadge, 52.dp * scale)
            Box(Modifier.offset(x = 40.dp * scale, y = 10.dp * scale)) { Crest(game.awayBadge, 52.dp * scale) }
        }
        Spacer(Modifier.width(18.dp * scale))
        Column(Modifier.weight(1f)) {
            Text("${game.home} × ${game.away}", style = Look.style((15 * scale).sp, Look.Ink, FontWeight.Medium), maxLines = 1)
            Text(meta(game, picks), style = Look.style((11 * scale).sp, Look.Faint), maxLines = 1)
            Text(clock12(game.kickoff, picks.place.zone, picks.latinDigits), style = Look.style((12 * scale).sp, Look.Soft))
        }
        Column(horizontalAlignment = Alignment.End) {
            val big = when {
                live -> "مباشر"
                days >= 1 -> Look.digits(days.toString(), picks.latinDigits)
                else -> countdown(left, picks.latinDigits)
            }
            Text(big, style = Look.style(((if (days >= 1 && !live) 44 else 28) * scale).sp, if (live) Look.Live else Look.Ink, big = true))
            Text(
                when { live -> "جارية الآن"; days >= 2 -> "أيام"; days == 1L -> "يوم"; else -> "على البداية" },
                style = Look.style((12 * scale).sp, Look.Gold, FontWeight.Medium),
            )
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
private fun SkyIcon(sky: Sky, picks: Picks, scale: Float) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp * scale), verticalAlignment = Alignment.CenterVertically) {
        SkyGlyph(sky.code, sky.day, 44.dp * scale)
        Spacer(Modifier.width(14.dp * scale))
        Text(deg(sky.temp, picks), style = Look.style((34 * scale).sp, big = true))
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(picks.place.name, style = Look.style((14 * scale).sp, Look.Gold, FontWeight.Medium))
            Spacer(Modifier.height(2.dp))
            Text(Look.ltr("↑ ${deg(sky.high, picks)}   ↓ ${deg(sky.low, picks)}"), style = Look.style((15 * scale).sp, Look.Soft))
        }
    }
}

/** the temperature very large with the sky's word under it, the drawing beside, and a slim bar for the day's range */
@Composable
private fun SkyBig(sky: Sky, picks: Picks, scale: Float) = Block(scale) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(deg(sky.temp, picks), style = Look.style((54 * scale).sp, big = true).copy(lineHeight = (56 * scale).sp))
            Text(listOf(skyWord(sky.code), picks.place.name).filter { it.isNotEmpty() }.joinToString(" في "),
                style = Look.style((15 * scale).sp, Look.Ink, FontWeight.Medium))
        }
        SkyGlyph(sky.code, sky.day, 64.dp * scale)
    }
    Spacer(Modifier.height(10.dp * scale))
    // where now sits between the day's low and high
    val span = (sky.high - sky.low).coerceAtLeast(1.0)
    val at = ((sky.temp - sky.low) / span).toFloat().coerceIn(0f, 1f)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(deg(sky.low, picks), style = Look.style((12 * scale).sp, Look.Soft))
        Spacer(Modifier.width(8.dp * scale))
        Canvas(Modifier.weight(1f).height(8.dp * scale)) {
            val y = size.height / 2
            val start = if (rtl) size.width else 0f   // the low end, beside the low figure
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

/** now, then the next five hours each with its time, drawing and temperature */
@Composable
private fun SkyHours(sky: Sky, picks: Picks, scale: Float) = Block(scale) {
    val soon = sky.hours.filter { it.at > System.currentTimeMillis() }.take(5)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SkyGlyph(sky.code, sky.day, 40.dp * scale)
            Text(deg(sky.temp, picks), style = Look.style((24 * scale).sp, big = true))
            Text("الآن", style = Look.style((11 * scale).sp, Look.Gold, FontWeight.Medium))
        }
        Spacer(Modifier.width(14.dp * scale))
        Box(Modifier.width(1.dp / 2).height(70.dp * scale).background(Look.Gold.copy(alpha = .4f)))
        Spacer(Modifier.width(6.dp * scale))
        if (soon.isEmpty()) {
            Text(picks.place.name, style = Look.style((14 * scale).sp, Look.Gold))
        } else soon.forEach { h ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                SkyGlyph(h.code, h.day, 26.dp * scale)
                Spacer(Modifier.height(4.dp * scale))
                Text(deg(h.temp, picks), style = Look.style((14 * scale).sp, Look.Ink, FontWeight.Medium))
                val hour = cityTime(h.at, picks.place.zone).get(Calendar.HOUR_OF_DAY)
                Text(Look.digits("${if (hour % 12 == 0) 12 else hour % 12}", picks.latinDigits) + if (hour < 12) " ص" else " م",
                    style = Look.style((10 * scale).sp, Look.Faint))
            }
        }
    }
}
