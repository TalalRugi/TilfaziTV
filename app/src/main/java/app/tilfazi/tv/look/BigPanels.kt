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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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

/** Full-height panels (and compact SmallLeague), designed to float cleanly without cluttered inner boxes. */

@Composable
private fun Title(text: String, scale: Float, trailing: String? = null) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp * scale), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = Look.style((14 * scale).sp, Look.Gold, FontWeight.Medium), modifier = Modifier.weight(1f), maxLines = 1)
        if (trailing != null) Text(trailing, style = Look.style((12 * scale).sp, Look.Faint), maxLines = 1)
    }
}

// ================================================================ the prayer

/** the whole day: the five prayers and the sunrise, a row each; the next lit with its countdown, the past ones faint */
@Composable
internal fun BigPrayer(now: Long, picks: Picks, scale: Float) {
    val place = picks.place
    val today = Salah.day(now, picks)
    val line = if (today.any { it.at > now }) today else Salah.day(now + 86_400_000L, picks)
    val rise = Salah.sun(line[0].at, picks).first
    val rows = listOf(line[0], Salat("الشروق", rise)) + line.drop(1)
    val next = line.firstOrNull { it.at > now }
    Column(Modifier.fillMaxWidth()) {
        Title("مواقيت الصلاة", scale, if (line === today) weekday(now, place.zone) else "غداً")
        rows.forEach { s ->
            val on = s == next
            val past = s.at <= now
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp * scale, vertical = 5.dp * scale),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    s.name,
                    style = Look.style((16.5f * scale).sp, if (on) Look.Gold else if (past) Look.Faint else Look.Ink, if (on) FontWeight.Bold else FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                )
                if (on) Text(countdown(s.at - now, picks.latinDigits), style = Look.style((13 * scale).sp, Look.Gold, FontWeight.Medium), modifier = Modifier.padding(end = 14.dp * scale))
                Text(
                    clock12(s.at, place.zone, picks.latinDigits),
                    style = Look.style((16.5f * scale).sp, if (on) Look.Gold else if (past) Look.Faint else Look.Ink, if (on) FontWeight.Bold else FontWeight.Medium),
                )
            }
        }
    }
}

// ================================================================ the games (Sports Center)

@Composable
private fun ScoreLine(g: Fixture, picks: Picks, scale: Float) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Crest(g.homeBadge, 30.dp * scale)
        Spacer(Modifier.width(6.dp * scale))
        Text(
            g.home,
            style = Look.style((14 * scale).sp, Look.Ink, FontWeight.Medium),
            maxLines = 1,
            softWrap = false,
        )
        val big = Look.style((21 * scale).sp, Look.Ink, big = true)
        Row(Modifier.padding(horizontal = 9.dp * scale), verticalAlignment = Alignment.CenterVertically) {
            Text(Look.digits("${g.homeScore ?: 0}", picks.latinDigits), style = big)
            Text("-", style = Look.style((15 * scale).sp, Look.Soft), modifier = Modifier.padding(horizontal = 5.dp * scale))
            Text(Look.digits("${g.awayScore ?: 0}", picks.latinDigits), style = big)
        }
        Text(
            g.away,
            style = Look.style((14 * scale).sp, Look.Ink, FontWeight.Medium),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(6.dp * scale))
        Crest(g.awayBadge, 30.dp * scale)
    }
}

