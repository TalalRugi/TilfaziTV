package app.tilfazi.tv.overlay

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import app.tilfazi.tv.feed.Choices
import app.tilfazi.tv.feed.Clubs
import app.tilfazi.tv.feed.EDGE_PARTS
import app.tilfazi.tv.feed.Feeds
import app.tilfazi.tv.feed.Fixture
import app.tilfazi.tv.feed.MatchEvent
import app.tilfazi.tv.feed.OVERLAY_ALPHA_VALS
import app.tilfazi.tv.feed.OVERLAY_POST_MINS
import app.tilfazi.tv.feed.OVERLAY_PRE_MINS
import app.tilfazi.tv.feed.Picks
import app.tilfazi.tv.feed.SIZE_SCALES
import app.tilfazi.tv.feed.Salah
import app.tilfazi.tv.home.HomeActivity
import app.tilfazi.tv.look.Look
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/** A temporary prayer notification shown inside the top Dynamic Island. */
data class PrayerAlert(
    val key: String,
    val title: String,
    val detail: String,
)

/**
 * Tilfazi's Top-Flush Dynamic Island («جزيرة تلفازي العلوية»):
 * - Docked flush against the top-centre bezel of the TV (y = 0) with concave top ears and rounded bottom corners,
 *   so it looks like a physical part of the TV frame rather than an obstruction over the video.
 * - Combines Live Football Matches and Prayer Alerts in one single-row island.
 * - When both a Prayer Alert and a Live Match occur simultaneously, the island expands HORIZONTALLY (never vertically)
 *   into two side-by-side wings so it never blocks the picture.
 * - Drawn directly with a hardware-accelerated [Canvas] [View] (< 1 MB RAM, 0 allocations in onDraw, 0% idle CPU).
 */
object MatchOverlay {
    private lateinit var app: Application
    private lateinit var feeds: Feeds
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var islandView: OverlayIslandView? = null
    private var attached = false

    /** true while HomeActivity is in front */
    private var homeInFront = false
    /** true while screensaver (AmbientActivity) is in front */
    private var ambientInFront = false
    /** true while the user is on the Dynamic Island settings page (shows live preview at top of TV) */
    private var settingsOpen = false
    /** true while a user-triggered preview is active */
    private var demoActive = false
    private var demoJob: Job? = null
    private var eventHideJob: Job? = null
    private var prayerHideJob: Job? = null

    /** current live match and active toast events */
    private var currentGame: Fixture? = null
    private var currentHomeBmp: Bitmap? = null
    private var currentAwayBmp: Bitmap? = null
    private var activeEvent: MatchEvent? = null
    private var activePrayer: PrayerAlert? = null

    /** per-match tracking to detect goals, red cards, and phase transitions */
    private class TrackedMatch(
        var state: String,
        var statusName: String,
        var homeScore: Int,
        var awayScore: Int,
        val seenEvents: MutableSet<String> = mutableSetOf(),
    )
    private val tracked = mutableMapOf<String, TrackedMatch>()

    fun canDraw(c: Context): Boolean = Settings.canDrawOverlays(c)

    /** Opens the system overlay permission screen if not yet granted. */
    fun requestPermission(c: Context) {
        if (canDraw(c)) return
        runCatching {
            val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${c.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            c.startActivity(i)
        }.onFailure {
            runCatching {
                val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${c.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                c.startActivity(i)
            }
        }
    }

    fun init(application: Application) {
        app = application
        feeds = Feeds(application)

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var homeCount = 0
            private var ambientCount = 0

            override fun onActivityStarted(a: Activity) {
                if (a is HomeActivity) {
                    homeCount++
                    homeInFront = true
                    syncVisibility()
                } else if (a.javaClass.name.contains("Ambient")) {
                    ambientCount++
                    ambientInFront = true
                    syncVisibility()
                }
            }

            override fun onActivityStopped(a: Activity) {
                if (a is HomeActivity) {
                    homeCount = (homeCount - 1).coerceAtLeast(0)
                    homeInFront = homeCount > 0
                    syncVisibility()
                } else if (a.javaClass.name.contains("Ambient")) {
                    ambientCount = (ambientCount - 1).coerceAtLeast(0)
                    ambientInFront = ambientCount > 0
                    syncVisibility()
                }
            }

            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })

        // React immediately whenever settings change
        scope.launch {
            Choices.picks.collect { picks ->
                islandView?.updatePicks(picks)
                if (attached) updateWindowParams(picks)
                if (settingsOpen && !demoActive) refreshSettingsSample(picks)
                syncVisibility()
            }
        }

        // 1. Local prayer-time watcher (100% offline math, zero network calls)
        scope.launch { prayerLoop() }
        // 2. Match watcher (sleeps outside match hours, polls every 45s only during live match windows)
        scope.launch { watchLoop() }
    }

