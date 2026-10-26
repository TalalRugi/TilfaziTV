package app.tilfazi.tv.ambient

import android.graphics.Bitmap
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * ◀ ▶ on the remote while the screensaver shows a gallery: the screensaver (activity or dream) passes the arrows here
 * and every other key closes it. A gallery reads [step] and moves by it.
 */
object Slides {
    var step by mutableIntStateOf(0)

    /** true when the key was an arrow taken for the gallery */
    fun key(k: KeyEvent, scene: Int): Boolean {
        if (scene != SCENE_PHOTOS) return false
        if (k.keyCode != KeyEvent.KEYCODE_DPAD_LEFT && k.keyCode != KeyEvent.KEYCODE_DPAD_RIGHT) return false
        if (k.action == KeyEvent.ACTION_DOWN) step += if (k.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) 1 else -1   // Arabic: ◀ is next
        return true
    }
}

const val SCENE_PHOTOS = 2

/**
 * The index into a list of [count] slides: moved by the remote's arrows, and every ten minutes on its own unless
 * the owner turned that off.
 */
@Composable
internal fun rememberSlide(count: Int, auto: Boolean): Int {
    var index by remember { mutableIntStateOf(0) }
    var seen by remember { mutableIntStateOf(Slides.step) }
    LaunchedEffect(Slides.step) { index += Slides.step - seen; seen = Slides.step }
    LaunchedEffect(auto, index) { if (auto) { delay(10 * 60_000L); index++ } }
    return if (count == 0) 0 else index.mod(count)
}

