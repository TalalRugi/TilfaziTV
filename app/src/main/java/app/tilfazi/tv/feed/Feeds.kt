package app.tilfazi.tv.feed

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** One of the coming hours. */
data class Hour(val at: Long, val temp: Double, val code: Int, val day: Boolean)

/** One of the coming days: its date (local midnight, epoch millis), its sky, and its range. */
data class Day(val at: Long, val code: Int, val high: Double, val low: Double)

/** Today's weather in the picked city. */
data class Sky(
    val at: Long, val temp: Double, val code: Int, val high: Double, val low: Double,
    val humidity: Int, val wind: Double, val day: Boolean, val hours: List<Hour> = emptyList(),
    val days: List<Day> = emptyList(),
)

/** The followed club's next game, or (with the scores) a game played. */
data class Fixture(
    val home: String, val away: String, val homeBadge: String?, val awayBadge: String?,
    val competition: String, val kickoff: Long,
    val homeScore: Int? = null, val awayScore: Int? = null,
)

/** One line of a league table. */
data class Standing(val rank: Int, val id: String, val name: String, val badge: String?, val played: Int, val points: Int)

/** The league a club plays in, and its table (the free key gives the top five). */
data class Table(val league: String, val rows: List<Standing>)

/** A background photo and who took it. */
data class Shot(val file: File, val by: String)

/**
 * The network feeds. Each answer is written to a small file next to its key, so the last good one is shown at once
 * after a restart and kept while the TV is offline.
 */
class Feeds(private val c: Context) {
    private val dir = File(c.filesDir, "feeds").apply { mkdirs() }

    // written whole to a side file and moved into place, so a TV switched off mid-write never leaves half a file
    private fun keep(name: String, body: String) {
        val part = File(dir, "$name.part")
        part.writeText(body)
        part.renameTo(File(dir, name))
    }
    private fun kept(name: String): String? = File(dir, name).takeIf { it.isFile }?.readText()

    // ---------------------------------------------------------------- weather (Open-Meteo)

    fun keptSky(place: Place): Sky? = kept("sky-${place.key}.json")?.let { runCatching { parseSky(JSONObject(it)) }.getOrNull() }

    suspend fun fetchSky(place: Place): Sky? = runCatching {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${place.lat}&longitude=${place.lon}" +
            "&timezone=${URLEncoder.encode(place.zone, "UTF-8")}" +
            "&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m,is_day" +
            "&daily=temperature_2m_max,temperature_2m_min,weather_code&forecast_days=4" +
            "&hourly=temperature_2m,weather_code,is_day&timeformat=unixtime&forecast_hours=8"
        val body = Net.json(url).put("fetchedAt", System.currentTimeMillis())
        keep("sky-${place.key}.json", body.toString())
        parseSky(body)
    }.onFailure { android.util.Log.w("Tilfazi", "weather: $it") }.getOrNull()

    private fun parseSky(o: JSONObject): Sky {
        val now = o.getJSONObject("current")
        val daily = o.getJSONObject("daily")
        return Sky(
            at = o.optLong("fetchedAt"),
            temp = now.getDouble("temperature_2m"),
            code = now.optInt("weather_code", -1),
            high = daily.getJSONArray("temperature_2m_max").getDouble(0),
            low = daily.getJSONArray("temperature_2m_min").getDouble(0),
            humidity = now.optInt("relative_humidity_2m"),
            wind = now.optDouble("wind_speed_10m"),
            day = now.optInt("is_day", 1) == 1,
            hours = o.optJSONObject("hourly")?.let { h ->
                val t = h.getJSONArray("time")
                (0 until t.length()).map { i ->
                    Hour(
                        at = t.getLong(i) * 1000,
                        temp = h.getJSONArray("temperature_2m").getDouble(i),
                        code = h.getJSONArray("weather_code").optInt(i, -1),
                        day = h.getJSONArray("is_day").optInt(i, 1) == 1,
                    )
                }
            } ?: emptyList(),
            days = run {
                val t = daily.getJSONArray("time")
                val codes = daily.optJSONArray("weather_code")
                (0 until t.length()).map { i ->
                    Day(
                        at = t.getLong(i) * 1000,
                        code = codes?.optInt(i, -1) ?: -1,
                        high = daily.getJSONArray("temperature_2m_max").getDouble(i),
                        low = daily.getJSONArray("temperature_2m_min").getDouble(i),
                    )
                }
            },
        )
    }

