package app.tilfazi.tv.look

import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** the backdrop's motions, as the settings name them; 0 is none */
val MOTIONS = listOf("بدون", "ثلج", "غبار ذهبي", "يراعات", "رمال", "نجوم ثمانية", "تموّج", "مضلعات زجاجية", "شبكة نقاط", "أشعة ضوء", "بوكيه")
val DENSITIES = listOf("خفيفة", "متوسطة", "كثيفة")

/** one moving thing: where it starts (0–1), its size and speed (0.5–1.5), and its phase */
private class Mote(val x: Float, val y: Float, val size: Float, val speed: Float, val phase: Float)

private fun lighter(c: Color, t: Float) = Color(c.red + (1 - c.red) * t, c.green + (1 - c.green) * t, c.blue + (1 - c.blue) * t)

/**
 * The glass backdrop's motion: drawn over the glow, in white or a light shade of the backdrop's colour, and always
 * quiet. [density] 0–2 sets how many; [strength] (lower at night) dims it with the glow.
 */
@Composable
fun BackdropMotion(kind: Int, density: Int, color: Color, strength: Float = 1f, modifier: Modifier = Modifier.fillMaxSize()) {
    if (kind <= 0 || Look.still) return
    val count = listOf(.5f, 1f, 1.8f)[density.coerceIn(0, 2)]
    val motes = remember(kind, density) {
        val r = Random((kind * 31 + density).toLong() xor 0x0637064406270644L)
        List((40 * count).toInt()) { Mote(r.nextFloat(), r.nextFloat(), .5f + r.nextFloat(), .5f + r.nextFloat(), r.nextFloat() * 6.283f) }
    }
    val t by rememberInfiniteTransition(label = "motion").animateFloat(0f, 1f, infiniteRepeatable(tween(120_000, easing = LinearEasing)), label = "t")
    val tint = lighter(color, .55f)
    val a = strength.coerceIn(.3f, 1f)
    val soft = kind == 10 && Build.VERSION.SDK_INT >= 31
    Canvas(modifier.then(if (soft) Modifier.blur(10.dp) else Modifier)) {
        val time = t * 120f   // seconds through the two-minute loop
        when (kind) {
            1 -> snow(motes, time, a)
            2 -> dust(motes, time, a)
            3 -> fireflies(motes, time, tint, a)
            4 -> sand(motes, time, tint, a)
            5 -> stars(motes, time, tint, a)
            6 -> ripples(time, tint, a, count)
            7 -> shards(motes, time, tint, a)
            8 -> dots(time, tint, a, count)
            9 -> rays(time, tint, a, count)
            10 -> bokeh(motes, time, tint, a)
        }
    }
}

/** a 0–1 position that loops [laps] times through the two-minute cycle */
private fun loop(start: Float, time: Float, laps: Float) = ((start + time / 120f * laps) % 1f + 1f) % 1f

private fun DrawScope.snow(m: List<Mote>, time: Float, a: Float) = m.forEach { p ->
    val y = loop(p.y, time, 6f * p.speed) * (size.height + 40f) - 20f
    val x = p.x * size.width + sin(time * .8f * p.speed + p.phase) * 24.dp.toPx()
    drawCircle(Color.White.copy(alpha = .7f * a * (.5f + p.size / 3)), 2.2.dp.toPx() * p.size, Offset(x, y))
}

private fun DrawScope.dust(m: List<Mote>, time: Float, a: Float) = m.forEach { p ->
    val y = (1 - loop(p.y, time, 3f * p.speed)) * size.height
    val twinkle = .4f + .6f * ((sin(time * 2f * p.speed + p.phase) + 1) / 2)
    val at = Offset(p.x * size.width + sin(time * .3f + p.phase) * 10f, y)
    drawCircle(Look.Gold.copy(alpha = .18f * a * twinkle), 6.dp.toPx() * p.size, at)
    drawCircle(lighter(Look.Gold, .4f).copy(alpha = .9f * a * twinkle), 2.2.dp.toPx() * p.size, at)
}

private fun DrawScope.fireflies(m: List<Mote>, time: Float, c: Color, a: Float) = m.take(m.size / 2 + 1).forEach { p ->
    val x = (p.x + sin(time * .11f * p.speed + p.phase) * .08f) * size.width
    val y = (p.y + cos(time * .09f * p.speed + p.phase) * .08f) * size.height
    val glow = ((sin(time * 1.2f * p.speed + p.phase) + 1) / 2)
    drawCircle(c.copy(alpha = .22f * a * glow), 10.dp.toPx() * p.size, Offset(x, y))
    drawCircle(Color.White.copy(alpha = .8f * a * glow), 2.dp.toPx(), Offset(x, y))
}

