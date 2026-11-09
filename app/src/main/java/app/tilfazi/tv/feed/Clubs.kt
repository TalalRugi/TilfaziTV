package app.tilfazi.tv.feed

/** A club to follow, by its ESPN id and primary domestic league slug. */
data class Club(val id: String, val name: String, val league: String = "ksa.1", val local: Boolean = league == "ksa.1")

/** A football league available in the picker, with its ESPN standings slug and clubs. */
data class League(val slug: String, val name: String, val shortName: String, val local: Boolean, val clubs: List<Club>)

object Clubs {
    val leagues: List<League> = listOf(
        League(
            slug = "ksa.1",
            name = "دوري روشن السعودي",
            shortName = "السعودي",
            local = true,
            clubs = listOf(
                Club("929", "الهلال", "ksa.1", true),
                Club("817", "النصر", "ksa.1", true),
                Club("2276", "الاتحاد", "ksa.1", true),
                Club("8346", "الأهلي", "ksa.1", true),
                Club("793", "الشباب", "ksa.1", true),
                Club("8363", "الاتفاق", "ksa.1", true),
                Club("22022", "القادسية", "ksa.1", true),
                Club("18459", "التعاون", "ksa.1", true),
                Club("13033", "الفتح", "ksa.1", true),
                Club("21827", "الفيحاء", "ksa.1", true),
                Club("21829", "الخليج", "ksa.1", true),
                Club("21965", "الرياض", "ksa.1", true),
                Club("22028", "الخلود", "ksa.1", true),
                Club("21964", "الحزم", "ksa.1", true),
                Club("130899", "نيوم", "ksa.1", true),
                Club("131746", "الدرعية", "ksa.1", true),
                Club("21446", "الفيصلي", "ksa.1", true),
                Club("21833", "أبها", "ksa.1", true),
                Club("655", "المنتخب السعودي", "ksa.1", true),
            ),
        ),
        League(
            slug = "eng.1",
            name = "الدوري الإنجليزي الممتاز",
            shortName = "الإنجليزي",
            local = false,
            clubs = listOf(
                Club("364", "ليفربول", "eng.1", false),
                Club("382", "مانشستر سيتي", "eng.1", false),
                Club("359", "أرسنال", "eng.1", false),
                Club("363", "تشيلسي", "eng.1", false),
                Club("360", "مانشستر يونايتد", "eng.1", false),
                Club("361", "نيوكاسل يونايتد", "eng.1", false),
                Club("367", "توتنهام", "eng.1", false),
                Club("362", "أستون فيلا", "eng.1", false),
                Club("331", "برايتون", "eng.1", false),
                Club("393", "نوتنغهام فورست", "eng.1", false),
                Club("370", "فولهام", "eng.1", false),
                Club("337", "برينتفورد", "eng.1", false),
                Club("384", "كريستال بالاس", "eng.1", false),
                Club("349", "بورنموث", "eng.1", false),
                Club("368", "إيفرتون", "eng.1", false),
                Club("371", "وست هام يونايتد", "eng.1", false),
                Club("380", "وولفرهامبتون", "eng.1", false),
                Club("375", "ليستر سيتي", "eng.1", false),
                Club("357", "ليدز يونايتد", "eng.1", false),
                Club("366", "سندرلاند", "eng.1", false),
                Club("373", "إبسويتش تاون", "eng.1", false),
                Club("388", "كوفنتري سيتي", "eng.1", false),
                Club("306", "هال سيتي", "eng.1", false),
            ),
        ),
        League(
            slug = "esp.1",
            name = "الدوري الإسباني",
            shortName = "الإسباني",
            local = false,
            clubs = listOf(
                Club("86", "ريال مدريد", "esp.1", false),
                Club("83", "برشلونة", "esp.1", false),
                Club("1068", "أتلتيكو مدريد", "esp.1", false),
                Club("93", "أتلتيك بلباو", "esp.1", false),
                Club("89", "ريال سوسيداد", "esp.1", false),
                Club("244", "ريال بيتيس", "esp.1", false),
                Club("102", "فياريال", "esp.1", false),
                Club("243", "إشبيلية", "esp.1", false),
                Club("94", "فالنسيا", "esp.1", false),
                Club("9812", "جيرونا", "esp.1", false),
                Club("85", "سيلتا فيغو", "esp.1", false),
                Club("97", "أوساسونا", "esp.1", false),
                Club("101", "رايو فايكانو", "esp.1", false),
                Club("2922", "خيتافي", "esp.1", false),
                Club("84", "ريال مايوركا", "esp.1", false),
                Club("96", "ألافيس", "esp.1", false),
                Club("88", "إسبانيول", "esp.1", false),
                Club("3751", "إلتشي", "esp.1", false),
                Club("1538", "ليفانتي", "esp.1", false),
                Club("90", "ديبورتيفو لاكورونيا", "esp.1", false),
                Club("99", "ملقا", "esp.1", false),
                Club("87", "راسينغ سانتاندير", "esp.1", false),
            ),
        ),
        League(
            slug = "ita.1",
            name = "الدوري الإيطالي",
            shortName = "الإيطالي",
            local = false,
            clubs = listOf(
                Club("110", "إنتر ميلان", "ita.1", false),
                Club("103", "إي سي ميلان", "ita.1", false),
                Club("111", "يوفنتوس", "ita.1", false),
                Club("114", "نابولي", "ita.1", false),
                Club("104", "روما", "ita.1", false),
                Club("112", "لاتسيو", "ita.1", false),
                Club("105", "أتالانتا", "ita.1", false),
                Club("109", "فيورنتينا", "ita.1", false),
                Club("107", "بولونيا", "ita.1", false),
                Club("239", "تورينو", "ita.1", false),
                Club("2572", "كومو", "ita.1", false),
                Club("3263", "جنوى", "ita.1", false),
                Club("118", "أودينيزي", "ita.1", false),
                Club("115", "بارما", "ita.1", false),
                Club("2925", "كالياري", "ita.1", false),
                Club("113", "ليتشي", "ita.1", false),
                Club("4007", "مونزا", "ita.1", false),
                Club("3997", "ساسولو", "ita.1", false),
                Club("17530", "فينيتسيا", "ita.1", false),
                Club("4057", "فروزينوني", "ita.1", false),
            ),
        ),
        League(
            slug = "ger.1",
            name = "الدوري الألماني",
            shortName = "الألماني",
            local = false,
            clubs = listOf(
                Club("132", "بايرن ميونخ", "ger.1", false),
                Club("124", "بوروسيا دورتموند", "ger.1", false),
                Club("131", "باير ليفركوزن", "ger.1", false),
                Club("11420", "لايبزيغ", "ger.1", false),
                Club("134", "شتوتغارت", "ger.1", false),
                Club("125", "آينتراخت فرانكفورت", "ger.1", false),
                Club("126", "فرايبورغ", "ger.1", false),
                Club("137", "فيردر بريمن", "ger.1", false),
                Club("268", "بوروسيا مونشنغلادباخ", "ger.1", false),
                Club("138", "فولفسبورغ", "ger.1", false),
                Club("2950", "ماينز", "ger.1", false),
                Club("7911", "هوفنهايم", "ger.1", false),
                Club("3841", "أوغسبورغ", "ger.1", false),
                Club("598", "يونيون برلين", "ger.1", false),
                Club("122", "كولن", "ger.1", false),
                Club("127", "هامبورغ", "ger.1", false),
                Club("133", "شالكه", "ger.1", false),
                Club("3307", "بادربورن", "ger.1", false),
                Club("10388", "إلفيرسبيرغ", "ger.1", false),
            ),
        ),
        League(
            slug = "fra.1",
            name = "الدوري الفرنسي",
            shortName = "الفرنسي",
            local = false,
            clubs = listOf(
                Club("160", "باريس سان جيرمان", "fra.1", false),
                Club("176", "مارسيليا", "fra.1", false),
                Club("174", "موناكو", "fra.1", false),
                Club("167", "ليون", "fra.1", false),
                Club("166", "ليل", "fra.1", false),
                Club("2502", "نيس", "fra.1", false),
                Club("175", "لانس", "fra.1", false),
                Club("169", "رين", "fra.1", false),
                Club("6997", "بريست", "fra.1", false),
                Club("180", "ستراسبورغ", "fra.1", false),
                Club("179", "تولوز", "fra.1", false),
                Club("165", "نانت", "fra.1", false),
                Club("172", "أوكسير", "fra.1", false),
                Club("273", "لوريان", "fra.1", false),
                Club("7868", "أنجيه", "fra.1", false),
                Club("3236", "لوهافر", "fra.1", false),
                Club("6851", "باريس إف سي", "fra.1", false),
                Club("170", "تروا", "fra.1", false),
                Club("2697", "لومان", "fra.1", false),
            ),
        ),
        League(
            slug = "por.1",
            name = "الدوري البرتغالي",
            shortName = "البرتغالي",
            local = false,
            clubs = listOf(
                Club("1929", "بنفيكا", "por.1", false),
                Club("437", "بورتو", "por.1", false),
                Club("2250", "سبورتينغ لشبونة", "por.1", false),
                Club("2994", "سبورتينغ براغا", "por.1", false),
                Club("5309", "فيتوريا غيماريش", "por.1", false),
                Club("3822", "ريو آفي", "por.1", false),
                Club("12698", "فاماليكاو", "por.1", false),
                Club("3699", "جيل فيسنتي", "por.1", false),
                Club("3696", "موريرينسي", "por.1", false),
                Club("21581", "كاسا بيا", "por.1", false),
                Club("12216", "إشتوريل", "por.1", false),
                Club("12215", "سانتا كلارا", "por.1", false),
                Club("15784", "أروكا", "por.1", false),
                Club("3472", "ناسيونال", "por.1", false),
                Club("552", "ماريتيمو", "por.1", false),
                Club("21610", "إشتريلا", "por.1", false),
                Club("21613", "ألفيركا", "por.1", false),
                Club("21607", "أكاديميكو فيزيو", "por.1", false),
            ),
        ),
        League(
            slug = "ned.1",
            name = "الدوري الهولندي",
            shortName = "الهولندي",
            local = false,
            clubs = listOf(
                Club("139", "أياكس أمستردام", "ned.1", false),
                Club("148", "آيندهوفن", "ned.1", false),
                Club("142", "فينورد روتردام", "ned.1", false),
                Club("140", "ألكمار", "ned.1", false),
                Club("152", "تفينتي", "ned.1", false),
                Club("153", "أوترخت", "ned.1", false),
                Club("146", "هيرنفين", "ned.1", false),
                Club("147", "نيميخن", "ned.1", false),
                Club("145", "غرونينغن", "ned.1", false),
                Club("151", "سبارتا روتردام", "ned.1", false),
                Club("143", "فورتونا سيتارد", "ned.1", false),
                Club("2565", "زفوله", "ned.1", false),
                Club("3706", "غو أهيد إيغلز", "ned.1", false),
                Club("2566", "إكسيلسيور", "ned.1", false),
                Club("156", "فيليم ٢", "ned.1", false),
                Club("2726", "أدو دن هاغ", "ned.1", false),
                Club("3736", "كامبور", "ned.1", false),
                Club("3735", "تيلستار", "ned.1", false),
            ),
        ),
        League(
            slug = "tur.1",
            name = "الدوري التركي",
            shortName = "التركي",
            local = false,
            clubs = listOf(
                Club("432", "غلطة سراي", "tur.1", false),
                Club("436", "فنربخشة", "tur.1", false),
                Club("1895", "بشكتاش", "tur.1", false),
                Club("997", "طرابزون سبور", "tur.1", false),
                Club("7914", "باشاك شهير", "tur.1", false),
                Club("6870", "قاسم باشا", "tur.1", false),
                Club("9078", "ألانيا سبور", "tur.1", false),
                Club("7648", "قونيا سبور", "tur.1", false),
                Club("7656", "ريزه سبور", "tur.1", false),
                Club("20070", "غازي عنتاب", "tur.1", false),
                Club("11429", "سامسون سبور", "tur.1", false),
                Club("789", "جوزتيبي", "tur.1", false),
                Club("20729", "أيوب سبور", "tur.1", false),
                Club("995", "كوجالي سبور", "tur.1", false),
                Club("996", "غنتشلربيرليغي", "tur.1", false),
                Club("19267", "أرضروم", "tur.1", false),
                Club("132334", "تشوروم", "tur.1", false),
                Club("132335", "آمد سبور", "tur.1", false),
            ),
        ),
    )

