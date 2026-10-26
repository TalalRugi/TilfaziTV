package app.tilfazi.tv.feed

/** A club to follow, by its ESPN id. */
data class Club(val id: String, val name: String)

object Clubs {
    /** the Saudi Pro League clubs, by their ESPN ids */
    val followable: List<Club> = listOf(
        Club("929", "الهلال"),
        Club("817", "النصر"),
        Club("2276", "الاتحاد"),
        Club("8346", "الأهلي"),
        Club("793", "الشباب"),
        Club("8363", "الاتفاق"),
        Club("22022", "القادسية"),
        Club("18459", "التعاون"),
        Club("13033", "الفتح"),
        Club("21827", "الفيحاء"),
        Club("21829", "الخليج"),
        Club("21965", "الرياض"),
        Club("22028", "الخلود"),
        Club("21964", "الحزم"),
        Club("130899", "نيوم"),
        Club("131746", "الدرعية"),
        Club("21446", "الفيصلي"),
        Club("21833", "أبها"),
    )

    /** the ids an earlier version saved (TheSportsDB's), and the ESPN id of the same club */
    private val moved = mapOf(
        "136013" to "929", "136022" to "817", "136018" to "2276", "137721" to "8346", "136020" to "793",
        "136017" to "8363", "136015" to "22022", "136012" to "18459", "136011" to "13033", "136014" to "21827",
        "139080" to "21829", "147445" to "21965", "149112" to "22028", "136200" to "21964", "150637" to "130899",
        "152917" to "131746", "136021" to "21446", "137855" to "21833",
    )

    fun current(id: String?): String? = id?.let { moved[it] ?: it }

    /** Arabic names for clubs the followed ones meet, by ESPN's English name in lower case without «al» */
    private val byName: Map<String, String> = mapOf(
        "sadd" to "السد", "shamal" to "الشمال", "gharafa" to "الغرافة", "rayyan" to "الريان", "duhail" to "الدحيل",
        "wakrah" to "الوكرة", "ain" to "العين", "shabab al ahli" to "شباب الأهلي", "sharjah" to "الشارقة",
        "wasl" to "الوصل", "wahda" to "الوحدة", "wehda" to "الوحدة", "raed" to "الرائد", "anwar" to "الأنوار",
        "najma" to "النجمة", "damac" to "ضمك", "okhdood" to "الأخدود", "akhdoud" to "الأخدود", "taawoun" to "التعاون",
        "orobah" to "العروبة", "jubail" to "الجبيل", "jabalain" to "الجبلين", "batin" to "الباطن", "tai" to "الطائي",
        "esteghlal" to "استقلال طهران", "persepolis" to "برسبوليس", "tractor" to "تراكتور", "sepahan" to "سباهان",
        "pakhtakor" to "باختاكور", "nasaf" to "ناساف", "ulsan hd" to "أولسان", "ulsan" to "أولسان",
        "yokohama f. marinos" to "يوكوهاما", "kawasaki frontale" to "كاواساكي", "vissel kobe" to "فيسيل كوبي",
        "shanghai port" to "شنغهاي بورت", "buriram united" to "بوريرام", "johor darul ta'zim" to "جوهور",
        "saudi arabia" to "المنتخب السعودي",
    )

    fun arabic(id: String?, fallback: String): String {
        followable.firstOrNull { it.id == id }?.let { return it.name }
        val key = fallback.lowercase().replace("-", " ").removePrefix("al ").trim()
        return byName[key] ?: byName[fallback.lowercase()] ?: fallback
    }

    fun find(id: String?): Club? = followable.firstOrNull { it.id == current(id) }

    fun competition(name: String): String = when (name) {
        "Saudi-Arabian Pro League", "Saudi Pro League" -> "دوري روشن السعودي"
        "AFC Champions League Elite" -> "دوري أبطال آسيا للنخبة"
        "AFC Champions League" -> "دوري أبطال آسيا"
        "AFC Champions League Two" -> "دوري أبطال آسيا ٢"
        "Saudi King Cup", "King's Cup", "Saudi Kings Cup", "Saudi King's Cup" -> "كأس الملك"
        "Saudi Super Cup" -> "كأس السوبر السعودي"
        "FIFA Club World Cup" -> "كأس العالم للأندية"
        "FIFA World Cup" -> "كأس العالم"
        "AFC Asian Cup" -> "كأس آسيا"
        "FIFA Arab Cup" -> "كأس العرب"
        "Arabian Gulf Cup" -> "كأس الخليج"
        "International Friendlies", "Club Friendlies" -> "ودية"
        else -> name
    }
}
