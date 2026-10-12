package com.techmare.paydebt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val state = AppState(this)
        Reminders.createChannel(this)
        Reminders.schedule(this, state.data.settings)

        val prefs = getSharedPreferences("ui", MODE_PRIVATE)
        setContent {
            var wallpaperColours by remember { mutableStateOf(prefs.getBoolean("dynamic", true)) }
            PayDebtTheme(dynamic = wallpaperColours) {
                App(
                    state = state,
                    wallpaperColours = wallpaperColours,
                    onWallpaperColours = {
                        wallpaperColours = it
                        prefs.edit().putBoolean("dynamic", it).apply()
                    },
                )
            }
        }
    }
}
