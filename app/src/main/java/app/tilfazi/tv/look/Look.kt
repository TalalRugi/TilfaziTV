package app.tilfazi.tv.look

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Colours, faces and number formatting shared by every screen. */
object Look {
    val Gold = Color(0xFFD6AF6B)
    val Ink = Color(0xFFFFFDF6)
    val Soft = Color(0xB3FFFFFF)
    val Faint = Color(0x66FFFFFF)
    val Live = Color(0xFFFF6B5E)
    val NightTop = Color(0xFF202124)
    val NightMid = Color(0xFF16171A)
    val NightLow = Color(0xFF0C0D0F)

    /** text over a photo: a soft dark halo keeps it readable */
    private val halo = Shadow(Color(0xB3000000), Offset(0f, 2f), 16f)

    // state, so every screen redraws in the new faces as soon as another set is picked
    var text by mutableStateOf<FontFamily>(FontFamily.SansSerif)
        private set
    var figures by mutableStateOf<FontFamily>(FontFamily.Serif)
        private set
    /** Amiri, a naskh made for the Quran's text: for the day's verse and hadith */
    var scripture by mutableStateOf<FontFamily>(FontFamily.Serif)
        private set

    /**
     * «إيقاف الحركة»: for slower TVs. Nothing loops on the home screen, the time moves once a minute, and the
     * countdowns drop their seconds.
     */
    var still by mutableStateOf(false)

    /**
     * The fonts: Alyamama, shipped with the app, and any the owner's agent added on the TV, in tilfazi's own folder
     * (Android/data/app.tilfazi.tv/files/fonts). A file named Name-Bold.ttf, Name-Regular.otf… is one weight of Name;
     * a single file is a variable font set to each weight.
     */
    var FONTS by mutableStateOf(listOf("اليمامة"))
        private set
    val available get() = FONTS.indices.toList()

    /** the font picked in the settings, loaded once per screen start */
    fun load(c: Context) {
        val set = c.getSharedPreferences("choices", Context.MODE_PRIVATE).getInt("look.font", 0)
        val weights = listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.Bold)
        fun variable(make: (FontWeight, FontVariation.Settings) -> Font) = weights.map { w -> make(w, FontVariation.Settings(FontVariation.weight(w.weight))) }
        val built = variable { w, v -> Font("fonts/Alyamama.ttf", c.assets, w, variationSettings = v) }

        val added = (c.getExternalFilesDir("fonts")?.listFiles().orEmpty())
            .filter { it.isFile && it.extension.lowercase() in setOf("ttf", "otf") }
            .groupBy { it.nameWithoutExtension.substringBefore('[').substringBefore('-').substringBefore('_') }
            .toSortedMap()
        FONTS = listOf("اليمامة") + added.keys
        val files = added.values.toList().getOrNull(set - 1)
        val faces = when {
            files == null -> built
            files.size == 1 -> variable { w, v -> Font(files[0], w, variationSettings = v) }
            else -> files.map { f ->
                val n = f.nameWithoutExtension.lowercase()
                Font(f, when {
                    "extrabold" in n || "black" in n || "heavy" in n -> FontWeight.ExtraBold
                    "semibold" in n -> FontWeight.SemiBold
                    "bold" in n -> FontWeight.Bold
                    "medium" in n -> FontWeight.Medium
                    "light" in n || "thin" in n -> FontWeight.Light
                    else -> FontWeight.Normal
                })
            }
        }
        val family = runCatching { FontFamily(faces) }.getOrElse { FontFamily(built) }
        text = family
        figures = family
        scripture = FontFamily(Font("fonts/Amiri-Regular.ttf", c.assets, FontWeight.Normal), Font("fonts/Amiri-Bold.ttf", c.assets, FontWeight.Bold))
    }

    fun style(size: TextUnit, color: Color = Ink, weight: FontWeight = FontWeight.Normal, big: Boolean = false) =
        TextStyle(
            fontFamily = if (big) figures else text,
            fontWeight = if (big) FontWeight.Bold else weight,
            fontSize = size,
            color = color,
            shadow = halo,
        )

    val label get() = style(13.sp, Gold, FontWeight.Medium)
    val note get() = style(12.sp, Soft)

    private const val HINDI = "٠١٢٣٤٥٦٧٨٩"

    /** digits as the owner wants them: ١٢٣ by default, 123 when picked */
    fun digits(s: String, latin: Boolean): String =
        if (latin) s else buildString { s.forEach { ch -> append(if (ch in '0'..'9') HINDI[ch - '0'] else ch) } }

    /** kept left to right inside Arabic text, so «٢٨°» or «١:٠٥:٠٩» never flip */
    fun ltr(s: String) = "⁦$s⁩"
}
