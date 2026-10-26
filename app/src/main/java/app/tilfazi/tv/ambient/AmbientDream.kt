package app.tilfazi.tv.ambient

import android.service.dreams.DreamService
import app.tilfazi.tv.look.Board
import app.tilfazi.tv.feed.Picks
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.tilfazi.tv.feed.Choices
import app.tilfazi.tv.look.Look
import app.tilfazi.tv.look.rememberBoard

/**
 * What the screensaver shows, also drawn by [AmbientActivity] when it is started from the home screen: the scene
 * picked for it, drifting a few pixels every minute so nothing stays burnt into the panel, under the chosen dimming.
 * The gallery scene always has photos, from the photo source chosen for the home screen, even when that is on glass.
 */
@Composable
fun AmbientScreen() {
    app.tilfazi.tv.look.TvScreen {
        val chosen by Choices.picks.collectAsState()
        val picks = if (chosen.looks.scene == 2) chosen.copy(looks = chosen.looks.copy(backdrop = 1)) else chosen
        val board = rememberBoard(picks)
        AmbientFrame(chosen.looks.scene, chosen.looks.dim, board, picks)
    }
}

/** one scene with the screensaver's drift and dimming; the settings draw it small as a preview */
@Composable
fun AmbientFrame(scene: Int, dim: Int, board: Board, picks: Picks) {
    val step = (board.now / 60_000L % 8).toInt()   // a new place every minute, round a small square
    val dx by animateDpAsState(listOf(0, 6, 12, 6, 0, -6, -12, -6)[step].dp, tween(4000), label = "dx")
    val dy by animateDpAsState(listOf(0, -4, 0, 4, 8, 4, 0, -4)[step].dp, tween(4000), label = "dy")
    Box(Modifier.fillMaxSize().background(Look.NightLow)) {
        Box(Modifier.fillMaxSize().offset(dx, dy)) { Scene(scene, board, picks) }
        if (dim > 0) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (dim == 1) .25f else .5f)))
    }
}

/**
 * The screensaver: the home screen's two columns, larger, on the same backdrop, with no buttons and no apps.
 * The whole stage drifts a few pixels every minute so nothing stays burnt into the panel.
 */
class AmbientDream : DreamService(), LifecycleOwner, SavedStateRegistryOwner {
    private val life = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = life
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        saved.performRestore(null)
        life.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true   // the arrows move a gallery; any other key ends the screensaver (dispatchKeyEvent)
        isFullscreen = true
        Look.load(this)
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AmbientDream)
            setViewTreeSavedStateRegistryOwner(this@AmbientDream)
            setContent { AmbientScreen() }
        }
        setContentView(view)
        window?.decorView?.let {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
        }
        life.currentState = Lifecycle.State.STARTED
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (Slides.key(event, app.tilfazi.tv.feed.Choices.picks.value.looks.scene)) return true
        if (event.action == android.view.KeyEvent.ACTION_UP) wakeUp()
        return true
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        life.currentState = Lifecycle.State.RESUMED
    }

    override fun onDreamingStopped() {
        life.currentState = Lifecycle.State.STARTED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        life.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }
}