@Composable
private fun BigMatchBlock(game: Fixture, now: Long, picks: Picks, scale: Float, compact: Boolean = false) {
    val zone = picks.place.zone
    val live = isGameLive(game, now)
    val score = if (live) liveScoreText(game, picks) else null
    val crestSize = if (compact) 44.dp * scale else 54.dp * scale
    val haloSize = if (compact) 58.dp * scale else 72.dp * scale

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(bottom = 8.dp * scale),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .background(Color(0x24D4AF37), RoundedCornerShape(999.dp))
                    .border(0.5.dp, Look.Gold.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 8.dp * scale, vertical = 2.dp * scale)
            ) {
                Text(game.competition, style = Look.style((11 * scale).sp, Look.Gold, FontWeight.Medium), maxLines = 1)
            }
            if (live) {
                Box(
                    Modifier
                        .background(Color(0x33E53935), RoundedCornerShape(999.dp))
                        .border(0.5.dp, Look.Live.copy(alpha = 0.6f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp * scale, vertical = 2.dp * scale)
                ) {
                    Text(liveClockLabel(game, picks), style = Look.style((11 * scale).sp, Look.Live, FontWeight.Bold), maxLines = 1)
                }
            } else {
                Text(
                    weekday(game.kickoff, zone) + " " + gregorianDate(game.kickoff, zone, picks.latinDigits),
                    style = Look.style((11 * scale).sp, Look.Soft),
                )
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Crest(game.homeBadge, crestSize, haloSize)
                Spacer(Modifier.height(4.dp * scale))
                Text(game.home, style = Look.style((14 * scale).sp, Look.Ink, FontWeight.SemiBold), maxLines = 1)
            }
            Column(Modifier.padding(horizontal = 6.dp * scale), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (live) (score ?: "مباشر") else clock12(game.kickoff, zone, picks.latinDigits),
                    style = Look.style(((if (compact) 24 else 28) * scale).sp, if (live && score == null) Look.Live else Look.Ink, big = true),
                )
                Spacer(Modifier.height(2.dp * scale))
                Text(
                    if (live) game.statusArabic
                    else until(game.kickoff - now, picks.latinDigits),
                    style = Look.style((11.5f * scale).sp, if (live) Look.Live else Look.Gold, FontWeight.Medium),
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Crest(game.awayBadge, crestSize, haloSize)
                Spacer(Modifier.height(4.dp * scale))
                Text(game.away, style = Look.style((14 * scale).sp, Look.Ink, FontWeight.SemiBold), maxLines = 1)
            }
        }
    }
}

/**
 * Full-height Sports Center:
 * - When following both a Local Team and a World Team (2 upcoming games), displays both matches cleanly separated by a gold hairline.
 * - When following 1 team, displays the hero upcoming match and, under a gold hairline, the last result.
 */
@Composable
internal fun BigGames(next: Fixture?, board: Board, picks: Picks, scale: Float) {
    val zone = picks.place.zone
    val upcoming = nextGames(board, picks).ifEmpty { listOfNotNull(next) }
    val last = board.results.firstOrNull()
    val dual = upcoming.size >= 2

    Column(Modifier.fillMaxWidth()) {
        if (dual) {
            upcoming.take(2).forEachIndexed { i, g ->
                if (i > 0) {
                    Spacer(Modifier.height(12.dp * scale))
                    Hairline()
                    Spacer(Modifier.height(12.dp * scale))
                }
                BigMatchBlock(g, board.now, picks, scale, compact = true)
            }
        } else {
            upcoming.firstOrNull()?.let { g ->
                BigMatchBlock(g, board.now, picks, scale, compact = false)
            }
            if (last != null) {
                Spacer(Modifier.height(16.dp * scale))
                Hairline()
                Spacer(Modifier.height(14.dp * scale))
                Title("آخر نتيجة", scale, gregorianDate(last.kickoff, zone, picks.latinDigits))
                ScoreLine(last, picks, scale)
            }
        }
    }
}

// ================================================================ the league

/**
 * Six rows of the table round the followed club: rank, crest, club, played, points — floating cleanly with only the
 * followed club lit in soft gold.
 */
