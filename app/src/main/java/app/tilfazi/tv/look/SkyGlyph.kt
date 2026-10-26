package app.tilfazi.tv.look

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

/** What the sky looks like, from Open-Meteo's WMO code. */
private enum class Kind { Clear, Partly, Cloud, Fog, Rain, Snow, Storm }

private fun kind(code: Int) = when (code) {
    0, 1 -> Kind.Clear
    2 -> Kind.Partly
    3 -> Kind.Cloud
    45, 48 -> Kind.Fog
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Kind.Rain
    71, 73, 75, 77, 85, 86 -> Kind.Snow
    95, 96, 99 -> Kind.Storm
    else -> Kind.Partly
}

private val Sun = Color(0xFFFFC857)
private val Moon = Color(0xFFF2E6C9)
private val CloudFill = Color(0xFFE9ECF1)
private val Drop = Color(0xFF8EC5FF)

/**
 * The weather as a small drawing: the sun (its rays turn slowly) or the moon at night, a cloud that drifts a little,
 * drops, flakes, fog bands or a bolt. Drawn in code, so it needs no image files.
 */
@Composable
fun SkyGlyph(code: Int, day: Boolean, size: Dp) {
    val k = kind(code)
    var turn = 0f
    var drift = 0f
    if (!Look.still) {
        val motion = rememberInfiniteTransition(label = "sky")
        turn = motion.animateFloat(0f, 360f, infiniteRepeatable(tween(40_000, easing = LinearEasing)), label = "turn").value
        drift = motion.animateFloat(-1f, 1f, infiniteRepeatable(tween(5_000), RepeatMode.Reverse), label = "drift").value
    }
    Canvas(Modifier.size(size).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        val u = this.size.minDimension
        when (k) {
            Kind.Clear -> if (day) sun(Offset(u * .5f, u * .5f), u * .2f, turn) else moon(Offset(u * .5f, u * .5f), u * .26f)
            Kind.Partly -> {
                if (day) sun(Offset(u * .62f, u * .36f), u * .15f, turn) else moon(Offset(u * .62f, u * .34f), u * .18f)
                cloud(Offset(u * .44f + drift * u * .03f, u * .62f), u * .62f)
            }
            Kind.Cloud -> {
                cloud(Offset(u * .58f - drift * u * .02f, u * .42f), u * .46f, CloudFill.copy(alpha = .55f))
                cloud(Offset(u * .46f + drift * u * .03f, u * .6f), u * .66f)
            }
            Kind.Fog -> repeat(4) { i ->
                val y = u * (.32f + i * .13f)
                val shift = if (i % 2 == 0) drift else -drift
                drawLine(CloudFill.copy(alpha = .85f - i * .12f), Offset(u * .16f + shift * u * .04f, y), Offset(u * .84f + shift * u * .04f, y), u * .055f, StrokeCap.Round)
            }
            Kind.Rain, Kind.Snow, Kind.Storm -> {
                cloud(Offset(u * .5f, u * .4f), u * .66f)
                val fall = (turn / 360f * 40f) % 1f   // the drops loop down
                for (i in 0 until 3) {
                    val x = u * (.34f + i * .16f)
                    val y = u * (.68f + ((fall + i * .33f) % 1f) * .16f)
                    when (k) {
                        Kind.Snow -> drawCircle(Color.White, u * .035f, Offset(x, y))
                        else -> drawLine(Drop, Offset(x, y), Offset(x - u * .03f, y + u * .07f), u * .035f, StrokeCap.Round)
                    }
                }
                if (k == Kind.Storm) bolt(u)
            }
        }
    }
}

private fun DrawScope.sun(c: Offset, r: Float, turn: Float) {
    rotate(turn, c) {
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val from = Offset(c.x + cos(a).toFloat() * r * 1.45f, c.y + sin(a).toFloat() * r * 1.45f)
            val to = Offset(c.x + cos(a).toFloat() * r * 1.9f, c.y + sin(a).toFloat() * r * 1.9f)
            drawLine(Sun, from, to, r * .28f, StrokeCap.Round)
        }
    }
    drawCircle(Sun, r, c)
}

private fun DrawScope.moon(c: Offset, r: Float) {
    drawCircle(Moon, r, c)
    drawCircle(Color.Black, r * .92f, Offset(c.x + r * .5f, c.y - r * .3f), blendMode = BlendMode.Clear)   // cut the crescent out
}

/** a cloud whose bottom edge is flat, centred on [c] and [w] wide */
private fun DrawScope.cloud(c: Offset, w: Float, color: Color = CloudFill) {
    val h = w * .36f
    drawRoundRect(color, Offset(c.x - w / 2, c.y - h * .1f), Size(w, h * .6f), CornerRadius(h * .3f))
    drawCircle(color, h * .42f, Offset(c.x - w * .2f, c.y))
    drawCircle(color, h * .58f, Offset(c.x + w * .06f, c.y - h * .12f))
    drawCircle(color, h * .36f, Offset(c.x + w * .3f, c.y + h * .06f))
}

private fun DrawScope.bolt(u: Float) {
    val p = androidx.compose.ui.graphics.Path().apply {
        moveTo(u * .54f, u * .56f); lineTo(u * .42f, u * .76f); lineTo(u * .52f, u * .76f)
        lineTo(u * .46f, u * .94f); lineTo(u * .64f, u * .7f); lineTo(u * .54f, u * .7f); close()
    }
    drawPath(p, Sun)
}
