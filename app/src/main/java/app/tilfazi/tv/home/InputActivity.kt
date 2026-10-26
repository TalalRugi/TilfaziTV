package app.tilfazi.tv.home

import android.app.Activity
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvView
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast

/**
 * One of the TV's sources, played by tilfazi itself in a full-screen TvView: so HDMI works even on TVs whose own
 * viewer will not open from another app. Back returns to the home screen.
 */
class InputActivity : Activity() {
    private lateinit var view: TvView
    private var playing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view = TvView(this)
        setContentView(view)
        val id = intent.getStringExtra(EXTRA_ID) ?: return finish()
        val type = intent.getIntExtra(EXTRA_TYPE, TvInputInfo.TYPE_HDMI)
        // some makers keep HDMI's picture for their own viewer: when none comes within a few seconds, theirs is asked
        val source = app.tilfazi.tv.feed.Source(id, "", type)
        val fallback = Runnable { if (!playing) { runCatching { view.reset() }; app.tilfazi.tv.feed.Inputs.openElsewhere(this, source); finish() } }
        view.setCallback(object : TvView.TvInputCallback() {
            override fun onVideoAvailable(inputId: String) { playing = true }
            override fun onConnectionFailed(inputId: String) { fallback.run() }
            override fun onDisconnected(inputId: String) { fallback.run() }
        })
        // the tuner wants a channel of its own, which only the TV's own viewer knows: it goes there; tuning it to its list
        // of channels made the system throw inside the app, past any catch here
        if (app.tilfazi.tv.feed.Inputs.broadcast(source) || type == TvInputInfo.TYPE_TUNER) { app.tilfazi.tv.feed.Inputs.openElsewhere(this, source); finish(); return }
        view.postDelayed(fallback, 5000)
        runCatching { view.tune(id, TvContract.buildChannelUriForPassthroughInput(id)) }.onFailure { fail() }
    }

    private fun fail() {
        Toast.makeText(this, "تعذّر فتح هذا المصدر", Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) { finish(); return true }
        return view.dispatchKeyEvent(event) || super.onKeyDown(keyCode, event)
    }

    override fun onStop() {
        super.onStop()
        runCatching { view.reset() }
        finish()
    }

    companion object {
        const val EXTRA_ID = "input"
        const val EXTRA_TYPE = "type"
    }
}