@Composable
internal fun BigLeague(
    table: Table,
    picks: Picks,
    scale: Float,
    clubId: String? = picks.clubId,
    totalTables: Int = 1,
    tableIndex: Int = 0,
    isBrowsing: Boolean = false,
    focused: Boolean = false,
    scrollOffset: Int = 0,
) {
    fun n(v: Int) = Look.digits(v.toString(), picks.latinDigits)
    val followed = picks.clubIds.toSet()
    val targetId = clubId ?: picks.clubIds.firstOrNull { id -> table.rows.any { it.id == id } }
    val rowCount = 6

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp * scale), verticalAlignment = Alignment.CenterVertically) {
            Text(table.league, style = Look.style((14 * scale).sp, Look.Gold, FontWeight.Medium), maxLines = 1, modifier = Modifier.weight(1f))
            if (totalTables > 1) {
                Row(modifier = Modifier.padding(horizontal = 8.dp * scale), horizontalArrangement = Arrangement.spacedBy(5.dp * scale), verticalAlignment = Alignment.CenterVertically) {
                    repeat(totalTables) { idx ->
                        val active = idx == tableIndex
                        Box(Modifier.size(5.dp * scale).background(if (active) Look.Gold else Color(0x40FFFFFF), CircleShape))
                    }
                }
            }
            if (isBrowsing) {
                Text("◀ تصفح الدوريات ▶", style = Look.style((11.5f * scale).sp, Look.Gold, FontWeight.Bold))
            } else if (focused) {
                Text("اضغط OK للتصفح", style = Look.style((11 * scale).sp, Look.Soft))
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp * scale, vertical = 2.dp * scale)) {
            Text("", modifier = Modifier.weight(1f))
            Text("لعب", style = Look.style((11 * scale).sp, Look.Faint), modifier = Modifier.width(40.dp * scale), textAlign = TextAlign.Center)
            Text("نقاط", style = Look.style((11 * scale).sp, Look.Faint), modifier = Modifier.width(40.dp * scale), textAlign = TextAlign.Center)
        }
        val at = table.rows.indexOfFirst { it.id == targetId }
        val defaultFrom = if (at < 0) 0 else (at - 2).coerceIn(0, (table.rows.size - rowCount).coerceAtLeast(0))
        val maxFrom = (table.rows.size - rowCount).coerceAtLeast(0)
        val from = if (isBrowsing) scrollOffset.coerceIn(0, maxFrom) else defaultFrom
        table.rows.drop(from).take(rowCount).forEach { r ->
            val on = r.id in followed
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp * scale, vertical = 5.dp * scale),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    n(r.rank),
                    style = Look.style((14 * scale).sp, if (on) Look.Gold else Look.Soft, if (on) FontWeight.Bold else FontWeight.Medium),
                    modifier = Modifier.width(24.dp * scale),
                )
                Crest(r.badge, 24.dp * scale)
                Spacer(Modifier.width(9.dp * scale))
                Text(
                    r.name,
                    style = Look.style((15 * scale).sp, if (on) Look.Gold else Look.Ink, if (on) FontWeight.Bold else FontWeight.Medium),
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    n(r.played),
                    style = Look.style((13.5f * scale).sp, if (on) Look.Gold else Look.Soft, if (on) FontWeight.Bold else FontWeight.Normal),
                    modifier = Modifier.width(40.dp * scale),
                    textAlign = TextAlign.Center,
                )
                Text(
                    n(r.points),
                    style = Look.style((15.5f * scale).sp, if (on) Look.Gold else Look.Ink, big = true),
                    modifier = Modifier.width(40.dp * scale),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Compact 3-row mini table around the followed club when placed in a half-height slot */
@Composable
internal fun SmallLeague(
    table: Table,
    picks: Picks,
    scale: Float,
    clubId: String? = picks.clubId,
    totalTables: Int = 1,
    tableIndex: Int = 0,
    isBrowsing: Boolean = false,
    focused: Boolean = false,
    scrollOffset: Int = 0,
) {
    fun n(v: Int) = Look.digits(v.toString(), picks.latinDigits)
    val followed = picks.clubIds.toSet()
    val targetId = clubId ?: picks.clubIds.firstOrNull { id -> table.rows.any { it.id == id } }
    val at = table.rows.indexOfFirst { it.id == targetId }
    val defaultFrom = if (at < 0) 0 else (at - 1).coerceIn(0, (table.rows.size - 3).coerceAtLeast(0))
    val maxFrom = (table.rows.size - 3).coerceAtLeast(0)
    val from = if (isBrowsing) scrollOffset.coerceIn(0, maxFrom) else defaultFrom

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp * scale), verticalAlignment = Alignment.CenterVertically) {
            Text(table.league, style = Look.style((12.5f * scale).sp, Look.Gold, FontWeight.Medium), modifier = Modifier.weight(1f), maxLines = 1)
            if (totalTables > 1) {
                Row(modifier = Modifier.padding(horizontal = 6.dp * scale), horizontalArrangement = Arrangement.spacedBy(4.dp * scale), verticalAlignment = Alignment.CenterVertically) {
                    repeat(totalTables) { idx ->
                        val active = idx == tableIndex
                        Box(Modifier.size(4.dp * scale).background(if (active) Look.Gold else Color(0x40FFFFFF), CircleShape))
                    }
                }
            }
            if (isBrowsing) {
                Text("◀ تصفح ▶", style = Look.style((11 * scale).sp, Look.Gold, FontWeight.Bold), modifier = Modifier.padding(end = 4.dp * scale))
            } else if (focused) {
                Text("OK", style = Look.style((10.5f * scale).sp, Look.Soft), modifier = Modifier.padding(end = 4.dp * scale))
            }
            Text("لعب", style = Look.style((10.5f * scale).sp, Look.Faint), modifier = Modifier.width(34.dp * scale), textAlign = TextAlign.Center)
            Text("نقاط", style = Look.style((10.5f * scale).sp, Look.Faint), modifier = Modifier.width(36.dp * scale), textAlign = TextAlign.Center)
        }
        table.rows.drop(from).take(3).forEach { r ->
            val on = r.id in followed
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp * scale, vertical = 3.5.dp * scale),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(n(r.rank), style = Look.style((13 * scale).sp, if (on) Look.Gold else Look.Soft, if (on) FontWeight.Bold else FontWeight.Medium), modifier = Modifier.width(22.dp * scale))
                Crest(r.badge, 22.dp * scale)
                Spacer(Modifier.width(8.dp * scale))
                Text(r.name, style = Look.style((14 * scale).sp, if (on) Look.Gold else Look.Ink, if (on) FontWeight.Bold else FontWeight.Medium), maxLines = 1, modifier = Modifier.weight(1f))
                Text(n(r.played), style = Look.style((12.5f * scale).sp, if (on) Look.Gold else Look.Soft, if (on) FontWeight.Bold else FontWeight.Normal), modifier = Modifier.width(34.dp * scale), textAlign = TextAlign.Center)
                Text(n(r.points), style = Look.style((14.5f * scale).sp, if (on) Look.Gold else Look.Ink, big = true), modifier = Modifier.width(36.dp * scale), textAlign = TextAlign.Center)
            }
        }
    }
}

