package app.tilfazi.tv.look

import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Choices
import app.tilfazi.tv.feed.Looks
import app.tilfazi.tv.feed.Place
import app.tilfazi.tv.feed.Salah

/** A colour for the glass backdrop, with its name in the settings. */
data class Tint(val name: String, val color: Color)

object Tints {
    val all = listOf(
        Tint("ذهبي", Color(0xFFC8963E)),
        Tint("أزرق ملكي", Color(0xFF2F6FED)),
        Tint("زمردي", Color(0xFF1F9D74)),
        Tint("عنابي", Color(0xFF9E2B4A)),
        Tint("بنفسجي", Color(0xFF6C4BD8)),
        Tint("فيروزي", Color(0xFF1FA3B3)),
        Tint("غروب", Color(0xFFE0703A)),
        Tint("فحمي", Color(0xFF5B6270)),
    )

    fun of(i: Int) = all[i.coerceIn(0, all.lastIndex)]
}

/** the photos' five blur levels and their names, from none to very soft; the glass is always at its softest */
object Blurs {
    val names = listOf("بدون", "خفيف", "متوسط", "قوي", "قوي جداً")
    private val photo = listOf(0.dp, 3.dp, 6.dp, 11.dp, 18.dp)
    val GLASS = 170.dp
    fun photo(level: Int): Dp = photo[level.coerceIn(0, 4)]
}

/**
 * The backdrop, in one of two modes:
 * - glass: the chosen colour glows strongly from the top corner (the right, where Arabic starts) and fades away towards
 *   the bottom of the screen, softened by the chosen blur, under a faint frost;
 * - photo: an Unsplash photo, blurred as chosen, under a dark wash (the glass when there is no photo yet).
 */
@Composable
fun Scenery(board: Board, looks: Looks, place: Place, dimmer: Float = 0f, content: @Composable BoxScope.() -> Unit) {
    // from Maghrib to Fajr the glow is turned down further, so a dark room is not lit by it
    val night = remember(board.now / 60_000L, place) {
        val day = Salah.day(board.now, place)
        board.now < day.first().at || board.now >= day[3].at
    }
    val strength by animateFloatAsState(if (night) .55f else 1f, tween(3000), label = "night")
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Look.NightTop, Look.NightMid, Look.NightLow)))
    ) {
        val photo = if (looks.backdrop == 1) board.photo else null   // the glass until the first photo is in
        if (photo == null) {
            Glass(Tints.of(looks.tint).color, Blurs.GLASS, strength = strength, place = looks.glow)
            BackdropMotion(looks.motion, looks.density, Tints.of(looks.tint).color, strength)
        }
        Crossfade(targetState = photo, animationSpec = tween(1800), label = "photo") { bmp ->
            if (bmp != null) {
                val soft = Blurs.photo(looks.blur)
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (Build.VERSION.SDK_INT >= 31 && soft > 0.dp) Modifier.blur(soft) else Modifier),
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(0f to Color(0xC7000000), 0.3f to Color(0x99000000), 0.6f to Color(0x8C000000), 1f to Color(0xD9000000))
                    )
                )
            }
        }
        if (dimmer > 0f) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dimmer)))
        if (photo != null && looks.photos == 2 && board.photoBy.isNotBlank()) {
            Text(
                "تصوير ${board.photoBy} · Unsplash",
                style = Look.style(9.sp, Look.Faint),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 8.dp),
            )
        }
        content()
    }
}

/**
 * The glass mode: a deep, quiet version of the colour pooled in the top corner and blurred into one soft glow, a
 * frost over all with a faint light along the top edge. [strength] turns the glow down (at night).
 */
@Composable
fun Glass(
    color: Color,
    soft: Dp,
    modifier: Modifier = Modifier.fillMaxSize(),
    strength: Float = 1f,
    place: Int = Choices.picks.value.looks.glow,
) {
    val deep = deepen(color)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val blurred = Build.VERSION.SDK_INT >= 31
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().then(if (blurred) Modifier.blur(soft) else Modifier)) {
            val w = size.width
            val h = size.height
            // x is measured from the right (where Arabic starts) for the right corner, from the left for the left one
            val fromRight = (place == GLOW_RIGHT) == rtl
            fun at(x: Float, y: Float) = Offset(if (fromRight) w - x else x, y)
            fun mid(dx: Float, y: Float) = Offset(w / 2 + dx, y)
            // the glow's heart, then where the softer discs around it sit
            val discs: List<Triple<Offset, Float, Float>> = when (place) {   // centre, radius, alpha
                GLOW_CENTRE -> listOf(
                    Triple(mid(0f, h * .5f), w * .26f, .40f), Triple(mid(-w * .16f, h * .42f), w * .18f, .22f),
                    Triple(mid(w * .15f, h * .6f), w * .18f, .20f), Triple(mid(0f, h * .48f), w * .08f, .14f),
                )
                GLOW_TOP -> listOf(
                    Triple(mid(0f, -h * .14f), w * .34f, .46f), Triple(mid(-w * .2f, h * .04f), w * .2f, .24f),
                    Triple(mid(w * .2f, h * .04f), w * .2f, .24f), Triple(mid(0f, 0f), w * .1f, .14f),
                )
                else -> listOf(
                    Triple(at(w * .02f, -h * .12f), w * .32f, .46f), Triple(at(w * .28f, h * .06f), w * .24f, .26f),
                    Triple(at(w * .08f, h * .02f), w * .1f, .14f), Triple(at(0f, h * .55f), w * .28f, .12f),
                )
            }
            if (blurred) {
                discs.forEachIndexed { i, (c, r, a) -> drawCircle((if (i == 2 && place != GLOW_CENTRE || i == 3 && place == GLOW_CENTRE) color else deep).copy(alpha = a * strength), r, c) }
            } else {   // no blur before Android 12: the same glow as a gradient
                val (c, _, _) = discs.first()
                drawRect(Brush.radialGradient(listOf(deep.copy(alpha = .45f * strength), deep.copy(alpha = .15f * strength), Color.Transparent), c, w * .7f))
            }
        }
        // the frost: barely there, a little lighter at the top, darker at the bottom where the apps are
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x0DFFFFFF), Color(0x03FFFFFF), Color(0x40000000)))))
        // light caught along the glass's top edge
        Box(Modifier.fillMaxWidth().height(1.dp).background(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x1FFFFFFF), Color(0x00FFFFFF)))))
    }
}

/** where the glass's colour glows from, as the settings name them */
const val GLOW_RIGHT = 0
const val GLOW_LEFT = 1
const val GLOW_CENTRE = 2
const val GLOW_TOP = 3
val GLOW_PLACES = listOf("يمين", "يسار", "وسط الشاشة", "الوسط أعلى")

/** the colour a little darker, keeping its hue: a glow, not a light */
private fun deepen(c: Color) = Color(c.red * .85f, c.green * .85f, c.blue * .85f, 1f)
