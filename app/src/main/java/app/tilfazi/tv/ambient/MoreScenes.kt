package app.tilfazi.tv.ambient

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Picks
import app.tilfazi.tv.feed.Salah
import app.tilfazi.tv.look.Board
import app.tilfazi.tv.look.BackdropMotion
import app.tilfazi.tv.look.Blurs
import app.tilfazi.tv.look.Glass
import app.tilfazi.tv.look.Look
import app.tilfazi.tv.look.Tints
import app.tilfazi.tv.look.countdown
import app.tilfazi.tv.look.gregorianDate
import app.tilfazi.tv.look.hijriDate
import app.tilfazi.tv.look.weekday
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private fun clockOf(now: Long, zone: String): Triple<Int, Int, Int> {
    val k = Calendar.getInstance(TimeZone.getTimeZone(zone), Locale.US).apply { timeInMillis = now }
    return Triple(k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE), k.get(Calendar.SECOND))
}

private fun digital(now: Long, picks: Picks): String {
    val (h, m, _) = clockOf(now, picks.place.zone)
    return Look.ltr(Look.digits("${if (h % 12 == 0) 12 else h % 12}:${m.toString().padStart(2, '0')}", picks.latinDigits))
}

private fun upcoming(now: Long, picks: Picks): String {
    val next = Salah.next(now, picks.place) ?: return ""
    return "${next.name} بعد ${countdown(next.at - now, picks.latinDigits)}"
}

/** the day and the Hijri date, a drawn gold dot, the Gregorian date */
@Composable
private fun DatesLine(now: Long, picks: Picks, size: Float, ink: Color = Look.Ink) {
    val zone = picks.place.zone
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(weekday(now, zone) + " " + hijriDate(now, zone, picks.latinDigits), style = Look.style(size.sp, ink, FontWeight.Medium))
        Box(Modifier.padding(horizontal = 12.dp).size(5.dp).background(Look.Gold, CircleShape))
        Text(gregorianDate(now, zone, picks.latinDigits), style = Look.style(size.sp, Look.Soft, FontWeight.Medium))
    }
}

// ================================================================ the time in words

private val HOURS = listOf("الثانية عشرة", "الواحدة", "الثانية", "الثالثة", "الرابعة", "الخامسة", "السادسة", "السابعة",
    "الثامنة", "التاسعة", "العاشرة", "الحادية عشرة")

private val UNITS = listOf("", "", "", "ثلاث", "أربع", "خمس", "ست", "سبع", "ثماني", "تسع", "عشر")
private val TEENS_FIRST = listOf("", "إحدى", "اثنتا", "ثلاث", "أربع", "خمس", "ست", "سبع", "ثماني", "تسع")
private val UNITS_WITH_TENS = listOf("", "إحدى", "اثنتان", "ثلاث", "أربع", "خمس", "ست", "سبع", "ثماني", "تسع")

/**
 * [n] minutes (1–29) in words, as counted in Arabic: «دقيقة», «دقيقتان», «ثلاث دقائق», «إحدى عشرة دقيقة»,
 * «خمس وعشرون دقيقة». [after] is true after «إلا», where the two-forms change: «دقيقتين», «اثنتي عشرة».
 */
internal fun minutesInWords(n: Int, after: Boolean = false): String = when {
    n == 1 -> "دقيقة"
    n == 2 -> if (after) "دقيقتين" else "دقيقتان"
    n <= 10 -> UNITS[n] + " دقائق"
    n < 20 -> (if (n == 12 && after) "اثنتي" else TEENS_FIRST[n - 10]) + " عشرة دقيقة"
    n == 20 -> (if (after) "عشرين" else "عشرون") + " دقيقة"
    else -> {
        val u = n - 20
        val unit = if (u == 2 && after) "اثنتين" else UNITS_WITH_TENS[u]
        unit + " و" + (if (after) "عشرين" else "عشرون") + " دقيقة"
    }
}

/**
 * The hour and the exact minute as said aloud: «الثامنة / تماماً», «الثامنة / وسبع دقائق», «الثامنة / والربع»,
 * «الثامنة / والنصف وثلاث دقائق», «التاسعة / إلا ربعاً», «التاسعة / إلا دقيقتين». After the half hour (from :41,
 * and at :40) the time is counted to the next hour, except for the few minutes past the half.
 */
