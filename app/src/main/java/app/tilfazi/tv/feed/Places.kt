package app.tilfazi.tv.feed

/** A city the prayer times and the weather are worked out for. */
data class Place(val key: String, val name: String, val lat: Double, val lon: Double, val zone: String)

object Places {
    val all: List<Place> = listOf(
        Place("riyadh", "الرياض", 24.71360637, 46.67530644, "Asia/Riyadh"),
        Place("jeddah", "جدة", 21.4858, 39.1925, "Asia/Riyadh"),
        Place("makkah", "مكة المكرمة", 21.38910627, 39.85790644, "Asia/Riyadh"),
        Place("madinah", "المدينة المنورة", 24.5247, 39.5692, "Asia/Riyadh"),
        Place("taif", "الطائف", 21.2703, 40.4158, "Asia/Riyadh"),
        Place("dammam", "الدمام", 26.4207, 50.0888, "Asia/Riyadh"),
        Place("khobar", "الخبر", 26.2172, 50.1971, "Asia/Riyadh"),
        Place("hofuf", "الأحساء", 25.3833, 49.5864, "Asia/Riyadh"),
        Place("qassim", "بريدة", 26.3592, 43.9818, "Asia/Riyadh"),
        Place("unaizah", "عنيزة", 26.0843, 43.9935, "Asia/Riyadh"),
        Place("hail", "حائل", 27.5114, 41.7208, "Asia/Riyadh"),
        Place("tabuk", "تبوك", 28.3838, 36.5550, "Asia/Riyadh"),
        Place("abha", "أبها", 18.2164, 42.5053, "Asia/Riyadh"),
        Place("khamis", "خميس مشيط", 18.3000, 42.7333, "Asia/Riyadh"),
        Place("jazan", "جازان", 16.8892, 42.5511, "Asia/Riyadh"),
        Place("najran", "نجران", 17.4924, 44.1277, "Asia/Riyadh"),
        Place("baha", "الباحة", 20.0129, 41.4677, "Asia/Riyadh"),
        Place("jouf", "سكاكا", 29.9697, 40.2064, "Asia/Riyadh"),
        Place("arar", "عرعر", 30.9753, 41.0381, "Asia/Riyadh"),
        Place("yanbu", "ينبع", 24.0895, 38.0618, "Asia/Riyadh"),
        Place("kuwait", "الكويت", 29.3759, 47.9774, "Asia/Kuwait"),
        Place("doha", "الدوحة", 25.2854, 51.5310, "Asia/Qatar"),
        Place("manama", "المنامة", 26.2285, 50.5860, "Asia/Bahrain"),
        Place("dubai", "دبي", 25.2048, 55.2708, "Asia/Dubai"),
        Place("abudhabi", "أبوظبي", 24.4539, 54.3773, "Asia/Dubai"),
        Place("muscat", "مسقط", 23.5880, 58.3829, "Asia/Muscat"),
    )

    fun find(key: String?): Place = all.firstOrNull { it.key == key } ?: all.first()
}
