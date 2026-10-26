package app.tilfazi.tv.ambient

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Picks
import app.tilfazi.tv.feed.Salah
import app.tilfazi.tv.look.Board
import app.tilfazi.tv.look.Look
import app.tilfazi.tv.look.Scenery
import app.tilfazi.tv.look.Stage
import app.tilfazi.tv.look.clock12
import app.tilfazi.tv.look.countdown
import app.tilfazi.tv.look.gregorianDate
import app.tilfazi.tv.look.hijriDate
import app.tilfazi.tv.look.weekday
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** the screensaver's scenes, as the settings name them */
val SCENES = listOf("مثل الرئيسية", "ساعة كبيرة", "معرض الصور", "سماء الليل", "مسار الشمس",
    "الوقت بالكلمات", "المدارات", "زخرفة", "الكثبان", "الشفق القطبي")
val DIMS = listOf("بدون", "خفيف", "قوي")

/** One scene, the whole screen. The photo scene needs photos even when the home screen is on glass: see AmbientScreen. */
@Composable
fun Scene(index: Int, board: Board, picks: Picks) {
    when (index) {
        1 -> BigClock(board, picks)
        2 -> Gallery(board, picks)
        3 -> NightSky(board, picks)
        4 -> SunPath(board, picks)
        5 -> WordsScene(board, picks)
        6 -> OrbitScene(board, picks)
        7 -> PatternScene(board, picks)
        8 -> DunesScene(board, picks)
        9 -> AuroraScene(board, picks)
        else -> Scenery(board, picks.looks, picks.place) {
            Stage(board, picks, Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 48.dp), scale = 1.25f, panelWidth = 400.dp)
        }
    }
}

private fun hm(now: Long, zone: String): Pair<Int, Int> {
    val k = Calendar.getInstance(TimeZone.getTimeZone(zone), Locale.US).apply { timeInMillis = now }
    return k.get(Calendar.HOUR_OF_DAY) to k.get(Calendar.MINUTE)
}

private fun time(now: Long, picks: Picks): String {
    val (h, m) = hm(now, picks.place.zone)
    return Look.ltr(Look.digits("${if (h % 12 == 0) 12 else h % 12}:${m.toString().padStart(2, '0')}", picks.latinDigits))
}

/** «الظهر بعد ٤:٢٠:١٠» */
private fun nextLine(now: Long, picks: Picks): String {
    val next = Salah.next(now, picks.place) ?: return ""
    return "${next.name} بعد ${countdown(next.at - now, picks.latinDigits)}"
}

// ================================================================ the big clock

/** the time alone in the middle, very large, the dates and the next prayer quietly under it */
@Composable
private fun BigClock(board: Board, picks: Picks) = Scenery(board, picks.looks, picks.place) {
    val zone = picks.place.zone
    val (h, _) = hm(board.now, zone)
    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(time(board.now, picks), style = Look.style(190.sp, Look.Ink, big = true).copy(lineHeight = 190.sp))
            Spacer(Modifier.width(18.dp))
            Text(if (h < 12) "ص" else "م", style = Look.style(40.sp, Look.Gold, FontWeight.Medium))
        }
        Spacer(Modifier.height(10.dp))
        // a drawn dot between the dates: «·» beside a digit reads as ٠ in the Arabic face
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(weekday(board.now, zone) + " " + hijriDate(board.now, zone, picks.latinDigits), style = Look.style(22.sp, Look.Ink, FontWeight.Medium))
            Box(Modifier.padding(horizontal = 14.dp).size(6.dp).background(Look.Gold, CircleShape))
            Text(gregorianDate(board.now, zone, picks.latinDigits), style = Look.style(22.sp, Look.Soft, FontWeight.Medium))
        }
        Spacer(Modifier.height(28.dp))
        Text(nextLine(board.now, picks), style = Look.style(20.sp, Look.Gold, FontWeight.Medium))
    }
}

// ================================================================ the gallery

/**
 * The photo itself, sharp and full, slowly moving closer and drifting (a new photo every half hour); the time,
 * the date and the next prayer small in a corner over a soft shade.
 */