internal fun timeInWords(h: Int, m: Int): Pair<String, String> {
    val toNext = m >= 40
    val hour = HOURS[(if (toNext) h + 1 else h) % 12]
    val rest = when (m) {
        0 -> "تماماً"
        15 -> "والربع"
        20 -> "والثلث"
        30 -> "والنصف"
        40 -> "إلا ثلثاً"
        45 -> "إلا ربعاً"
        in 1..29 -> "و" + minutesInWords(m)
        in 31..39 -> "والنصف و" + minutesInWords(m - 30)
        else -> "إلا " + minutesInWords(60 - m, after = true)
    }
    return hour to rest
}

private fun partOfDay(h: Int) = when (h) {
    in 4..11 -> "صباحاً"
    in 12..14 -> "ظهراً"
    in 15..17 -> "عصراً"
    in 18..21 -> "مساءً"
    else -> "ليلاً"
}

/** the time written out, large and calm: «الساعة / الثامنة / وسبع دقائق / صباحاً», changing every minute; the figures faint under it */
@Composable
internal fun WordsScene(board: Board, picks: Picks) {
    val (h, m, _) = clockOf(board.now, picks.place.zone)
    val (hour, rest) = timeInWords(h, m)
    Box(Modifier.fillMaxSize().background(Look.NightLow)) {
        // the glass in the owner's colour, with their motion (gold dust when none is picked)
        val tint = Tints.of(picks.looks.tint).color
        Glass(tint, Blurs.GLASS, place = picks.looks.glow)
        BackdropMotion(if (picks.looks.motion > 0) picks.looks.motion else 2, picks.looks.density, tint)
        Column(Modifier.align(Alignment.CenterStart).padding(start = 110.dp)) {
            Text("الساعة", style = Look.style(30.sp, Look.Gold, FontWeight.Medium))
            Text(hour, style = Look.style(120.sp, Look.Ink, big = true).copy(lineHeight = 130.sp))
            Text(rest, style = Look.style(58.sp, Look.Ink, FontWeight.Light).copy(lineHeight = 70.sp))
            Text(partOfDay(h), style = Look.style(34.sp, Look.Soft, FontWeight.Light))
            Spacer(Modifier.height(36.dp))
            DatesLine(board.now, picks, 18f)
            Spacer(Modifier.height(8.dp))
            Text(digital(board.now, picks) + "   " + upcoming(board.now, picks), style = Look.style(15.sp, Look.Faint))
        }
    }
}

// ================================================================ the orbits

/** three rings round the time: the hours (gold, thick), the minutes, and a gold dot running the seconds round */
@Composable
internal fun OrbitScene(board: Board, picks: Picks) {
    val (h, m, _) = clockOf(board.now, picks.place.zone)
    // the time read on every frame, so the seconds dot glides round without a jump each second
    val frame by produceState(System.currentTimeMillis()) { while (true) withFrameMillis { value = System.currentTimeMillis() } }
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF1A1B20), Color(0xFF09090B)))), contentAlignment = Alignment.Center) {
        Canvas(Modifier.padding(bottom = 60.dp).size(380.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            fun ring(r: Float, part: Float, width: Float, color: Color, track: Color) {
                drawCircle(track, r, c, style = Stroke(width))
                drawArc(color, -90f, 360f * part, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(width, cap = StrokeCap.Round))
            }
            val outer = size.minDimension / 2 - 8.dp.toPx()
            val k = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone(picks.place.zone)).apply { timeInMillis = frame }
            val secs = k.get(java.util.Calendar.SECOND) + k.get(java.util.Calendar.MILLISECOND) / 1000f
            val sec = if (Look.still) 0f else secs / 60f
            ring(outer * .72f, ((h % 12) + m / 60f) / 12f, 7.dp.toPx(), Look.Gold, Color.White.copy(alpha = .06f))
            ring(outer * .86f, (m + secs / 60f) / 60f, 2.5.dp.toPx(), Look.Ink.copy(alpha = .85f), Color.White.copy(alpha = .06f))
            drawCircle(Color.White.copy(alpha = .1f), outer, c, style = Stroke(1.dp.toPx()))
            val a = 2 * PI * sec - PI / 2
            val dot = Offset(c.x + cos(a).toFloat() * outer, c.y + sin(a).toFloat() * outer)
            drawCircle(Look.Gold.copy(alpha = .25f), 12.dp.toPx(), dot)
            drawCircle(Look.Gold, 5.dp.toPx(), dot)
        }
        Column(Modifier.padding(bottom = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digital(board.now, picks), style = Look.style(72.sp, Look.Ink, big = true))
            Text(if (h < 12) "صباحاً" else "مساءً", style = Look.style(16.sp, Look.Gold, FontWeight.Medium))
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DatesLine(board.now, picks, 18f)
            Spacer(Modifier.height(6.dp))
            Text(upcoming(board.now, picks), style = Look.style(15.sp, Look.Gold, FontWeight.Medium))
        }
    }
}

