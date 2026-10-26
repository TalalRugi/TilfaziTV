package app.tilfazi.tv.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.KeyEvent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.feed.Title
import app.tilfazi.tv.look.Look

/**
 * OK opens, a held OK asks for the menu: the remote has no other button for it. Only a press that began here counts:
 * when a held OK opens a menu, the rest of that press (its repeats and its release) reaches the menu's first row,
 * which must not take it as its own OK.
 */
fun Modifier.okKeys(onOk: () -> Unit, onHold: () -> Unit): Modifier = composed {
    val press = remember { booleanArrayOf(false, false) }   // [0]: the press began here; [1]: it already opened the menu
    onKeyEvent { e ->
        val k = e.nativeKeyEvent
        if (k.keyCode != KeyEvent.KEYCODE_DPAD_CENTER && k.keyCode != KeyEvent.KEYCODE_ENTER &&
            k.keyCode != KeyEvent.KEYCODE_NUMPAD_ENTER
        ) return@onKeyEvent false
        when (k.action) {
            KeyEvent.ACTION_DOWN ->
                if (k.repeatCount == 0) { press[0] = true; press[1] = false }
                else if (press[0] && !press[1]) { press[1] = true; onHold() }
            KeyEvent.ACTION_UP -> {
                if (press[0] && !press[1]) onOk()
                press[0] = false
            }
        }
        true
    }
}


/** A 16:9 card: the app's banner filling it, or its icon on a dark card. Grows a little with a white ring when focused. */
@Composable
fun AppTile(app: Title, width: Dp, onFocus: () -> Unit, onOk: () -> Unit, onHold: () -> Unit, showName: Boolean, modifier: Modifier = Modifier, lifted: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    val grow by animateFloatAsState(if (lifted) 1.16f else if (focused) 1.1f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "grow")
    val art = app.picture
    val shape = RoundedCornerShape(10.dp)
    Column(modifier.width(width), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .scale(grow)
                .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }
                .okKeys(onOk, onHold)
                .focusable()
                .clip(shape)
                .background(Color(0xFF1C1C1E))
                .then(if (lifted) Modifier.border(3.dp, Look.Gold, shape) else if (focused) Modifier.border(2.dp, Color(0xE6FFFFFF), shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            if (app.wide) {
                Image(art, app.label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
                    Image(art, null, Modifier.size(width / 4))
                    Spacer(Modifier.width(8.dp))
                    Text(app.label, style = Look.style(12.sp, Look.Ink, FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (showName) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (lifted) "◀ ▲ ▼ ▶  ثم OK" else app.label, style = Look.style(11.sp, if (lifted) Look.Gold else if (focused) Look.Ink else Look.Faint),
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            )
        }
    }
}

/** A round button at the top: its icon alone, and its name beside it while it has the focus. */
@Composable
fun Pill(icon: Painter, name: String, onOk: () -> Unit, onFocus: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Row(
        Modifier
            .height(34.dp)
            .onFocusChanged { focused = it.isFocused; if (it.isFocused) onFocus() }
            .okKeys(onOk, onOk)
            .focusable()
            .clip(CircleShape)
            .background(if (focused) Color(0xF2FFFFFF) else Color(0x26FFFFFF))
            .border(1.dp, Color(0x1FFFFFFF), CircleShape)
            .animateContentSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, name, tint = if (focused) Color(0xFF111111) else Color.White, modifier = Modifier.size(18.dp))
        if (focused) {
            Spacer(Modifier.width(6.dp))
            Text(name, style = Look.style(13.sp, Color(0xFF111111), FontWeight.Medium).copy(shadow = null))
        }
    }
}