    // ---------------------------------------------------------------- games and table (ESPN's public data, no key)

    private val espn = "https://site.api.espn.com/apis/site/v2/sports/soccer"

    /** a club's crest on ESPN, by its id (the schedule leaves many crests out) */
    private fun crestOf(id: String, name: String = "") =
        if (id.isBlank()) null else "https://a.espncdn.com/i/teamlogos/soccer/500/$id.png#" + URLEncoder.encode(name, "UTF-8")

    fun keptFixtures(clubId: String): List<Fixture> =
        kept("games-$clubId.json")?.let { runCatching { parseGames(JSONArray(it)) }.getOrNull() } ?: emptyList()

    /** the club's coming games in every competition: the league, the cups, Asia */
    suspend fun fetchFixtures(clubId: String): List<Fixture>? = runCatching {
        val events = Net.json("$espn/all/teams/$clubId/schedule?fixture=true").optJSONArray("events") ?: JSONArray()
        keep("games-$clubId.json", events.toString())
        parseGames(events)
    }.onFailure { android.util.Log.w("Tilfazi", "games: $it") }.getOrNull()

    /** the club's games already played, newest first */
    fun keptResults(clubId: String): List<Fixture> =
        kept("played-$clubId.json")?.let { runCatching { parseGames(JSONArray(it)).filter { g -> g.homeScore != null }.reversed() }.getOrNull() } ?: emptyList()

    suspend fun fetchResults(clubId: String): List<Fixture>? = runCatching {
        val events = Net.json("$espn/all/teams/$clubId/schedule").optJSONArray("events") ?: JSONArray()
        keep("played-$clubId.json", events.toString())
        parseGames(events).filter { it.homeScore != null }.reversed()
    }.onFailure { android.util.Log.w("Tilfazi", "results: $it") }.getOrNull()

    private fun parseGames(a: JSONArray): List<Fixture> {
        val utc = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        return (0 until a.length()).mapNotNull { i ->
            val e = a.getJSONObject(i)
            val kick = runCatching { utc.parse(e.optString("date").take(16))!!.time }.getOrNull() ?: return@mapNotNull null
            val c = e.optJSONArray("competitions")?.optJSONObject(0) ?: return@mapNotNull null
            val sides = c.optJSONArray("competitors") ?: return@mapNotNull null
            fun side(where: String) = (0 until sides.length()).map { sides.getJSONObject(it) }.firstOrNull { it.optString("homeAway") == where }
            val home = side("home") ?: return@mapNotNull null
            val away = side("away") ?: return@mapNotNull null
            val done = c.optJSONObject("status")?.optJSONObject("type")?.optBoolean("completed") == true
            fun score(x: JSONObject): Int? = if (!done) null else
                (x.optJSONObject("score")?.optString("displayValue") ?: x.optString("score")).toIntOrNull()
            fun team(x: JSONObject) = x.getJSONObject("team")
            Fixture(
                home = Clubs.arabic(team(home).optString("id"), team(home).optString("displayName")),
                away = Clubs.arabic(team(away).optString("id"), team(away).optString("displayName")),
                homeBadge = crestOf(team(home).optString("id"), team(home).optString("displayName")),
                awayBadge = crestOf(team(away).optString("id"), team(away).optString("displayName")),
                competition = Clubs.competition(e.optJSONObject("league")?.optString("name") ?: ""),
                kickoff = kick,
                homeScore = score(home),
                awayScore = score(away),
            )
        }.sortedBy { it.kickoff }
    }

    fun keptTable(clubId: String): Table? = kept("table.json")?.let { runCatching { parseTable(JSONObject(it)) }.getOrNull() }

