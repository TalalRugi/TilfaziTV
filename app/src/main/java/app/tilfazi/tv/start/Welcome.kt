package app.tilfazi.tv.start

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.PI
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Choices
import app.tilfazi.tv.feed.Clubs
import app.tilfazi.tv.feed.Places
import app.tilfazi.tv.home.MenuRow
import app.tilfazi.tv.home.okKeys
import androidx.compose.foundation.focusable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.ui.focus.onFocusChanged
import app.tilfazi.tv.look.Glass
import app.tilfazi.tv.look.Look
import app.tilfazi.tv.look.Tints
import app.tilfazi.tv.setup.Chips
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The first-run screens: a welcome, then the two things that matter (the city and the team), and a finish. Making
 * tilfazi the TV's home is done for the owner when it is installed; everything else waits for the settings.
 * Each step slides in from the left (forward in Arabic); Back goes one step back.
 */
@Composable
fun Welcome(onDone: () -> Unit) {
    val picks by Choices.picks.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    val last = 5
    BackHandler(enabled = step in 1 until last) { step-- }
    fun next() { if (step < last) step++ }
    val tint by animateColorAsState(Tints.of(picks.looks.tint).color, tween(900), label = "tint")

    Box(Modifier.fillMaxSize().background(Look.NightLow)) {
        // as on the TV's own setup: a plain dark screen, a faint colour from the top corner and nothing else
        // on the colour and glow steps it is the preview itself, at full strength
        Glass(tint, 170.dp, strength = if (step == 3 || step == 4) 1f else .35f, place = if (step == 4) picks.looks.glow else 0)
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally(tween(550, easing = FastOutSlowInEasing)) { w -> if (forward) -w / 4 else w / 4 } + fadeIn(tween(450)))
                    .togetherWith(slideOutHorizontally(tween(450)) { w -> if (forward) w / 4 else -w / 4 } + fadeOut(tween(300)))
            },
            label = "step",
        ) { s ->
            when (s) {
                0 -> Hello { next() }
                1 -> Step("أين تسكن؟", "لمواقيت الصلاة والطقس", 1) {
                    Pick(Places.all.map { it.name }, Places.all.indexOfFirst { it.key == picks.place.key }.coerceAtLeast(0), perRow = 6) {
                        Choices.setPlace(Places.all[it].key); next()
                    }
                }
                2 -> Step("من تشجّع؟", "نعرض لك موعد مباراته القادمة", 2) {
                    val names = Clubs.followable.map { it.name } + "لا أتابع"
                    val now = Clubs.followable.indexOfFirst { it.id == picks.clubId }.let { if (it < 0) names.lastIndex else it }
                    Pick(names, now, perRow = 5) { i -> Choices.setClub(Clubs.followable.getOrNull(i)?.id); next() }
                }
                3 -> Step("اختر لونك", "توهّج زجاجي خلف الواجهة: تنقّل بين الألوان وشاهده يتغيّر خلفك", 3) {
                    LivePick(Tints.all.map { it.name }, picks.looks.tint, { Choices.setLook("tint", it) }) { next() }
                }
                4 -> Step("من أين يتوهّج؟", "اختر موضع اللون على الشاشة، وشاهده يتحرك أمامك", 4) {
                    LivePick(app.tilfazi.tv.look.GLOW_PLACES, picks.looks.glow, { Choices.setLook("glow", it) }) { next() }
                }
                else -> Ready(onDone)
            }
        }

    }
}

// ---------------------------------------------------------------- the welcome

/** the crescent draws itself in gold, its glow breathes, then the name rises letter-soft into place */
@Composable
private fun Hello(onStart: () -> Unit) {
    val draw = remember { Animatable(0f) }
    val name = remember { Animatable(0f) }
    val rest = remember { Animatable(0f) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        launch { draw.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
        delay(900)
        launch { name.animateTo(1f, spring(dampingRatio = .7f, stiffness = 120f)) }
        delay(700)
        rest.animateTo(1f, tween(700))
        runCatching { focus.requestFocus() }
    }
    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(.55f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "b")
    Row(Modifier.fillMaxSize().padding(horizontal = 96.dp, vertical = 80.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Canvas(Modifier.size(84.dp).graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; alpha = .4f + .6f * draw.value }) {
                drawMark(draw.value)
            }
            Spacer(Modifier.height(34.dp))
            Text("مرحباً بك في تلفازي", style = Look.style(52.sp, Look.Ink, big = true),
                modifier = Modifier.graphicsLayer { alpha = name.value; translationY = (1 - name.value) * 30f })
            Spacer(Modifier.height(14.dp))
            Text("واجهة عربية لتلفزيونك: الوقت والصلاة والطقس ومباريات فريقك وتطبيقاتك، في شاشة واحدة.",
                style = Look.style(20.sp, Look.Soft), modifier = Modifier.width(460.dp).graphicsLayer { alpha = rest.value })
        }
        Column(Modifier.width(340.dp).graphicsLayer { alpha = rest.value }) {
            Box(Modifier.focusRequester(focus)) { Row2("ابدأ", onStart) }
        }
    }
}

