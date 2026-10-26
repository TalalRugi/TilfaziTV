package app.tilfazi.tv.look

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Fixture
import app.tilfazi.tv.feed.Picks
import app.tilfazi.tv.feed.Salah
import app.tilfazi.tv.feed.Salat
import app.tilfazi.tv.feed.Sky
import app.tilfazi.tv.feed.Table
import java.util.Calendar

/** the large add-ons: each fills about two thirds of the column, or all of it when alone */

@Composable
private fun Title(text: String, scale: Float, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp * scale), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Look.style((14 * scale).sp, Look.Gold, FontWeight.Medium), modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, style = Look.style((12 * scale).sp, Look.Faint))
    }
}

/** a row lit in soft gold: the next prayer, the followed club */
private fun Modifier.lit(on: Boolean, scale: Float): Modifier {
    if (!on) return this
    val shape = RoundedCornerShape(12.dp * scale)
    return background(Brush.horizontalGradient(listOf(Look.Gold.copy(alpha = .26f), Look.Gold.copy(alpha = .06f))), shape)
        .border(1.dp, Look.Gold.copy(alpha = .45f), shape)
}

// ================================================================ the prayer

/** the whole day: the five prayers and the sunrise, a row each; the next lit with its countdown, the past ones faint */
@Composable
internal fun BigPrayer(now: Long, picks: Picks, scale: Float) {
    val place = picks.place
    val today = Salah.day(now, place)
    val line = if (today.any { it.at > now }) today else Salah.day(now + 86_400_000L, place)
    val rise = Salah.sun(line[0].at, place).first
    val rows = listOf(line[0], Salat("الشروق", rise)) + line.drop(1)
    val next = line.firstOrNull { it.at > now }
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp * scale)) {
        Title("مواقيت الصلاة", scale, if (line === today) weekday(now, place.zone) else "غداً")
        rows.forEach { s ->
            val on = s == next
            val past = s.at <= now
            Row(
                Modifier.fillMaxWidth().lit(on, scale).padding(horizontal = 12.dp * scale, vertical = 5.dp * scale),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(s.name, style = Look.style((17 * scale).sp, if (on) Look.Gold else if (past) Look.Faint else Look.Ink, FontWeight.Medium),
                    modifier = Modifier.weight(1f))
                if (on) Text(countdown(s.at - now, picks.latinDigits), style = Look.style((13 * scale).sp, Look.Gold), modifier = Modifier.padding(end = 14.dp * scale))
                Text(clock12(s.at, place.zone, picks.latinDigits), style = Look.style((17 * scale).sp, if (past && !on) Look.Faint else Look.Ink, FontWeight.Medium))
            }
        }
    }
}

// ================================================================ the games

@Composable
private fun ScoreLine(g: Fixture, picks: Picks, scale: Float) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Crest(g.homeBadge, 28.dp * scale); Spacer(Modifier.width(8.dp * scale))
            Text(g.home, style = Look.style((15 * scale).sp, Look.Ink, FontWeight.Medium), maxLines = 1)
        }
        // each score by its own club, read from the right like the names: home score, a dash, away score
        val big = Look.style((22 * scale).sp, Look.Ink, big = true)
        Row(Modifier.padding(horizontal = 10.dp * scale), verticalAlignment = Alignment.CenterVertically) {
            Text(Look.digits("${g.homeScore ?: 0}", picks.latinDigits), style = big)
            Text("-", style = Look.style((18 * scale).sp, Look.Soft), modifier = Modifier.padding(horizontal = 8.dp * scale))
            Text(Look.digits("${g.awayScore ?: 0}", picks.latinDigits), style = big)
        }
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            Text(g.away, style = Look.style((15 * scale).sp, Look.Ink, FontWeight.Medium), maxLines = 1)
            Spacer(Modifier.width(8.dp * scale)); Crest(g.awayBadge, 28.dp * scale)
        }
    }
}

