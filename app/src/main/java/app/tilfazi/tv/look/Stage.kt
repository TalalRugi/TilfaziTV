package app.tilfazi.tv.look

import android.icu.text.SimpleDateFormat
import android.icu.util.ULocale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import app.tilfazi.tv.feed.ADDON_GAME
import app.tilfazi.tv.feed.ADDON_HADITH
import app.tilfazi.tv.feed.ADDON_LEAGUE
import app.tilfazi.tv.feed.ADDON_NONE
import app.tilfazi.tv.feed.ADDON_PRAYER
import app.tilfazi.tv.feed.ADDON_SKY
import app.tilfazi.tv.feed.ADDON_VERSE
import app.tilfazi.tv.feed.Fixture
import app.tilfazi.tv.feed.LAYOUT_LARGE_BOTTOM
import app.tilfazi.tv.feed.LAYOUT_LARGE_TOP
import app.tilfazi.tv.feed.LAYOUT_ONE
import app.tilfazi.tv.feed.LAYOUT_THREE
import app.tilfazi.tv.feed.Picks
import app.tilfazi.tv.feed.Sky
import app.tilfazi.tv.feed.placesOf
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Compact clock & dual-date capsule for the top header bar of the Home Screen, freeing the main stage for the
 * full-width Bento widget dashboard.
 */
@Composable
fun TopClockPill(now: Long, picks: Picks, modifier: Modifier = Modifier) {
    val zone = picks.place.zone
    val k = cityTime(now, zone)
    val h = k.get(Calendar.HOUR_OF_DAY)
    val m = k.get(Calendar.MINUTE)
    val h12 = if (h % 12 == 0) 12 else h % 12
    val timeText = Look.ltr(Look.digits("$h12:${two(m)}", picks.latinDigits))
    val amPm = if (h < 12) "ص" else "م"
    val hijri = weekday(now, zone) + " " + hijriDate(now, zone, picks.latinDigits, year = false)
    val greg = gregorianDate(now, zone, picks.latinDigits, year = false)

    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(timeText, style = Look.style(26.sp, Look.Ink, big = true))
        Spacer(Modifier.width(6.dp))
        Text(amPm, style = Look.style(14.sp, Look.Gold, FontWeight.Medium))
        Box(
            Modifier
                .padding(horizontal = 14.dp)
                .width(1.dp)
                .height(16.dp)
                .background(Look.Gold.copy(alpha = 0.38f))
        )
        Text(hijri, style = Look.style(15.sp, Look.Ink, FontWeight.Medium))
        Box(
            Modifier
                .padding(horizontal = 10.dp)
                .size(4.dp)
                .background(Look.Gold.copy(alpha = 0.85f), CircleShape)
        )
        Text(greg, style = Look.style(14.sp, Look.Soft))
    }
}

/**
 * On the Home Screen ([showClock] = false): a full-width Smart Bento Grid (1, 2, or 3 columns depending on active
 * widgets) so every widget renders at full native scale with zero crowding.
 * On the Screensaver ([showClock] = true): the artistic [ClockFace] on the right and Bento cards on the left.
 */