// ---------------------------------------------------------------- the steps

/** One step as on the TV's own setup: the question and a line under it on the right, the choices in a list on the left. */
@Composable
private fun Step(title: String, line: String, number: Int = 0, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxSize().padding(start = 96.dp, end = 72.dp, top = 80.dp, bottom = 80.dp)) {
        Column(Modifier.weight(1f).padding(end = 48.dp, top = 40.dp)) {
            if (number > 0) Text(Look.digits("$number من 4", false), style = Look.style(16.sp, Look.Faint, FontWeight.Medium),
                modifier = Modifier.padding(bottom = 16.dp))
            Text(title, style = Look.style(46.sp, Look.Ink, big = true))
            Spacer(Modifier.height(14.dp))
            Text(line, style = Look.style(20.sp, Look.Soft), modifier = Modifier.width(400.dp))
        }
        Box(Modifier.width(380.dp).fillMaxHeight()) { content() }
    }
}

/** one line of a setup list: grey at rest, white with dark text under the focus, as on Google TV */
@Composable
private fun Row2(name: String, onOk: () -> Unit, modifier: Modifier = Modifier, on: Boolean = false, onFocus: () -> Unit = {}) {
    var focused by remember { androidx.compose.runtime.mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth()
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }
            .okKeys(onOk, onOk)
            .focusable()
            .background(if (focused) Color(0xFFF1F3F4) else Color(0x1AFFFFFF), androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(horizontal = 22.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = Look.style(19.sp, if (focused) Color(0xFF202124) else Look.Ink, FontWeight.Medium).copy(shadow = null), modifier = Modifier.weight(1f))
        if (on) Text("✓", style = Look.style(18.sp, if (focused) Color(0xFF202124) else Look.Gold, FontWeight.Bold).copy(shadow = null))
    }
}

/**
 * Choices that show themselves: the one under the focus is put in force at once, so the screen behind changes as the
 * owner moves; «التالي» (or OK on a choice) keeps it and moves on.
 */
@Composable
private fun LivePick(names: List<String>, current: Int, onTry: (Int) -> Unit, onNext: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(350); runCatching { focus.requestFocus() } }
    Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        names.forEachIndexed { i, name ->
            Row2(name, onNext, if (i == current) Modifier.focusRequester(focus) else Modifier, on = i == current, onFocus = { onTry(i) })
        }
        Spacer(Modifier.height(10.dp))
        Row2("التالي ←", onNext)
    }
}

/** the choices as a list, the current one ticked and focused at once; OK picks and moves on */
@Composable
private fun Pick(names: List<String>, current: Int, perRow: Int, onPick: (Int) -> Unit) {
    val focus = remember { FocusRequester() }
    val list = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = (current - 2).coerceAtLeast(0))
    LaunchedEffect(Unit) { delay(350); runCatching { focus.requestFocus() } }
    androidx.compose.foundation.lazy.LazyColumn(state = list, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(names.size) { i ->
            Row2(names[i], { onPick(i) }, if (i == current) Modifier.focusRequester(focus) else Modifier, on = i == current)
        }
    }
}

/**
 * The mark (as in res/drawable/mark.xml), in a 100-unit box: a gold tile grows in, the «ت» bowl draws itself across
 * it, then its two dots drop into place.
 */
