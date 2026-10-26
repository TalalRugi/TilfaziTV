package app.tilfazi.tv.feed

import android.icu.util.IslamicCalendar
import android.icu.util.ULocale
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.tan

/** One of the five prayers on a given day. */
data class Salat(val name: String, val at: Long)

/**
 * The five prayer times worked out on the device, Umm al-Qura style: Fajr with the sun 18.5° below the horizon,
 * Asr when a shadow is its length plus the noon shadow, Maghrib at sunset, Isha 90 minutes after it (120 in Ramadan).
 * The sun's place comes from the low-precision formulas of the Astronomical Almanac: good to about a minute.
 */
object Salah {
    private const val FAJR_DEPTH = 18.5
    private const val HORIZON = 0.833   // refraction and the sun's radius

    /** the prayers of the local day that holds [millis] */
    fun day(millis: Long, place: Place): List<Salat> {
        val zone = TimeZone.getTimeZone(place.zone)
        val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return dayOf(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), place, inRamadan(millis, zone))
    }

    /** the sunrise and the sunset of the local day that holds [millis] */
    fun sun(millis: Long, place: Place): Pair<Long, Long> {
        val zone = TimeZone.getTimeZone(place.zone)
        val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
        val (rise, set) = horizon(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), place)
        return rise to set
    }

    /** the first prayer still to come, looking into tomorrow after Isha */
    fun next(now: Long, place: Place): Salat? =
        (day(now, place) + day(now + 86_400_000L, place)).firstOrNull { it.at > now }

    /** the day's noon (UTC hours), and the hours from noon until the sun stands at an altitude */
    private class Day(y: Int, m: Int, d: Int, place: Place) {
        private val sun = Sun(julianDay(y, m, d) + 0.5 - place.lon / 360.0)   // around local noon
        val noon = 12.0 - place.lon / 15.0 - sun.equationOfTime
        val lat: Double = Math.toRadians(place.lat)
        val decl = sun.declination
        val midnightUtc: Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(y, m - 1, d) }.timeInMillis

        fun span(altitude: Double): Double {
            val c = (sin(Math.toRadians(altitude)) - sin(lat) * sin(decl)) / (cos(lat) * cos(decl))
            return Math.toDegrees(acos(c.coerceIn(-1.0, 1.0))) / 15.0
        }

        fun at(hours: Double) = midnightUtc + ((hours * 60.0).roundToLong()) * 60_000L
    }

    private fun horizon(y: Int, m: Int, d: Int, place: Place): Pair<Long, Long> {
        val day = Day(y, m, d, place)
        return day.at(day.noon - day.span(-HORIZON)) to day.at(day.noon + day.span(-HORIZON))
    }

    fun dayOf(y: Int, m: Int, d: Int, place: Place, ramadan: Boolean): List<Salat> {
        val day = Day(y, m, d, place)
        val noon = day.noon
        val lat = day.lat
        val decl = day.decl
        fun span(altitude: Double) = day.span(altitude)

        val asrAltitude = Math.toDegrees(atan(1.0 / (1.0 + tan(abs(lat - decl)))))
        val fajr = noon - span(-FAJR_DEPTH)
        val dhuhr = noon
        val asr = noon + span(asrAltitude)
        val maghrib = noon + span(-HORIZON)
        val isha = maghrib + (if (ramadan) 120.0 else 90.0) / 60.0

        fun at(hours: Double) = day.at(hours)
        return listOf(
            Salat("الفجر", at(fajr)), Salat("الظهر", at(dhuhr)), Salat("العصر", at(asr)),
            Salat("المغرب", at(maghrib)), Salat("العشاء", at(isha)),
        )
    }

    private fun inRamadan(millis: Long, zone: TimeZone): Boolean = try {
        val h = IslamicCalendar(android.icu.util.TimeZone.getTimeZone(zone.id), ULocale("ar_SA@calendar=islamic-umalqura"))
        h.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        h.timeInMillis = millis
        h.get(Calendar.MONTH) == 8
    } catch (e: Throwable) {
        false
    }

    private fun julianDay(y: Int, m: Int, d: Int): Double {
        var yy = y
        var mm = m
        if (mm <= 2) { yy -= 1; mm += 12 }
        val a = floor(yy / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (yy + 4716)) + floor(30.6001 * (mm + 1)) + d + b - 1524.5
    }

    /** the sun's declination (radians) and the equation of time (hours) on a Julian day */
    private class Sun(jd: Double) {
        val declination: Double
        val equationOfTime: Double

        init {
            val n = jd - 2451545.0
            val g = Math.toRadians(wrap(357.529 + 0.98560028 * n))
            val q = wrap(280.459 + 0.98564736 * n)
            val lambda = Math.toRadians(wrap(q + 1.915 * sin(g) + 0.020 * sin(2 * g)))
            val tilt = Math.toRadians(23.439 - 0.00000036 * n)
            val ra = Math.toDegrees(atan2(cos(tilt) * sin(lambda), cos(lambda))) / 15.0
            declination = asin(sin(tilt) * sin(lambda))
            var eq = q / 15.0 - wrapHours(ra)
            if (eq > 12) eq -= 24
            if (eq < -12) eq += 24
            equationOfTime = eq
        }

        private fun wrap(deg: Double) = ((deg % 360.0) + 360.0) % 360.0
        private fun wrapHours(h: Double) = ((h % 24.0) + 24.0) % 24.0
    }
}