    /** Called by SetupActivity when the Dynamic Island settings page is shown or hidden. */
    fun setSettingsOpen(open: Boolean) {
        settingsOpen = open
        scope.launch {
            if (open && !demoActive) {
                refreshSettingsSample(Choices.picks.value)
            } else if (!open && !demoActive) {
                currentGame = null
                activeEvent = null
                activePrayer = null
            }
            syncVisibility()
        }
    }

    private suspend fun refreshSettingsSample(picks: Picks) {
        val l = picks.looks
        val localClub = Clubs.find(picks.clubId) ?: Clubs.find("929") ?: Clubs.localClubs.first()
        val rivalClub = if (localClub.id == "929") Clubs.find("817") else Clubs.find("929")
        val sample = Fixture(
            home = localClub.name,
            away = rivalClub?.name ?: "النصر",
            homeBadge = "https://a.espncdn.com/i/teamlogos/soccer/500/${localClub.id}.png",
            awayBadge = "https://a.espncdn.com/i/teamlogos/soccer/500/${rivalClub?.id ?: "817"}.png",
            competition = "دوري روشن السعودي",
            kickoff = System.currentTimeMillis(),
            homeScore = 1,
            awayScore = 1,
            state = "in",
            clock = "74'",
            clubId = localClub.id,
        )
        if (l.overlayMode > 0) {
            currentGame = sample
            currentHomeBmp = sample.homeBadge?.let { feeds.badge(it) }
            currentAwayBmp = sample.awayBadge?.let { feeds.badge(it) }
            activeEvent = if (l.overlayMode == 1) MatchEvent("demo-goal", "goal", "هدف التعادل", "74'") else null
        } else {
            currentGame = null
            currentHomeBmp = null
            currentAwayBmp = null
            activeEvent = null
        }
        val nextSalat = Salah.next(System.currentTimeMillis(), picks)?.name ?: "العشاء"
        val pMode = l.overlayPrayer.coerceIn(0, 3)
        val preMins = OVERLAY_PRE_MINS.getOrElse(l.overlayPre.coerceIn(0, OVERLAY_PRE_MINS.lastIndex)) { 10 }
        val postMins = OVERLAY_POST_MINS.getOrElse(l.overlayPost.coerceIn(0, OVERLAY_POST_MINS.lastIndex)) { 15 }
        activePrayer = when (pMode) {
            1 -> PrayerAlert("demo-prayer", "أذان $nextSalat", "مضى على الأذان ${Look.digits(postMins.toString(), picks.latinDigits)} دقيقة")
            2 -> PrayerAlert("demo-prayer", "اقترب أذان $nextSalat", "بعد ${Look.ltr(Look.digits("${preMins.toString().padStart(2, '0')}:00", picks.latinDigits))}")
            3 -> PrayerAlert("demo-prayer", "أذان $nextSalat", "بعد ${Look.ltr(Look.digits("02:15:30", picks.latinDigits))}")
            else -> null
        }
        islandView?.setState(currentGame, currentHomeBmp, currentAwayBmp, activeEvent, activePrayer)
    }

