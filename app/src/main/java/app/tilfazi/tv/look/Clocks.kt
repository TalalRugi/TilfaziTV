package app.tilfazi.tv.look

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Picks
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

/** the clock's shapes, as the settings name them: four digital, then four with hands */
val CLOCK_STYLES = listOf("كلاسيكية", "مكدّسة", "عقارب", "رفيعة", "بطاقات", "ثنائية اللون", "فاخرة", "مينا سوداء", "مثمّنة")

@Composable
fun ClockFace(style: Int, now: Long, picks: Picks, scale: Float) = when (style) {
    1 -> StackedClock(now, picks, scale)
    2 -> DialClock(now, picks, scale)
    3 -> ThinClock(now, picks, scale)
    4 -> TileClock(now, picks, scale)
    5 -> DuoClock(now, picks, scale)
    6 -> LuxuryDial(now, picks, scale)
    7 -> BlackDial(now, picks, scale)
    8 -> StarDial(now, picks, scale)
    else -> ClassicClock(now, picks, scale)
}

private fun hm(now: Long, zone: String): Triple<Int, Int, Int> {
    val k = cityTime(now, zone)
    val h = k.get(Calendar.HOUR_OF_DAY)
    return Triple(h, k.get(Calendar.MINUTE), k.get(Calendar.SECOND))
}

private fun h12(h: Int) = if (h % 12 == 0) 12 else h % 12

/** the time large, ص or م beside it in gold, a short hairline, then both dates on one line */
@Composable
private fun ClassicClock(now: Long, picks: Picks, scale: Float) {
    val zone = picks.place.zone
    val (h, m, _) = hm(now, zone)
    Column(horizontalAlignment = Alignment.Start) {   // Start is the right in Arabic
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Look.ltr(Look.digits("${h12(h)}:${two(m)}", picks.latinDigits)), style = Look.style((92 * scale).sp, Look.Ink, big = true))
            Spacer(Modifier.width(12.dp * scale))
            Text(if (h < 12) "ص" else "م", style = Look.style((24 * scale).sp, Look.Gold, FontWeight.Medium))
        }
        Box(
            Modifier.padding(vertical = 14.dp * scale).width(220.dp * scale).height(1.dp / 2)
                .background(Brush.horizontalGradient(listOf(Look.Gold.copy(alpha = .45f), Color.Transparent)))
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(weekday(now, zone) + " " + hijriDate(now, zone, picks.latinDigits), style = Look.style((18 * scale).sp, Look.Ink, FontWeight.Medium))
            Box(Modifier.padding(horizontal = 10.dp * scale).size(4.dp * scale).background(Look.Gold.copy(alpha = .8f), CircleShape))
            Text(gregorianDate(now, zone, picks.latinDigits), style = Look.style((16 * scale).sp, Look.Soft))
        }
    }
}

/**
 * The hours above the minutes, both huge, the hours in gold; at their left a narrow column with the day, the dates and
 * ص or م, tied to them by a thin gold rule.
 */
@Composable
private fun StackedClock(now: Long, picks: Picks, scale: Float) {
    val zone = picks.place.zone
    val (h, m, _) = hm(now, zone)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Look.digits(two(h12(h)), picks.latinDigits), style = Look.style((84 * scale).sp, Look.Gold, big = true).copy(lineHeight = (84 * scale).sp))
            Text(Look.digits(two(m), picks.latinDigits), style = Look.style((84 * scale).sp, Look.Ink, big = true).copy(lineHeight = (84 * scale).sp))
        }
        Spacer(Modifier.width(20.dp * scale))
        Box(Modifier.width(1.dp / 2).height(150.dp * scale).background(Brush.verticalGradient(listOf(Color.Transparent, Look.Gold.copy(alpha = .6f), Color.Transparent))))
        Spacer(Modifier.width(20.dp * scale))
        Column {
            Text(if (h < 12) "صباحاً" else "مساءً", style = Look.style((14 * scale).sp, Look.Gold, FontWeight.Medium))
            Spacer(Modifier.height(10.dp * scale))
            Text(weekday(now, zone), style = Look.style((26 * scale).sp, big = true))
            Spacer(Modifier.height(4.dp * scale))
            Text(hijriDate(now, zone, picks.latinDigits), style = Look.style((15 * scale).sp, Look.Ink, FontWeight.Medium))
            Text(gregorianDate(now, zone, picks.latinDigits, year = true), style = Look.style((13 * scale).sp, Look.Soft))
        }
    }
}

/** A dial with fine ticks, gold quarter marks and a gold second hand; the time and the dates small under it. */
@Composable
private fun DialClock(now: Long, picks: Picks, scale: Float) {
    val zone = picks.place.zone
    val (h, m, s) = hm(now, zone)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(200.dp * scale)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension / 2
            drawCircle(Color(0x14FFFFFF), r, c)
            drawCircle(Look.Gold.copy(alpha = .35f), r, c, style = Stroke(1.dp.toPx()))
            for (i in 0 until 60) {
                val a = Math.toRadians(i * 6.0 - 90)
                val quarter = i % 15 == 0
                val hour = i % 5 == 0
                val from = r * if (quarter) .80f else if (hour) .86f else .92f
                val to = r * .96f
                drawLine(
                    if (quarter) Look.Gold else Color.White.copy(alpha = if (hour) .7f else .22f),
                    Offset(c.x + cos(a).toFloat() * from, c.y + sin(a).toFloat() * from),
                    Offset(c.x + cos(a).toFloat() * to, c.y + sin(a).toFloat() * to),
                    (if (quarter) 3.dp else if (hour) 2.dp else 1.dp).toPx(), StrokeCap.Round,
                )
            }
            fun hand(turns: Double, length: Float, width: Float, color: Color) {
                val a = Math.toRadians(turns * 360 - 90)
                drawLine(color, Offset(c.x - cos(a).toFloat() * r * .1f, c.y - sin(a).toFloat() * r * .1f),
                    Offset(c.x + cos(a).toFloat() * r * length, c.y + sin(a).toFloat() * r * length), width, StrokeCap.Round)
            }
            hand(((h % 12) + m / 60.0) / 12.0, .5f, 5.dp.toPx(), Look.Ink)
            hand((m + s / 60.0) / 60.0, .74f, 3.dp.toPx(), Look.Ink)
            hand(s / 60.0, .82f, 1.2.dp.toPx(), Look.Gold)
            drawCircle(Look.Gold, 4.dp.toPx(), c)
        }
        Spacer(Modifier.height(16.dp * scale))
        Text(clock12(now, zone, picks.latinDigits), style = Look.style((24 * scale).sp, big = true))
        Spacer(Modifier.height(4.dp * scale))
        Text(weekday(now, zone) + " " + hijriDate(now, zone, picks.latinDigits, year = false), style = Look.style((15 * scale).sp, Look.Ink, FontWeight.Medium))
        Text(gregorianDate(now, zone, picks.latinDigits), style = Look.style((13 * scale).sp, Look.Soft))
    }
}