    /** all followable clubs across every league */
    val followable: List<Club> = leagues.flatMap { it.clubs }

    /** local clubs (Saudi Pro League + Saudi National Team) */
    val localClubs: List<Club> = leagues.filter { it.local }.flatMap { it.clubs }

    /** international leagues */
    val worldLeagues: List<League> = leagues.filterNot { it.local }

    /** international clubs across all European leagues */
    val worldClubs: List<Club> = worldLeagues.flatMap { it.clubs }

    private val byId: Map<String, Club> = followable.associateBy { it.id }

    /** the ids an earlier version saved (TheSportsDB's), and the ESPN id of the same club */
    private val moved = mapOf(
        "136013" to "929", "136022" to "817", "136018" to "2276", "137721" to "8346", "136020" to "793",
        "136017" to "8363", "136015" to "22022", "136012" to "18459", "136011" to "13033", "136014" to "21827",
        "139080" to "21829", "147445" to "21965", "149112" to "22028", "136200" to "21964", "150637" to "130899",
        "152917" to "131746", "136021" to "21446", "137855" to "21833",
    )

    fun current(id: String?): String? = id?.let { moved[it] ?: it }

    /** Arabic names for clubs that followed ones meet in continental or cup competitions */
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
        "saudi arabia" to "المنتخب السعودي", "ahly" to "الأهلي المصري", "zamalek" to "الزمالك",
        "wydad" to "الوداد", "raja" to "الرجاء", "esperance" to "الترجي", "inter miami" to "إنتر ميامي",
        "inter miami cf" to "إنتر ميامي", "celtic" to "سيلتيك", "rangers" to "رينجرز",
        "shakhtar donetsk" to "شاختار دونيتسك", "fc salzburg" to "سالزبورغ", "red bull salzburg" to "سالزبورغ",
        "club brugge" to "كلوب بروج", "dinamo zagreb" to "دينامو زغرب", "young boys" to "يونغ بويز",
        "bsc young boys" to "يونغ بويز", "slavia prague" to "سلافيا براغ", "sparta prague" to "سبارتا براغ",
        "aek athens" to "أيك أثينا", "olympiacos" to "أولمبياكوس", "panathinaikos" to "باناثينايكوس",
        "paok" to "باوك", "crvena zvezda" to "النجم الأحمر", "red star belgrade" to "النجم الأحمر",
        "bodo/glimt" to "بودو غليمت", "fc copenhagen" to "كوبنهاغن", "copenhagen" to "كوبنهاغن",
        "malmo ff" to "مالمو", "anderlecht" to "أندرلخت", "genk" to "جينك", "antwerp" to "أنتويرب",
        "sturm graz" to "شتورم غراتس", "slovan bratislava" to "سلوفان براتيسلافا",
        "southampton" to "ساوثهامبتون", "burnley" to "بيرنلي", "sheffield united" to "شيفيلد يونايتد",
        "luton town" to "لوتون تاون", "middlesbrough" to "ميدلزبره", "west bromwich albion" to "وست بروميتش",
        "norwich city" to "نوريتش سيتي", "watford" to "واتفورد", "blackburn rovers" to "بلاكبيرن",
        "las palmas" to "لاس بالماس", "real valladolid" to "بلد الوليد", "leganes" to "ليغانيس",
        "cadiz" to "قادش", "almeria" to "ألميريا", "granada" to "غرناطة", "real zaragoza" to "سرقسطة",
        "empoli" to "إمبولي", "hellas verona" to "هيلاس فيرونا", "verona" to "هيلاس فيرونا",
        "salernitana" to "ساليرنيتانا", "sampdoria" to "سامبدوريا", "spezia" to "سبيتسيا",
        "bochum" to "بوخوم", "vfl bochum" to "بوخوم", "heidenheim" to "هايدنهايم", "1. fc heidenheim" to "هايدنهايم",
        "st. pauli" to "سانت باولي", "fc st. pauli" to "سانت باولي", "holstein kiel" to "هولشتاين كيل",
        "hertha berlin" to "هيرتا برلين", "fortuna dusseldorf" to "فورتونا دوسلدورف",
        "montpellier" to "مونبلييه", "reims" to "ريمس", "stade de reims" to "ريمس",
        "saint-etienne" to "سانت إتيان", "as saint-etienne" to "سانت إتيان", "metz" to "ميتز",
    )

    fun arabic(id: String?, fallback: String): String {
        byId[current(id)]?.let { return it.name }
        val key = fallback.lowercase().replace("-", " ").removePrefix("al ").trim()
        return byName[key] ?: byName[fallback.lowercase()] ?: fallback
    }

    fun find(id: String?): Club? = byId[current(id)]

    fun leagueName(slug: String): String = leagues.firstOrNull { it.slug == slug }?.name ?: competition(slug)

    fun competition(name: String): String = when {
        name in setOf("Saudi-Arabian Pro League", "Saudi Pro League", "ksa.1") -> "دوري روشن السعودي"
        name == "AFC Champions League Elite" -> "دوري أبطال آسيا للنخبة"
        name == "AFC Champions League" -> "دوري أبطال آسيا"
        name == "AFC Champions League Two" -> "دوري أبطال آسيا ٢"
        name in setOf("Saudi King Cup", "King's Cup", "Saudi Kings Cup", "Saudi King's Cup") -> "كأس الملك"
        name == "Saudi Super Cup" -> "كأس السوبر السعودي"
        name in setOf("UEFA Champions League", "uefa.champions") -> "دوري أبطال أوروبا"
        name in setOf("UEFA Europa League", "uefa.europa") -> "الدوري الأوروبي"
        name in setOf("UEFA Conference League", "UEFA Europa Conference League", "uefa.europa.conf") -> "دوري المؤتمر الأوروبي"
        name == "UEFA Super Cup" -> "كأس السوبر الأوروبي"
        name in setOf("English Premier League", "Premier League", "eng.1") || name.contains("English Premier League") -> "الدوري الإنجليزي الممتاز"
        name in setOf("English FA Cup", "FA Cup") -> "كأس الاتحاد الإنجليزي"
        name in setOf("English Carabao Cup", "EFL Cup", "Carabao Cup", "English League Cup") -> "كأس الرابطة الإنجليزية"
        name in setOf("English Community Shield", "FA Community Shield") -> "الدرع الخيرية"
        name in setOf("Spanish LALIGA", "Spanish LaLiga", "LaLiga", "Spanish Primera División", "esp.1") || name.contains("LALIGA", true) -> "الدوري الإسباني"
        name in setOf("Spanish Copa del Rey", "Copa del Rey") -> "كأس ملك إسبانيا"
        name in setOf("Spanish Supercopa", "Supercopa de España") -> "كأس السوبر الإسباني"
        name in setOf("Italian Serie A", "Serie A", "ita.1") || name.contains("Italian Serie A") -> "الدوري الإيطالي"
        name in setOf("Italian Coppa Italia", "Coppa Italia") -> "كأس إيطاليا"
        name in setOf("Italian Supercoppa", "Supercoppa Italiana") -> "كأس السوبر الإيطالي"
        name in setOf("German Bundesliga", "Bundesliga", "ger.1") || name.contains("German Bundesliga") -> "الدوري الألماني"
        name in setOf("German DFB Pokal", "DFB-Pokal") -> "كأس ألمانيا"
        name in setOf("German Supercup", "DFL-Supercup") -> "كأس السوبر الألماني"
        name in setOf("French Ligue 1", "Ligue 1", "fra.1") || name.contains("French Ligue 1") -> "الدوري الفرنسي"
        name in setOf("French Coupe de France", "Coupe de France") -> "كأس فرنسا"
        name in setOf("French Trophée des Champions", "Trophée des Champions") -> "كأس السوبر الفرنسي"
        name in setOf("Portuguese Primeira Liga", "Primeira Liga", "Liga Portugal", "por.1") || name.contains("Portuguese Liga") -> "الدوري البرتغالي"
        name in setOf("Dutch Eredivisie", "Eredivisie", "ned.1") || name.contains("Dutch Eredivisie") -> "الدوري الهولندي"
        name in setOf("Turkish Super Lig", "Süper Lig", "tur.1") || name.contains("Turkish Super Lig") -> "الدوري التركي"
        name == "FIFA Club World Cup" -> "كأس العالم للأندية"
        name == "FIFA World Cup" -> "كأس العالم"
        name.startsWith("FIFA World Cup Qualifying") -> "تصفيات كأس العالم"
        name == "AFC Asian Cup" -> "كأس آسيا"
        name == "UEFA European Championship" || name == "UEFA EURO" -> "كأس أمم أوروبا"
        name == "UEFA Nations League" -> "دوري الأمم الأوروبية"
        name == "FIFA Arab Cup" -> "كأس العرب"
        name == "Arabian Gulf Cup" -> "كأس الخليج"
        name in setOf("International Friendlies", "Club Friendlies") -> "ودية"
        else -> name
    }
}