    /**
     * Triggers a live preview of the top Dynamic Island so the user can test all states:
     * @param kind "goal", "countdown", "prayer", or "both"
     */
    fun triggerDemo(kind: String = "both") {
        demoJob?.cancel()
        demoJob = scope.launch {
            val picks = Choices.picks.value
            val localClub = Clubs.find(picks.clubId) ?: Clubs.find("929") ?: Clubs.localClubs.first()
            val rivalClub = if (localClub.id == "929") Clubs.find("817") else Clubs.find("929")
            val homeName = localClub.name
            val awayName = rivalClub?.name ?: "النصر"
            val sample = Fixture(
                home = homeName,
                away = awayName,
                homeBadge = "https://a.espncdn.com/i/teamlogos/soccer/500/${localClub.id}.png",
                awayBadge = "https://a.espncdn.com/i/teamlogos/soccer/500/${rivalClub?.id ?: "817"}.png",
                competition = "دوري روشن السعودي",
                kickoff = System.currentTimeMillis(),
                homeScore = 1,
                awayScore = 1,
                state = "in",
                clock = "74'",
                clubId = localClub.id,
            )

            demoActive = true
            when (kind) {
                "countdown" -> {
                    val nextSalat = Salah.next(System.currentTimeMillis(), picks)?.name ?: "المغرب"
                    currentGame = null
                    currentHomeBmp = null
                    currentAwayBmp = null
                    activeEvent = null
                    for (sec in 10 downTo 1) {
                        val mm = "09"
                        val ss = sec.toString().padStart(2, '0')
                        activePrayer = PrayerAlert("demo-prayer", "اقترب أذان $nextSalat", "بعد ${Look.ltr(Look.digits("$mm:$ss", picks.latinDigits))}")
                        syncVisibility()
                        islandView?.setState(null, null, null, null, activePrayer)
                        delay(1_000L)
                    }
                }
                "prayer" -> {
                    val nextSalat = Salah.next(System.currentTimeMillis(), picks)?.name ?: "العشاء"
                    currentGame = null
                    currentHomeBmp = null
                    currentAwayBmp = null
                    activeEvent = null
                    activePrayer = PrayerAlert("demo-prayer", "أذان $nextSalat", "مضى على الأذان ٥ دقائق")
                    syncVisibility()
                    islandView?.setState(null, null, null, null, activePrayer)
                    delay(8_000L)
                }
                "goal" -> {
                    val hBmp = sample.homeBadge?.let { feeds.badge(it) }
                    val aBmp = sample.awayBadge?.let { feeds.badge(it) }
                    currentGame = sample
                    currentHomeBmp = hBmp
                    currentAwayBmp = aBmp
                    activePrayer = null
                    activeEvent = MatchEvent("demo-goal", "goal", "هدف التعادل", "74'")
                    syncVisibility()
                    islandView?.setState(currentGame, currentHomeBmp, currentAwayBmp, activeEvent, null)
                    delay(5_500L)
                    activeEvent = null
                    islandView?.setState(currentGame, currentHomeBmp, currentAwayBmp, null, null)
                    delay(3_500L)
                }
                else -> {
                    val hBmp = sample.homeBadge?.let { feeds.badge(it) }
                    val aBmp = sample.awayBadge?.let { feeds.badge(it) }
                    val nextSalat = Salah.next(System.currentTimeMillis(), picks)?.name ?: "العشاء"
                    currentGame = sample
                    currentHomeBmp = hBmp
                    currentAwayBmp = aBmp
                    activeEvent = MatchEvent("demo-goal", "goal", "هدف التعادل", "74'")
                    activePrayer = PrayerAlert("demo-prayer", "اقترب أذان $nextSalat", "بعد ${Look.ltr(Look.digits("08:30", picks.latinDigits))}")
                    syncVisibility()
                    islandView?.setState(currentGame, currentHomeBmp, currentAwayBmp, activeEvent, activePrayer)
                    delay(6_000L)
                    activePrayer = null
                    activeEvent = null
                    islandView?.setState(currentGame, currentHomeBmp, currentAwayBmp, null, null)
                    delay(3_500L)
                }
            }
            demoActive = false
            if (settingsOpen) {
                refreshSettingsSample(Choices.picks.value)
            } else {
                activePrayer = null
                activeEvent = null
            }
            syncVisibility()
        }
    }