/** the last result, the next game large, and the club's place in its league */
@Composable
internal fun BigGames(next: Fixture?, board: Board, picks: Picks, scale: Float) {
    val zone = picks.place.zone
    val last = board.results.firstOrNull()
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp * scale)) {
        if (next != null) {
            Title("المباراة القادمة", scale, next.competition)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Crest(next.homeBadge, 46.dp * scale, 60.dp * scale)
                    Text(next.home, style = Look.style((14 * scale).sp, Look.Soft, FontWeight.Medium), maxLines = 1)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val live = next.kickoff <= board.now
                    Text(if (live) "مباشر" else clock12(next.kickoff, zone, picks.latinDigits), style = Look.style((24 * scale).sp, if (live) Look.Live else Look.Ink, big = true))
                    Text(weekday(next.kickoff, zone) + " " + gregorianDate(next.kickoff, zone, picks.latinDigits), style = Look.style((12 * scale).sp, Look.Soft))
                    if (!live) Text(until(next.kickoff - board.now, picks.latinDigits), style = Look.style((12 * scale).sp, Look.Gold))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Crest(next.awayBadge, 46.dp * scale, 60.dp * scale)
                    Text(next.away, style = Look.style((14 * scale).sp, Look.Soft, FontWeight.Medium), maxLines = 1)
                }
            }
        }
        if (last != null) {
            Spacer(Modifier.height(12.dp * scale))
            Hairline()
            Spacer(Modifier.height(10.dp * scale))
            Title("آخر نتيجة", scale, last.competition + "  " + gregorianDate(last.kickoff, zone, picks.latinDigits))
            ScoreLine(last, picks, scale)
        }
        val table = board.table
        val mine = table?.rows?.firstOrNull { it.id == picks.clubId }
        if (table != null && mine != null) {
            Spacer(Modifier.height(10.dp * scale))
            Text(
                "المركز ${Look.ltr(Look.digits(mine.rank.toString(), picks.latinDigits))} في ${table.league}، ${Look.digits(mine.points.toString(), picks.latinDigits)} نقطة",
                style = Look.style((13 * scale).sp, Look.Gold, FontWeight.Medium), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ================================================================ the league

/** six rows of the table round the followed club (the top six when it is near the top): rank, crest, club, played, points */
@Composable
internal fun BigLeague(table: Table, picks: Picks, scale: Float) {
    fun n(v: Int) = Look.digits(v.toString(), picks.latinDigits)
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp * scale)) {
        Title(table.league, scale)
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp * scale, vertical = 2.dp * scale)) {
            Text("", modifier = Modifier.weight(1f))
            Text("لعب", style = Look.style((11 * scale).sp, Look.Faint), modifier = Modifier.width(44.dp * scale), textAlign = TextAlign.Center)
            Text("نقاط", style = Look.style((11 * scale).sp, Look.Faint), modifier = Modifier.width(44.dp * scale), textAlign = TextAlign.Center)
        }
        val at = table.rows.indexOfFirst { it.id == picks.clubId }
        val from = if (at < 0) 0 else (at - 2).coerceIn(0, (table.rows.size - 6).coerceAtLeast(0))
        table.rows.drop(from).take(6).forEach { r ->
            val on = r.id == picks.clubId
            Row(
                Modifier.fillMaxWidth().lit(on, scale).padding(horizontal = 12.dp * scale, vertical = 5.dp * scale),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(n(r.rank), style = Look.style((15 * scale).sp, if (on) Look.Gold else Look.Soft, FontWeight.Medium), modifier = Modifier.width(24.dp * scale))
                Crest(r.badge, 24.dp * scale)
                Spacer(Modifier.width(10.dp * scale))
                Text(r.name, style = Look.style((16 * scale).sp, if (on) Look.Gold else Look.Ink, FontWeight.Medium), maxLines = 1, modifier = Modifier.weight(1f))
                Text(n(r.played), style = Look.style((14 * scale).sp, Look.Soft), modifier = Modifier.width(44.dp * scale), textAlign = TextAlign.Center)
                Text(n(r.points), style = Look.style((16 * scale).sp, Look.Ink, big = true), modifier = Modifier.width(44.dp * scale), textAlign = TextAlign.Center)
            }
        }
    }
}

