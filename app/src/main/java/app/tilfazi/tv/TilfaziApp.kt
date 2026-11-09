package app.tilfazi.tv

import android.app.Application
import app.tilfazi.tv.feed.Choices
import app.tilfazi.tv.overlay.MatchOverlay

class TilfaziApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Choices.init(this)
        MatchOverlay.init(this)
    }
}