private fun DrawScope.sand(m: List<Mote>, time: Float, c: Color, a: Float) = m.forEach { p ->
    val x = loop(p.x, time, 10f * p.speed) * (size.width + 200f) - 100f
    val y = p.y * size.height + sin(time * .5f + p.phase) * 14f
    val len = 40.dp.toPx() * p.size
    drawLine(Brush.horizontalGradient(listOf(Color.Transparent, lighter(c, .3f).copy(alpha = .35f * a)), x - len, x),
        Offset(x - len, y), Offset(x, y + len * .06f), 1.2.dp.toPx(), StrokeCap.Round)
}

private fun DrawScope.stars(m: List<Mote>, time: Float, c: Color, a: Float) = m.take(m.size / 3 + 2).forEach { p ->
    val center = Offset((p.x + sin(time * .05f + p.phase) * .03f) * size.width, (1 - loop(p.y, time, .8f * p.speed)) * size.height)
    val side = 26.dp.toPx() * p.size
    rotate(time * 6f * p.speed + p.phase * 50f, center) {
        for (turn in listOf(0f, 45f)) rotate(turn, center) {
            drawRect(c.copy(alpha = .28f * a), Offset(center.x - side / 2, center.y - side / 2), Size(side, side), style = Stroke(1.dp.toPx()))
        }
    }
}

private fun DrawScope.ripples(time: Float, c: Color, a: Float, count: Float) {
    val origin = Offset(size.width * .9f, size.height * .08f)   // where the glow is strongest
    val rings = (4 * count).toInt().coerceAtLeast(2)
    for (i in 0 until rings) {
        val f = loop(i.toFloat() / rings, time, 12f)
        drawCircle(c.copy(alpha = .3f * a * (1 - f)), size.width * .9f * f, origin, style = Stroke(1.5.dp.toPx()))
    }
}

private fun DrawScope.shards(m: List<Mote>, time: Float, c: Color, a: Float) = m.take(m.size / 3 + 2).forEach { p ->
    val center = Offset((p.x + sin(time * .04f + p.phase) * .05f) * size.width, (p.y + cos(time * .05f + p.phase) * .05f) * size.height)
    val r = 34.dp.toPx() * p.size
    rotate(time * 3f * p.speed + p.phase * 40f, center) {
        val path = Path().apply {
            moveTo(center.x, center.y - r); lineTo(center.x + r * .87f, center.y + r * .5f); lineTo(center.x - r * .87f, center.y + r * .5f); close()
        }
        drawPath(path, Brush.linearGradient(listOf(Color.White.copy(alpha = .10f * a), c.copy(alpha = .04f * a)),
            Offset(center.x, center.y - r), Offset(center.x, center.y + r)))
        drawPath(path, Color.White.copy(alpha = .16f * a), style = Stroke(1.dp.toPx()))
    }
}

private fun DrawScope.dots(time: Float, c: Color, a: Float, count: Float) {
    val step = (54 / count.coerceAtLeast(.6f)).dp.toPx()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) {
            val wave = sin(x / size.width * 6f + y / size.height * 4f - time * .6f)
            drawCircle(c.copy(alpha = (.12f + .2f * (wave + 1) / 2) * a), 1.6.dp.toPx() * (1 + wave * .4f), Offset(x, y + wave * 5f))
            x += step
        }
        y += step
    }
}

private fun DrawScope.rays(time: Float, c: Color, a: Float, count: Float) {
    val beams = (5 * count).toInt().coerceAtLeast(3)
    for (i in 0 until beams) {
        val sway = sin(time * .15f + i * 1.3f)
        val x = size.width * (.45f + i * (.55f / beams)) + sway * 30.dp.toPx()
        val w = (60 + i * 17 % 50).dp.toPx()
        val path = Path().apply {
            moveTo(x, 0f); lineTo(x + w, 0f); lineTo(x + w - size.height * .55f, size.height); lineTo(x - size.height * .55f - w * .4f, size.height); close()
        }
        drawPath(path, Brush.verticalGradient(listOf(lighter(c, .5f).copy(alpha = (.10f + .05f * sway) * a), Color.Transparent), 0f, size.height * .9f))
    }
}

private fun DrawScope.bokeh(m: List<Mote>, time: Float, c: Color, a: Float) = m.take(m.size / 3 + 2).forEach { p ->
    val center = Offset((p.x + sin(time * .03f * p.speed + p.phase) * .06f) * size.width, (1 - loop(p.y, time, .5f * p.speed)) * size.height)
    val r = 38.dp.toPx() * p.size
    val pulse = .7f + .3f * ((sin(time * .5f + p.phase) + 1) / 2)
    drawCircle(lighter(c, .4f).copy(alpha = .16f * a * pulse), r, center)
    drawCircle(Color.White.copy(alpha = .08f * a * pulse), r, center, style = Stroke(1.5.dp.toPx()))
}