@Composable
private fun Gallery(board: Board, picks: Picks) {
    val drift = rememberInfiniteTransition(label = "drift")
    val t by drift.animateFloat(0f, 1f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Reverse), label = "t")
    // the photos themselves, moved by the remote's arrows and every ten minutes (unless the owner wants them still)
    val c = androidx.compose.ui.platform.LocalContext.current
    val photos by androidx.compose.runtime.produceState(emptyList<app.tilfazi.tv.feed.Gallery.Pick>(), picks.looks.photos) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            (if (picks.looks.photos == 1) app.tilfazi.tv.feed.Gallery.owned(c) else emptyList()).ifEmpty { app.tilfazi.tv.feed.Gallery.scenes(c) }
        }
    }
    val i = rememberSlide(photos.size, picks.looks.artAuto == 1)
    val shown by androidx.compose.runtime.produceState<Bitmap?>(null, photos, i) {
        value = photos.getOrNull(i)?.let { p -> kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { app.tilfazi.tv.feed.Gallery.decode(c, p) } }
    }
    Box(Modifier.fillMaxSize().background(Look.NightLow)) {
        Crossfade(targetState = if (picks.looks.photos == 2) board.photo else shown, animationSpec = tween(2500), label = "photo") { bmp: Bitmap? ->
            if (bmp != null) Image(
                bmp.asImageBitmap(), null, contentScale = ContentScale.Crop,
                modifier = if (picks.looks.artAuto == 0) Modifier.fillMaxSize() else Modifier.fillMaxSize().graphicsLayer {   // still photos: no drift
                    val grow = 1.04f + .1f * t
                    scaleX = grow; scaleY = grow
                    translationX = (t - .5f) * size.width * .04f
                    translationY = (.5f - t) * size.height * .02f
                },
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, .55f to Color.Transparent, 1f to Color(0xB3000000))))
        val zone = picks.place.zone
        val (h, _) = hm(board.now, zone)
        Column(Modifier.align(Alignment.BottomStart).padding(start = 64.dp, bottom = 52.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(time(board.now, picks), style = Look.style(64.sp, Look.Ink, big = true))
                Spacer(Modifier.width(10.dp))
                Text(if (h < 12) "ص" else "م", style = Look.style(20.sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(bottom = 14.dp))
            }
            Text(weekday(board.now, zone) + " " + hijriDate(board.now, zone, picks.latinDigits), style = Look.style(18.sp, Look.Ink, FontWeight.Medium))
            Spacer(Modifier.height(4.dp))
            Text(nextLine(board.now, picks), style = Look.style(15.sp, Look.Gold, FontWeight.Medium))
        }
        if (board.photoBy.isNotBlank()) {
            Text("تصوير ${board.photoBy} · Unsplash", style = Look.style(9.sp, Look.Faint),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 10.dp))
        }
    }
}

// ================================================================ the night sky

private class Star(val x: Float, val y: Float, val r: Float, val phase: Float, val speed: Float)

/** the moon's lit fraction and whether it is waxing, from the Umm al-Qura day of the month */
private fun moonOf(now: Long, zone: String): Pair<Float, Boolean> {
    val day = runCatching {
        val h = android.icu.util.IslamicCalendar(android.icu.util.TimeZone.getTimeZone(zone), android.icu.util.ULocale("ar_SA@calendar=islamic-umalqura"))
        h.calculationType = android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        h.timeInMillis = now
        h.get(Calendar.DAY_OF_MONTH)
    }.getOrDefault(14)
    val age = (day - 1 + .5f) / 29.53f   // 0 new, .5 full
    return ((1 - cos(2 * PI * age)) / 2).toFloat() to (age < .5f)
}

/**
 * A deep night: stars that twinkle at their own pace and turn slowly round the screen, and the moon in tonight's
 * phase; the time and the Hijri date in the middle of the lower half.
 */
@Composable
private fun NightSky(board: Board, picks: Picks) {
    val stars = remember {
        val rnd = Random(0x0637064406270644L)   // a fixed sky: the same stars in the same places on every TV
        List(170) { Star(rnd.nextFloat(), rnd.nextFloat() * .9f, .6f + rnd.nextFloat() * 1.6f, rnd.nextFloat() * 6.28f, .6f + rnd.nextFloat() * 1.8f) }
    }
    val clock = rememberInfiniteTransition(label = "sky")
    val beat by clock.animateFloat(0f, 6.283f, infiniteRepeatable(tween(9_000, easing = LinearEasing)), label = "beat")
    val turn by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(600_000, easing = LinearEasing)), label = "turn")
    val zone = picks.place.zone
    val (lit, waxing) = remember(board.now / 3_600_000L) { moonOf(board.now, zone) }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF070B1A), Color(0xFF0C1430), Color(0xFF141A2E)))) ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            stars.forEach { s ->
                val x = ((s.x + turn) % 1f) * w
                val a = .35f + .65f * ((sin(beat * s.speed + s.phase) + 1) / 2)
                drawCircle(Color.White.copy(alpha = a), s.r * density, Offset(x, s.y * h))
            }
        }
        // the moon: a soft halo, and over it the lit disc with the dark part cut away (on a layer of its own, so the
        // cut does not reach the halo)
        Box(Modifier.align(Alignment.TopStart).padding(start = 110.dp, top = 60.dp).size(180.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(180.dp)) {
                drawCircle(Brush.radialGradient(listOf(Color(0x2EF2E6C9), Color(0x0AF2E6C9), Color.Transparent)), size.minDimension / 2)
            }
            Canvas(Modifier.size(76.dp).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            val r = size.minDimension / 2 / .62f
            val c = Offset(size.width / 2, size.height / 2)
            drawCircle(Color(0xFFF2E6C9), r * .62f, c)
            // the shadow: a disc shifted sideways by how much is lit (waxing: lit on the right, as seen from Arabia;
            // the canvas is not mirrored for Arabic, so right is right)
            val shift = r * .62f * 2 * lit
            val side = if (waxing) -1 else 1
            if (lit < .97f) drawCircle(Color.Black, r * .62f, Offset(c.x + side * shift, c.y), blendMode = BlendMode.DstOut)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time(board.now, picks), style = Look.style(110.sp, Look.Ink, big = true))
            Text(weekday(board.now, zone) + " " + hijriDate(board.now, zone, picks.latinDigits), style = Look.style(24.sp, Color(0xFFF2E6C9), FontWeight.Medium))
            Spacer(Modifier.height(16.dp))
            Text(nextLine(board.now, picks), style = Look.style(17.sp, Look.Gold, FontWeight.Medium))
        }
    }
}