    private suspend fun prayerLoop() {
        while (true) {
            val picks = Choices.picks.value
            val pMode = picks.looks.overlayPrayer.coerceIn(0, 3)
            val now = System.currentTimeMillis()

            if (pMode == 0 || demoActive) {
                if (!demoActive && activePrayer != null) {
                    activePrayer = null
                    islandView?.setState(currentGame, currentHomeBmp, currentAwayBmp, activeEvent, null)
                    syncVisibility()
                }
                delay(if (demoActive) 1_000L else 10_000L)
                continue
            }

            val preMins = OVERLAY_PRE_MINS.getOrElse(picks.looks.overlayPre.coerceIn(0, OVERLAY_PRE_MINS.lastIndex)) { 10 }
            val postMins = OVERLAY_POST_MINS.getOrElse(picks.looks.overlayPost.coerceIn(0, OVERLAY_POST_MINS.lastIndex)) { 15 }
            val preMs = preMins.toLong() * 60_000L
            val postMs = postMins.toLong() * 60_000L

            val prayers = Salah.day(now - 86_400_000L, picks) +
                Salah.day(now, picks) +
                Salah.day(now + 86_400_000L, picks)

            val lastPrayer = prayers.filter { it.at <= now }.maxByOrNull { it.at }
            val nextPrayer = prayers.filter { it.at > now }.minByOrNull { it.at }

            var newPrayerAlert: PrayerAlert? = null

            // 1. Post-Adhan window (modes 1, 2, 3): elapsed time after prayer
            if (lastPrayer != null && (now - lastPrayer.at) < postMs) {
                val elapsedMs = now - lastPrayer.at
                val elapsedMins = (elapsedMs / 60_000L).toInt()
                val detailStr = if (elapsedMins == 0) {
                    "حان الآن"
                } else {
                    val mStr = when (elapsedMins) {
                        1 -> "دقيقة واحدة"
                        2 -> "دقيقتان"
                        in 3..10 -> "${Look.digits(elapsedMins.toString(), picks.latinDigits)} دقائق"
                        else -> "${Look.digits(elapsedMins.toString(), picks.latinDigits)} دقيقة"
                    }
                    "مضى على الأذان $mStr"
                }
                newPrayerAlert = PrayerAlert(
                    key = "post-${lastPrayer.name}-${lastPrayer.at}",
                    title = "أذان ${lastPrayer.name}",
                    detail = detailStr,
                )
            }
            // 2. Pre-Adhan window (modes 2, 3): live countdown down to Adhan
            else if (nextPrayer != null && (pMode in 2..3) && (nextPrayer.at - now) <= preMs) {
                val remainMs = (nextPrayer.at - now).coerceAtLeast(0)
                val totalSecs = (remainMs + 999) / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                val timeStr = Look.ltr("${Look.digits(mins.toString().padStart(2, '0'), picks.latinDigits)}:${Look.digits(secs.toString().padStart(2, '0'), picks.latinDigits)}")
                newPrayerAlert = PrayerAlert(
                    key = "pre-${nextPrayer.name}-${nextPrayer.at}",
                    title = "اقترب أذان ${nextPrayer.name}",
                    detail = "بعد $timeStr",
                )
            }
            // 3. Continuous countdown mode (mode 3): always visible countdown to next prayer
            else if (nextPrayer != null && pMode == 3) {
                val remainMs = (nextPrayer.at - now).coerceAtLeast(0)
                val totalSecs = (remainMs + 999) / 1000
                val hours = totalSecs / 3600
                val mins = (totalSecs % 3600) / 60
                val secs = totalSecs % 60
                val timeStr = if (hours > 0) {
                    Look.ltr("${Look.digits(hours.toString().padStart(2, '0'), picks.latinDigits)}:${Look.digits(mins.toString().padStart(2, '0'), picks.latinDigits)}:${Look.digits(secs.toString().padStart(2, '0'), picks.latinDigits)}")
                } else {
                    Look.ltr("${Look.digits(mins.toString().padStart(2, '0'), picks.latinDigits)}:${Look.digits(secs.toString().padStart(2, '0'), picks.latinDigits)}")
                }
                newPrayerAlert = PrayerAlert(
                    key = "cnt-${nextPrayer.name}-${nextPrayer.at}",
                    title = "أذان ${nextPrayer.name}",
                    detail = "بعد $timeStr",
                )
            }

            if (activePrayer != newPrayerAlert) {
                activePrayer = newPrayerAlert
                islandView?.setState(currentGame, currentHomeBmp, currentAwayBmp, activeEvent, activePrayer)
                syncVisibility()
            }

            // If an active prayer alert is shown or mode is continuous, update every second for live ticker
            val sleepMs = if (activePrayer != null || pMode == 3) 1_000L else 10_000L
            delay(sleepMs)
        }
    }

    private suspend fun watchLoop() {
        while (true) {
            val picks = Choices.picks.value
            val clubs = picks.clubIds
            val mode = picks.looks.overlayMode

            if (mode == 0 || clubs.isEmpty()) {
                if (!demoActive) {
                    currentGame = null
                    activeEvent = null
                    syncVisibility()
                }
                delay(15_000L)
                continue
            }

            val now = System.currentTimeMillis()
            val cached = withContext(Dispatchers.IO) {
                clubs.flatMap { feeds.keptFixtures(it) }
            }
            val inWindow = cached.any { g ->
                g.isLive || (!g.isDone && now >= g.kickoff - 5 * 60_000L && now - g.kickoff < 165 * 60_000L)
            } || tracked.values.any { it.state == "in" }

            if (!inWindow && cached.isNotEmpty()) {
                if (!demoActive) {
                    currentGame = null
                    activeEvent = null
                    syncVisibility()
                }
                val nextKick = cached.filter { !it.isDone && it.kickoff > now }.minOfOrNull { it.kickoff }
                val waitMs = if (nextKick != null) {
                    (nextKick - now - 3 * 60_000L).coerceIn(60_000L, 20 * 60_000L)
                } else {
                    20 * 60_000L
                }
                delay(waitMs)
                withContext(Dispatchers.IO) { clubs.forEach { feeds.fetchFixtures(it) } }
                continue
            }

            val fresh = withContext(Dispatchers.IO) {
                clubs.flatMap { club -> feeds.fetchFixtures(club) ?: feeds.keptFixtures(club) }
            }

            processGames(fresh)

            val stillLive = fresh.any { g ->
                g.isLive || (!g.isDone && System.currentTimeMillis() >= g.kickoff - 5 * 60_000L && System.currentTimeMillis() - g.kickoff < 165 * 60_000L)
            }
            delay(if (stillLive) 45_000L else 10 * 60_000L)
        }
    }

