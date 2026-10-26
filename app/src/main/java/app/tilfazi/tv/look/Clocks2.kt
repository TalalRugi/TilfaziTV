package app.tilfazi.tv.look

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Picks
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private class Now(val h: Int, val m: Int, val s: Int, val ms: Int)

private fun now(at: Long, zone: String): Now {
    val k = cityTime(at, zone)
    return Now(k.get(Calendar.HOUR_OF_DAY), k.get(Calendar.MINUTE), k.get(Calendar.SECOND), k.get(Calendar.MILLISECOND))
}

/** a clock face or a time is always laid out left to right, whatever the screen's direction: 3 at the right, 8:32 not 32:8 */
@Composable
private fun Ltr(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { content() }

private fun h12(h: Int) = if (h % 12 == 0) 12 else h % 12
private fun two(n: Int) = n.toString().padStart(2, '0')

/** the day and the Hijri date, a gold dot, the Gregorian date: one line */
@Composable
private fun DateLine(at: Long, picks: Picks, scale: Float, size: Float = 17f) {
    val zone = picks.place.zone
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(weekday(at, zone) + " " + hijriDate(at, zone, picks.latinDigits), style = Look.style((size * scale).sp, Look.Ink, FontWeight.Medium))
        Box(Modifier.padding(horizontal = 10.dp * scale).size(4.dp * scale).background(Look.Gold.copy(alpha = .8f), CircleShape))
        Text(gregorianDate(at, zone, picks.latinDigits), style = Look.style(((size - 1) * scale).sp, Look.Soft))
    }
}

// ================================================================ digital

/** very large and light, the seconds as a gold line filling under it */
@Composable
internal fun ThinClock(at: Long, picks: Picks, scale: Float) {
    val t = now(at, picks.place.zone)
    Column(horizontalAlignment = Alignment.Start) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Look.ltr(Look.digits("${h12(t.h)}:${two(t.m)}", picks.latinDigits)),
                style = Look.style((110 * scale).sp, Look.Ink, FontWeight.Light).copy(lineHeight = (110 * scale).sp))
            Spacer(Modifier.width(10.dp * scale))
            Text(if (t.h < 12) "ص" else "م", style = Look.style((22 * scale).sp, Look.Gold, FontWeight.Light), modifier = Modifier.padding(bottom = 18.dp * scale))
        }
        Canvas(Modifier.padding(vertical = 12.dp * scale).width(300.dp * scale).height(3.dp * scale)) {
            val y = size.height / 2
            drawLine(Color.White.copy(alpha = .12f), Offset(0f, y), Offset(size.width, y), size.height, StrokeCap.Round)
            val f = (t.s + t.ms / 1000f) / 60f
            drawLine(Look.Gold, Offset(size.width, y), Offset(size.width * (1 - f), y), size.height, StrokeCap.Round)   // from the right
        }
        DateLine(at, picks, scale)
    }
}

/** each half in its own frosted tile, like a flip clock; a gold colon between, blinking with the seconds */
@Composable
internal fun TileClock(at: Long, picks: Picks, scale: Float) {
    val t = now(at, picks.place.zone)
    @Composable
    fun tile(text: String) {
        val shape = RoundedCornerShape(18.dp * scale)
        Box(
            Modifier.size(150.dp * scale, 170.dp * scale)
                .background(Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x14FFFFFF))), shape)
                .border(1.dp, Brush.verticalGradient(listOf(Color(0x40FFFFFF), Color(0x0DFFFFFF))), shape),
            contentAlignment = Alignment.Center,
        ) {
            Text(Look.digits(text, picks.latinDigits), style = Look.style((96 * scale).sp, Look.Ink, big = true))
            // the fold across the middle
            Box(Modifier.align(Alignment.Center).width(150.dp * scale).height(1.dp).background(Color(0x66000000)))
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Ltr { Row(verticalAlignment = Alignment.CenterVertically) {
            tile(two(h12(t.h)))
            Column(Modifier.padding(horizontal = 12.dp * scale), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp * scale)) {
                val on = t.s % 2 == 0
                repeat(2) { Box(Modifier.size(9.dp * scale).background(Look.Gold.copy(alpha = if (on) 1f else .3f), CircleShape)) }
            }
            tile(two(t.m))
        } }
        Spacer(Modifier.height(14.dp * scale))
        Text(if (t.h < 12) "صباحاً" else "مساءً", style = Look.style((16 * scale).sp, Look.Gold, FontWeight.Medium))
        Spacer(Modifier.height(6.dp * scale))
        DateLine(at, picks, scale, 16f)
    }
}