// ================================================================ the sun's path

/**
 * The day as one arc across the screen: the horizon, the sun's path from sunrise to sunset with the day's prayers
 * marked on it, and the sun (or the moon at night, along the night's own arc) where it is now; the time over it.
 */
@Composable
private fun SunPath(board: Board, picks: Picks) = Scenery(board, picks.looks, picks.place) {
    val place = picks.place
    val now = board.now
    val (rise, set) = Salah.sun(now, place)
    val day = now in rise..set
    val prayers = Salah.day(now, place)
    // at night the arc runs from this sunset to the next sunrise
    val (from, to) = when {
        day -> rise to set
        now > set -> set to Salah.sun(now + 86_400_000L, place).first
        else -> Salah.sun(now - 86_400_000L, place).second to rise
    }
    val at = ((now - from).toFloat() / (to - from)).coerceIn(0f, 1f)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val glow by rememberInfiniteTransition(label = "sun").animateFloat(.7f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "glow")

    Column(Modifier.align(Alignment.TopCenter).padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time(now, picks), style = Look.style(84.sp, Look.Ink, big = true))
        Text(weekday(now, place.zone) + " " + hijriDate(now, place.zone, picks.latinDigits), style = Look.style(20.sp, Look.Soft, FontWeight.Medium))
    }
    Canvas(Modifier.fillMaxWidth().height(260.dp).align(Alignment.BottomCenter).padding(horizontal = 80.dp).padding(bottom = 70.dp)) {
        val w = size.width
        val base = size.height
        val top = size.height * .12f
        fun point(f: Float): Offset {   // f from 0 (the arc's start, at the right in Arabic) to 1
            val a = PI * f
            val x = (1 - cos(a).toFloat()) / 2 * w
            return Offset(if (rtl) w - x else x, base - sin(a).toFloat() * (base - top))
        }
        drawLine(Color.White.copy(alpha = .25f), Offset(0f, base), Offset(w, base), 1.dp.toPx())
        val path = Path().apply { (0..60).forEach { i -> val p = point(i / 60f); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        drawPath(path, Color.White.copy(alpha = .18f), style = Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
        val lived = Path().apply { (0..60).forEach { i -> val p = point(at * i / 60f); if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) } }
        drawPath(lived, Look.Gold.copy(alpha = .8f), style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        if (day) {   // the daytime prayers on the arc
            prayers.filter { it.at in rise..set }.forEach { s ->
                drawCircle(if (s.at <= now) Look.Gold else Color.White.copy(alpha = .5f), 5.dp.toPx(), point((s.at - rise).toFloat() / (set - rise)))
            }
        }
        val here = point(at)
        val body = if (day) Color(0xFFFFC857) else Color(0xFFF2E6C9)
        drawCircle(body.copy(alpha = .22f * glow), 34.dp.toPx(), here)
        drawCircle(body, 14.dp.toPx(), here)
    }
    // the prayers' names and times along the bottom
    Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 80.dp, vertical = 22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        val next = prayers.firstOrNull { it.at > now }
        // in the day's order: Fajr, then the sunrise, then the other four
        val marks = listOf(prayers[0]) + app.tilfazi.tv.feed.Salat("الشروق", rise) + prayers.drop(1)
        marks.forEach { s ->
            val on = s == next
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(s.name, style = Look.style(13.sp, if (on) Look.Gold else Look.Soft, FontWeight.Medium), textAlign = TextAlign.Center)
                Text(clock12(s.at, place.zone, picks.latinDigits), style = Look.style(14.sp, if (on) Look.Gold else Look.Ink, FontWeight.Medium))
            }
        }
    }
}