private fun DrawScope.drawMark(t: Float) {
    val u = size.minDimension / 100f
    fun p(x: Float, y: Float) = Offset(x * u, y * u)
    val tile = ((t) / .35f).coerceIn(0f, 1f)
    val inset = 50f * (1 - tile)
    drawRoundRect(
        Brush.linearGradient(listOf(Color(0xFFF3D9A4), Look.Gold, Color(0xFF9C7438)), p(0f, 0f), p(100f, 100f)),
        topLeft = p(inset, inset), size = androidx.compose.ui.geometry.Size((100 - 2 * inset) * u, (100 - 2 * inset) * u),
        cornerRadius = CornerRadius(22 * u * tile),
    )
    val ink = Color(0xFF14110C)
    val bowl = Path().apply { moveTo(22 * u, 44 * u); cubicTo(22 * u, 66 * u, 34 * u, 76 * u, 50 * u, 76 * u); cubicTo(66 * u, 76 * u, 78 * u, 66 * u, 78 * u, 44 * u) }
    val trace = ((t - .3f) / .45f).coerceIn(0f, 1f)
    if (trace > 0f) {
        val part = Path()
        PathMeasure().apply { setPath(bowl, false); getSegment(0f, length * trace, part, true) }
        drawPath(part, ink, style = Stroke(9 * u, cap = StrokeCap.Round))
    }
    for ((k, x) in listOf(42f, 58f).withIndex()) {
        val d = ((t - .75f - k * .08f) / .15f).coerceIn(0f, 1f)
        if (d <= 0f) continue
        val y = 28f - (1 - d) * 14f
        val dot = Path().apply { moveTo(x * u, (y - 6) * u); lineTo((x + 6) * u, y * u); lineTo(x * u, (y + 6) * u); lineTo((x - 6) * u, y * u); close() }
        drawPath(dot, ink.copy(alpha = d))
    }
}

/** a gold check that draws itself in a ring of sparks, then home */
@Composable
private fun Ready(onDone: () -> Unit) {
    val check = remember { Animatable(0f) }
    val burst = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { burst.animateTo(1f, tween(1400, easing = FastOutSlowInEasing)) }
        check.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        delay(1400)
        Choices.setOnboarded(true)
        onDone()
    }
    val sparks = remember { val r = Random(7); List(26) { r.nextFloat() * 6.283f to (.6f + r.nextFloat() * .4f) } }
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Canvas(Modifier.size(220.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.minDimension * .22f
            sparks.forEach { (a, far) ->
                val d = r * (1.2f + 1.3f * burst.value * far)
                drawCircle(Look.Gold.copy(alpha = (1 - burst.value) * .9f), 3.dp.toPx() * far, Offset(c.x + cos(a) * d, c.y + sin(a) * d))
            }
            drawCircle(Look.Gold.copy(alpha = .18f), r * 1.15f, c)
            drawArc(Look.Gold, -90f, 360f * check.value, false, Offset(c.x - r, c.y - r), androidx.compose.ui.geometry.Size(r * 2, r * 2), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            val p1 = Offset(c.x - r * .45f, c.y + r * .02f)
            val p2 = Offset(c.x - r * .1f, c.y + r * .36f)
            val p3 = Offset(c.x + r * .5f, c.y - r * .32f)
            val t = check.value
            if (t > 0f) drawLine(Look.Ink, p1, Offset(p1.x + (p2.x - p1.x) * (t * 2).coerceAtMost(1f), p1.y + (p2.y - p1.y) * (t * 2).coerceAtMost(1f)), 5.dp.toPx(), StrokeCap.Round)
            if (t > .5f) { val u = (t - .5f) * 2; drawLine(Look.Ink, p2, Offset(p2.x + (p3.x - p2.x) * u, p2.y + (p3.y - p2.y) * u), 5.dp.toPx(), StrokeCap.Round) }
        }
        Text("كل شيء جاهز", style = Look.style(50.sp, Look.Ink, big = true), modifier = Modifier.graphicsLayer { alpha = check.value })
        Text("الباقي تجده في «إعدادات تلفازي» متى أردت", style = Look.style(18.sp, Look.Soft), textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { alpha = check.value })
    }
}

// ---------------------------------------------------------------- small pieces

/** where we are: a dot per step, the current one a gold pill */
@Composable
private fun Dots(at: Int, count: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val w by animateFloatAsState(if (i == at) 26f else 8f, spring(stiffness = 300f), label = "dot")
            Box(Modifier.size(w.dp, 8.dp).background(if (i <= at) Look.Gold else Color(0x40FFFFFF), CircleShape))
        }
    }
}

/** faint gold motes drifting upward across the whole screen */
@Composable
private fun Sparks() {
    val motes = remember { val r = Random(1448); List(40) { Triple(r.nextFloat(), r.nextFloat(), .5f + r.nextFloat()) } }
    val t by rememberInfiniteTransition(label = "motes").animateFloat(0f, 1f, infiniteRepeatable(tween(30_000, easing = LinearEasing)), label = "t")
    Canvas(Modifier.fillMaxSize()) {
        motes.forEach { (x, y, s) ->
            val yy = ((y - t * s) % 1f + 1f) % 1f
            val a = .10f + .25f * sin(yy * 3.14f)
            drawCircle(Look.Gold.copy(alpha = a), 1.6.dp.toPx() * s, Offset(x * size.width, yy * size.height))
        }
    }
}