// ================================================================ the pattern

/** one eight-pointed star (two squares, one turned by 45°) centred on [c], [r] from centre to point */
private fun DrawScope.khatam(c: Offset, r: Float, color: Color, width: Float) {
    for (turn in listOf(0f, 45f)) rotate(turn, c) {
        val side = r * 1.414f
        drawRect(color, Offset(c.x - side / 2, c.y - side / 2), Size(side, side), style = Stroke(width))
    }
    drawCircle(color, r * .38f, c, style = Stroke(width))
}

/**
 * A field of eight-pointed stars in gold lines, as on old doors and ceilings, turning very slowly and breathing a
 * little; the time in a dark round medallion in the middle.
 */
@Composable
internal fun PatternScene(board: Board, picks: Picks) {
    val motion = rememberInfiniteTransition(label = "pattern")
    val turn by motion.animateFloat(0f, 360f, infiniteRepeatable(tween(900_000, easing = LinearEasing)), label = "turn")
    val breathe by motion.animateFloat(.10f, .2f, infiniteRepeatable(tween(8000), RepeatMode.Reverse), label = "breathe")
    Box(Modifier.fillMaxSize().background(Color(0xFF0C0D10)), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            val step = 120.dp.toPx()
            rotate(turn, c) {
                val reach = size.width * .75f
                var y = c.y - reach
                var row = 0
                while (y < c.y + reach) {
                    var x = c.x - reach + if (row % 2 == 0) 0f else step / 2
                    while (x < c.x + reach) {
                        khatam(Offset(x, y), step * .42f, Look.Gold.copy(alpha = breathe), 1.2.dp.toPx())
                        x += step
                    }
                    y += step * .5f
                    row++
                }
            }
            // a soft dark pool behind the time, fading out with no edge
            drawRect(Brush.radialGradient(listOf(Color(0xE60C0D10), Color(0x990C0D10), Color.Transparent), c, size.height * .55f))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digital(board.now, picks), style = Look.style(80.sp, Look.Ink, big = true))
            DatesLine(board.now, picks, 16f)
            Spacer(Modifier.height(6.dp))
            Text(upcoming(board.now, picks), style = Look.style(14.sp, Look.Gold, FontWeight.Medium))
        }
    }
}

// ================================================================ the dunes

/** the sky's two colours (top, horizon) for the time of day, from the prayer times and the sun */
private fun skyColours(now: Long, picks: Picks): Pair<Color, Color> {
    val day = Salah.day(now, picks.place)
    val (rise, set) = Salah.sun(now, picks.place)
    val night = Color(0xFF070A18) to Color(0xFF1B2140)
    val dawn = Color(0xFF2A2350) to Color(0xFFE3A07A)
    val noon = Color(0xFF3C7BC7) to Color(0xFFBFD8EE)
    val late = Color(0xFF3F5FA0) to Color(0xFFF1C27D)
    val dusk = Color(0xFF2B1E47) to Color(0xFFE0703A)
    fun mix(a: Pair<Color, Color>, b: Pair<Color, Color>, t: Float) = lerp(a.first, b.first, t) to lerp(a.second, b.second, t)
    fun at(from: Long, to: Long) = ((now - from).toFloat() / (to - from).coerceAtLeast(1)).coerceIn(0f, 1f)
    val asr = day[2].at
    val isha = day[4].at
    return when {
        now < day[0].at || now >= isha -> night
        now < rise -> mix(night, dawn, at(day[0].at, rise))
        now < rise + 3_600_000L -> mix(dawn, noon, at(rise, rise + 3_600_000L))
        now < asr -> noon
        now < set - 1_800_000L -> mix(noon, late, at(asr, set - 1_800_000L))
        now < set + 1_200_000L -> mix(late, dusk, at(set - 1_800_000L, set + 1_200_000L))
        else -> mix(dusk, night, at(set + 1_200_000L, isha))
    }
}