    private suspend fun processGames(games: List<Fixture>) {
        if (demoActive) return
        val now = System.currentTimeMillis()

        val candidates = games.filter { g ->
            g.isLive || (now - g.kickoff in -5 * 60_000L..165 * 60_000L)
        }

        var newlyTriggeredEvent: MatchEvent? = null
        var eventGame: Fixture? = null

        for (g in candidates) {
            val id = g.eventId.ifBlank { "${g.home}-${g.away}-${g.kickoff}" }
            val prev = tracked[id]
            val hScore = g.homeScore ?: 0
            val aScore = g.awayScore ?: 0

            if (prev == null) {
                val init = TrackedMatch(g.state, g.statusName, hScore, aScore)
                if (g.isLive && g.eventId.isNotBlank()) {
                    val existing = withContext(Dispatchers.IO) { feeds.fetchMatchEvents(g.eventId) }
                    existing.forEach { init.seenEvents += it.key }
                }
                tracked[id] = init
            } else {
                if (prev.state == "pre" && g.state == "in") {
                    newlyTriggeredEvent = MatchEvent("kickoff-$id", "phase", "انطلقت المباراة", "${g.home} ضد ${g.away}")
                    eventGame = g
                } else if (prev.statusName != "STATUS_HALFTIME" && g.statusName == "STATUS_HALFTIME") {
                    newlyTriggeredEvent = MatchEvent("ht-$id", "phase", "استراحة بين الشوطين", "${g.home} $hScore - $aScore ${g.away}")
                    eventGame = g
                } else if (prev.statusName == "STATUS_HALFTIME" && g.statusName == "STATUS_SECOND_HALF") {
                    newlyTriggeredEvent = MatchEvent("2h-$id", "phase", "بداية الشوط الثاني", "${g.home} $hScore - $aScore ${g.away}")
                    eventGame = g
                } else if (prev.state == "in" && g.state == "post") {
                    newlyTriggeredEvent = MatchEvent("ft-$id", "phase", "نهاية المباراة", "${g.home} $hScore - $aScore ${g.away}")
                    eventGame = g
                }

                if (g.isLive && g.eventId.isNotBlank()) {
                    val events = withContext(Dispatchers.IO) { feeds.fetchMatchEvents(g.eventId) }
                    val unseen = events.firstOrNull { it.key !in prev.seenEvents }
                    events.forEach { prev.seenEvents += it.key }
                    if (unseen != null) {
                        newlyTriggeredEvent = unseen
                        eventGame = g
                    } else if (hScore > prev.homeScore || aScore > prev.awayScore) {
                        val scorerTeam = if (hScore > prev.homeScore) g.home else g.away
                        newlyTriggeredEvent = MatchEvent(
                            key = "score-$hScore-$aScore-${g.clock}",
                            kind = "goal",
                            title = "هدف · $scorerTeam",
                            detail = g.clock,
                        )
                        eventGame = g
                    }
                }

                prev.state = g.state
                prev.statusName = g.statusName
                prev.homeScore = hScore
                prev.awayScore = aScore
            }
        }

        val liveNow = eventGame?.takeIf { it.isLive } ?: games.firstOrNull { it.isLive } ?: eventGame
        currentGame = liveNow
        if (liveNow != null) {
            currentHomeBmp = liveNow.homeBadge?.let { feeds.badge(it) }
            currentAwayBmp = liveNow.awayBadge?.let { feeds.badge(it) }
        } else {
            currentHomeBmp = null
            currentAwayBmp = null
        }

        if (newlyTriggeredEvent != null) {
            showEventToast(newlyTriggeredEvent)
        } else {
            islandView?.setState(currentGame, currentHomeBmp, currentAwayBmp, activeEvent, activePrayer)
            syncVisibility()
        }
    }

    private fun showEventToast(ev: MatchEvent) {
        activeEvent = ev
        islandView?.setState(currentGame, currentHomeBmp, currentAwayBmp, activeEvent, activePrayer)
        syncVisibility()
        eventHideJob?.cancel()
        eventHideJob = scope.launch {
            delay(10_000L)
            activeEvent = null
            if (currentGame?.isLive != true) currentGame = null
            islandView?.setState(currentGame, currentHomeBmp, currentAwayBmp, null, activePrayer)
            syncVisibility()
        }
    }

    private fun shouldShowMatchWing(): Boolean {
        val game = currentGame ?: return false
        if (demoActive) return true
        if (settingsOpen) return Choices.picks.value.looks.overlayMode > 0
        return when (Choices.picks.value.looks.overlayMode) {
            1 -> activeEvent != null
            2 -> game.isLive || activeEvent != null
            else -> false
        }
    }

    private fun shouldShowWindow(): Boolean {
        if (!canDraw(app)) return false
        if (demoActive) return true
        if (ambientInFront) return false
        val picks = Choices.picks.value
        if (homeInFront && picks.looks.overlayHome == 0 && !settingsOpen) return false
        val hasPrayer = activePrayer != null && picks.looks.overlayPrayer > 0
        val hasMatch = shouldShowMatchWing()
        return hasPrayer || hasMatch
    }

    private fun syncVisibility() {
        val show = shouldShowWindow()
        val shownGame = if (shouldShowMatchWing()) currentGame else null
        if (show && !attached) {
            attachWindow(shownGame)
        } else if (!show && attached) {
            detachWindow()
        } else if (show && attached) {
            islandView?.setState(shownGame, currentHomeBmp, currentAwayBmp, activeEvent, activePrayer)
            updateWindowParams(Choices.picks.value)
        }
    }

