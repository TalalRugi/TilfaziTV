package app.tilfazi.tv.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tilfazi.tv.look.Look

/** One line of a menu or a settings list: light when it has the focus, OK runs it. */
@Composable
fun MenuRow(name: String, onOk: () -> Unit, modifier: Modifier = Modifier, detail: String? = null) {
    var focused by remember { mutableStateOf(false) }
    Row(
        modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .okKeys(onOk, onOk)
            .focusable()
            .background(if (focused) Color(0xF2FFFFFF) else Color(0x0FFFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        val ink = if (focused) Color(0xFF111111) else Look.Ink
        Text(name, style = Look.style(16.sp, ink, FontWeight.Medium).copy(shadow = null), modifier = Modifier.weight(1f))
        if (detail != null) {
            Text(detail, style = Look.style(14.sp, if (focused) Color(0xFF444444) else Look.Gold).copy(shadow = null))
        }
    }
}