/** the desert drawn as layers of dunes under a sky that follows the day, the sun or the moon on its way across */
@Composable
internal fun DunesScene(board: Board, picks: Picks) {
    val now = board.now
    val (top, horizon) = skyColours(now, picks)
    val (rise, set) = Salah.sun(now, picks.place)
    val day = now in rise..set
    val across = if (day) (now - rise).toFloat() / (set - rise) else 0.75f
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Brush.verticalGradient(listOf(top, horizon), 0f, h * .7f))
            // the sun or the moon
            val x = w * (1 - across)   // from the east at the right
            val y = h * .62f - sin(PI * across).toFloat() * h * .45f
            val body = if (day) Color(0xFFFFD38A) else Color(0xFFF2E6C9)
            drawCircle(body.copy(alpha = .18f), 70.dp.toPx(), Offset(x, y))
            drawCircle(body, 26.dp.toPx(), Offset(x, y))
            // three dunes, far to near, each darker than the one behind it
            val sand = if (day) listOf(Color(0xFFD8A267), Color(0xFFB97F48), Color(0xFF8E5A30)) else listOf(Color(0xFF3A3550), Color(0xFF29243A), Color(0xFF1A1626))
            listOf(.62f to .05f, .72f to .07f, .82f to .06f).forEachIndexed { i, (base, height) ->
                val p = Path().apply {
                    moveTo(0f, h)
                    var px = 0f
                    while (px <= w) {
                        val wave = sin(px / w * (2.2f + i) * PI + i * 1.7f).toFloat() * .6f + sin(px / w * (5.3f + i) * PI + i).toFloat() * .4f
                        lineTo(px, h * (base - height * wave))
                        px += w / 80
                    }
                    lineTo(w, h); close()
                }
                drawPath(p, sand[i])
            }
        }
        Column(Modifier.align(Alignment.TopCenter).padding(top = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digital(now, picks), style = Look.style(96.sp, Look.Ink, big = true))
            DatesLine(now, picks, 18f)
        }
        Text(upcoming(now, picks), style = Look.style(16.sp, Look.Ink, FontWeight.Medium),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 40.dp))
    }
}

// ================================================================ the aurora

/**
 * Ribbons of soft light in the backdrop's colour and its neighbours, waving slowly over a dark sky and blurred into
 * an aurora; the time small and low.
 */
@Composable
internal fun AuroraScene(board: Board, picks: Picks) {
    val motion = rememberInfiniteTransition(label = "aurora")
    val phase by motion.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "phase")
    val tint = Tints.of(picks.looks.tint).color
    val colours = listOf(tint, Color(0xFF1FB59A), Color(0xFF6C4BD8))
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF05070D), Color(0xFF0B0F1C))))) {
        Canvas(Modifier.fillMaxSize().then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(36.dp) else Modifier)) {
            val w = size.width
            val h = size.height
            colours.forEachIndexed { i, col ->
                val p = Path()
                var x = -w * .1f
                var first = true
                while (x <= w * 1.1f) {
                    val y = h * (.28f + i * .1f) + sin(x / w * 2.6f * PI + phase + i * 1.9f).toFloat() * h * .08f +
                        sin(x / w * 6.1f * PI - phase * 1.3f + i).toFloat() * h * .025f
                    if (first) { p.moveTo(x, y); first = false } else p.lineTo(x, y)
                    x += w / 60
                }
                drawPath(p, Brush.verticalGradient(listOf(col.copy(alpha = .0f), col.copy(alpha = .75f), col.copy(alpha = .0f))),
                    style = Stroke((46 - i * 8).dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(digital(board.now, picks), style = Look.style(88.sp, Look.Ink, big = true))
            DatesLine(board.now, picks, 18f)
            Spacer(Modifier.height(8.dp))
            Text(upcoming(board.now, picks), style = Look.style(15.sp, Look.Gold, FontWeight.Medium))
        }
    }
}
