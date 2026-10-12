package com.techmare.paydebt

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Holds the saved data. Screens call [read]; after any edit call [changed] and they redraw.
 */
class AppState(private val context: Context) {
    var data: Data = Store.load(context)
        private set

    private var rev by mutableIntStateOf(0)

    /** Returns the data and makes the calling composable redraw whenever [changed] runs. */
    fun read(): Data = data.also { rev }

    /** Saves, resets the reminder and redraws everything that called [read]. */
    fun changed() {
        Store.save(context, data)
        Reminders.schedule(context, data.settings)
        rev++
    }

    /** Replaces everything (restore and erase). */
    fun replace(fresh: Data) {
        data = fresh
        changed()
    }
}
