package app.tilfazi.tv.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import app.tilfazi.tv.look.Look

/** The home screen. Home pressed while it is already in front brings the focus back to the first favourite. */
class HomeActivity : ComponentActivity() {
    /** bumped on Home pressed again, and when apps are installed or removed */
    private val homeAgain = mutableIntStateOf(0)
    /** bumped by the remote's Source (Input) key: the home screen opens its sources list */
    private val sourceKey = mutableIntStateOf(0)

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        // the Source key used to reach the TV's own home; tilfazi is the home now, so it answers it
        if (keyCode == android.view.KeyEvent.KEYCODE_TV_INPUT) { sourceKey.intValue++; return true }
        return super.onKeyDown(keyCode, event)
    }
    private val appsChanged = mutableIntStateOf(0)

    private val packages = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) { appsChanged.intValue++ }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Look.load(this)
        registerReceiver(packages, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        })
        setContent {
            app.tilfazi.tv.look.TvScreen {
                HomeScreen(homeAgain = homeAgain.intValue, appsChanged = appsChanged.intValue, sourceKey = sourceKey.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        homeAgain.intValue++
    }

    override fun onResume() {
        super.onResume()
        appsChanged.intValue++   // an app may have changed while another was in front
    }

    override fun onDestroy() {
        unregisterReceiver(packages)
        super.onDestroy()
    }
}
