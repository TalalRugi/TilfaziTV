package app.tilfazi.tv.look

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Picks
import org.json.JSONArray
import java.util.TimeZone

/** A verse: its text, its surah, and its first and last verse numbers. */
class Verse(val text: String, val surah: String, val from: Int, val to: Int)

/** A hadith: the words, the companion who narrated them, and where they are recorded. */
class Hadith(val text: String, val narrator: String, val source: String)

/**
 * The day's verse and hadith, from lists shipped with the app (assets/words, see SOURCES.md): the same for the whole
 * day in the picked city, a new one each day. A small place only takes those short enough to sit well in it.
 */
object Words {
    private var verses: List<Verse>? = null
    private var hadith: List<Hadith>? = null

    private fun read(c: Context, name: String) = JSONArray(c.assets.open("words/$name").bufferedReader().readText())

    private fun verses(c: Context) = verses ?: read(c, "verses.json").let { a ->
        (0 until a.length()).map { a.getJSONObject(it) }.map { Verse(it.getString("t"), it.getString("s"), it.getInt("a"), it.getInt("b")) }
    }.also { verses = it }

    private fun hadith(c: Context) = hadith ?: read(c, "hadith.json").let { a ->
        (0 until a.length()).map { a.getJSONObject(it) }.map { Hadith(it.getString("t"), it.getString("n"), it.getString("s")) }
    }.also { hadith = it }

    /** days since 1970 in the city: the day changes at its midnight */
    private fun day(now: Long, zone: String) = ((now + TimeZone.getTimeZone(zone).getOffset(now)) / 86_400_000L).toInt()

    /** today's pick; for a small place, today's or the next one after it that is short enough */
    private fun <T> pick(list: List<T>, now: Long, zone: String, max: Int, length: (T) -> Int): T? {
        if (list.isEmpty()) return null
        val start = day(now, zone).mod(list.size)
        return (0 until list.size).map { list[(start + it) % list.size] }.firstOrNull { length(it) <= max } ?: list[start]
    }

    fun verse(c: Context, now: Long, zone: String, large: Boolean) = pick(verses(c), now, zone, if (large) 330 else 120) { it.text.length }
    fun hadith(c: Context, now: Long, zone: String, large: Boolean) = pick(hadith(c), now, zone, if (large) 260 else 110) { it.text.length }
}

/**
 * Text that never runs out of its room: it is laid out at [start] and, while it overflows [maxLines], drawn a size
 * smaller (down to 60%), so a long verse is smaller rather than cut.
 */
@Composable
fun FittedText(text: String, style: TextStyle, maxLines: Int, modifier: Modifier = Modifier) {
    var factor by remember(text, maxLines) { mutableFloatStateOf(1f) }
    Text(
        text,
        style = style.copy(fontSize = style.fontSize * factor, lineHeight = style.fontSize * factor * 1.65f),
        maxLines = maxLines,
        textAlign = TextAlign.Center,
        onTextLayout = { if (it.hasVisualOverflow && factor > .6f) factor *= .9f },
        modifier = modifier,
    )
}

/** a small eight-pointed star in gold lines */
@Composable
private fun Star(size: Float, alpha: Float = .85f) {
    Canvas(Modifier.size(size.dp)) {
        val c = Offset(this.size.width / 2, this.size.height / 2)
        val side = this.size.minDimension * .62f
        for (turn in listOf(0f, 45f)) rotate(turn, c) {
            drawRect(Look.Gold.copy(alpha = alpha), Offset(c.x - side / 2, c.y - side / 2), Size(side, side), style = Stroke(1.2.dp.toPx()))
        }
    }
}

@Composable
private fun Divider(scale: Float) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Hairline(Modifier.weight(1f))
        Box(Modifier.padding(horizontal = 10.dp * scale)) { Star(13 * scale) }
        Hairline(Modifier.weight(1f))
    }
}

@Composable
private fun SpiritualHeader(title: String, sourcePill: String, scale: Float) {
    val pill = RoundedCornerShape(50)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Star(12 * scale, .9f)
        Spacer(Modifier.width(7.dp * scale))
        Text(title, style = Look.style((12 * scale).sp, Look.Gold, FontWeight.Medium), modifier = Modifier.weight(1f))
        Box(
            Modifier
                .background(Brush.verticalGradient(listOf(Color(0x540A0C12), Color(0x380A0C12))), pill)
                .border(1.dp, Look.Gold.copy(alpha = 0.35f), pill)
                .padding(horizontal = 10.dp * scale, vertical = 2.5.dp * scale),
        ) {
            Text(sourcePill, style = Look.style((10.5f * scale).sp, Look.Soft, FontWeight.Medium), maxLines = 1)
        }
    }
}

/** the day's verse: Bento card with header pill and centred Amiri calligraphy */
@Composable
fun VerseWidget(now: Long, picks: Picks, scale: Float, large: Boolean) {
    val c = LocalContext.current
    val v = remember(now / 3_600_000L, picks.place.zone, large) { Words.verse(c, now, picks.place.zone, large) } ?: return
    val numbers = Look.digits(if (v.to > v.from) "${v.from}–${v.to}" else "${v.from}", picks.latinDigits)
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp * scale), horizontalAlignment = Alignment.CenterHorizontally) {
        SpiritualHeader("آية اليوم", "سورة ${v.surah} · $numbers", scale)
        Spacer(Modifier.height((if (large) 14 else 8).dp * scale))
        val body = (if (v.to > v.from) "" else "﴿ ") + v.text + (if (v.to > v.from) "" else " ﴾")
        FittedText(
            body,
            TextStyle(fontFamily = Look.scripture, fontSize = ((if (large) 25 else 20) * scale).sp, color = Look.Ink),
            maxLines = if (large) 7 else 3,
            modifier = Modifier.fillMaxWidth(),
        )
        if (large) {
            Spacer(Modifier.height(14.dp * scale))
            Divider(scale)
        }
    }
}

/** the day's hadith: Bento card with header pill and centred Amiri calligraphy */
@Composable
fun HadithWidget(now: Long, picks: Picks, scale: Float, large: Boolean) {
    val c = LocalContext.current
    val h = remember(now / 3_600_000L, picks.place.zone, large) { Words.hadith(c, now, picks.place.zone, large) } ?: return
    val two = h.narrator.split(' ').any { it.startsWith("و") } || h.narrator.startsWith("عبد الله بن عمر") || h.narrator.startsWith("الحسن")
    val honour = if (two) "رضي الله عنهما" else "رضي الله عنه"
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp * scale), horizontalAlignment = Alignment.CenterHorizontally) {
        SpiritualHeader("حديث اليوم", "رواه ${h.source}", scale)
        Spacer(Modifier.height((if (large) 14 else 6).dp * scale))
        FittedText(
            "«${h.text}»",
            TextStyle(fontFamily = Look.scripture, fontSize = ((if (large) 23 else 19) * scale).sp, color = Look.Ink),
            maxLines = if (large) 6 else 3,
            modifier = Modifier.fillMaxWidth(),
        )
        if (large) {
            Spacer(Modifier.height(12.dp * scale))
            Divider(scale)
            Text(
                "عن ${h.narrator} $honour",
                style = Look.style((11.5f * scale).sp, Look.Soft),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp * scale),
            )
        }
    }
}