    private fun attachWindow(shownGame: Fixture?) {
        if (attached || !canDraw(app)) return
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val picks = Choices.picks.value
        val view = islandView ?: OverlayIslandView(app).also { islandView = it }
        view.updatePicks(picks)
        view.setState(shownGame, currentHomeBmp, currentAwayBmp, activeEvent, activePrayer)
        runCatching {
            wm.addView(view, buildLayoutParams(picks))
            attached = true
        }
    }

    private fun detachWindow() {
        if (!attached) return
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        islandView?.let { v -> runCatching { wm.removeViewImmediate(v) } }
        attached = false
    }

    private fun updateWindowParams(picks: Picks) {
        if (!attached) return
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        islandView?.let { v -> runCatching { wm.updateViewLayout(v, buildLayoutParams(picks)) } }
    }

    private fun buildLayoutParams(picks: Picks): WindowManager.LayoutParams {
        val dm = app.resources.displayMetrics
        val edgeFraction = EDGE_PARTS.getOrElse(picks.looks.edge.coerceIn(0, 3)) { 0f }
        // Flush with the top edge of the visible screen (y = 0 unless overscan margin is set)
        val topY = (dm.heightPixels * edgeFraction).roundToInt()

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = topY
        }
    }
}

/**
 * Custom [View] that draws the Top-Flush Dynamic Island directly onto a [Canvas].
 * - Flat top edge fused with the TV bezel, concave top-left/right shoulder ears, rounded bottom corners.
 * - Single-row horizontal layout that expands horizontally when Prayer + Match coincide.
 */
