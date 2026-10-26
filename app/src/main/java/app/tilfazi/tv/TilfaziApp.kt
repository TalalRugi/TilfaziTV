package app.tilfazi.tv

import android.app.Application
import app.tilfazi.tv.feed.Choices

class TilfaziApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Choices.init(this)
    }
}