/** the hours bold in gold, the minutes light in white, side by side and set close, the dates under them */
@Composable
internal fun DuoClock(at: Long, picks: Picks, scale: Float) {
    val t = now(at, picks.place.zone)
    Column(horizontalAlignment = Alignment.Start) {
        Ltr { Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Look.digits(two(h12(t.h)), picks.latinDigits), style = Look.style((104 * scale).sp, Look.Gold, big = true))
            Box(Modifier.padding(horizontal = 14.dp * scale).width(2.dp * scale).height(84.dp * scale)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.White.copy(alpha = .5f), Color.Transparent))))
            Text(Look.digits(two(t.m), picks.latinDigits), style = Look.style((104 * scale).sp, Look.Ink, FontWeight.Light))
        } }
        Text(if (t.h < 12) "صباحاً" else "مساءً", style = Look.style((16 * scale).sp, Look.Gold, FontWeight.Medium))
        Spacer(Modifier.height(10.dp * scale))
        DateLine(at, picks, scale)
    }
}

// ================================================================ with hands

/** a tapered hand: diamond-shaped (dauphine) or a slim baton, from just behind the centre to the tip */
private fun DrawScope.hand(c: Offset, turns: Double, length: Float, width: Float, color: Color, diamond: Boolean, tail: Float = .12f) {
    rotate((turns * 360).toFloat(), c) {
        val r = size.minDimension / 2
        if (diamond) {
            val p = Path().apply {
                moveTo(c.x, c.y + r * tail)
                lineTo(c.x - width, c.y - r * length * .18f)
                lineTo(c.x, c.y - r * length)
                lineTo(c.x + width, c.y - r * length * .18f)
                close()
            }
            drawPath(p, color)
            // the light catches one half
            val lit = Path().apply { moveTo(c.x, c.y + r * tail); lineTo(c.x, c.y - r * length); lineTo(c.x + width, c.y - r * length * .18f); close() }
            drawPath(lit, Color.White.copy(alpha = .22f))
        } else {
            drawLine(color, Offset(c.x, c.y + r * tail), Offset(c.x, c.y - r * length), width, StrokeCap.Round)
        }
    }
}

private fun secondsTurn(t: Now) = (t.s + t.ms / 1000.0) / 60.0
private fun minutesTurn(t: Now) = (t.m + t.s / 60.0) / 60.0
private fun hoursTurn(t: Now) = ((t.h % 12) + t.m / 60.0) / 12.0

/** the Arabic hour figures round a dial, as text placed on a circle */
@Composable
private fun Figures(picks: Picks, dial: Dp, radius: Float, only: List<Int>, size: Float, color: Color, scale: Float) {
    only.forEach { n ->
        val a = 2 * PI * n / 12 - PI / 2
        val x = (cos(a) * radius * dial.value / 2).toFloat()
        val y = (sin(a) * radius * dial.value / 2).toFloat()
        Text(
            Look.digits(n.toString(), picks.latinDigits), style = Look.style((size * scale).sp, color, big = true).copy(shadow = null),
            textAlign = TextAlign.Center,
            modifier = Modifier.offset(x.dp, y.dp),
        )
    }
}

/**
 * The dress watch: a gold double bezel, a face of fine guilloché rings, Arabic figures at the quarters and gold
 * batons between, dauphine hands with a light edge, and a slim gold seconds hand with a crescent counterweight.
 */