private class OverlayIslandView(context: Context) : View(context) {
    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float) = v * density

    private val boldFace: Typeface = runCatching {
        Typeface.createFromAsset(context.assets, "fonts/thmanyahsans-Bold.otf")
    }.getOrElse { Typeface.DEFAULT_BOLD }

    private var scale = 1f
    private var latinDigits = false
    private var bgAlpha = 0.96f

    private var game: Fixture? = null
    private var homeBmp: Bitmap? = null
    private var awayBmp: Bitmap? = null
    private var event: MatchEvent? = null
    private var prayer: PrayerAlert? = null

    private val notchPath = Path()
    private val borderPath = Path()
    private val bmpRect = RectF()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33D6AF6B
        strokeWidth = dp(1f)
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    private val teamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(boldFace, Typeface.BOLD)
        color = 0xFFFFFDF6.toInt()
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(boldFace, Typeface.BOLD)
        color = 0xFFD6AF6B.toInt()
    }
    private val clockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(boldFace, Typeface.NORMAL)
        color = 0xFF4CD964.toInt()
    }
    private val alertTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(boldFace, Typeface.BOLD)
    }
    private val alertDetailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(boldFace, Typeface.NORMAL)
        color = 0xCCFFFFFF.toInt()
    }

    fun updatePicks(picks: Picks) {
        scale = SIZE_SCALES.getOrElse(picks.looks.size.coerceIn(0, 4)) { 1f }
        latinDigits = picks.latinDigits
        bgAlpha = OVERLAY_ALPHA_VALS.getOrElse(picks.looks.overlayAlpha.coerceIn(0, 2)) { 0.96f }
        applyTextSizes()
        requestLayout()
        invalidate()
    }

    fun setState(g: Fixture?, hBmp: Bitmap?, aBmp: Bitmap?, ev: MatchEvent?, pr: PrayerAlert?) {
        val changed = game != g || homeBmp !== hBmp || awayBmp !== aBmp || event != ev || prayer != pr
        game = g
        homeBmp = hBmp
        awayBmp = aBmp
        event = ev
        prayer = pr
        if (changed) {
            applyTextSizes()
            requestLayout()
            invalidate()
        }
    }

    private fun applyTextSizes() {
        teamPaint.textSize = dp(13.5f * scale)
        scorePaint.textSize = dp(15.5f * scale)
        clockPaint.textSize = dp(12f * scale)
        alertTitlePaint.textSize = dp(13f * scale)
        alertDetailPaint.textSize = dp(12f * scale)
    }

    private fun scoreString(g: Fixture): String {
        val h = Look.digits((g.homeScore ?: 0).toString(), latinDigits)
        val a = Look.digits((g.awayScore ?: 0).toString(), latinDigits)
        return "$h - $a"
    }

    private fun clockString(g: Fixture): String =
        Look.digits(g.clock.ifEmpty { if (g.isDone) "انتهت" else "مباشر" }, latinDigits)

    private fun measurePrayerWing(pr: PrayerAlert): Float {
        val gap = dp(7f * scale)
        val iconW = alertTitlePaint.measureText("☪")
        val titleW = alertTitlePaint.measureText(pr.title)
        val detailW = if (pr.detail.isNotEmpty()) alertDetailPaint.measureText(" · " + Look.digits(pr.detail, latinDigits)) else 0f
        return iconW + gap + titleW + detailW
    }

    private fun measureMatchWing(g: Fixture, ev: MatchEvent?): Float {
        val badgeSize = dp(20f * scale)
        val gap = dp(7f * scale)
        val dotSize = dp(6.5f * scale)

        val homeW = teamPaint.measureText(g.home)
        val awayW = teamPaint.measureText(g.away)
        val scoreW = scorePaint.measureText(scoreString(g))
        val clockW = clockPaint.measureText(clockString(g))

        var w = badgeSize + gap + homeW + gap * 1.3f +
            scoreW + gap * 1.3f +
            awayW + gap + badgeSize +
            gap * 1.5f + dotSize + gap * 0.6f + clockW

        if (ev != null) {
            val evTitleW = alertTitlePaint.measureText(ev.title)
            val evDetailW = if (ev.detail.isNotEmpty()) alertDetailPaint.measureText(" · " + Look.digits(ev.detail, latinDigits)) else 0f
            w += gap * 2.2f + dotSize + gap * 0.7f + evTitleW + evDetailW
        }
        return w
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val g = game
        val pr = prayer
        if (g == null && pr == null) {
            setMeasuredDimension(0, 0)
            return
        }

        val earR = dp(10f * scale)
        val padH = earR + dp(16f * scale)
        val wingGap = dp(22f * scale)

        val prayerW = if (pr != null) measurePrayerWing(pr) else 0f
        val matchW = if (g != null) measureMatchWing(g, event) else 0f
        val both = pr != null && g != null

        val totalW = padH * 2 + prayerW + matchW + (if (both) wingGap else 0f)
        // Always a single compact row hanging from the top bezel!
        val h = dp(36f * scale).roundToInt()
        setMeasuredDimension(totalW.roundToInt(), h)
    }

    override fun onDraw(canvas: Canvas) {
        val g = game
        val pr = prayer
        if (g == null && pr == null) return

        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val earR = dp(10f * scale)
        val bottomR = dp(17f * scale)
        val stroke = borderPaint.strokeWidth / 2f
        val bottomY = h - stroke

        // Build the Top-Flush Notch silhouette:
        // Flat top at y = 0, concave outward ears at (0,0)->(earR,earR) and (w-earR,earR)->(w,0), rounded bottom corners
        notchPath.reset()
        notchPath.moveTo(0f, 0f)
        notchPath.quadTo(earR, 0f, earR, earR)
        notchPath.lineTo(earR, bottomY - bottomR)
        notchPath.quadTo(earR, bottomY, earR + bottomR, bottomY)
        notchPath.lineTo(w - earR - bottomR, bottomY)
        notchPath.quadTo(w - earR, bottomY, w - earR, bottomY - bottomR)
        notchPath.lineTo(w - earR, earR)
        notchPath.quadTo(w - earR, 0f, w, 0f)
        notchPath.close()

        bgPaint.color = Color.argb((bgAlpha * 255).roundToInt().coerceIn(0, 255), 10, 11, 14)
        canvas.drawPath(notchPath, bgPaint)

        // Subtle bottom & side contour border (open at top y = 0 so it merges with the physical black TV frame)
        borderPath.reset()
        borderPath.moveTo(0f, stroke)
        borderPath.quadTo(earR, stroke, earR, earR)
        borderPath.lineTo(earR, bottomY - bottomR)
        borderPath.quadTo(earR, bottomY, earR + bottomR, bottomY)
        borderPath.lineTo(w - earR - bottomR, bottomY)
        borderPath.quadTo(w - earR, bottomY, w - earR, bottomY - bottomR)
        borderPath.lineTo(w - earR, earR)
        borderPath.quadTo(w - earR, stroke, w, stroke)

        val ev = event
        borderPaint.color = when {
            ev?.kind == "red" -> 0xCCFF6B5E.toInt()
            pr != null || ev != null -> 0xB3D6AF6B.toInt()
            else -> 0x2EFFFFFF
        }
        canvas.drawPath(borderPath, borderPaint)

        val padH = earR + dp(16f * scale)
        val centerY = h * 0.48f
        var xRight = w - padH

        // ---------------- Wing 1 (Right side): Prayer Alert if active ----------------
        if (pr != null) {
            val gap = dp(7f * scale)
            alertTitlePaint.color = 0xFFD6AF6B.toInt()
            val textBase = centerY - (alertTitlePaint.descent() + alertTitlePaint.ascent()) / 2f

            val crescent = "☪"
            val cW = alertTitlePaint.measureText(crescent)
            canvas.drawText(crescent, xRight - cW, textBase, alertTitlePaint)
            xRight -= cW + gap

            val tW = alertTitlePaint.measureText(pr.title)
            canvas.drawText(pr.title, xRight - tW, textBase, alertTitlePaint)
            xRight -= tW

            if (pr.detail.isNotEmpty()) {
                val detStr = " · " + Look.digits(pr.detail, latinDigits)
                val dW = alertDetailPaint.measureText(detStr)
                val detBase = centerY - (alertDetailPaint.descent() + alertDetailPaint.ascent()) / 2f
                canvas.drawText(detStr, xRight - dW, detBase, alertDetailPaint)
                xRight -= dW
            }

            // If a Match Wing is also shown, draw a golden vertical separator between the two wings
            if (g != null) {
                val halfGap = dp(11f * scale)
                xRight -= halfGap
                canvas.drawLine(xRight, centerY - dp(8f * scale), xRight, centerY + dp(8f * scale), dividerPaint)
                xRight -= halfGap
            }
        }

        // ---------------- Wing 2 (Left side): Live Match + Event ----------------
        if (g != null) {
            val badgeSize = dp(20f * scale)
            val gap = dp(7f * scale)
            val textBase = centerY - (teamPaint.descent() + teamPaint.ascent()) / 2f

            // 1. Home badge
            homeBmp?.let { bmp ->
                bmpRect.set(xRight - badgeSize, centerY - badgeSize / 2f, xRight, centerY + badgeSize / 2f)
                canvas.drawBitmap(bmp, null, bmpRect, bmpPaint)
            }
            xRight -= badgeSize + gap

            // 2. Home name
            val homeW = teamPaint.measureText(g.home)
            canvas.drawText(g.home, xRight - homeW, textBase, teamPaint)
            xRight -= homeW + gap * 1.3f

            // 3. Score
            val scoreStr = scoreString(g)
            val scoreW = scorePaint.measureText(scoreStr)
            val scoreBase = centerY - (scorePaint.descent() + scorePaint.ascent()) / 2f
            canvas.drawText(scoreStr, xRight - scoreW, scoreBase, scorePaint)
            xRight -= scoreW + gap * 1.3f

            // 4. Away name
            val awayW = teamPaint.measureText(g.away)
            canvas.drawText(g.away, xRight - awayW, textBase, teamPaint)
            xRight -= awayW + gap

            // 5. Away badge
            awayBmp?.let { bmp ->
                bmpRect.set(xRight - badgeSize, centerY - badgeSize / 2f, xRight, centerY + badgeSize / 2f)
                canvas.drawBitmap(bmp, null, bmpRect, bmpPaint)
            }
            xRight -= badgeSize + gap * 0.8f

            // 6. Divider before clock
            canvas.drawLine(xRight, centerY - dp(7.5f * scale), xRight, centerY + dp(7.5f * scale), dividerPaint)
            xRight -= gap * 0.8f

            // 7. Clock + live dot
            val clkStr = clockString(g)
            val clkW = clockPaint.measureText(clkStr)
            clockPaint.color = if (g.isDone) 0xFFD6AF6B.toInt() else 0xFF4CD964.toInt()
            val clkBase = centerY - (clockPaint.descent() + clockPaint.ascent()) / 2f
            canvas.drawText(clkStr, xRight - clkW, clkBase, clockPaint)
            xRight -= clkW + gap * 0.6f

            val dotR = dp(3.2f * scale)
            dotPaint.color = if (g.isDone) 0xFFD6AF6B.toInt() else 0xFF4CD964.toInt()
            canvas.drawCircle(xRight - dotR, centerY, dotR, dotPaint)
            xRight -= dotR * 2f

            // 8. Optional horizontal expansion for Match Event (Goal / Red Card / Phase)
            if (ev != null) {
                xRight -= gap * 1.1f
                canvas.drawLine(xRight, centerY - dp(7.5f * scale), xRight, centerY + dp(7.5f * scale), dividerPaint)
                xRight -= gap * 1.1f

                val accent = if (ev.kind == "red") 0xFFFF6B5E.toInt() else 0xFFD6AF6B.toInt()
                dotPaint.color = accent
                alertTitlePaint.color = accent

                canvas.drawCircle(xRight - dotR, centerY, dotR, dotPaint)
                xRight -= dotR * 2f + gap * 0.7f

                val evBase = centerY - (alertTitlePaint.descent() + alertTitlePaint.ascent()) / 2f
                val tW = alertTitlePaint.measureText(ev.title)
                canvas.drawText(ev.title, xRight - tW, evBase, alertTitlePaint)
                xRight -= tW

                if (ev.detail.isNotEmpty()) {
                    val detStr = " · " + Look.digits(ev.detail, latinDigits)
                    val dW = alertDetailPaint.measureText(detStr)
                    val detBase = centerY - (alertDetailPaint.descent() + alertDetailPaint.ascent()) / 2f
                    canvas.drawText(detStr, xRight - dW, detBase, alertDetailPaint)
                }
            }
        }
    }
}