/** one line: the followed club's place, or the leader when the club is not in the top rows */
@Composable
internal fun SmallLeague(table: Table, picks: Picks, scale: Float) {
    fun n(v: Int) = Look.digits(v.toString(), picks.latinDigits)
    val mine = table.rows.firstOrNull { it.id == picks.clubId }
    val shown = mine ?: table.rows.first()
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp * scale), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(56.dp * scale), contentAlignment = Alignment.Center) {
            Text(n(shown.rank), style = Look.style((40 * scale).sp, Look.Gold, big = true))
        }
        Crest(shown.badge, 34.dp * scale)
        Spacer(Modifier.width(12.dp * scale))
        Column(Modifier.weight(1f)) {
            Text(shown.name, style = Look.style((17 * scale).sp, Look.Ink, FontWeight.Medium))
            Text(table.league, style = Look.style((12 * scale).sp, Look.Soft))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(n(shown.points), style = Look.style((24 * scale).sp, Look.Ink, big = true))
            Text("نقطة", style = Look.style((11 * scale).sp, Look.Faint))
        }
    }
}

// ================================================================ the weather

/** now large, the coming hours, then the next three days */
@Composable
internal fun BigSky(sky: Sky, picks: Picks, scale: Float) {
    val zone = picks.place.zone
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp * scale)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkyGlyph(sky.code, sky.day, 56.dp * scale)
            Spacer(Modifier.width(14.dp * scale))
            Text(deg(sky.temp, picks), style = Look.style((44 * scale).sp, big = true))
            Spacer(Modifier.width(14.dp * scale))
            Column(Modifier.weight(1f)) {
                Text(skyWord(sky.code).ifEmpty { "الطقس" }, style = Look.style((16 * scale).sp, Look.Ink, FontWeight.Medium))
                Text(picks.place.name, style = Look.style((13 * scale).sp, Look.Gold))
            }
            Text(Look.ltr("↑ ${deg(sky.high, picks)}  ↓ ${deg(sky.low, picks)}"), style = Look.style((14 * scale).sp, Look.Soft))
        }
        val soon = sky.hours.filter { it.at > System.currentTimeMillis() }.take(5)
        if (soon.isNotEmpty()) {
            Spacer(Modifier.height(12.dp * scale))
            Row(Modifier.fillMaxWidth()) {
                soon.forEach { h ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        val hour = cityTime(h.at, zone).get(Calendar.HOUR_OF_DAY)
                        Text(Look.digits("${if (hour % 12 == 0) 12 else hour % 12}", picks.latinDigits) + if (hour < 12) " ص" else " م",
                            style = Look.style((11 * scale).sp, Look.Faint))
                        SkyGlyph(h.code, h.day, 24.dp * scale)
                        Text(deg(h.temp, picks), style = Look.style((13 * scale).sp, Look.Ink, FontWeight.Medium))
                    }
                }
            }
        }
        val days = sky.days.drop(1).take(3)
        if (days.isNotEmpty()) {
            Spacer(Modifier.height(10.dp * scale))
            Hairline()
            days.forEach { d ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp * scale), verticalAlignment = Alignment.CenterVertically) {
                    Text(weekday(d.at + 12 * 3_600_000L, zone), style = Look.style((14 * scale).sp, Look.Ink, FontWeight.Medium), modifier = Modifier.weight(1f))
                    SkyGlyph(d.code, true, 22.dp * scale)
                    Spacer(Modifier.width(18.dp * scale))
                    Text(Look.ltr("${deg(d.high, picks)}  ${deg(d.low, picks)}"), style = Look.style((14 * scale).sp, Look.Soft),
                        modifier = Modifier.width(90.dp * scale), textAlign = TextAlign.End)
                }
            }
        }
    }
}