@Composable
internal fun LuxuryDial(at: Long, picks: Picks, scale: Float) {
    val t = now(at, picks.place.zone)
    val dial = 230.dp * scale
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Ltr {
        Box(Modifier.size(dial), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(dial)) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(Color(0xFF2A2620), Color(0xFF14120F)), c, r), r, c)
                for (i in 1..14) drawCircle(Color.White.copy(alpha = .025f), r * .62f * i / 14, c, style = Stroke(1f))   // guilloché
                drawCircle(Brush.sweepGradient(listOf(Look.Gold, Color(0xFF8C6B33), Look.Gold, Color(0xFFF3D9A4), Look.Gold), c), r - 3.dp.toPx(), c, style = Stroke(5.dp.toPx()))
                drawCircle(Look.Gold.copy(alpha = .5f), r * .88f, c, style = Stroke(1.dp.toPx()))
                for (i in 0 until 60) {
                    val a = Math.toRadians(i * 6.0 - 90)
                    val hour = i % 5 == 0
                    if (hour && i % 15 == 0) continue   // the figures stand there
                    val from = r * if (hour) .74f else .84f
                    val to = r * .86f
                    drawLine(if (hour) Look.Gold else Color.White.copy(alpha = .35f),
                        Offset(c.x + cos(a).toFloat() * from, c.y + sin(a).toFloat() * from),
                        Offset(c.x + cos(a).toFloat() * to, c.y + sin(a).toFloat() * to),
                        (if (hour) 3.5f else 1f).dp.toPx(), StrokeCap.Round)
                }
            }
            Figures(picks, dial, .7f, listOf(12, 3, 6, 9), 26f, Look.Gold, scale)
            Canvas(Modifier.size(dial)) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2
                hand(c, hoursTurn(t), .5f, 7.dp.toPx(), Look.Gold, diamond = true)
                hand(c, minutesTurn(t), .78f, 5.dp.toPx(), Color(0xFFF3E3C0), diamond = true)
                rotate((secondsTurn(t) * 360).toFloat(), c) {
                    drawLine(Look.Gold, Offset(c.x, c.y + r * .2f), Offset(c.x, c.y - r * .84f), 1.5.dp.toPx(), StrokeCap.Round)
                    drawCircle(Look.Gold, 5.dp.toPx(), Offset(c.x, c.y + r * .2f))
                    drawCircle(Color(0xFF14120F), 4.2.dp.toPx(), Offset(c.x + 2.dp.toPx(), c.y + r * .2f - 1.dp.toPx()))   // the crescent
                }
                drawCircle(Look.Gold, 6.dp.toPx(), c)
                drawCircle(Color(0xFF14120F), 2.dp.toPx(), c)
            }
        }
        }
        Spacer(Modifier.height(14.dp * scale))
        DateLine(at, picks, scale, 16f)
    }
}

/**
 * The sports-luxury watch: a deep black face, long gold batons, slim hands with a luminous stripe, a small seconds
 * dial at six, and the date in a window at three.
 */
