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

/** Calculation parameters for a recognized Islamic prayer authority */
data class CalcMethod(
    val name: String,
    val fajrAngle: Double,
    val ishaAngle: Double? = null,
    val ishaIntervalMinutes: Double? = null,
    val ishaIntervalRamadanMinutes: Double? = null,
)

/**
 * The five prayer times worked out on the device with configurable calculation method, madhhab for Asr,
 * and manual minute offsets.
 */
object Salah {
    private const val HORIZON = 0.833   // refraction and the sun's radius

    val METHODS = listOf(
        CalcMethod("أم القرى (مكة المكرمة)", fajrAngle = 18.5, ishaIntervalMinutes = 90.0, ishaIntervalRamadanMinutes = 120.0),
        CalcMethod("رابطة العالم الإسلامي", fajrAngle = 18.0, ishaAngle = 17.0),
        CalcMethod("الهيئة المصرية العامة للمساحة", fajrAngle = 19.5, ishaAngle = 17.5),
        CalcMethod("جامعة العلوم الإسلامية بكراتشي", fajrAngle = 18.0, ishaAngle = 18.0),
        CalcMethod("الاتحاد الإسلامي لأمريكا الشمالية (ISNA)", fajrAngle = 15.0, ishaAngle = 15.0),
        CalcMethod("رئاسة الشؤون الدينية التركية (ديانت)", fajrAngle = 18.0, ishaAngle = 17.0),
        CalcMethod("وزارة الأوقاف الكويتية", fajrAngle = 18.0, ishaAngle = 17.5),
        CalcMethod("الهيئة العامة للشؤون الإسلامية (الإمارات)", fajrAngle = 18.2, ishaAngle = 18.2),
        CalcMethod("وزارة الأوقاف والشؤون الإسلامية (قطر)", fajrAngle = 18.0, ishaIntervalMinutes = 90.0),
        CalcMethod("اتحاد المنظمات الإسلامية بفرنسا (UOIF)", fajrAngle = 12.0, ishaAngle = 12.0),
        CalcMethod("مجلس الشؤون الإسلامية بسنغافورة (MUIS)", fajrAngle = 20.0, ishaAngle = 18.0),
        CalcMethod("معهد الجيوفيزياء بجامعة طهران", fajrAngle = 17.7, ishaAngle = 14.0),
    )

    val MADHHABS = listOf(
        "الجمهور (شافعي، حنبلي، مالكي)",
        "الحنفي",
    )

    /** the prayers of the local day that holds [millis] */
    fun day(
        millis: Long,
        place: Place,
        method: Int = 0,
        madhhab: Int = 0,
        adjust: List<Int> = emptyList(),
    ): List<Salat> {
        val zone = TimeZone.getTimeZone(place.zone)
        val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
        return dayOf(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            place,
            inRamadan(millis, zone),
            method,
            madhhab,
            adjust,
        )
    }

    fun day(millis: Long, picks: Picks): List<Salat> =
        day(millis, picks.place, picks.prayerMethod, picks.prayerMadhhab, picks.prayerAdjust)

    /** the sunrise and the sunset of the local day that holds [millis] */
    fun sun(millis: Long, place: Place, adjust: List<Int> = emptyList()): Pair<Long, Long> {
        val zone = TimeZone.getTimeZone(place.zone)
        val cal = Calendar.getInstance(zone).apply { timeInMillis = millis }
        val (rise, set) = horizon(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), place)
        val riseAdj = rise + adjust.getOrElse(1) { 0 } * 60_000L
        val setAdj = set + adjust.getOrElse(4) { 0 } * 60_000L
        return riseAdj to setAdj
    }

    fun sun(millis: Long, picks: Picks): Pair<Long, Long> =
        sun(millis, picks.place, picks.prayerAdjust)

    /** the first prayer still to come, looking into tomorrow after Isha */
    fun next(
        now: Long,
        place: Place,
        method: Int = 0,
        madhhab: Int = 0,
        adjust: List<Int> = emptyList(),
    ): Salat? =
        (day(now, place, method, madhhab, adjust) + day(now + 86_400_000L, place, method, madhhab, adjust)).firstOrNull { it.at > now }

    fun next(now: Long, picks: Picks): Salat? =
        next(now, picks.place, picks.prayerMethod, picks.prayerMadhhab, picks.prayerAdjust)

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

    fun dayOf(
        y: Int,
        m: Int,
        d: Int,
        place: Place,
        ramadan: Boolean,
        method: Int = 0,
        madhhab: Int = 0,
        adjust: List<Int> = emptyList(),
    ): List<Salat> {
        val day = Day(y, m, d, place)
        val noon = day.noon
        val lat = day.lat
        val decl = day.decl
        fun span(altitude: Double) = day.span(altitude)

        val calc = METHODS.getOrElse(method.coerceIn(0, METHODS.lastIndex)) { METHODS[0] }
        val shadowFactor = if (madhhab == 1) 2.0 else 1.0
        val asrAltitude = Math.toDegrees(atan(1.0 / (shadowFactor + tan(abs(lat - decl)))))

        val fajr = noon - span(-calc.fajrAngle)
        val dhuhr = noon + (1.0 / 60.0)
        val asr = noon + span(asrAltitude)
        val maghrib = noon + span(-HORIZON)
        val isha = if (calc.ishaIntervalMinutes != null) {
            val interval = if (ramadan && calc.ishaIntervalRamadanMinutes != null) calc.ishaIntervalRamadanMinutes else calc.ishaIntervalMinutes
            maghrib + interval / 60.0
        } else {
            noon + span(-(calc.ishaAngle ?: 17.0))
        }

        fun at(hours: Double) = day.at(hours)

        val fajrAdj = adjust.getOrElse(0) { 0 } * 60_000L
        val dhuhrAdj = adjust.getOrElse(2) { 0 } * 60_000L
        val asrAdj = adjust.getOrElse(3) { 0 } * 60_000L
        val maghribAdj = adjust.getOrElse(4) { 0 } * 60_000L
        val ishaAdj = adjust.getOrElse(5) { 0 } * 60_000L

        return listOf(
            Salat("الفجر", at(fajr) + fajrAdj),
            Salat("الظهر", at(dhuhr) + dhuhrAdj),
            Salat("العصر", at(asr) + asrAdj),
            Salat("المغرب", at(maghrib) + maghribAdj),
            Salat("العشاء", at(isha) + ishaAdj),
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