@Composable
fun Stage(
    board: Board,
    picks: Picks,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    panelWidth: Dp = 440.dp,
    showClock: Boolean = false,
    leagueFocusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    isBrowsingLeague: Boolean = false,
    onLeagueClick: (() -> Unit)? = null,
    onLeagueFocusChanged: ((Boolean) -> Unit)? = null,
    leagueIndex: Int = 0,
    leagueScroll: Int = 0,
) {
    val cardsMode = picks.looks.cards.coerceIn(0, 1)
    val floating = cardsMode == 0
    val slots = placesOf(picks.looks.layout)
    val active = slots.indices.mapNotNull { i ->
        val kind = picks.looks.addons.getOrElse(i) { ADDON_NONE }
        if (hasAddon(kind, board, large = true)) kind else null
    }.distinct()

    var leagueCardFocused by remember { mutableStateOf(false) }
    val leagueInteractionSource = remember { MutableInteractionSource() }

    fun leagueModifier(base: Modifier): Modifier {
        if (leagueFocusRequester == null) return base
        val shape = RoundedCornerShape(22.dp * scale)
        val styleMod = if (isBrowsingLeague) {
            Modifier
                .clip(shape)
                .background(Look.Gold.copy(alpha = 0.12f), shape)
                .border(2.dp, Look.Gold, shape)
        } else if (leagueCardFocused) {
            Modifier
                .clip(shape)
                .background(Look.Gold.copy(alpha = 0.06f), shape)
                .border(2.dp, Look.Gold.copy(alpha = 0.95f), shape)
        } else {
            Modifier
        }
        val clickMod = if (onLeagueClick != null) {
            Modifier.clickable(
                interactionSource = leagueInteractionSource,
                indication = null,
                onClick = onLeagueClick,
            )
        } else {
            Modifier.focusable()
        }
        return base
            .then(styleMod)
            .focusRequester(leagueFocusRequester)
            .onFocusChanged { state ->
                leagueCardFocused = state.isFocused
                onLeagueFocusChanged?.invoke(state.isFocused)
            }
            .then(clickMod)
    }

    if (showClock) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            ShrinkToFit { ClockFace(picks.looks.clock, board.now, picks, scale) }
            val shown = active.take(2)
            Column(
                Modifier.width(panelWidth * scale).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(if (floating) 6.dp * scale else 12.dp * scale, Alignment.CenterVertically),
            ) {
                shown.forEachIndexed { idx, kind ->
                    if (idx > 0 && floating) Hairline()
                    BentoCard(
                        scale = scale,
                        cardsMode = cardsMode,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) {
                        Addon(kind, board, picks, scale, large = shown.size == 1)
                    }
                }
            }
        }
        return
    }

    @Composable
    fun RenderAddon(kind: Int, large: Boolean, modifier: Modifier) {
        val isLeague = kind == ADDON_LEAGUE
        val cardMod = if (isLeague) leagueModifier(modifier) else modifier
        BentoCard(scale, cardsMode, cardMod) {
            Addon(
                kind = kind,
                board = board,
                picks = picks,
                scale = scale,
                large = large,
                isBrowsingLeague = if (isLeague) isBrowsingLeague else false,
                focusedLeague = if (isLeague) leagueCardFocused else false,
                leagueIndex = if (isLeague) leagueIndex else 0,
                leagueScroll = if (isLeague) leagueScroll else 0,
            )
        }
    }

    @Composable
    fun RenderStacked(kinds: List<Int>, modifier: Modifier) {
        if (floating) {
            Column(
                modifier,
                verticalArrangement = Arrangement.Center,
            ) {
                kinds.forEachIndexed { idx, kind ->
                    if (idx > 0) {
                        Spacer(Modifier.height(16.dp * scale))
                        Hairline()
                        Spacer(Modifier.height(16.dp * scale))
                    }
                    RenderAddon(kind, large = false, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            Column(
                modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp * scale),
            ) {
                kinds.forEach { kind ->
                    RenderAddon(kind, large = false, modifier = Modifier.fillMaxWidth().weight(1f))
                }
            }
        }
    }

    val layout = picks.looks.layout.coerceIn(0, 3)
    when (layout) {
        LAYOUT_ONE -> {
            val kind = picks.looks.addons.getOrElse(0) { ADDON_NONE }
            if (kind != ADDON_NONE && hasAddon(kind, board, large = true)) {
                Box(modifier, contentAlignment = Alignment.Center) {
                    RenderAddon(kind, large = true, modifier = Modifier.width(560.dp * scale).fillMaxHeight())
                }
            } else {
                Box(modifier)
            }
        }
        LAYOUT_LARGE_BOTTOM -> {
            val k0 = picks.looks.addons.getOrElse(0) { ADDON_NONE }
            val k1 = picks.looks.addons.getOrElse(1) { ADDON_NONE }
            val list = listOfNotNull(
                if (k0 != ADDON_NONE && hasAddon(k0, board, large = true)) k0 else null,
                if (k1 != ADDON_NONE && hasAddon(k1, board, large = true)) k1 else null,
            )
            if (list.isEmpty()) {
                Box(modifier)
            } else {
                Row(
                    modifier,
                    horizontalArrangement = Arrangement.spacedBy(if (floating) 10.dp * scale else 16.dp * scale),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    list.forEachIndexed { idx, kind ->
                        if (idx > 0 && floating) VerticalHairline()
                        RenderAddon(kind, large = true, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
        LAYOUT_LARGE_TOP -> {
            val k0 = picks.looks.addons.getOrElse(0) { ADDON_NONE }
            val k1 = picks.looks.addons.getOrElse(1) { ADDON_NONE }
            val k2 = picks.looks.addons.getOrElse(2) { ADDON_NONE }

            val stacked = listOfNotNull(
                if (k0 != ADDON_NONE && hasAddon(k0, board, large = false)) k0 else null,
                if (k1 != ADDON_NONE && hasAddon(k1, board, large = false)) k1 else null,
            )
            val hero = if (k2 != ADDON_NONE && hasAddon(k2, board, large = true)) k2 else null

            if (stacked.isEmpty() && hero == null) {
                Box(modifier)
            } else if (stacked.isEmpty()) {
                Box(modifier, contentAlignment = Alignment.Center) {
                    RenderAddon(hero!!, large = true, modifier = Modifier.width(560.dp * scale).fillMaxHeight())
                }
            } else if (hero == null) {
                Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    RenderStacked(stacked, Modifier.width(420.dp * scale))
                }
            } else {
                Row(
                    modifier,
                    horizontalArrangement = Arrangement.spacedBy(if (floating) 18.dp * scale else 16.dp * scale),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RenderStacked(stacked, Modifier.weight(1.05f))
                    if (floating) VerticalHairline()
                    RenderAddon(hero, large = true, modifier = Modifier.weight(0.95f).fillMaxHeight())
                }
            }
        }
        else -> {
            // LAYOUT_THREE (4 slots: 0 compact, 1 compact, 2 large, 3 large)
            val k0 = picks.looks.addons.getOrElse(0) { ADDON_NONE }
            val k1 = picks.looks.addons.getOrElse(1) { ADDON_NONE }
            val k2 = picks.looks.addons.getOrElse(2) { ADDON_NONE }
            val k3 = picks.looks.addons.getOrElse(3) { ADDON_NONE }

            val stacked = listOfNotNull(
                if (k0 != ADDON_NONE && hasAddon(k0, board, large = false)) k0 else null,
                if (k1 != ADDON_NONE && hasAddon(k1, board, large = false)) k1 else null,
            )
            val fullCols = listOfNotNull(
                if (k2 != ADDON_NONE && hasAddon(k2, board, large = true)) k2 else null,
                if (k3 != ADDON_NONE && hasAddon(k3, board, large = true)) k3 else null,
            )

            if (stacked.isEmpty() && fullCols.isEmpty()) {
                Box(modifier)
            } else if (stacked.isEmpty()) {
                Row(
                    modifier,
                    horizontalArrangement = Arrangement.spacedBy(if (floating) 14.dp * scale else 16.dp * scale),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    fullCols.forEachIndexed { idx, kind ->
                        if (idx > 0 && floating) VerticalHairline()
                        RenderAddon(kind, large = true, modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            } else if (fullCols.isEmpty()) {
                Row(modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    RenderStacked(stacked, Modifier.width(440.dp * scale))
                }
            } else {
                Row(
                    modifier,
                    horizontalArrangement = Arrangement.spacedBy(if (floating) 16.dp * scale else 14.dp * scale),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RenderStacked(stacked, Modifier.weight(if (fullCols.size == 2) 1.08f else 1.02f))
                    fullCols.forEach { kind ->
                        if (floating) VerticalHairline()
                        val w = if (kind == ADDON_LEAGUE) 0.96f else 1.02f
                        RenderAddon(kind, large = true, modifier = Modifier.weight(w).fillMaxHeight())
                    }
                }
            }
        }
    }
}

/**
 * A modern Bento container:
 * - [cardsMode] == 0: transparent/floating directly on the backdrop with zero background box
 * - [cardsMode] >= 1: sleek dark obsidian translucent card
 */
@Composable
fun BentoCard(
    scale: Float,
    cardsMode: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (cardsMode == 0) {
        Box(
            modifier.padding(horizontal = 10.dp * scale, vertical = 6.dp * scale),
            contentAlignment = Alignment.Center,
        ) {
            ShrinkToFit(Modifier.fillMaxWidth()) {
                content()
            }
        }
        return
    }
    val shape = RoundedCornerShape(22.dp * scale)
    val bg = Brush.verticalGradient(listOf(Color(0x540A0C12), Color(0x360A0C12)))
    val rim = Brush.verticalGradient(listOf(Look.Gold.copy(alpha = 0.30f), Color(0x14FFFFFF)))
    Box(
        modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, rim, shape)
            .padding(horizontal = 16.dp * scale, vertical = 10.dp * scale),
        contentAlignment = Alignment.Center,
    ) {
        ShrinkToFit(Modifier.fillMaxWidth()) {
            content()
        }
    }
}

/**
 * Lays its content out at its natural height and, when that is taller than the room it has, draws it smaller (from
 * the middle) so that every combination of shapes fits without being cut off.
 */
@Composable
fun ShrinkToFit(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, constraints ->
        val p = measurables.first().measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        val room = constraints.maxHeight
        val s = if (room != Constraints.Infinity && p.height > room) room.toFloat() / p.height else 1f
        val h = if (s < 1f) room else p.height
        layout(p.width, h) {
            p.placeWithLayer(0, (h - p.height) / 2) {
                scaleX = s; scaleY = s
                transformOrigin = TransformOrigin(.5f, .5f)
            }
        }
    }
}

/** whether an add-on has something to show now: off has nothing, a game only when one is coming, the weather only when fresh */
fun hasAddon(kind: Int, board: Board, large: Boolean = false) = when (kind) {
    ADDON_PRAYER -> true
    ADDON_GAME -> nextGame(board) != null || (large && board.results.isNotEmpty())
    ADDON_SKY -> freshSky(board) != null
    ADDON_LEAGUE -> board.table?.rows?.isNotEmpty() == true || board.tables.values.any { it.rows.isNotEmpty() } || board.tables.isNotEmpty()
    ADDON_VERSE, ADDON_HADITH -> true
    else -> false
}

/** one add-on: compact Bento card or full-height Bento card */
@Composable
fun Addon(
    kind: Int,
    board: Board,
    picks: Picks,
    scale: Float,
    game: Fixture? = nextGame(board, picks),
    sky: Sky? = freshSky(board),
    large: Boolean = false,
    isBrowsingLeague: Boolean = false,
    focusedLeague: Boolean = false,
    leagueIndex: Int = 0,
    leagueScroll: Int = 0,
) {
    val availableTables = remember(board.tables, board.table, picks.leagues) {
        val allEnabled = (picks.leagues + picks.clubIds.mapNotNull { app.tilfazi.tv.feed.Clubs.find(it)?.league }).distinct()
        val list = allEnabled.mapNotNull { l ->
            board.tables[l] ?: board.tables.values.firstOrNull { it.league == app.tilfazi.tv.feed.Clubs.leagueName(l) }
        }.filter { it.rows.isNotEmpty() }.distinctBy { it.league }
        if (list.isNotEmpty()) list
        else {
            val fallback = board.tables.values.filter { it.rows.isNotEmpty() }.distinctBy { it.league }
            if (fallback.isNotEmpty()) fallback else listOfNotNull(board.table)
        }
    }
    val autoScrollOn = picks.looks.autoScroll == 1 && !Look.still
    val turn = if (autoScrollOn && availableTables.size > 1) {
        ((board.now / 12_000L).toInt()).mod(availableTables.size)
    } else 0
    val currentTable = if (isBrowsingLeague && availableTables.isNotEmpty()) {
        availableTables[leagueIndex.mod(availableTables.size)]
    } else if (availableTables.isNotEmpty()) {
        availableTables[turn]
    } else {
        board.table
    }
    val currentTableIndex = if (isBrowsingLeague) leagueIndex.mod(availableTables.size.coerceAtLeast(1)) else turn
    val activeClub = picks.clubIds.firstOrNull { id -> currentTable?.rows?.any { it.id == id } == true } ?: picks.clubIds.firstOrNull()

    if (large) when (kind) {
        ADDON_PRAYER -> BigPrayer(board.now, picks, scale)
        ADDON_GAME -> BigGames(game, board, picks, scale)
        ADDON_SKY -> sky?.let { BigSky(it, picks, scale) }
        ADDON_LEAGUE -> currentTable?.let {
            BigLeague(
                table = it,
                picks = picks,
                scale = scale,
                clubId = if (isBrowsingLeague) null else activeClub,
                totalTables = availableTables.size,
                tableIndex = currentTableIndex,
                isBrowsing = isBrowsingLeague,
                focused = focusedLeague,
                scrollOffset = leagueScroll,
            )
        }
        ADDON_VERSE -> VerseWidget(board.now, picks, scale, large = true)
        ADDON_HADITH -> HadithWidget(board.now, picks, scale, large = true)
    } else when (kind) {
        ADDON_PRAYER -> PrayerWidget(picks.looks.prayer, board.now, picks, scale)
        ADDON_GAME -> game?.let {
            val games = nextGames(board, picks)
            val gIdx = games.indexOf(it).coerceAtLeast(0)
            GameWidget(picks.looks.game, it, board.now, picks, scale, gamesCount = games.size, gameIndex = gIdx)
        }
        ADDON_SKY -> sky?.let { SkyWidget(picks.looks.sky, it, picks, scale) }
        ADDON_LEAGUE -> currentTable?.let {
            SmallLeague(
                table = it,
                picks = picks,
                scale = scale,
                clubId = if (isBrowsingLeague) null else activeClub,
                totalTables = availableTables.size,
                tableIndex = currentTableIndex,
                isBrowsing = isBrowsingLeague,
                focused = focusedLeague,
                scrollOffset = leagueScroll,
            )
        }
        ADDON_VERSE -> VerseWidget(board.now, picks, scale, large = false)
        ADDON_HADITH -> HadithWidget(board.now, picks, scale, large = false)
    }
}

private fun rememberActiveClub(board: Board, picks: Picks): String? {
    val withTables = picks.clubIds.filter { board.tables[it]?.rows?.isNotEmpty() == true }
    if (withTables.isEmpty()) return picks.clubIds.firstOrNull()
    val autoScrollOn = picks.looks.autoScroll == 1 && !Look.still
    val turn = if (!autoScrollOn) 0 else (board.now / 12_000L).toInt().mod(withTables.size)
    return withTables[turn]
}

fun nextGameFor(board: Board, clubId: String?): Fixture? =
    if (clubId == null) null
    else board.fixtures.firstOrNull { it.clubId == clubId && !it.isDone && (it.isLive || it.kickoff + 2 * 3_600_000L > board.now) }

fun nextGames(board: Board, picks: Picks): List<Fixture> {
    val byClub = picks.clubIds.mapNotNull { nextGameFor(board, it) }
    if (byClub.isNotEmpty()) return byClub
    return listOfNotNull(board.fixtures.firstOrNull { !it.isDone && (it.isLive || it.kickoff + 2 * 3_600_000L > board.now) })
}

fun nextGame(board: Board, picks: Picks? = null): Fixture? {
    val games = if (picks != null) nextGames(board, picks)
    else board.fixtures.filter { !it.isDone && (it.isLive || it.kickoff + 2 * 3_600_000L > board.now) }
    if (games.isEmpty()) return null
    games.firstOrNull { it.isLive || it.kickoff <= board.now }?.let { return it }
    val autoScrollOn = (picks?.looks?.autoScroll ?: 1) == 1 && !Look.still
    val turn = if (!autoScrollOn) 0 else (board.now / 10_000L).toInt().mod(games.size)
    return games[turn]
}

fun freshSky(board: Board): Sky? = board.sky?.takeIf { board.now - it.at < 3 * 3_600_000L }

/**
 * A frosted card behind one add-on (used in Settings previews).
 */
@Composable
fun GlassCard(scale: Float, content: @Composable () -> Unit) {
    BentoCard(scale = scale, cardsMode = 1, modifier = Modifier.fillMaxWidth(), content = content)
}

/** a 1-pixel gold line that fades out at both ends */
@Composable
fun Hairline(modifier: Modifier = Modifier.fillMaxWidth()) {
    Box(
        modifier
            .height(1.dp / 2)
            .background(Brush.horizontalGradient(listOf(Color.Transparent, Look.Gold.copy(alpha = .45f), Look.Gold.copy(alpha = .45f), Color.Transparent)))
    )
}

/** a vertical 1-pixel gold line between floating Bento columns */
@Composable
fun VerticalHairline(modifier: Modifier = Modifier.fillMaxHeight().padding(vertical = 18.dp)) {
    Box(
        modifier
            .width(1.dp / 2)
            .background(Brush.verticalGradient(listOf(Color.Transparent, Look.Gold.copy(alpha = .40f), Look.Gold.copy(alpha = .40f), Color.Transparent)))
    )
}

// ---------------------------------------------------------------- dates and times in the city

internal val DAYS = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
internal val MONTHS = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")

internal fun cityTime(millis: Long, zone: String): Calendar =
    Calendar.getInstance(TimeZone.getTimeZone(zone), Locale.US).apply { timeInMillis = millis }

/** two digits, always 0-9: String.format would follow the TV's language and give ٠٧ even when 123 is picked */
internal fun two(n: Number) = n.toString().padStart(2, '0')

/** «٥:٠١ ص» */
fun clock12(millis: Long, zone: String, latin: Boolean): String {
    val k = cityTime(millis, zone)
    val h = k.get(Calendar.HOUR_OF_DAY)
    val text = "${if (h % 12 == 0) 12 else h % 12}:${two(k.get(Calendar.MINUTE))}"
    return Look.digits(text, latin) + if (h < 12) " ص" else " م"
}

/** «١٣ جمادى الأولى ١٤٤٨ هـ», Umm al-Qura */
internal fun hijriDate(millis: Long, zone: String, latin: Boolean, year: Boolean = true): String = runCatching {
    val f = SimpleDateFormat(if (year) "d MMMM y" else "d MMMM", ULocale("ar_SA@calendar=islamic-umalqura;numbers=latn"))
    f.timeZone = android.icu.util.TimeZone.getTimeZone(zone)
    val s = f.format(java.util.Date(millis)).replace(Regex("\\s*هـ\\s*$"), "")
    Look.digits(if (year) "$s هـ" else s, latin)
}.getOrDefault("")

/** «٢٤ أكتوبر» */
internal fun gregorianDate(millis: Long, zone: String, latin: Boolean, year: Boolean = false): String {
    val k = cityTime(millis, zone)
    val s = "${k.get(Calendar.DAY_OF_MONTH)} ${MONTHS[k.get(Calendar.MONTH)]}" + if (year) " ${k.get(Calendar.YEAR)}" else ""
    return Look.digits(s, latin)
}

internal fun weekday(millis: Long, zone: String) = DAYS[cityTime(millis, zone).get(Calendar.DAY_OF_WEEK) - 1]

/** «بعد ٣ أيام», «بكرة», «بعد ٢ س و١٠ د» */
internal fun until(ms: Long, latin: Boolean): String {
    val minutes = ms / 60_000
    val hours = minutes / 60
    val days = hours / 24
    fun n(v: Long) = Look.digits(v.toString(), latin)
    return when {
        days >= 2 -> "بعد ${n(days)} " + if (days <= 10) "أيام" else "يومًا"
        days == 1L -> "بكرة"
        hours >= 1 -> "بعد ${n(hours)} س و${n(minutes % 60)} د"
        else -> "بعد ${n(maxOf(1, minutes))} د"
    }
}

/** «٥:٤١:٥٦», or «٤١:٥٦» under an hour */
internal fun countdown(ms: Long, latin: Boolean): String {
    val left = (ms / 1000).coerceAtLeast(0)
    if (Look.still) {   // minutes only: the screen is redrawn once a minute
        val m = (left + 59) / 60
        return Look.ltr(Look.digits("${m / 60}:${two(m % 60)}", latin))
    }
    val s = if (left >= 3600) "${left / 3600}:${two(left / 60 % 60)}:${two(left % 60)}" else "${two(left / 60)}:${two(left % 60)}"
    return Look.ltr(Look.digits(s, latin))
}