    /** the Saudi Pro League's whole table */
    suspend fun fetchTable(clubId: String): Table? = runCatching {
        val body = Net.json("https://site.api.espn.com/apis/v2/sports/soccer/ksa.1/standings")
        keep("table.json", body.toString())
        parseTable(body)
    }.onFailure { android.util.Log.w("Tilfazi", "table: $it") }.getOrNull()

    private fun parseTable(o: JSONObject): Table {
        val group = o.getJSONArray("children").getJSONObject(0)
        val entries = group.getJSONObject("standings").getJSONArray("entries")
        return Table(
            league = Clubs.competition(group.optString("name")),
            rows = (0 until entries.length()).map { i ->
                val e = entries.getJSONObject(i)
                val t = e.getJSONObject("team")
                val stats = e.getJSONArray("stats")
                fun stat(name: String) = (0 until stats.length()).map { stats.getJSONObject(it) }
                    .firstOrNull { it.optString("name") == name }?.optDouble("value")?.toInt() ?: 0
                Standing(
                    rank = stat("rank").takeIf { it > 0 } ?: (i + 1),
                    id = t.optString("id"),
                    name = Clubs.arabic(t.optString("id"), t.optString("displayName")),
                    badge = crestOf(t.optString("id")),
                    played = stat("gamesPlayed"),
                    points = stat("points"),
                )
            }.sortedBy { it.rank },
        )
    }

    /** a club's crest, downloaded once (TheSportsDB's small copy) */
    suspend fun badge(url: String): Bitmap? = runCatching {
        val f = File(dir, "badge-" + Integer.toHexString(url.hashCode()) + ".png")
        if (!f.isFile) {
            // ESPN's 500 px crest asked for at 128 px through its resizer; ESPN has none for some clubs (Al-Shamal):
            // then TheSportsDB's, found by the club's name carried after the «#»
            val bare = url.substringBefore('#')
            val name = java.net.URLDecoder.decode(url.substringAfter('#', ""), "UTF-8")
            val bytes = runCatching { Net.bytes("https://a.espncdn.com/combiner/i?img=" + bare.removePrefix("https://a.espncdn.com") + "&w=128&h=128") }
                .recoverCatching {
                    // its search finds «Al Shamal» but not «Al-Shamal»
                    val teams = Net.json("https://www.thesportsdb.com/api/v1/json/123/searchteams.php?t=" + URLEncoder.encode(name.replace('-', ' '), "UTF-8")).getJSONArray("teams")
                    Net.bytes(teams.getJSONObject(0).getString("strBadge") + "/small")
                }.getOrThrow()
            f.writeBytes(bytes)
        }
        BitmapFactory.decodeFile(f.path)
    }.getOrNull()

    // ---------------------------------------------------------------- photos (Unsplash, with the owner's key)

    fun keptShot(): Shot? {
        val f = File(dir, "shot.jpg")
        return if (f.isFile) Shot(f, kept("shot-by.txt") ?: "") else null
    }

    /** a new random photo for the topic; null without a key, or offline */
    suspend fun fetchShot(key: String, topic: String): Shot? {
        if (key.isBlank()) return null
        return runCatching {
            val auth = mapOf("Authorization" to "Client-ID $key", "Accept-Version" to "v1")
            val photo = Net.json(
                "https://api.unsplash.com/photos/random?orientation=landscape&content_filter=high&query=" +
                    URLEncoder.encode(topic, "UTF-8"), auth
            )
            val raw = photo.getJSONObject("urls").getString("raw")
            val bytes = Net.bytes("$raw&w=1920&h=1080&fit=crop&q=75&fm=jpg")
            // Unsplash asks for this call whenever a photo is shown
            runCatching { Net.bytes(photo.getJSONObject("links").getString("download_location"), auth) }
            val tmp = File(dir, "shot.tmp").apply { writeBytes(bytes) }
            val f = File(dir, "shot.jpg")
            tmp.renameTo(f)
            val by = photo.getJSONObject("user").optString("name")
            keep("shot-by.txt", by)
            Shot(f, by)
        }.getOrNull()
    }

    companion object {
    }
}
