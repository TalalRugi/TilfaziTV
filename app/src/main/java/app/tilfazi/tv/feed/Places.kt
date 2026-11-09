package app.tilfazi.tv.feed

/** A city the prayer times and the weather are worked out for. */
data class Place(
    val key: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val zone: String,
    val country: String = "السعودية",
)

object Places {
    val all: List<Place> = listOf(
        // المملكة العربية السعودية
        Place("riyadh", "الرياض", 24.7136, 46.6753, "Asia/Riyadh", "السعودية"),
        Place("makkah", "مكة المكرمة", 21.3891, 39.8579, "Asia/Riyadh", "السعودية"),
        Place("madinah", "المدينة المنورة", 24.5247, 39.5692, "Asia/Riyadh", "السعودية"),
        Place("jeddah", "جدة", 21.4858, 39.1925, "Asia/Riyadh", "السعودية"),
        Place("taif", "الطائف", 21.2703, 40.4158, "Asia/Riyadh", "السعودية"),
        Place("dammam", "الدمام", 26.4207, 50.0888, "Asia/Riyadh", "السعودية"),
        Place("khobar", "الخبر", 26.2172, 50.1971, "Asia/Riyadh", "السعودية"),
        Place("jubail", "الجبيل", 27.0046, 49.6586, "Asia/Riyadh", "السعودية"),
        Place("hofuf", "الأحساء", 25.3833, 49.5864, "Asia/Riyadh", "السعودية"),
        Place("hafr", "حفر الباطن", 28.4328, 45.9708, "Asia/Riyadh", "السعودية"),
        Place("qassim", "بريدة", 26.3592, 43.9818, "Asia/Riyadh", "السعودية"),
        Place("unaizah", "عنيزة", 26.0843, 43.9935, "Asia/Riyadh", "السعودية"),
        Place("rass", "الرس", 25.8694, 43.4972, "Asia/Riyadh", "السعودية"),
        Place("hail", "حائل", 27.5114, 41.7208, "Asia/Riyadh", "السعودية"),
        Place("tabuk", "تبوك", 28.3838, 36.5550, "Asia/Riyadh", "السعودية"),
        Place("abha", "أبها", 18.2164, 42.5053, "Asia/Riyadh", "السعودية"),
        Place("khamis", "خميس مشيط", 18.3000, 42.7333, "Asia/Riyadh", "السعودية"),
        Place("jazan", "جازان", 16.8892, 42.5511, "Asia/Riyadh", "السعودية"),
        Place("najran", "نجران", 17.4924, 44.1277, "Asia/Riyadh", "السعودية"),
        Place("baha", "الباحة", 20.0129, 41.4677, "Asia/Riyadh", "السعودية"),
        Place("jouf", "سكاكا", 29.9697, 40.2064, "Asia/Riyadh", "السعودية"),
        Place("qurayyat", "القريات", 31.3318, 37.3428, "Asia/Riyadh", "السعودية"),
        Place("arar", "عرعر", 30.9753, 41.0381, "Asia/Riyadh", "السعودية"),
        Place("yanbu", "ينبع", 24.0895, 38.0618, "Asia/Riyadh", "السعودية"),
        Place("kharj", "الخرج", 24.1556, 47.3120, "Asia/Riyadh", "السعودية"),
        Place("dawasir", "وادي الدواسر", 20.4500, 44.8000, "Asia/Riyadh", "السعودية"),
        Place("bisha", "بيشة", 20.0000, 42.6000, "Asia/Riyadh", "السعودية"),
        Place("majmaah", "المجمعة", 25.9000, 45.3333, "Asia/Riyadh", "السعودية"),
        Place("zulfi", "الزلفي", 26.2972, 44.8028, "Asia/Riyadh", "السعودية"),

        // الكويت
        Place("kuwait", "مدينة الكويت", 29.3759, 47.9774, "Asia/Kuwait", "الكويت"),
        Place("hawally", "حولي", 29.3328, 48.0281, "Asia/Kuwait", "الكويت"),
        Place("salmiya", "السالمية", 29.3344, 48.0772, "Asia/Kuwait", "الكويت"),
        Place("ahmadi", "الأحمدي", 29.0769, 48.0839, "Asia/Kuwait", "الكويت"),
        Place("farwaniya", "الفروانية", 29.2778, 47.9589, "Asia/Kuwait", "الكويت"),
        Place("jahra", "الجهراء", 29.3375, 47.6581, "Asia/Kuwait", "الكويت"),

        // الإمارات العربية المتحدة
        Place("dubai", "دبي", 25.2048, 55.2708, "Asia/Dubai", "الإمارات"),
        Place("abudhabi", "أبوظبي", 24.4539, 54.3773, "Asia/Dubai", "الإمارات"),
        Place("sharjah", "الشارقة", 25.3463, 55.4209, "Asia/Dubai", "الإمارات"),
        Place("ajman", "عجمان", 25.4052, 55.5136, "Asia/Dubai", "الإمارات"),
        Place("alain", "العين", 24.2075, 55.7447, "Asia/Dubai", "الإمارات"),
        Place("rak", "رأس الخيمة", 25.6741, 55.9804, "Asia/Dubai", "الإمارات"),
        Place("fujairah", "الفجيرة", 25.1288, 56.3265, "Asia/Dubai", "الإمارات"),

        // قطر
        Place("doha", "الدوحة", 25.2854, 51.5310, "Asia/Qatar", "قطر"),
        Place("rayyan", "الريان", 25.2919, 51.4244, "Asia/Qatar", "قطر"),
        Place("wakrah", "الوكرة", 25.1768, 51.6048, "Asia/Qatar", "قطر"),
        Place("khor", "الخور", 25.6839, 51.5058, "Asia/Qatar", "قطر"),

        // البحرين
        Place("manama", "المنامة", 26.2285, 50.5860, "Asia/Bahrain", "البحرين"),
        Place("muharraq", "المحرق", 26.2572, 50.6119, "Asia/Bahrain", "البحرين"),
        Place("riffa", "الرفاع", 26.1300, 50.5550, "Asia/Bahrain", "البحرين"),

        // سلطنة عُمان
        Place("muscat", "مسقط", 23.5880, 58.3829, "Asia/Muscat", "عُمان"),
        Place("salalah", "صلالة", 17.0151, 54.0924, "Asia/Muscat", "عُمان"),
        Place("sohar", "صحار", 24.3644, 56.7468, "Asia/Muscat", "عُمان"),
        Place("nizwa", "نزوى", 22.9333, 57.5333, "Asia/Muscat", "عُمان"),

        // مصر
        Place("cairo", "القاهرة", 30.0444, 31.2357, "Africa/Cairo", "مصر"),
        Place("alexandria", "الإسكندرية", 31.2001, 29.9187, "Africa/Cairo", "مصر"),
        Place("giza", "الجيزة", 30.0131, 31.2089, "Africa/Cairo", "مصر"),
        Place("mansoura", "المنصورة", 31.0409, 31.3785, "Africa/Cairo", "مصر"),
        Place("tanta", "طنطا", 30.7865, 31.0004, "Africa/Cairo", "مصر"),
        Place("port_said", "بورسعيد", 31.2653, 32.3019, "Africa/Cairo", "مصر"),
        Place("suez", "السويس", 29.9668, 32.5498, "Africa/Cairo", "مصر"),
        Place("ismailia", "الإسماعيلية", 30.5965, 32.2715, "Africa/Cairo", "مصر"),
        Place("asyut", "أسيوط", 27.1783, 31.1859, "Africa/Cairo", "مصر"),
        Place("aswan", "أسوان", 24.0889, 32.8998, "Africa/Cairo", "مصر"),
        Place("luxor", "الأقصر", 25.6872, 32.6396, "Africa/Cairo", "مصر"),
        Place("hurghada", "الغردقة", 27.2579, 33.8116, "Africa/Cairo", "مصر"),
        Place("sharm", "شرم الشيخ", 27.9158, 34.3299, "Africa/Cairo", "مصر"),

        // الأردن
        Place("amman", "عمّان", 31.9454, 35.9284, "Asia/Amman", "الأردن"),
        Place("irbid", "إربد", 32.5568, 35.8469, "Asia/Amman", "الأردن"),
        Place("zarqa", "الزرقاء", 32.0728, 36.0880, "Asia/Amman", "الأردن"),
        Place("aqaba", "العقبة", 29.5321, 35.0063, "Asia/Amman", "الأردن"),
        Place("salt", "السلط", 32.0392, 35.7272, "Asia/Amman", "الأردن"),

        // العراق
        Place("baghdad", "بغداد", 33.3152, 44.3661, "Asia/Baghdad", "العراق"),
        Place("basra", "البصرة", 30.5085, 47.7804, "Asia/Baghdad", "العراق"),
        Place("mosul", "الموصل", 36.3400, 43.1300, "Asia/Baghdad", "العراق"),
        Place("erbil", "أربيل", 36.1911, 44.0092, "Asia/Baghdad", "العراق"),
        Place("najaf", "النجف الأشرف", 31.9959, 44.3148, "Asia/Baghdad", "العراق"),
        Place("karbala", "كربلاء", 32.6160, 44.0249, "Asia/Baghdad", "العراق"),
        Place("sulaymaniyah", "السليمانية", 35.5558, 45.4355, "Asia/Baghdad", "العراق"),
        Place("kirkuk", "كركوك", 35.4681, 44.3922, "Asia/Baghdad", "العراق"),

        // فلسطين
        Place("jerusalem", "القدس الشريف", 31.7683, 35.2137, "Asia/Hebron", "فلسطين"),
        Place("gaza", "غزة", 31.5017, 34.4668, "Asia/Gaza", "فلسطين"),
        Place("ramallah", "رام الله", 31.9038, 35.2034, "Asia/Hebron", "فلسطين"),
        Place("hebron", "الخليل", 31.5326, 35.0998, "Asia/Hebron", "فلسطين"),
        Place("nablus", "نابلس", 32.2211, 35.2544, "Asia/Hebron", "فلسطين"),
        Place("jenin", "جنين", 32.4646, 35.2957, "Asia/Hebron", "فلسطين"),

        // سوريا
        Place("damascus", "دمشق", 33.5138, 36.2765, "Asia/Damascus", "سوريا"),
        Place("aleppo", "حلب", 36.2021, 37.1343, "Asia/Damascus", "سوريا"),
        Place("homs", "حمص", 34.7324, 36.7137, "Asia/Damascus", "سوريا"),
        Place("latakia", "اللاذقية", 35.5317, 35.7901, "Asia/Damascus", "سوريا"),
        Place("hama", "حماة", 35.1318, 36.7578, "Asia/Damascus", "سوريا"),

        // لبنان
        Place("beirut", "بيروت", 33.8938, 35.5018, "Asia/Beirut", "لبنان"),
        Place("tripoli_lb", "طرابلس", 34.4367, 35.8497, "Asia/Beirut", "لبنان"),
        Place("sidon", "صيدا", 33.5631, 35.3689, "Asia/Beirut", "لبنان"),

        // اليمن
        Place("sanaa", "صنعاء", 15.3694, 44.1910, "Asia/Aden", "اليمن"),
        Place("aden", "عدن", 12.7855, 45.0187, "Asia/Aden", "اليمن"),
        Place("taiz", "تعز", 13.5795, 44.0209, "Asia/Aden", "اليمن"),
        Place("mukalla", "المكلا", 14.5425, 49.1242, "Asia/Aden", "اليمن"),
        Place("hodeidah", "الحديدة", 14.7978, 42.9545, "Asia/Aden", "اليمن"),

        // المغرب
        Place("rabat", "الرباط", 34.0209, -6.8416, "Africa/Casablanca", "المغرب"),
        Place("casablanca", "الدار البيضاء", 33.5731, -7.5898, "Africa/Casablanca", "المغرب"),
        Place("marrakech", "مراكش", 31.6295, -7.9811, "Africa/Casablanca", "المغرب"),
        Place("fez", "فاس", 34.0181, -5.0078, "Africa/Casablanca", "المغرب"),
        Place("tangier", "طنجة", 35.7595, -5.8340, "Africa/Casablanca", "المغرب"),
        Place("agadir", "أغادير", 30.4278, -9.5981, "Africa/Casablanca", "المغرب"),
        Place("oujda", "وجدة", 34.6867, -1.9114, "Africa/Casablanca", "المغرب"),

        // الجزائر
        Place("algiers", "الجزائر العاصمة", 36.7538, 3.0588, "Africa/Algiers", "الجزائر"),
        Place("oran", "وهران", 35.6987, -0.6349, "Africa/Algiers", "الجزائر"),
        Place("constantine", "قسنطينة", 36.3650, 6.6147, "Africa/Algiers", "الجزائر"),
        Place("annaba", "عنابة", 36.9000, 7.7667, "Africa/Algiers", "الجزائر"),
        Place("setif", "سطيف", 36.1911, 5.4137, "Africa/Algiers", "الجزائر"),

        // تونس
        Place("tunis", "تونس العاصمة", 36.8065, 10.1815, "Africa/Tunis", "تونس"),
        Place("sfax", "صفاقس", 34.7406, 10.7603, "Africa/Tunis", "تونس"),
        Place("sousse", "سوسة", 35.8256, 10.6369, "Africa/Tunis", "تونس"),
        Place("kairouan", "القيروان", 35.6781, 10.0963, "Africa/Tunis", "تونس"),
        Place("bizerte", "بنزرت", 37.2744, 9.8739, "Africa/Tunis", "تونس"),

        // ليبيا
        Place("tripoli_ly", "طرابلس الغرب", 32.8872, 13.1913, "Africa/Tripoli", "ليبيا"),
        Place("benghazi", "بنغازي", 32.1167, 20.0667, "Africa/Tripoli", "ليبيا"),
        Place("misrata", "مصراتة", 32.3754, 15.0925, "Africa/Tripoli", "ليبيا"),

        // السودان
        Place("khartoum", "الخرطوم", 15.5007, 32.5599, "Africa/Khartoum", "السودان"),
        Place("omdurman", "أم درمان", 15.6445, 32.4777, "Africa/Khartoum", "السودان"),
        Place("port_sudan", "بورتسودان", 19.6175, 37.2164, "Africa/Khartoum", "السودان"),

        // تركيا
        Place("istanbul", "إسطنبول", 41.0082, 28.9784, "Europe/Istanbul", "تركيا"),
        Place("ankara", "أنقرة", 39.9334, 32.8597, "Europe/Istanbul", "تركيا"),
        Place("izmir", "إزمير", 38.4237, 27.1428, "Europe/Istanbul", "تركيا"),
        Place("bursa", "بورصة", 40.1885, 29.0610, "Europe/Istanbul", "تركيا"),
        Place("antalya", "أنطاليا", 36.8969, 30.7133, "Europe/Istanbul", "تركيا"),
        Place("konya", "قونية", 37.8746, 32.4932, "Europe/Istanbul", "تركيا"),
        Place("gaziantep", "غازي عنتاب", 37.0662, 37.3833, "Europe/Istanbul", "تركيا"),
        Place("trabzon", "طرابزون", 41.0027, 39.7168, "Europe/Istanbul", "تركيا"),

        // المملكة المتحدة
        Place("london", "لندن", 51.5074, -0.1278, "Europe/London", "بريطانيا"),
        Place("manchester", "مانشستر", 53.4808, -2.2426, "Europe/London", "بريطانيا"),
        Place("birmingham", "برمنغهام", 52.4862, -1.8904, "Europe/London", "بريطانيا"),
        Place("glasgow", "غلاسكو", 55.8642, -4.2518, "Europe/London", "بريطانيا"),
        Place("liverpool", "ليفربول", 53.4084, -2.9916, "Europe/London", "بريطانيا"),
        Place("leeds", "ليدز", 53.8008, -1.5491, "Europe/London", "بريطانيا"),

        // فرنسا
        Place("paris", "باريس", 48.8566, 2.3522, "Europe/Paris", "فرنسا"),
        Place("marseille", "مارسيليا", 43.2965, 5.3698, "Europe/Paris", "فرنسا"),
        Place("lyon", "ليون", 45.7640, 4.8357, "Europe/Paris", "فرنسا"),
        Place("lille", "ليل", 50.6292, 3.0573, "Europe/Paris", "فرنسا"),
        Place("toulouse", "تولوز", 43.6047, 1.4442, "Europe/Paris", "فرنسا"),

        // ألمانيا
        Place("berlin", "برلين", 52.5200, 13.4050, "Europe/Berlin", "ألمانيا"),
        Place("munich", "ميونخ", 48.1351, 11.5820, "Europe/Berlin", "ألمانيا"),
        Place("frankfurt", "فرانكفورت", 50.1109, 8.6821, "Europe/Berlin", "ألمانيا"),
        Place("hamburg", "هامبورغ", 53.5511, 9.9937, "Europe/Berlin", "ألمانيا"),
        Place("cologne", "كولونيا", 50.9375, 6.9603, "Europe/Berlin", "ألمانيا"),
        Place("dusseldorf", "دوسلدورف", 51.2277, 6.7735, "Europe/Berlin", "ألمانيا"),

        // الولايات المتحدة
        Place("new_york", "نيويورك", 40.7128, -74.0060, "America/New_York", "أمريكا"),
        Place("chicago", "شيكاغو", 41.8781, -87.6298, "America/Chicago", "أمريكا"),
        Place("los_angeles", "لوس أنجلوس", 34.0522, -118.2437, "America/Los_Angeles", "أمريكا"),
        Place("houston", "هيوستن", 29.7604, -95.3698, "America/Chicago", "أمريكا"),
        Place("dallas", "دالاس", 32.7767, -96.7970, "America/Chicago", "أمريكا"),
        Place("washington", "واشنطن العاصمة", 38.9072, -77.0369, "America/New_York", "أمريكا"),
        Place("san_francisco", "سان فرانسيسكو", 37.7749, -122.4194, "America/Los_Angeles", "أمريكا"),
        Place("detroit", "ديترويت (ديربورن)", 42.3314, -83.0458, "America/Detroit", "أمريكا"),
        Place("philadelphia", "فيلادلفيا", 39.9526, -75.1652, "America/New_York", "أمريكا"),
        Place("boston", "بوسطن", 42.3601, -71.0589, "America/New_York", "أمريكا"),
        Place("atlanta", "أتلانتا", 33.7490, -84.3880, "America/New_York", "أمريكا"),
        Place("seattle", "سياتل", 47.6062, -122.3321, "America/Los_Angeles", "أمريكا"),

        // كندا
        Place("toronto", "تورونتو", 43.6532, -79.3832, "America/Toronto", "كندا"),
        Place("montreal", "مونتريال", 45.5017, -73.5673, "America/Toronto", "كندا"),
        Place("vancouver", "فانكوفر", 49.2827, -123.1207, "America/Vancouver", "كندا"),
        Place("ottawa", "أوتاوا", 45.4215, -75.6972, "America/Toronto", "كندا"),
        Place("calgary", "كالغاري", 51.0447, -114.0719, "America/Edmonton", "كندا"),

        // أستراليا
        Place("sydney", "سيدني", -33.8688, 151.2093, "Australia/Sydney", "أستراليا"),
        Place("melbourne", "ملبورن", -37.8136, 144.9631, "Australia/Melbourne", "أستراليا"),
        Place("brisbane", "بريزبان", -27.4698, 153.0251, "Australia/Brisbane", "أستراليا"),
        Place("perth", "بيرث", -31.9505, 115.8605, "Australia/Perth", "أستراليا"),

        // إسبانيا
        Place("madrid", "مدريد", 40.4168, -3.7038, "Europe/Madrid", "إسبانيا"),
        Place("barcelona", "برشلونة", 41.3851, 2.1734, "Europe/Madrid", "إسبانيا"),
        Place("valencia", "فالنسيا", 39.4699, -0.3763, "Europe/Madrid", "إسبانيا"),
        Place("granada", "غرناطة", 37.1773, -3.5986, "Europe/Madrid", "إسبانيا"),
        Place("cordoba", "قرطبة", 37.8882, -4.7794, "Europe/Madrid", "إسبانيا"),

        // إيطاليا
        Place("rome", "روما", 41.9028, 12.4964, "Europe/Rome", "إيطاليا"),
        Place("milan", "ميلانو", 45.4642, 9.1900, "Europe/Rome", "إيطاليا"),
        Place("turin", "تورينو", 45.0703, 7.6869, "Europe/Rome", "إيطاليا"),

        // هولندا وبلجيكا
        Place("amsterdam", "أمستردام", 52.3676, 4.9041, "Europe/Amsterdam", "هولندا"),
        Place("rotterdam", "روتردام", 51.9244, 4.4777, "Europe/Amsterdam", "هولندا"),
        Place("hague", "لاهاي", 52.0705, 4.3007, "Europe/Amsterdam", "هولندا"),
        Place("brussels", "بروكسل", 50.8503, 4.3517, "Europe/Brussels", "بلجيكا"),

        // إندونيسيا
        Place("jakarta", "جاكرتا", -6.2088, 106.8456, "Asia/Jakarta", "إندونيسيا"),
        Place("surabaya", "سورابايا", -7.2575, 112.7521, "Asia/Jakarta", "إندونيسيا"),
        Place("bandung", "باندونغ", -6.9175, 107.6191, "Asia/Jakarta", "إندونيسيا"),
        Place("medan", "ميدان", 3.5952, 98.6722, "Asia/Jakarta", "إندونيسيا"),

        // ماليزيا
        Place("kuala_lumpur", "كوالالمبور", 3.1390, 101.6869, "Asia/Kuala_Lumpur", "ماليزيا"),
        Place("penang", "بينانغ", 5.4141, 100.3288, "Asia/Kuala_Lumpur", "ماليزيا"),
        Place("johor", "جوهور بهرو", 1.4927, 103.7414, "Asia/Kuala_Lumpur", "ماليزيا"),

        // باكستان
        Place("karachi", "كراتشي", 24.8607, 67.0011, "Asia/Karachi", "باكستان"),
        Place("lahore", "لاهور", 31.5204, 74.3587, "Asia/Karachi", "باكستان"),
        Place("islamabad", "إسلام آباد", 33.6844, 73.0479, "Asia/Karachi", "باكستان"),
        Place("peshawar", "بيشاور", 34.0151, 71.5249, "Asia/Karachi", "باكستان"),

        // الهند
        Place("delhi", "نيودلهي", 28.6139, 77.2090, "Asia/Kolkata", "الهند"),
        Place("mumbai", "مومباي", 19.0760, 72.8777, "Asia/Kolkata", "الهند"),
        Place("hyderabad", "حيدر آباد", 17.3850, 78.4867, "Asia/Kolkata", "الهند"),
        Place("bangalore", "بنغالور", 12.9716, 77.5946, "Asia/Kolkata", "الهند"),
        Place("lucknow", "لكناو", 26.8467, 80.9462, "Asia/Kolkata", "الهند"),

        // بنغلاديش
        Place("dhaka", "دكا", 23.8103, 90.4125, "Asia/Dhaka", "بنغلاديش"),
        Place("chittagong", "تشيتاغونغ", 22.3569, 91.7832, "Asia/Dhaka", "بنغلاديش"),

        // روسيا والقوقاز
        Place("moscow", "موسكو", 55.7558, 37.6173, "Europe/Moscow", "روسيا"),
        Place("kazan", "قازان", 55.7961, 49.1064, "Europe/Moscow", "روسيا"),
        Place("makhachkala", "محج قلعة (داغستان)", 42.9831, 47.5046, "Europe/Moscow", "روسيا"),
        Place("grozny", "غروزني (الشيشان)", 43.3170, 45.6983, "Europe/Moscow", "روسيا"),
        Place("ufa", "أوفا", 54.7388, 55.9721, "Asia/Yekaterinburg", "روسيا"),

        // البوسنة ودول البلقان
        Place("sarajevo", "سراييفو", 43.8563, 18.4131, "Europe/Sarajevo", "البوسنة"),
        Place("mostar", "موستار", 43.3438, 17.8078, "Europe/Sarajevo", "البوسنة"),

        // دول إضافية
        Place("dakar", "داكار", 14.7167, -17.4677, "Africa/Dakar", "السنغال"),
        Place("lagos", "لاغوس", 6.5244, 3.3792, "Africa/Lagos", "نيجيريا"),
        Place("johannesburg", "جوهانسبرغ", -26.2041, 28.0473, "Africa/Johannesburg", "جنوب أفريقيا"),
        Place("stockholm", "ستوكهولم", 59.3293, 18.0686, "Europe/Stockholm", "السويد"),
        Place("oslo", "أوسلو", 59.9139, 10.7522, "Europe/Oslo", "النرويج"),
        Place("copenhagen", "كوبنهاغن", 55.6761, 12.5683, "Europe/Copenhagen", "الدانمارك"),
        Place("zurich", "زيورخ", 47.3769, 8.5417, "Europe/Zurich", "سويسرا"),
        Place("vienna", "فيينا", 48.2082, 16.3738, "Europe/Vienna", "النمسا"),
        Place("tokyo", "طوكيو", 35.6762, 139.6503, "Asia/Tokyo", "اليابان"),
        Place("seoul", "سيؤول", 37.5665, 126.9780, "Asia/Seoul", "كوريا الجنوبية"),
        Place("beijing", "بكين", 39.9042, 116.4074, "Asia/Shanghai", "الصين"),
        Place("urumqi", "أورومتشي", 43.8256, 87.6168, "Asia/Urumqi", "الصين"),
    )

    val countries: List<String> get() = all.map { it.country }.distinct()

    fun forCountry(country: String): List<Place> = all.filter { it.country == country }

    fun find(key: String?): Place = all.firstOrNull { it.key == key } ?: all.first()
}