@Composable
internal fun BlackDial(at: Long, picks: Picks, scale: Float) {
    val t = now(at, picks.place.zone)
    val dial = 230.dp * scale
    val day = cityTime(at, picks.place.zone).get(Calendar.DAY_OF_MONTH)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Ltr {
        Box(Modifier.size(dial), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(dial)) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2
                drawCircle(Color(0xFF3A3A3E), r, c)
                drawCircle(Brush.radialGradient(listOf(Color(0xFF16161A), Color(0xFF050506)), c, r), r * .93f, c)
                for (i in 0 until 12) {
                    val a = Math.toRadians(i * 30.0 - 90)
                    val wide = i % 3 == 0
                    drawLine(Look.Gold, Offset(c.x + cos(a).toFloat() * r * .66f, c.y + sin(a).toFloat() * r * .66f),
                        Offset(c.x + cos(a).toFloat() * r * .86f, c.y + sin(a).toFloat() * r * .86f), (if (wide) 7f else 4f).dp.toPx(), StrokeCap.Butt)
                }
                // the small seconds at six
                val sc = Offset(c.x, c.y + r * .42f)
                drawCircle(Color.White.copy(alpha = .06f), r * .2f, sc)
                drawCircle(Color.White.copy(alpha = .25f), r * .2f, sc, style = Stroke(1.dp.toPx()))
                val sa = 2 * PI * secondsTurn(t) - PI / 2
                drawLine(Look.Gold, sc, Offset(sc.x + cos(sa).toFloat() * r * .17f, sc.y + sin(sa).toFloat() * r * .17f), 1.5.dp.toPx(), StrokeCap.Round)
                // the date window at three
                drawRoundRect(Color(0xFFF3EDE0), Offset(c.x + r * .5f, c.y - r * .09f), Size(r * .24f, r * .18f), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
                hand(c, hoursTurn(t), .5f, 7.dp.toPx(), Color(0xFFE9E4D8), diamond = false)
                hand(c, minutesTurn(t), .8f, 5.dp.toPx(), Color(0xFFE9E4D8), diamond = false)
                // the luminous stripes down the hands
                hand(c, hoursTurn(t), .46f, 2.5.dp.toPx(), Look.Gold, diamond = false, tail = -.12f)
                hand(c, minutesTurn(t), .76f, 2.dp.toPx(), Look.Gold, diamond = false, tail = -.12f)
                drawCircle(Color(0xFFE9E4D8), 6.dp.toPx(), c)
            }
            Text(Look.digits(day.toString(), picks.latinDigits), style = Look.style((12 * scale).sp, Color(0xFF111111), FontWeight.Bold).copy(shadow = null),
                modifier = Modifier.offset((dial.value * .31f).dp, 0.dp))
        }
        }
        Spacer(Modifier.height(14.dp * scale))
        DateLine(at, picks, scale, 16f)
    }
}

/**
 * The Arabian dial: an eight-pointed star in gold lines framing the face, all twelve Arabic figures round it,
 * slender gold hands, and a seconds hand that ends in a small crescent.
 */
@Composable
internal fun StarDial(at: Long, picks: Picks, scale: Float) {
    val t = now(at, picks.place.zone)
    val dial = 230.dp * scale
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Ltr {
        Box(Modifier.size(dial), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(dial)) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2
                drawCircle(Brush.radialGradient(listOf(Color(0x332A2416), Color(0x00000000)), c, r), r, c)
                for (turn in listOf(0f, 45f)) rotate(turn, c) {   // the khatam: two squares
                    val side = r * 1.4f
                    drawRect(Look.Gold.copy(alpha = .7f), Offset(c.x - side / 2, c.y - side / 2), Size(side, side), style = Stroke(1.5.dp.toPx()))
                }
                drawCircle(Look.Gold.copy(alpha = .35f), r * .62f, c, style = Stroke(1.dp.toPx()))
                drawCircle(Look.Gold.copy(alpha = .2f), r * .2f, c, style = Stroke(1.dp.toPx()))
            }
            Figures(picks, dial, .8f, (1..12).toList(), 15f, Look.Ink, scale)
            Canvas(Modifier.size(dial)) {
                val c = Offset(size.width / 2, size.height / 2)
                val r = size.minDimension / 2
                hand(c, hoursTurn(t), .42f, 5.dp.toPx(), Look.Gold, diamond = true)
                hand(c, minutesTurn(t), .62f, 3.5.dp.toPx(), Look.Gold, diamond = true)
                rotate((secondsTurn(t) * 360).toFloat(), c) {
                    drawLine(Color.White.copy(alpha = .8f), c, Offset(c.x, c.y - r * .66f), 1.dp.toPx(), StrokeCap.Round)
                    val tip = Offset(c.x, c.y - r * .7f)
                    drawCircle(Look.Gold, 5.dp.toPx(), tip)
                    drawCircle(Color(0xFF0C0D0F), 4.3.dp.toPx(), Offset(tip.x + 2.2.dp.toPx(), tip.y - 1.dp.toPx()))
                }
                drawCircle(Look.Gold, 5.dp.toPx(), c)
            }
        }
        }
        Spacer(Modifier.height(12.dp * scale))
        Text(clock12(at, picks.place.zone, picks.latinDigits), style = Look.style((22 * scale).sp, Look.Ink, big = true))
        DateLine(at, picks, scale, 15f)
    }
}