// ================================================================ the weather

/** now large, the coming hours, then the next three days */
@Composable
internal fun BigSky(sky: Sky, picks: Picks, scale: Float) {
    val zone = picks.place.zone
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SkyGlyph(sky.code, sky.day, 52.dp * scale)
            Spacer(Modifier.width(14.dp * scale))
            Text(deg(sky.temp, picks), style = Look.style((42 * scale).sp, big = true))
            Spacer(Modifier.width(14.dp * scale))
            Column(Modifier.weight(1f)) {
                Text(skyWord(sky.code).ifEmpty { "الطقس" }, style = Look.style((16 * scale).sp, Look.Ink, FontWeight.Medium), maxLines = 1)
                Text(picks.place.name, style = Look.style((13 * scale).sp, Look.Gold), maxLines = 1)
            }
            Text(Look.ltr("↑ ${deg(sky.high, picks)}  ↓ ${deg(sky.low, picks)}"), style = Look.style((13.5f * scale).sp, Look.Soft))
        }
        val soon = sky.hours.filter { it.at > System.currentTimeMillis() }.take(5)
        if (soon.isNotEmpty()) {
            Spacer(Modifier.height(12.dp * scale))
            Row(Modifier.fillMaxWidth()) {
                soon.forEach { h ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        val hour = cityTime(h.at, zone).get(Calendar.HOUR_OF_DAY)
                        Text(
                            Look.digits("${if (hour % 12 == 0) 12 else hour % 12}", picks.latinDigits) + if (hour < 12) " ص" else " م",
                            style = Look.style((11 * scale).sp, Look.Faint),
                        )
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
                    Text(weekday(d.at + 12 * 3_600_000L, zone), style = Look.style((13.5f * scale).sp, Look.Ink, FontWeight.Medium), modifier = Modifier.weight(1f))
                    SkyGlyph(d.code, true, 22.dp * scale)
                    Spacer(Modifier.width(16.dp * scale))
                    Text(
                        Look.ltr("${deg(d.high, picks)}  ${deg(d.low, picks)}"),
                        style = Look.style((13.5f * scale).sp, Look.Soft),
                        modifier = Modifier.width(86.dp * scale),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
