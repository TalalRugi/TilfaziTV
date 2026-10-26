package app.tilfazi.tv.ambient

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import app.tilfazi.tv.look.Look

/** The screensaver on demand, from the home screen's button: any key goes back. */
class AmbientActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Look.load(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { AmbientScreen() }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (Slides.key(event, app.tilfazi.tv.feed.Choices.picks.value.looks.scene)) return true   // ◀ ▶ in a gallery
        if (event.action == KeyEvent.ACTION_UP) finish()
        return true
    }
}
